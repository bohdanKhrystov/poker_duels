package duels.poker.server.db

import duels.poker.server.http.DuelCursor
import duels.poker.server.http.DuelFilter
import duels.poker.server.http.ProfileReads
import duels.poker.server.protocol.http.DuelOutcomeLabel
import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.outcomeOf
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

/**
 * Reads player profiles, coin balances and duel history from PostgreSQL.
 *
 * This class enforces the repository boundary (ADR-0011): all SQL runs here, nothing outside
 * `duels.poker.server.db` sees the database or SQL. No `ResultSet`, `Connection` or other JDBC
 * type escapes through the [ProfileReads] port.
 */
public class PostgresProfileReads(private val dataSource: DataSource) : ProfileReads {
    override suspend fun profileOf(playerId: PlayerId): ProfileResponse? = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(PROFILE_OF_SQL).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    if (rows.next()) {
                        ProfileResponse(
                            rows.getString(1),
                            rows.getInt(2),
                            rows.getString(3),
                            rows.getBoolean(4),
                            rows.getBoolean(5),
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }

    /**
     * Reads [playerId]'s duels newest-first, at most [limit] of them, strictly after [after]
     * when one is given, and narrowed to only the outcome [filter] names when its `outcome` is
     * non-null.
     *
     * `after = null` adds no cursor predicate — every row [filter] admits is eligible, in the
     * same order. A non-null [after] adds
     * `(d.finished_at, d.id) < (after.finishedAt, after.duelId)` as a single PostgreSQL row-value
     * comparison — the row the cursor names never reappears, and no duel that ties it on
     * `finished_at` is skipped.
     *
     * [filter]'s `outcome` narrows the read to exactly the rows whose `coin_delta` [outcomeOf]
     * would report as that outcome — the filter reads the same sign, on the same reasoning
     * (ADR-0014), rather than comparing against a stored magnitude. [DuelFilter.NONE] reads every
     * outcome, excluding none by sign.
     *
     * [filter]'s `opponent` narrows the read to duels whose opponent's display name contains it as
     * a substring, folded under the `und-x-icu` collation pinned by ADR-0029 §1 so a search agrees
     * with the name's own uniqueness about what two names differing only in case are. A `null`
     * `opponent` narrows nothing, and an opponent who has never set a name never matches a non-null
     * term.
     */
    override suspend fun recentDuelsOf(
        playerId: PlayerId,
        limit: Int,
        after: DuelCursor?,
        filter: DuelFilter,
    ): List<DuelSummaryResponse> =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                val sql = if (after == null) RECENT_DUELS_SQL else DUELS_AFTER_SQL
                connection.prepareStatement(sql).use { statement ->
                    statement.setObject(1, UUID.fromString(playerId.value))
                    statement.bindOutcome(2, filter.outcome)
                    statement.bindOpponent(4, filter.opponent)
                    if (after == null) {
                        statement.setInt(6, limit)
                    } else {
                        statement.setObject(6, OffsetDateTime.ofInstant(after.finishedAt, ZoneOffset.UTC))
                        statement.setObject(7, after.duelId)
                        statement.setInt(8, limit)
                    }
                    statement.executeQuery().use { rows ->
                        val duels = mutableListOf<DuelSummaryResponse>()
                        while (rows.next()) {
                            duels += readDuelSummary(rows)
                        }
                        duels
                    }
                }
            }
        }

    private fun readDuelSummary(rows: ResultSet): DuelSummaryResponse {
        val coinDelta = rows.getInt("coin_delta")
        return DuelSummaryResponse(
            duelId = rows.getObject("duel_id", UUID::class.java).toString(),
            opponentPlayerId = rows.getObject("opponent_id", UUID::class.java).toString(),
            outcome = outcomeOf(coinDelta),
            coinDelta = coinDelta,
            handsPlayed = rows.getInt("hands_played"),
            finishedAt = rows.getObject("finished_at", OffsetDateTime::class.java)
                .toInstant()
                .toString(),
            opponentDisplayName = rows.getString("opponent_display_name"),
        )
    }

    // The clause has two `?` for one value (JDBC has no named parameters), so both positions
    // are bound identically here rather than the two-branch `if` this would otherwise take at
    // every call site.
    private fun PreparedStatement.bindOutcome(first: Int, outcome: DuelOutcomeLabel?) {
        val sign = outcome?.let(::coinDeltaSignOf)
        for (index in first..first + 1) {
            if (sign == null) setNull(index, Types.INTEGER) else setInt(index, sign)
        }
    }

    // Same two-position shape as bindOutcome above. Nothing lower-cases opponent here: folding
    // happens once, in SQL, under the collation POSITION's lower() calls pin — doing it again in
    // Kotlin would risk the two sides disagreeing about what "same letter" means.
    private fun PreparedStatement.bindOpponent(first: Int, opponent: String?) {
        for (index in first..first + 1) {
            if (opponent == null) setNull(index, Types.VARCHAR) else setString(index, opponent)
        }
    }

    // No `else`: a fourth DuelOutcomeLabel would fail to compile here rather than silently
    // filter nothing.
    private fun coinDeltaSignOf(outcome: DuelOutcomeLabel): Int =
        when (outcome) {
            DuelOutcomeLabel.WON -> 1
            DuelOutcomeLabel.LOST -> -1
            DuelOutcomeLabel.DREW -> 0
        }

    private companion object {
        // A correlated EXISTS, never a LEFT JOIN: a player may hold more than one retired name,
        // so a join would return one row per retired name for a single profile and
        // `if (rows.next())` would silently take the first. EXISTS is a semijoin — one boolean
        // per profile row whatever name_registry holds. Correlated to p.id, never to a second
        // `?`: the statement binds the caller's player id exactly once, so a later edit cannot
        // make the boolean describe a different player than the row does.
        // `r.reason = 'RETIRED'` is redundant under the partial index
        // name_registry_retired_from, and stays — the statement should read correctly without
        // the constraint in hand.
        // device_route_live is a second, independent correlated EXISTS on the same reasoning: a
        // player has at most one live device_binding row but may hold several revoked ones, so a
        // JOIN would again multiply the profile row. Correlated to p.id, never to a second `?`,
        // so this boolean cannot drift to describe a different player than the row it rides on.
        private const val PROFILE_OF_SQL =
            """
            SELECT p.id,
                   p.coin_balance,
                   p.display_name,
                   (p.display_name IS NULL
                    AND EXISTS (SELECT 1 FROM name_registry r
                                 WHERE r.retired_from = p.id AND r.reason = 'RETIRED')) AS display_name_removed,
                   EXISTS (SELECT 1 FROM device_binding b
                            WHERE b.player_id = p.id AND b.revoked_at IS NULL) AS device_route_live
            FROM player p
            WHERE p.id = ?
            """

        // ADR-0015: every participant of every completed duel has exactly one `duel_result`
        // row, including both rows of a draw (`coin_delta = 0`). That is what makes this
        // self-join safe: the opponent's row always exists, so a drawn duel is not a special
        // case and needs no `coin_delta <> 0` filter that would silently drop it.
        // duel_result.player_id is NOT NULL REFERENCES player (id) in V1, so exactly one
        // `player` row matches every opponent result row. The inner join can neither drop a
        // duel nor duplicate one.
        private const val DUEL_LINES =
            """
            SELECT d.id AS duel_id,
                   o.player_id AS opponent_id,
                   p.display_name AS opponent_display_name,
                   r.coin_delta AS coin_delta,
                   d.finished_at AS finished_at,
                   d.hands_played AS hands_played
            FROM duel_result r
            JOIN duel d ON d.id = r.duel_id
            JOIN duel_result o ON o.duel_id = r.duel_id AND o.player_id <> r.player_id
            JOIN player p ON p.id = o.player_id
            WHERE r.player_id = ?
              AND (?::int IS NULL OR sign(r.coin_delta) = ?::int)
              AND (?::text IS NULL OR POSITION(lower(?::text COLLATE "und-x-icu") IN lower(p.display_name COLLATE "und-x-icu")) > 0)
            """

        private const val DUEL_ORDER = "ORDER BY d.finished_at DESC, d.id DESC LIMIT ?"

        private const val RECENT_DUELS_SQL = "$DUEL_LINES $DUEL_ORDER"

        // The row-value comparison mirrors DUEL_ORDER exactly: PostgreSQL compares finished_at
        // first and falls through to id only on a tie, which is the same sequence the ORDER BY
        // produces. The explicit ::timestamptz / ::uuid casts exist because parameter type
        // inference inside a row constructor is not something to rely on.
        private const val DUELS_AFTER_SQL =
            "$DUEL_LINES AND (d.finished_at, d.id) < (?::timestamptz, ?::uuid) $DUEL_ORDER"
    }
}
