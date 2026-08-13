package duels.poker.server.http

import duels.poker.server.protocol.http.RecentDuelsResponse
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

public const val DEVICE_ID_HEADER: String = "X-Device-Id"

/**
 * Installs the `GET /api/me` and `GET /api/me/duels` routes that return a device's profile and
 * recent duels.
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
 * This route holds a `ProfileReads` and nothing else — no `PlayerDirectory`, no `DataSource`, no
 * `resolve`. Profile creation happens on the socket handshake only (`ADR-0012`), so a crawler
 * hitting this endpoint mints no rows. It is installed by the caller rather than from
 * `Application.module()` because installing it there means handing `module()` a `DataSource`,
 * which `STORY-0212` owns — so the only caller is still a test.
 *
 * @param reads The port for reading player profiles and balances.
 */
public fun Application.profileRoutes(reads: ProfileReads) {
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
