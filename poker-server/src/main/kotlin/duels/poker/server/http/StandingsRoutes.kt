package duels.poker.server.http

import duels.poker.server.protocol.http.SelfStandingResponse
import duels.poker.server.protocol.http.StandingRow
import duels.poker.server.protocol.http.StandingsResponse
import duels.poker.server.season.Season
import duels.poker.server.season.currentSeason
import duels.poker.server.session.PlayerId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Installs `GET /api/standings`, which answers one page of the current season's coin ladder.
 *
 * `GET /api/standings?limit=N&after=C` returns a [StandingsResponse]: the season the response was
 * computed for, up to `limit` rows ranked by coins, a cursor for the following page, and the
 * caller's own standing. `limit` is parsed by [duelLimitOrNull] — the same parser
 * `GET /api/me/duels` uses, so this ladder inherits its default of [DEFAULT_DUEL_LIMIT], its cap
 * of [MAX_DUEL_LIMIT], and its one refusal vocabulary: a limit that is non-numeric, negative, or
 * zero answers `400 Bad Request`. `after` is optional: absent, it asks for the first page of a
 * new walk; present, it is decoded with [standingsCursorOrNull], and anything that function
 * refuses — including a cursor whose cutoff falls outside the season the request is answered in —
 * answers `400 Bad Request` before [StandingsReads.standingsPage] is ever called. These are the
 * only two parameters this route reads; there is no `playerId`, no *jump to me*, and no `season`
 * (`ADR-0065` §3 and §5, `ADR-0061` §7) — `DEC-057` and `DEC-060` are still open, and this route
 * pre-empts neither.
 *
 * The cutoff that bounds the read is minted once per walk, not once per request (`ADR-0066` §2):
 * a cursorless request — the first page of a new walk — reads [clock] and mints `asOf`; every
 * later request in that same walk carries a cursor and reuses **that cursor's** `asOf` instead,
 * so [clock] is never consulted again for the rest of the walk. A page therefore never moves
 * underneath a reader because a duel finished while they were paging through it.
 *
 * The `season` named in the response is always the season [clock] is in right now, read the same
 * way [duelLimitOrNull]'s sibling ports read it — never a season the client names, since there is
 * no `season` parameter to name one with.
 *
 * `self` is the caller's own standing, resolved from the `X-Device-Id` header alone. Its three
 * shapes (`ADR-0065` §4) are: a rank and a coin standing when the header names a known device that
 * has finished a duel this season; both `null` when the header names a known device that has
 * finished none; and the whole `self` object `null` when the header is absent, blank, or names no
 * device this server knows — decided before [StandingsReads.standingOf] is ever called
 * (`ADR-0066` §6).
 *
 * Unlike every other route in this package, **this route never answers `401`**: the ladder is
 * the same page for every reader, named or anonymous (`ADR-0065` §4), so there is no identity to
 * refuse before serving it.
 *
 * @param reads The port for resolving `X-Device-Id` into a profile, used only to find [self]'s
 *   player id.
 * @param standings The port for reading a season's ladder and one player's place on it.
 * @param clock The wall clock (`ADR-0062`) a cursorless request reads, at most once, to mint the
 *   walk's cutoff. Has no default: the one production instance of it lives at the composition
 *   root (`TASK-050201`), not here.
 */
public fun Application.standingsRoutes(reads: ProfileReads, standings: StandingsReads, clock: Clock) {
    routing {
        get("/api/standings") { call.respondWithStandings(reads, standings, clock) }
    }
}

/**
 * Handles `GET /api/standings`, extracted out of [standingsRoutes] to keep that function inside
 * detekt's `LongMethod` line budget. See [standingsRoutes]'s KDoc for the endpoint's documented
 * behaviour; this function is its implementation, not a second source of truth for it.
 */
private suspend fun io.ktor.server.application.ApplicationCall.respondWithStandings(
    reads: ProfileReads,
    standings: StandingsReads,
    clock: Clock,
) {
    val season = currentSeason(clock)
    val limit = duelLimitOrNull(request.queryParameters["limit"])
    if (limit == null) {
        respond(HttpStatusCode.BadRequest)
        return
    }
    val rawCursor = request.queryParameters["after"]
    val cursor = if (rawCursor == null) null else standingsCursorOrNull(rawCursor, season)
    if (rawCursor != null && cursor == null) {
        respond(HttpStatusCode.BadRequest)
        return
    }
    // ADR-0066 §2: a cursorless request mints the walk's cutoff right here; a cursored request
    // reuses the cutoff its own cursor already carries instead. Either way this is the only place
    // the clock is read, and it is read at most once.
    val asOf = cursor?.asOf ?: clock.instant()
    val rows = standings.standingsPage(season, asOf, limit + 1, cursor)
    val profile = deviceIdOrNull()?.let { reads.profileOf(it) }
    val self =
        profile?.let {
            standings.standingOf(PlayerId(it.playerId), season, asOf)
                ?: SelfStandingResponse(it.playerId, null, null)
        }
    respond(rows.toStandingsResponse(season, limit, asOf, self))
}

/**
 * Trims [this] — up to `limit + 1` rows from [respondWithStandings]'s probing read — down to a
 * page of at most [limit], and reports whether another page follows. Extracted out of
 * [respondWithStandings] to keep that function, in turn, well inside detekt's `LongMethod` budget.
 *
 * The `(limit + 1)`th row, when present, is the probe: proof that another page exists and nothing
 * more. It is dropped here and never serialised, and `nextCursor` is minted from the row actually
 * served last, carrying the *same* [asOf] this page was read at — never the probe row, which
 * names a row one past the page just served. A cursor minted from the probe would make the next
 * request start there, and the row between the two would never be returned — silently, and
 * forever, the same trap `recentDuelsPage`'s KDoc names.
 */
private fun List<StandingRow>.toStandingsResponse(
    season: Season,
    limit: Int,
    asOf: Instant,
    self: SelfStandingResponse?,
): StandingsResponse {
    if (size <= limit) return StandingsResponse(season.toString(), this, nextCursor = null, self)
    val page = subList(0, limit)
    val lastRow = page.last()
    val nextCursor = StandingsCursor(asOf, lastRow.coins, UUID.fromString(lastRow.playerId))
    return StandingsResponse(season.toString(), page, nextCursor = nextCursor.encoded(), self)
}
