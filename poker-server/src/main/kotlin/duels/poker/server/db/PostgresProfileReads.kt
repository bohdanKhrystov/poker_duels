package duels.poker.server.db

import duels.poker.server.http.DuelCursor
import duels.poker.server.http.DuelFilter
import duels.poker.server.http.ProfileReads
import duels.poker.server.protocol.http.DuelOutcomeLabel
import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.outcomeOf
import duels.poker.server.session.DeviceId
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
    override suspend fun profileOf(deviceId: DeviceId): ProfileResponse? = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT id, coin_balance, display_name FROM player WHERE device_id = ?")
                .use { statement ->
                    statement.setString(1, deviceId.value)
                    statement.executeQuery().use { rows ->
                        if (rows.next()) ProfileResponse(rows.getString(1), rows.getInt(2), rows.getString(3)) else null
                    }
                }
        }
    }

    override suspend fun recentDuelsOf(playerId: PlayerId, limit: Int, after: DuelCursor?): List<DuelSummaryResponse> =
        recentDuelsOf(playerId, limit, after, DuelFilter.NONE)

    /**
     * Reads [playerId]'s duels newest-first, at most [limit] of them, strictly after [after]
     * when one is given, and narrowed to only the outcome [filter] names when its `outcome` is
     * non-null.
     *
     * `after = null` reads exactly what the three-argument [recentDuelsOf] always has: same rows,
     * same order, same query shape. A non-null [after] adds
     * `(d.finished_at, d.id) < (after.finishedAt, after.duelId)` as a single PostgreSQL row-value
     * comparison — the row the cursor names never reappears, and no duel that ties it on
     * `finished_at` is skipped.
     *
     * [filter]'s `outcome` narrows the read to exactly the rows whose `coin_delta` [outcomeOf]
     * would report as that outcome — the filter reads the same sign, on the same reasoning
     * (ADR-0014), rather than comparing against a stored magnitude. [DuelFilter.NONE] reads every
     * outcome: exactly what the three-argument overload above always has.
     */
    public suspend fun recentDuelsOf(
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
                    if (after == null) {
                        statement.setInt(4, limit)
                    } else {
                        statement.setObject(4, OffsetDateTime.ofInstant(after.finishedAt, ZoneOffset.UTC))
                        statement.setObject(5, after.duelId)
                        statement.setInt(6, limit)
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

    // No `else`: a fourth DuelOutcomeLabel would fail to compile here rather than silently
    // filter nothing.
    private fun coinDeltaSignOf(outcome: DuelOutcomeLabel): Int =
        when (outcome) {
            DuelOutcomeLabel.WON -> 1
            DuelOutcomeLabel.LOST -> -1
            DuelOutcomeLabel.DREW -> 0
        }

    private companion object {
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
