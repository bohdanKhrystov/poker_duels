package duels.poker.server.http

import duels.poker.server.auth.ClaimPendingResult
import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.Credentials
import duels.poker.server.auth.Identity
import duels.poker.server.auth.IdentityResolver
import duels.poker.server.auth.PasswordResets
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.RecoveryEmails
import duels.poker.server.auth.RecoveryMailer
import duels.poker.server.auth.RecoveryTokens
import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.VerificationToken
import duels.poker.server.auth.VerifyEmailResult
import duels.poker.server.auth.emailAddressOrNull
import duels.poker.server.mail.NoRecoveryMailer
import duels.poker.server.protocol.http.AttachRecoveryEmailRequest
import duels.poker.server.protocol.http.DetachRecoveryEmailRequest
import duels.poker.server.protocol.http.ForgotPasswordRequest
import duels.poker.server.protocol.http.ResetPasswordRequest
import duels.poker.server.protocol.http.VerifyEmailRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
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
 * Also installs `POST /api/auth/forgot-password`, which answers `202` in every case and mints a
 * reset token only for a verified address that has not been mailed in the last fifteen minutes, per
 * `ADR-0031` §4 and §5 and `ADR-0082` §4. This route is **unauthenticated**, exactly as
 * `verify-email` above: the caller cannot sign in, which is the whole point of the endpoint.
 *
 * The body decodes as [ForgotPasswordRequest] first, but unlike every other decode step in this
 * file, a decode failure answers `202`, not `400` — `ADR-0031` §5 answers `202` always and lists no
 * exception for a malformed body, and a `400` here would distinguish a well-formed unknown address
 * from a malformed one, which is a weaker version of the very oracle §5 refuses. [emailAddressOrNull]
 * then judges `request.address`; a syntactically invalid address answers the identical `202` and
 * reaches neither [recoveryEmails] nor [passwordResets].
 *
 * **The `202` is written next, before [RecoveryEmails.resetRecipientOf] is called, before
 * [PasswordResets.issue], and before any send** — the timing defence, not an optimisation: every
 * outcome from here on (an address nobody has mentioned, one that is pending but unverified, one
 * that is verified, and a build with no sender configured) is indistinguishable from the outside,
 * and the join `ADR-0082` added to [RecoveryEmails.resetRecipientOf] runs entirely after this
 * response, so it costs the caller nothing observable.
 *
 * `ADR-0082` §4's five lines follow, and no others: [RecoveryEmails.resetRecipientOf] answers a
 * [duels.poker.server.auth.ResetRecipient] or this handler returns; [RecoveryTokens.newResetToken]
 * mints a token; [PasswordResets.issue] stores it, superseding any token the player already held,
 * unless one was issued less than fifteen minutes ago, in which case it answers `false` and nothing
 * more happens; and only on `true` does [mailer].[sendPasswordReset][RecoveryMailer.sendPasswordReset]
 * send it, carrying the recipient's own handle. A `null` from [RecoveryEmails.resetRecipientOf] — an
 * address nobody has mentioned, one that is only pending, or a verified address whose owner holds no
 * `password` credential, the three indistinguishable per `ADR-0082` §1 — mints no token: a token
 * that could never be mailed would still spend the player's fifteen-minute window and supersede a
 * link they may be holding.
 *
 * **The address never appears in a response, a header or a log line from this handler**, exactly as
 * `recovery-email`'s own handler below — and now that `ADR-0082` gives this handler a login handle
 * too, neither does that: no `call.respond` here carries a body, and no string template in this
 * function interpolates [ForgotPasswordRequest.address], an
 * [EmailAddress][duels.poker.server.auth.EmailAddress] value, or
 * [duels.poker.server.auth.ResetRecipient.handle].
 *
 * One thing this route deliberately does not do, named because a future ticket does it: the budget
 * `ADR-0079` admits **after** the `202` this route writes — `TASK-041628`'s one line, not built here.
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
 * Also installs `POST /api/auth/recovery-email`, which records a pending claim on an address for
 * the caller, per `ADR-0031` §3 and §5. It follows the identical guard order the `DELETE` on this
 * same path below uses — identity, then the credential — with one gate between them that
 * `DELETE` has no need for, in this fixed order:
 *
 * 1. [identities] resolves the caller, **before the body is even read**, exactly as `DELETE
 *    /api/auth/recovery-email` below: only [Identity.Session] counts as resolved here, and
 *    [Identity.Device], [Identity.UnknownDevice], [Identity.Refused] and [Identity.Anonymous] all
 *    answer `401 Unauthorized` with an empty body and never reach [credentials] — a device
 *    identity, however well it resolves, is not a credential-bearing session.
 * 2. The body decodes as [AttachRecoveryEmailRequest]; every decode failure is `400`, the specific
 *    cause never changing the answer, exactly as this file's other decode steps.
 * 3. [emailAddressOrNull] judges `request.address`; `null` answers `400`. `ADR-0078` §1 is the
 *    whole rule, and §5 makes this `400` the endpoint's only feedback about the address — every
 *    other outcome below is a silent `202`.
 * 4. [Credentials.verifyCurrent] decides: a wrong current password answers `403 Forbidden`, and
 *    [recoveryEmails] is never touched. `ADR-0031` §3: attaching an address costs the current
 *    password even inside a valid session, because a session token is a bearer credential in web
 *    storage, and without this a minute at an unattended browser converts into permanent
 *    ownership of the account.
 * 5. [RecoveryEmails.claimPending] runs and its [ClaimPendingResult] decides whether step 6 may
 *    send anything at all: [ClaimPendingResult.Claimed] may reach the mailer,
 *    [ClaimPendingResult.Suppressed] never does — `ADR-0031` §5's fifteen-minute rule, applied
 *    here (`TASK-041637`). The `202` below is written before this is branched on and never varies
 *    with the outcome, so a caller can never tell [ClaimPendingResult.Claimed] from
 *    [ClaimPendingResult.Suppressed] by anything this route answers.
 * 6. On [ClaimPendingResult.Claimed], [mailer].[sendVerification][RecoveryMailer.sendVerification]
 *    sends the token just minted — **unless [RecoveryEmails.verifiedOwnerOf] already names another
 *    player for this address**, in which case nothing is sent either. `ADR-0031` §5: the route
 *    answers `202` even when the address already belongs to someone else, and sending mail in that
 *    case would be this server acting as a relay pointed at a stranger's inbox — the alternative,
 *    telling the caller the address is taken, is the oracle §5 forbids instead. On
 *    [ClaimPendingResult.Suppressed], nothing is sent: the token minted for this request was never
 *    stored, and the outstanding one from the earlier claim is untouched.
 *
 * The route answers `202 Accepted` with an empty body **immediately once step 5 returns, before
 * step 6 runs** — `ADR-0031` §5 makes every outcome but a malformed address indistinguishable from
 * the outside, including the two `ClaimPendingResult` values and the already-taken address in step
 * 6: a `202` answers nothing about what happened, on purpose, and it answers before the thing it
 * says nothing about has even been decided.
 *
 * **The address never appears in a response, a header or a log line from this handler.** No
 * `call.respond` here carries a body, and no string template in this function interpolates
 * [AttachRecoveryEmailRequest.address] or an [EmailAddress][duels.poker.server.auth.EmailAddress]
 * value — `ADR-0031` §6.3's "the address never leaves the database layer except into the mail
 * port" applies to this handler exactly as it applies to storage: the only place the address goes
 * is into [mailer], never into anything this route writes back to the caller.
 *
 * One thing this route deliberately does not do, named because a future ticket does it: the budget
 * `ADR-0079` §2 and §3 specify — five attaches per rolling sixty seconds, keyed by remote address,
 * admitted after step 3 and before step 4 — is `TASK-041628`'s one line, not built here. The
 * fifteen-minute resend suppression `ADR-0031` §5 describes is real at the storage layer
 * (`TASK-041636`) and now wired to skip the send in step 6 (`TASK-041637`, this ticket): the
 * per-account cap `ADR-0079` §Consequences named as the missing half of the endpoint's budget is
 * the one now in force, holding across every remote address at once, unlike the budget above.
 *
 * Also installs `DELETE /api/auth/recovery-email`, which erases the caller's proven recovery
 * address, per `ADR-0031` §5 and its closing `DEC-029`. It follows the identical guard order
 * `DELETE /api/me/device` uses (`ADR-0049` §5) — identity, then the credential, then the write —
 * in this fixed order:
 *
 * 1. [identities] resolves the caller, **before the body is even read** — the same order `POST
 *    /api/auth/sign-up` resolves its own caller (`ADR-0027` §4), so a stranger never reaches the
 *    `403`. But only [Identity.Session] counts as resolved here: the narrower guard `DELETE
 *    /api/me/device` uses (`ADR-0049` §5), not sign-up's more permissive one. A device identity,
 *    however well it resolves, is not a credential-bearing session, and this route is gated on a
 *    credential — [Identity.Device], [Identity.UnknownDevice], [Identity.Refused] and
 *    [Identity.Anonymous] all answer `401 Unauthorized` with an empty body and never reach
 *    [credentials].
 * 2. The body decodes as [DetachRecoveryEmailRequest]; every decode failure is `400`, the specific
 *    cause never changing the answer, exactly as `verify-email`'s and `reset-password`'s own
 *    decode steps above.
 * 3. [Credentials.verifyCurrent] decides: a wrong current password answers `403 Forbidden`, and
 *    [recoveryEmails] is never touched. The `403` is reachable only by somebody already holding a
 *    valid session, so it discloses nothing they could not already learn from `GET /api/me`.
 *
 * On a right password, [RecoveryEmails.detach] runs and the route answers `204 No Content`
 * **whether or not a row existed** — a `404` for "you had none" would tell a caller holding a
 * stolen session whether the account has recovery configured, one of the two facts
 * [RecoveryEmails.hasRecoveryEmail] deliberately gates behind the profile read.
 *
 * All four of `recoveryEmails`, `passwordResets`, `identities` and `credentials` have been
 * declared since `TASK-041618`; the `DELETE` handler below was the first to use [identities] and
 * [credentials], and neither route ticket in this chain has ever edited that part of this
 * signature or its single call site in `Application.kt` — five route tickets contending for one
 * line is how a sequential chain deadlocks. [mailer] and [tokens] were new when `recovery-email`'s
 * own ticket added them — the first handler in this file to need a sender — and remain trailing
 * parameters with defaults ([NoRecoveryMailer] and a fresh [RecoveryTokens]) that already match
 * `Application.kt`'s unconfigured build, so its call site stays unedited by `forgot-password`'s
 * ticket too: a defaulted trailing parameter is the same avoidance one step further, now serving a
 * second caller.
 *
 * @param recoveryEmails The port `verify-email`'s, `forgot-password`'s, `recovery-email`'s and
 *   `DELETE recovery-email`'s handlers call: [RecoveryEmails.verifyPending],
 *   [RecoveryEmails.resetRecipientOf], [RecoveryEmails.claimPending] plus
 *   [RecoveryEmails.verifiedOwnerOf], and [RecoveryEmails.detach] respectively.
 * @param passwordResets The port `forgot-password`'s and `reset-password`'s handlers call:
 *   [PasswordResets.issue] and [PasswordResets.consume] respectively.
 * @param identities The port `recovery-email`'s and `DELETE recovery-email`'s handlers call to
 *   resolve the caller's session before the body is read; a device identity does not count as
 *   resolved here.
 * @param credentials The port `recovery-email`'s and `DELETE recovery-email`'s handlers call, once
 *   identity is confirmed, to verify the presented current password: [Credentials.verifyCurrent]
 *   alone.
 * @param mailer The port `forgot-password`'s and `recovery-email`'s handlers call — the former to
 *   send the reset mail once [passwordResets] confirms a token was actually issued, the latter to
 *   send the verification mail a claim produces unless the address is already proven for someone
 *   else. Defaults to [NoRecoveryMailer], the implementation `ADR-0077` names for every developer
 *   machine and every CI run.
 * @param tokens Mints the verification token `recovery-email`'s handler hands to [recoveryEmails]
 *   and, on the same value, to [mailer], and the reset token `forgot-password`'s handler hands to
 *   [passwordResets] and, on the same value, to [mailer]. Defaults to a fresh [RecoveryTokens].
 */
