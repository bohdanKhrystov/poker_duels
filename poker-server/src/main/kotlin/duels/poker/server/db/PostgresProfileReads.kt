package duels.poker.server.db

import duels.poker.server.http.ProfileReads
import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.outcomeOf
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
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
            connection.prepareStatement("SELECT id, coin_balance FROM player WHERE device_id = ?")
                .use { statement ->
                    statement.setString(1, deviceId.value)
                    statement.executeQuery().use { rows ->
                        if (rows.next()) ProfileResponse(rows.getString(1), rows.getInt(2)) else null
                    }
                }
        }
    }

    override suspend fun recentDuelsOf(playerId: PlayerId, limit: Int): List<DuelSummaryResponse> =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { connection ->
                connection.prepareStatement(RECENT_DUELS_SQL).use { statement ->
                    statement.setObject(1, UUID.fromString(playerId.value))
                    statement.setInt(2, limit)
                    statement.executeQuery().use { rows ->
                        val duels = mutableListOf<DuelSummaryResponse>()
                        while (rows.next()) {
                            val coinDelta = rows.getInt("coin_delta")
                            duels +=
                                DuelSummaryResponse(
                                    duelId = rows.getObject("duel_id", UUID::class.java).toString(),
                                    opponentPlayerId = rows.getObject("opponent_id", UUID::class.java).toString(),
                                    outcome = outcomeOf(coinDelta),
                                    coinDelta = coinDelta,
                                    // The `duel` table has no `hands_played` column; `DEC-014` decides
                                    // whether it gains one. No number is invented here, and no other
                                    // column stands in for it.
                                    handsPlayed = null,
                                    finishedAt = rows.getObject("finished_at", OffsetDateTime::class.java)
                                        .toInstant()
                                        .toString(),
                                )
                        }
                        duels
                    }
                }
            }
        }

    private companion object {
        // ADR-0015: every participant of every completed duel has exactly one `duel_result`
        // row, including both rows of a draw (`coin_delta = 0`). That is what makes this
        // self-join safe: the opponent's row always exists, so a drawn duel is not a special
        // case and needs no `coin_delta <> 0` filter that would silently drop it.
        private const val RECENT_DUELS_SQL =
            """
            SELECT d.id AS duel_id,
                   o.player_id AS opponent_id,
                   r.coin_delta AS coin_delta,
                   d.finished_at AS finished_at
            FROM duel_result r
            JOIN duel d ON d.id = r.duel_id
            JOIN duel_result o ON o.duel_id = r.duel_id AND o.player_id <> r.player_id
            WHERE r.player_id = ?
            ORDER BY d.finished_at DESC, d.id DESC
            LIMIT ?
            """
    }
}
