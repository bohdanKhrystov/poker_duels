package duels.poker.server.http

import duels.poker.server.auth.CreateCredentialResult
import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.Credentials
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.protocol.http.SignUpRequest
import duels.poker.server.session.PlayerId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException

/**
 * Installs `POST /api/auth/sign-up`, which attaches a password credential to the calling
 * device's profile: identity, then decode, then field rules, then the guard, then the write.
 *
 * Identity is resolved first, **before the body is even read**, exactly as `PUT /api/me/name`
 * resolves it: an absent, blank or unknown device id answers `401` with an empty body, and
 * neither [Credentials] method is called. Answering `409` or `422` before identity is confirmed
 * would let an anonymous caller learn whether a handle is taken. Only then is the body decoded as
 * [SignUpRequest] — every decode failure is `400`, the cause never changing the answer — and only
 * then does [signUpFieldsOf] judge the handle and password; a `Refused` answers its status (`400`
 * or `422`) and stops, still before [Credentials] is touched.
 *
 * [Credentials.holdsCredential] then answers `409` without writing (`ADR-0030` §1), and
 * [Credentials.create] answers `201 Created` — not `204`, because exactly one row is created and
 * that is this endpoint's entire purpose — or `409` if the handle was taken concurrently. There
 * is no `Location` header: no credential is ever read back (`ADR-0027` §1). The player id written
 * is always `profile.playerId`, the identity the server resolved; [SignUpRequest] has no field to
 * carry one, so a client can never assert who it is (`ADR-0002`).
 *
 * @param reads The port for resolving the calling device's profile.
 * @param credentials The port for checking and creating password credentials.
 */
public fun Application.authRoutes(reads: ProfileReads, credentials: Credentials) {
    routing {
        post("/api/auth/sign-up") {
            // Identity first: an unauthenticated caller is refused before the body is read, so a
            // stranger never reaches the 409/422 that would tell them whether a handle is taken.
            val profile = call.deviceIdOrNull()?.let { reads.profileOf(it) }
            if (profile == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            // Every way a body can fail to become a SignUpRequest — empty, the wrong content
            // type, malformed JSON, or a missing/unrecognised field — is a client error, `400`,
            // not a server one; the specific cause does not change the answer.
            val request = try {
                call.receive<SignUpRequest>()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (ignored: Exception) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val handle = when (val fields = signUpFieldsOf(request)) {
                is SignUpFields.Refused -> {
                    call.respond(fields.status)
                    return@post
                }
                is SignUpFields.Accepted -> fields.handle
            }
            val playerId = PlayerId(profile.playerId)
            if (credentials.holdsCredential(playerId, CredentialKind.PASSWORD)) {
                call.respond(HttpStatusCode.Conflict)
                return@post
            }
            val secret = PresentedSecret(request.password)
            when (credentials.create(playerId, CredentialKind.PASSWORD, handle, secret)) {
                CreateCredentialResult.Created -> call.respond(HttpStatusCode.Created)
                CreateCredentialResult.IdentifierTaken -> call.respond(HttpStatusCode.Conflict)
            }
        }
    }
}
