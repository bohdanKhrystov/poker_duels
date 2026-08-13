package duels.poker.server.http

import duels.poker.server.session.DeviceId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

public const val DEVICE_ID_HEADER: String = "X-Device-Id"

/**
 * Installs the `GET /api/me` route that returns a device's profile and coin balance.
 *
 * Returns the device's profile as JSON if the `X-Device-Id` header is present and names a known
 * device, otherwise `401 Unauthorized` with an empty body. Absent, blank and unknown device ids
 * all receive the same response — `401` instead of `404` — because the device id is the only
 * credential v0.1 has, an unknown one is an invalid credential, and answering `404` for unknown
 * would tell a caller which device ids exist.
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
