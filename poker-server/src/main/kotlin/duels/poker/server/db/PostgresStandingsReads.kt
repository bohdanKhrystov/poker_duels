package duels.poker.server.db

import duels.poker.server.http.StandingsCursor
import duels.poker.server.http.StandingsReads
import duels.poker.server.protocol.http.SelfStandingResponse
import duels.poker.server.protocol.http.StandingRow
import duels.poker.server.season.Season
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

/**
 * Reads a season's standings — one page of the ladder, or one player's own place on it — from
 * PostgreSQL.
 *
 * This class enforces the repository boundary (ADR-0011): all SQL runs here, nothing outside
 * `duels.poker.server.db` sees the database or SQL. No `ResultSet`, `Connection` or other JDBC
 * type escapes through the [StandingsReads] port.
 */
public class PostgresStandingsReads(private val dataSource: DataSource) : StandingsReads {
    /**
     * Sums the ledger over `[season.start, asOf)`, ranks the sums against the whole ladder, and
     * returns at most [limit] rows strictly after [after] when one is given.
     *
     * `after = null` adds no cursor predicate — every row the window admits is eligible, in
     * ladder order. A non-null [after] adds `(r.coins, r.player_id) < (after.coins,
     * after.playerId)` as a single PostgreSQL row-value comparison — the row the cursor names
     * never reappears, and no player tied with it on `coins` is skipped. [after]'s own `asOf` is
     * not read here: keeping every page of one walk pinned to the same cutoff is the caller's
     * business, not this query's.
     */
    override suspend fun standingsPage(
        season: Season,
        asOf: Instant,
        limit: Int,
        after: StandingsCursor?,
    ): List<StandingRow> =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                val sql = if (after == null) STANDINGS_SQL else STANDINGS_AFTER_SQL
                connection.prepareStatement(sql).use { statement ->
                    statement.setObject(1, OffsetDateTime.ofInstant(season.start, ZoneOffset.UTC))
                    statement.setObject(2, OffsetDateTime.ofInstant(asOf, ZoneOffset.UTC))
                    if (after == null) {
                        statement.setInt(3, limit)
                    } else {
                        statement.setInt(3, after.coins)
                        statement.setObject(4, after.playerId)
                        statement.setInt(5, limit)
                    }
                    statement.executeQuery().use { rows ->
                        val standings = mutableListOf<StandingRow>()
                        while (rows.next()) {
                            standings += readStandingRow(rows)
                        }
                        standings
                    }
                }
            }
        }

    /**
     * A second statement over the same `standing`/`ranked` CTEs [standingsPage] reads, under
     * the same `[season.start, asOf)` window, on its own connection with no explicit
     * transaction spanning the two: the two answers agree because the window is closed, not
     * because they share a snapshot (`ADR-0066` §6). The rank still comes from `rank()` over
     * the whole `ranked` CTE, so it counts everybody standing above [playerId] on the ladder,
     * never the rows of whichever page a caller happened to draw (`ADR-0066` §5).
     */
    override suspend fun standingOf(
        playerId: PlayerId,
        season: Season,
        asOf: Instant,
    ): SelfStandingResponse? =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(SELF_STANDING_SQL).use { statement ->
                    statement.setObject(1, OffsetDateTime.ofInstant(season.start, ZoneOffset.UTC))
                    statement.setObject(2, OffsetDateTime.ofInstant(asOf, ZoneOffset.UTC))
                    statement.setObject(3, UUID.fromString(playerId.value))
                    statement.executeQuery().use { rows ->
                        if (rows.next()) {
                            SelfStandingResponse(
                                playerId = playerId.value,
                                rank = rows.getInt("rank"),
                                coins = rows.getInt("coins"),
                            )
                        } else {
                            null
                        }
                    }
                }
            }
        }

    private fun readStandingRow(rows: ResultSet): StandingRow =
        StandingRow(
            rank = rows.getInt("rank"),
            playerId = rows.getObject("player_id", UUID::class.java).toString(),
            displayName = rows.getString("display_name"),
            coins = rows.getInt("coins"),
        )

    private companion object {
        // Two CTEs on purpose. `rank()` is a window function and cannot appear in the WHERE of
        // the select that computes it, so every predicate that narrows to a page
        // (STANDINGS_AFTER_SQL) or to one player (SELF_STANDING_SQL) filters `ranked` from
        // outside. That is also what makes the rank a function of the whole ladder rather than
        // of the page, or of the one player asking, alone (ADR-0064 §1, ADR-0066 §5).
        private const val RANKED_CTE =
            """
            WITH standing AS (
                SELECT dr.player_id AS player_id, SUM(dr.coin_delta)::int AS coins
                FROM duel_result dr
                JOIN duel d ON d.id = dr.duel_id
                WHERE d.finished_at >= ?::timestamptz
                  AND d.finished_at <  ?::timestamptz
                GROUP BY dr.player_id
            ),
            ranked AS (
                SELECT s.player_id, s.coins, rank() OVER (ORDER BY s.coins DESC) AS rank
                FROM standing s
            )
            """

        private const val STANDINGS_LINES =
            """
            $RANKED_CTE
            SELECT r.rank, r.player_id, p.display_name, r.coins
            FROM ranked r
            JOIN player p ON p.id = r.player_id
            """

        private const val STANDINGS_ORDER = "ORDER BY r.coins DESC, r.player_id DESC LIMIT ?"

        private const val STANDINGS_SQL = "$STANDINGS_LINES $STANDINGS_ORDER"

        // The row-value comparison mirrors STANDINGS_ORDER exactly: PostgreSQL compares coins
        // first and falls through to player_id only on a tie, the same sequence the ORDER BY
        // produces. player_id is forced, not chosen: keyset paging needs a key that is unique
        // and immutable, and display_name stops being immutable the moment a takedown nulls it.
        private const val STANDINGS_AFTER_SQL =
            "$STANDINGS_LINES WHERE (r.coins, r.player_id) < (?::int, ?::uuid) $STANDINGS_ORDER"

        // No join to `player`: a self standing carries no display name (the caller already
        // knows who they are), so this tail never needs the row `STANDINGS_LINES` joins in.
        private const val SELF_STANDING_SQL =
            "$RANKED_CTE SELECT r.rank, r.coins FROM ranked r WHERE r.player_id = ?::uuid"
    }
}
