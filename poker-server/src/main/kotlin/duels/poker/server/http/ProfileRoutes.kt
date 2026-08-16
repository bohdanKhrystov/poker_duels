package duels.poker.server.http

import duels.poker.server.protocol.http.RecentDuelsResponse
import duels.poker.server.protocol.http.SetNameRequest
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException

public const val DEVICE_ID_HEADER: String = "X-Device-Id"

/**
 * Installs the `GET /api/me`, `GET /api/me/duels`, and `PUT /api/me/name` routes that return a
 * device's profile and recent duels, and let it set its display name.
 *
 * `GET /api/me` returns the device's profile as JSON if the `X-Device-Id` header is present and
 * names a known device, otherwise `401 Unauthorized` with an empty body. Absent, blank and unknown
 * device ids all receive the same response — `401` instead of `404` — because the device id is the
 * only credential v0.1 has, an unknown one is an invalid credential, and answering `404` for
 * unknown would tell a caller which device ids exist.
 *
 * `GET /api/me/duels?limit=N` returns the device's recent duels as a JSON array. The limit defaults
 * to [DEFAULT_DUEL_LIMIT] when absent, is clamped to [MAX_DUEL_LIMIT] when above the cap, and is
 * rejected with `400 Bad Request` when non-numeric, negative, or zero. An unauthenticated request
 * is refused with `401 Unauthorized` before the limit is parsed, so a bad limit never tells a
 * stranger that their device id was the problem. A device with no duels receives `200 OK` and an
 * empty array, not `404`.
 *
 * `PUT /api/me/name` sets the device's display name. Identity is resolved first — exactly as the
 * other two routes resolve it, and **before the body is even read**: an absent, blank or unknown
 * device id answers `401 Unauthorized` with an empty body, and [writes] is never invoked. This
 * order is the whole point, not an optimisation: answering `409` or `403` for a name before
 * identity is confirmed would let an anonymous caller learn whether a name is taken or already
 * held, which makes the endpoint an enumeration oracle. Only once identity is confirmed is the
 * body decoded as `SetNameRequest`; a body that fails to decode — a missing `name`, an
 * unrecognised field, or invalid JSON — answers `400 Bad Request`, still before [writes] is
 * called. The decoded name is then run through `canonicalDisplayNameOrNull`; a name the rules
 * refuse is a client error and also answers `400`, again before [writes] is called. Only a
 * canonical name reaches [ProfileWrites.setDisplayName]: `NameSet` answers `200 OK` with the
 * updated profile, `NameTaken` answers `409 Conflict`, and `AlreadyNamed` answers `403 Forbidden`
 * (`ADR-0029` §5).
 *
 * These routes hold a `ProfileReads` and a `ProfileWrites` and nothing else — no `PlayerDirectory`,
 * no `DataSource`, no `resolve`. Profile creation happens on the socket handshake only
 * (`ADR-0012`), so a crawler hitting this endpoint mints no rows. The routes are installed in
 * production by `Application.duelServer` against a shared set of `ServerComponents` that backs all
 * routes; a test may still install them directly with its own collaborators.
 *
 * @param reads The port for reading player profiles and balances.
 * @param writes The port for writing player profiles, used by `PUT /api/me/name` only.
 */
public fun Application.profileRoutes(reads: ProfileReads, writes: ProfileWrites) {
    routing {
        get("/api/me") {
            val profile = call.deviceIdOrNull()?.let { reads.profileOf(it) }
            if (profile == null) call.respond(HttpStatusCode.Unauthorized) else call.respond(profile)
        }
        get("/api/me/duels") {
            // Identity first: an unauthenticated request is refused before its query string is
            // parsed, so a bad limit never tells a stranger that their device id was the problem.
            val profile = call.deviceIdOrNull()?.let { reads.profileOf(it) }
            if (profile == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            val limit = duelLimitOrNull(call.request.queryParameters["limit"])
            if (limit == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            call.respond(RecentDuelsResponse(reads.recentDuelsOf(PlayerId(profile.playerId), limit)))
        }
        put("/api/me/name") {
            // Identity first: an unauthenticated request is refused before the body is read, so a
            // stranger never reaches the 409/403 that would tell them whether a name is taken.
            val profile = call.deviceIdOrNull()?.let { reads.profileOf(it) }
            if (profile == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@put
            }
            // Every way a body can fail to become a SetNameRequest — empty, the wrong content
            // type, malformed JSON, or a missing/unrecognised field — is a client error, `400`,
            // not a server one; the specific cause does not change the answer.
            val request = try {
                call.receive<SetNameRequest>()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (ignored: Exception) {
                call.respond(HttpStatusCode.BadRequest)
                return@put
            }
            val canonicalName = canonicalDisplayNameOrNull(request.name)
            if (canonicalName == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@put
            }
            when (val result = writes.setDisplayName(PlayerId(profile.playerId), canonicalName)) {
                is SetNameResult.NameSet -> call.respond(result.profile)
                SetNameResult.NameTaken -> call.respond(HttpStatusCode.Conflict)
                // 403 rather than 409 for a name already owned: 409 invites a retry, but no state
                // this client can reach makes the request succeed, so 403 is honest (ADR-0029 §5).
                SetNameResult.AlreadyNamed -> call.respond(HttpStatusCode.Forbidden)
            }
        }
    }
}

/**
 * Extracts the device id from the `X-Device-Id` header, or `null` if absent or blank.
 *
 * A header of only whitespace is treated as absent — `DeviceId`'s `init` rejects a blank value,
 * so without the `takeIf { it.isNotBlank() }` guard a header of spaces would throw and answer
 * `500` instead of `401`.
 */
private fun io.ktor.server.application.ApplicationCall.deviceIdOrNull(): DeviceId? =
    request.headers[DEVICE_ID_HEADER]?.takeIf { it.isNotBlank() }?.let(::DeviceId)
