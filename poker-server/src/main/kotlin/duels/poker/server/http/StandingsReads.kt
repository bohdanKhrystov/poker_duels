package duels.poker.server.http

import duels.poker.server.protocol.http.StandingRow
import duels.poker.server.season.Season
import java.time.Instant

/**
 * A port for reading one page of a season's standings ladder as it stood at a cutoff.
 *
 * This port exists so the routes can be tested without a database, and so no route ever holds
 * a `DataSource` (`ADR-0011`). It returns the response type rather than a parallel domain type
 * because the answer's shape *is* the wire's shape, and a parallel domain type would be a copy
 * nobody reads — the reason `ProfileReads`' KDoc gives.
 */
public interface StandingsReads {
    /**
     * Reads one page of [season]'s ladder as it stood at [asOf].
     *
     * The window is `[season.start, asOf)` — half-open, so a duel finishing exactly at [asOf]
     * belongs to the next walk, never this one (`ADR-0066` §2). Every player who finished a duel
     * in the window has a row, named or not: nothing narrows the read beyond the window
     * (`ADR-0063` §1) — no minimum duel count, no minimum standing, no display name.
     *
     * Rows are ranked against the *whole* ladder, then ordered `coins DESC, playerId DESC` and
     * paged with a row-value cursor (`ADR-0066` §3). `playerId` breaks ties, never `displayName`:
     * keyset paging needs a key that is unique *and* immutable, and `displayName` stops being
     * immutable the moment a takedown nulls it.
     *
     * @param season the season whose window bounds the read.
     * @param asOf the window's exclusive upper bound — the walk's cutoff.
     * @param limit the maximum number of rows to return.
     * @param after a position to resume after. `null` reads the first page.
     * @return up to [limit] rows, ordered by coins descending, then player id descending.
     */
    public suspend fun standingsPage(
        season: Season,
        asOf: Instant,
        limit: Int,
        after: StandingsCursor? = null,
    ): List<StandingRow>
}
