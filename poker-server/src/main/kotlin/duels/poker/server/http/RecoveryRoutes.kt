package duels.poker.server.http

import duels.poker.server.auth.Credentials
import duels.poker.server.auth.IdentityResolver
import duels.poker.server.auth.PasswordResets
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.RecoveryEmails
import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.VerificationToken
import duels.poker.server.auth.VerifyEmailResult
import duels.poker.server.protocol.http.ResetPasswordRequest
import duels.poker.server.protocol.http.VerifyEmailRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException

/**
 * Installs `POST /api/auth/verify-email`, which consumes a mailed token and proves the address
 * it names, per `ADR-0031` §5.
 *
 * The route is **unauthenticated**: it reads no `X-Device-Id` header, no `Authorization` header,
 * and calls nothing on [identities]. The token itself is the proof of ownership, so a player who
 * attached an address on a phone must be able to click the link on a laptop that has never seen
 * this site.
 *
 * The body decodes as [VerifyEmailRequest] first — every decode failure is `400`, the cause
 * never changing the answer, exactly the rule `POST /api/auth/sign-up`'s own decode step
 * follows. Only then is [RecoveryEmails.verifyPending] called, and its [VerifyEmailResult]
 * mapped with **no `else` branch**, so a fourth result value fails the build rather than being
 * silently absorbed by one of the three existing answers:
 * [VerifyEmailResult.Verified] answers `204 No Content`, [VerifyEmailResult.Refused] answers
 * `400`, and [VerifyEmailResult.AddressTaken] answers `409`.
 *
 * [VerifyEmailResult.Refused] alone covers a token that is unknown, expired, or already used —
 * `ADR-0031` §5 makes the three indistinguishable on purpose, and the port already collapses them
 * into one value before this route ever sees the result. This route must not undo that: it never
 * asks which of the three happened, and answers the identical `400` for all three.
 *
 * Also installs `POST /api/auth/reset-password`, which spends a mailed reset token and rewrites
 * the credential's password, per `ADR-0031` §4. It follows the identical decode-then-refuse shape:
 * decode as [ResetPasswordRequest] first — every decode failure is `400`, the cause never changing
 * the answer — then [PasswordResets.consume], then `204 No Content` on `true` and `400 Bad Request`
 * on `false`.
 *
 * This route is **unauthenticated**, exactly as `verify-email` above: it reads no `X-Device-Id`
 * header, no `Authorization` header, and calls nothing on [identities] or [credentials]. The token
 * itself is the proof of ownership, and the player is resetting precisely because they cannot sign
 * in.
 *
 * **The token is read from the decoded body only.** This handler contains no
 * `call.request.queryParameters` and no `call.parameters` anywhere — `ADR-0031` §4 makes this the
 * property that keeps the safe path the only path: a fragment never reaches a server, but a query
 * parameter reaches every access log, proxy log and `Referer` header between here and the browser.
 *
 * The response **issues no session and returns no token** on either branch: the body is always
 * empty, and no `Set-Cookie` header is ever written. `ADR-0031` §4 keeps this endpoint incapable of
 * handing out a credential, so a leaked reset link cannot be exchanged for a live session by
 * anything but a full sign-in with the new password.
 *
 * This handler runs no password policy and answers no `422`: `ADR-0080` §7 puts that check in
 * **front** of [PasswordResets.consume], and it arrives with `TASK-041629`. Until then, every
 * refused token answers `400` regardless of the new password's own shape.
 *
 * All four parameters are declared now, even though this ticket's handlers use only
 * [recoveryEmails] and [passwordResets], so that the remaining route tickets in this chain fill in
 * their own handlers without ever editing this signature or its single call site in
 * `Application.kt` — five route tickets contending for one line is how a sequential chain
 * deadlocks.
 *
 * @param recoveryEmails The port `verify-email`'s handler calls: [RecoveryEmails.verifyPending]
 *   alone.
 * @param passwordResets The port `reset-password`'s handler calls: [PasswordResets.consume] alone.
 * @param identities Declared now though unused here: filled in by a later route ticket in this
 *   chain (`TASK-041623`, `TASK-041625` or `TASK-041626`) that needs to resolve a caller.
 * @param credentials Declared now though unused here: filled in by a later route ticket in this
 *   chain (`TASK-041623`, `TASK-041625` or `TASK-041626`) that needs to check a credential.
 */
public fun Application.recoveryRoutes(
    recoveryEmails: RecoveryEmails,
    passwordResets: PasswordResets,
    @Suppress("UNUSED_PARAMETER") identities: IdentityResolver,
    @Suppress("UNUSED_PARAMETER") credentials: Credentials,
) {
    routing {
        post("/api/auth/verify-email") {
            // Every way a body can fail to become a VerifyEmailRequest — empty, the wrong
            // content type, malformed JSON, or a missing/mistyped field — is a client error,
            // 400, not a server one; the specific cause never changes the answer.
            val request = try {
                call.receive<VerifyEmailRequest>()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (ignored: Exception) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            when (recoveryEmails.verifyPending(VerificationToken(request.token))) {
                VerifyEmailResult.Verified -> call.respond(HttpStatusCode.NoContent)
                VerifyEmailResult.Refused -> call.respond(HttpStatusCode.BadRequest)
                VerifyEmailResult.AddressTaken -> call.respond(HttpStatusCode.Conflict)
            }
        }
        post("/api/auth/reset-password") {
            // Every way a body can fail to become a ResetPasswordRequest — empty, the wrong
            // content type, malformed JSON, or a missing/mistyped field — is a client error,
            // 400, not a server one; the specific cause never changes the answer, exactly as
            // verify-email's own decode step above.
            val request = try {
                call.receive<ResetPasswordRequest>()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (ignored: Exception) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            // The token travels in the body alone: no call.request.queryParameters and no
            // call.parameters anywhere in this handler (ADR-0031 §4). A query parameter reaches
            // every access log, proxy log and Referer header between here and the browser; a
            // fragment never reaches a server at all, which is the whole reason the client mails
            // the token in one and posts it from the other.
            val token = ResetToken(request.token)
            val secret = PresentedSecret(request.newPassword)
            if (passwordResets.consume(token, secret)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}
