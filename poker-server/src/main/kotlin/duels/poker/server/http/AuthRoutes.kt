package duels.poker.server.http

import duels.poker.server.auth.AttemptBudget
import duels.poker.server.auth.AuthSessions
import duels.poker.server.auth.CreateCredentialResult
import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.Credentials
import duels.poker.server.auth.IdentityResolver
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.loginHandleOrNull
import duels.poker.server.protocol.http.SignInRequest
import duels.poker.server.protocol.http.SignInResponse
import duels.poker.server.protocol.http.SignUpRequest
import duels.poker.server.session.PlayerId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException

/**
 * Installs `POST /api/auth/sign-up`, which attaches a password credential to the calling
 * identity's profile: identity, then decode, then field rules, then the guard, then the write.
 *
 * Identity is resolved first, **before the body is even read**, exactly as `PUT /api/me/name`
 * resolves it through [identities]: an unresolved caller answers `401` with an empty body, and
 * neither [Credentials] method is called. Answering `409` or `422` before identity is confirmed
 * would let an anonymous caller learn whether a handle is taken. Only then is the body decoded as
 * [SignUpRequest] — every decode failure is `400`, the cause never changing the answer — and only
 * then does [signUpFieldsOf] judge the handle and password; a `Refused` answers its status (`400`
 * or `422`) and stops, still before [Credentials] is touched.
 *
 * [Credentials.holdsCredential] then answers `409` without writing (`ADR-0030` §1). Only past that
 * guard is [budget] consulted — last, immediately before the write, because it meters *spending*
 * rather than failure (`ADR-0055` §1): a request refused by identity, decoding, the field rules or
 * the guard costs no Argon2, so it must cost no budget either. Over [budget], the answer is `429
 * Too Many Requests` with an empty body, nothing written and no hash computed (`ADR-0055` §3). The
 * key is [io.ktor.server.plugins.origin]'s remote address alone; until `EPIC-07` installs the
 * forwarded-header plugin, this must never honour a client-supplied forwarding header, which would
 * let a client pick its own budget key.
 *
 * [Credentials.create] then answers `201 Created` — not `204`, because exactly one row is created
 * and that is this endpoint's entire purpose — or `409` if the handle was taken concurrently. There
 * is no `Location` header: no credential is ever read back (`ADR-0027` §1). The player id written
 * is always `profile.playerId`, the identity the server resolved; [SignUpRequest] has no field to
 * carry one, so a client can never assert who it is (`ADR-0002`).
 *
 * Also installs `POST /api/auth/sign-in`, which verifies a handle and a password and answers with
 * a freshly issued session token, in this fixed order: decode, then the handle, then the
 * credential, then the write. Every decode failure is `400`. An unusable handle — one
 * [loginHandleOrNull] refuses — answers the **same** `401` a wrong password does, never `400`:
 * telling a stranger their handle's shape alone was the problem would tell them the handle's
 * validity is knowable without a credential at all. [Credentials.verify] alone then decides
 * success or failure (`ADR-0027` §6) — there is no separate "does this handle exist" check,
 * because Argon2's own constant-time dummy-hash path is what makes the no-such-account case cost
 * what the wrong-password case costs; a pre-check here would defeat it, silently. Only a verified
 * credential reaches [AuthSessions.issue], and this endpoint writes to `auth_session` alone — no
 * `player` row is created or read. Like sign-up, it resolves no identity: no `X-Device-Id` header
 * and no `Authorization` header are read, so a browser that has never connected can still recover
 * an account (`ADR-0030` §2).
 *
 * Finally installs `POST /api/auth/sign-out`, which deletes the presented session token and
 * answers `204 No Content` whether or not a session existed — an absent token is not an error
 * (`ADR-0030` §3). It closes no sockets: a socket opened as player `Q` stays `Q` until it closes,
 * because tearing one down would abandon a seat mid-duel and `ADR-0013`'s grace period would then
 * fold it — an authentication operation that costs a coin. The client closes its own socket and
 * reconnects; the server revokes future authentications only.
 *
 * @param reads The port for resolving the calling identity's profile.
 * @param credentials The port for checking and creating password credentials.
 * @param identities The port that resolves a session token or a device id into a player.
 * @param sessions The port for issuing a session token once sign-in's credential succeeds, and for
 *     deleting a session token on sign-out.
 * @param budget The sign-up rate limiter, checked immediately before [Credentials.create]. Sign-in
 *     and sign-out never consult it.
 */
public fun Application.authRoutes(
    reads: ProfileReads,
    credentials: Credentials,
    identities: IdentityResolver,
    sessions: AuthSessions,
    budget: AttemptBudget,
) {
    routing {
        post("/api/auth/sign-up") {
            // Identity first: an unresolved caller is refused before the body is read, so a
            // stranger never reaches the 409/422 that would tell them whether a handle is taken.
            val profile = call.resolvedPlayerOrNull(identities)?.let { reads.profileOf(it) }
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
            // Last, immediately before the write: the budget meters spending, not failure
            // (ADR-0055 §1), so every refusal above this line — identity, decode, fields, the
            // guard — must cost none of the five slots. Getting this order wrong is the whole
            // defect: a budget checked earlier turns a 401 into a 429 and vice versa.
            // Last, immediately before the write: the budget meters spending, not failure
            // (ADR-0055 §1), so every refusal above this line — identity, decode, fields, the
            // guard — must cost none of the five slots. Getting this order wrong is the whole
            // defect: a budget checked earlier turns a 401 into a 429 and vice versa.
            // Last, immediately before the write: the budget meters spending, not failure
            // (ADR-0055 §1), so every refusal above this line — identity, decode, fields, the
            // guard — must cost none of the five slots. Getting this order wrong is the whole
            // defect: a budget checked earlier turns a 401 into a 429 and vice versa.
            if (!budget.admit(call.request.origin.remoteAddress)) {
                call.respond(HttpStatusCode.TooManyRequests)
                return@post
            }
            val secret = PresentedSecret(request.password)
            when (credentials.create(playerId, CredentialKind.PASSWORD, handle, secret)) {
                CreateCredentialResult.Created -> call.respond(HttpStatusCode.Created)
                CreateCredentialResult.IdentifierTaken -> call.respond(HttpStatusCode.Conflict)
            }
        }
        post("/api/auth/sign-in") {
            // Every way a body can fail to become a SignInRequest is a client error, 400, exactly
            // as sign-up's own decode step treats it — the specific cause never changes the answer.
            val request = try {
                call.receive<SignInRequest>()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (ignored: Exception) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            // An unusable handle answers the same 401 a wrong password does, never 400: "that is
            // not a valid handle" would tell a stranger the shape of the namespace and, worse,
            // that their target's handle is valid — so this never short-circuits around
            // Credentials.verify's own constant-time dummy-hash path (ADR-0027 §6).
            val handle = loginHandleOrNull(request.handle)
            if (handle == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val secret = PresentedSecret(request.password)
            val playerId = credentials.verify(CredentialKind.PASSWORD, handle, secret)
            if (playerId == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            val token = sessions.issue(playerId)
            call.respond(HttpStatusCode.OK, SignInResponse(token.value))
        }
        post("/api/auth/sign-out") {
            // Delete the presented token, if any. An absent token is not an error: answering 204
            // whether or not a row was deleted keeps sign-out idempotent, and a 404 for unknown
            // would tell a caller which tokens exist (ADR-0030 §3).
            val token = call.sessionTokenOrNull()
            if (token != null) {
                sessions.delete(token)
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