public fun Application.recoveryRoutes(
    recoveryEmails: RecoveryEmails,
    passwordResets: PasswordResets,
    identities: IdentityResolver,
    credentials: Credentials,
    mailer: RecoveryMailer = NoRecoveryMailer,
    tokens: RecoveryTokens = RecoveryTokens(),
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
        post("/api/auth/forgot-password") {
            // Every way a body can fail to become a ForgotPasswordRequest — empty, the wrong
            // content type, malformed JSON, or a missing field — answers 202 here, unlike every
            // other decode step in this file: ADR-0031 §5 answers 202 always and lists no
            // exception for a malformed body, and a 400 would distinguish a well-formed unknown
            // address from a malformed one, a weaker version of the oracle §5 already refuses.
            val request = try {
                call.receive<ForgotPasswordRequest>()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (ignored: Exception) {
                call.respond(HttpStatusCode.Accepted)
                return@post
            }
            // ADR-0078 §1's rule, applied exactly as recovery-email's own decode step below uses
            // it — except a syntactically invalid address also answers 202, never 400, for the
            // same reason a decode failure does above.
            val address = emailAddressOrNull(request.address)
            if (address == null) {
                call.respond(HttpStatusCode.Accepted)
                return@post
            }
            // The 202 is written here, before resetRecipientOf, before issue and before any
            // send — the timing defence, not an optimisation (TASK-041626): every outcome from
            // here on is indistinguishable from the outside, and the join ADR-0082 added to
            // resetRecipientOf runs entirely after this response, so it costs the caller nothing
            // observable.
            call.respond(HttpStatusCode.Accepted)
            // ADR-0082 §4's five lines and no others. A null recipient — an address nobody has
            // mentioned, one that is only pending, or a verified address whose owner holds no
            // password credential, three states this route cannot and must not tell apart — mints
            // no token: a token that could never be mailed would still spend the player's
            // fifteen-minute window and supersede a link they may be holding.
            val recipient = recoveryEmails.resetRecipientOf(address) ?: return@post
            val token = tokens.newResetToken()
            if (passwordResets.issue(recipient.playerId, token)) {
                mailer.sendPasswordReset(address, token, recipient.handle)
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
        post("/api/auth/recovery-email") {
            // Identity first, before the body is read: the same guard the DELETE on this path
            // uses just below. Only a session counts as resolved here — a device identity,
            // however well it resolves, is not a credential-bearing session.
            val playerId = when (
                val identity = identities.resolve(call.sessionTokenOrNull(), call.deviceIdOrNull())
            ) {
                is Identity.Session -> identity.playerId
                is Identity.Device -> null
                is Identity.UnknownDevice -> null
                is Identity.Refused -> null
                is Identity.Anonymous -> null
            }
            if (playerId == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            // Every way a body can fail to become an AttachRecoveryEmailRequest — empty, the
            // wrong content type, malformed JSON, or a missing field — is a client error, 400,
            // not a server one; the specific cause never changes the answer, exactly as this
            // file's other decode steps.
            val request = try {
                call.receive<AttachRecoveryEmailRequest>()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (ignored: Exception) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            // ADR-0078 §1: an @ with something on either side, no control character, at most 254
            // code points, returned unchanged. This 400 is the endpoint's only feedback about the
            // address — ADR-0078 §5 — and it fires almost never by design.
            val address = emailAddressOrNull(request.address)
            if (address == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val presented = PresentedSecret(request.currentPassword)
            if (!credentials.verifyCurrent(playerId, CredentialKind.PASSWORD, presented)) {
                call.respond(HttpStatusCode.Forbidden)
                return@post
            }
            val token = tokens.newVerificationToken()
            val claim = recoveryEmails.claimPending(playerId, address, token)
            // The 202 is written before the when and never varies with the outcome: ADR-0031 §5
            // requires the answer before any mail work, and TASK-041626 calls this ordering the
            // timing defence rather than an optimisation — the same reading applies here.
            call.respond(HttpStatusCode.Accepted)
            // Exhaustive over ClaimPendingResult, no else: this is the one branch in the codebase
            // that decides whether outbound mail leaves the building, and an else would silently
            // absorb a third outcome the day ClaimPendingResult grows one.
            when (claim) {
                ClaimPendingResult.Claimed -> if (recoveryEmails.verifiedOwnerOf(address) == null) {
                    // ADR-0031 §5: nothing is sent when the address already belongs to another
                    // player — the alternative either tells a stranger an address is registered,
                    // or mails a stranger's mailbox that did nothing to deserve it.
                    mailer.sendVerification(address, token)
                }
                ClaimPendingResult.Suppressed -> Unit
            }
        }
        delete("/api/auth/recovery-email") {
            // Identity first, before the body is read: the same order sign-up uses (ADR-0027
            // §4), so a stranger never reaches the 403 that would tell them a password they do
            // not hold was right or wrong. Only a session counts as resolved here, though —
            // the narrower guard DELETE /api/me/device uses (ADR-0049 §5), not sign-up's own
            // resolvedPlayerOrNull: a device identity is not a credential-bearing session, and
            // this route is gated on one.
            val playerId = when (
                val identity = identities.resolve(call.sessionTokenOrNull(), call.deviceIdOrNull())
            ) {
                is Identity.Session -> identity.playerId
                is Identity.Device -> null
                is Identity.UnknownDevice -> null
                is Identity.Refused -> null
                is Identity.Anonymous -> null
            }
            if (playerId == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@delete
            }
            // Every way a body can fail to become a DetachRecoveryEmailRequest — empty, the
            // wrong content type, malformed JSON, or a missing field — is a client error, 400,
            // not a server one; the specific cause never changes the answer, exactly as
            // verify-email's and reset-password's own decode steps above.
            val request = try {
                call.receive<DetachRecoveryEmailRequest>()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (ignored: Exception) {
                call.respond(HttpStatusCode.BadRequest)
                return@delete
            }
            val presented = PresentedSecret(request.currentPassword)
            if (!credentials.verifyCurrent(playerId, CredentialKind.PASSWORD, presented)) {
                call.respond(HttpStatusCode.Forbidden)
                return@delete
            }
            // 204 whether or not a row existed: a distinct answer for "you had none" would tell
            // a caller holding a stolen session whether the account has recovery configured
            // (ADR-0031 §5), one of the two facts hasRecoveryEmail deliberately gates behind the
            // profile read.
            recoveryEmails.detach(playerId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
