package duels.poker.server.http

import duels.poker.server.protocol.http.DuelSummaryResponse
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
import java.time.Instant
import java.util.UUID

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
 * `GET /api/me/duels?limit=N&after=C&outcome=O&opponent=P` returns the device's recent duels as
 * a JSON array, optionally narrowed by outcome and opponent. The limit defaults to
 * [DEFAULT_DUEL_LIMIT] when absent, is clamped to [MAX_DUEL_LIMIT] when above the cap, and is
 * rejected with `400 Bad Request` when non-numeric, negative, or zero. The `after` cursor is
 * optional: absent, it asks for the newest page and `null` reaches `ProfileReads.recentDuelsOf`;
 * present, it is decoded with [duelCursorOrNull], and anything that function refuses — including an
 * empty value, which is present and unparseable, not absent — answers `400 Bad Request` before
 * `ProfileReads.recentDuelsOf` is ever called. `outcome` takes `WON`, `LOST` or `DREW`; anything
 * else, including a lower-case spelling, is `400 Bad Request`. `opponent` is a case-insensitive
 * substring of the opponent's display name, refused when blank or over 32 code points. An empty
 * value of either `outcome` or `opponent` is present-and-unusable, not absent, and is
 * `400 Bad Request` before `ProfileReads.recentDuelsOf` is ever called — the same rule the cursor
 * already follows. An unauthenticated request is refused with `401 Unauthorized` before the query
 * string is looked at, so none of the limit, the cursor, or the filter ever tells a stranger that
 * their device id was the problem; among the three, the limit is checked first, then the cursor,
 * then the filter. A device with no duels receives `200 OK` and an empty array, not `404`.
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
        get("/api/me/duels") { call.respondWithDuels(reads) }
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
 * Handles `GET /api/me/duels`, extracted out of [profileRoutes] to keep that function inside
 * detekt's `LongMethod` line budget. See the `GET /api/me/duels` paragraph on [profileRoutes]'s
 * KDoc for the endpoint's documented behaviour; this function is its implementation, not a second
 * source of truth for it.
 */
private suspend fun io.ktor.server.application.ApplicationCall.respondWithDuels(reads: ProfileReads) {
    // Identity first: an unauthenticated request is refused before its query string is parsed, so
    // a bad limit, cursor, or filter never tells a stranger that their device id was the problem.
    val profile = deviceIdOrNull()?.let { reads.profileOf(it) }
    if (profile == null) {
        respond(HttpStatusCode.Unauthorized)
        return
    }
    val limit = duelLimitOrNull(request.queryParameters["limit"])
    if (limit == null) {
        respond(HttpStatusCode.BadRequest)
        return
    }
    // Absent `after` means the newest page, so `null` reaches the port. Present, it is decoded
    // with duelCursorOrNull; anything that refuses — including an empty value — is `400` before
    // reads.recentDuelsOf runs. No `takeIf { it.isNotBlank() }` here: that would turn a broken
    // cursor into a silent first page, the one behaviour this endpoint exists to refuse.
    val rawCursor = request.queryParameters["after"]
    val cursor = if (rawCursor == null) null else duelCursorOrNull(rawCursor)
    if (rawCursor != null && cursor == null) {
        respond(HttpStatusCode.BadRequest)
        return
    }
    val filter = duelFilterOrNull(
        request.queryParameters["outcome"],
        request.queryParameters["opponent"],
    )
    if (filter == null) {
        respond(HttpStatusCode.BadRequest)
        return
    }
    // Ask for one row more than the page: a probe that answers "is there another page?" without a
    // second query. duelLimitOrNull already clamped limit to MAX_DUEL_LIMIT, so this never reads
    // more than MAX_DUEL_LIMIT + 1 rows.
    val rows = reads.recentDuelsOf(PlayerId(profile.playerId), limit + 1, cursor, filter)
    respond(rows.recentDuelsPage(limit))
}

/**
 * Trims [this] — up to `limit + 1` rows from [respondWithDuels]'s probing read — down to a page
 * of at most [limit], and reports whether another page follows. Extracted out of
 * [respondWithDuels] to keep that function, in turn, well inside detekt's `LongMethod` budget.
 *
 * The `(limit + 1)`th row, when present, is the probe: proof that another page exists and nothing
 * more. It is dropped here and never serialised — `duels.size` never exceeds [limit] — and
 * `nextCursor` is built from the row actually returned last, at index `limit - 1`, using that
 * row's own text. A cursor built from the probe instead would name a row one past the page just
 * served; the next request would start there, and the row between the two would never be
 * returned — silently, and forever. Both `finishedAt` and `duelId` were produced by this server
 * (`Instant.toString()`, `UUID.toString()`), so parsing them back here is never a place for a
 * `try`.
 */
private fun List<DuelSummaryResponse>.recentDuelsPage(limit: Int): RecentDuelsResponse {
    if (size <= limit) return RecentDuelsResponse(this, nextCursor = null)
    val page = subList(0, limit)
    val lastRow = page.last()
    val cursor = DuelCursor(Instant.parse(lastRow.finishedAt), UUID.fromString(lastRow.duelId))
    return RecentDuelsResponse(page, nextCursor = cursor.encoded())
}

/**
 * Extracts the device id from the `X-Device-Id` header, or `null` if absent or blank.
 *
 * A header of only whitespace is treated as absent — `DeviceId`'s `init` rejects a blank value,
 * so without the `takeIf { it.isNotBlank() }` guard a header of spaces would throw and answer
 * `500` instead of `401`.
 */
internal fun io.ktor.server.application.ApplicationCall.deviceIdOrNull(): DeviceId? =
    request.headers[DEVICE_ID_HEADER]?.takeIf { it.isNotBlank() }?.let(::DeviceId)
