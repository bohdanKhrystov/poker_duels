package duels.poker.server.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals

/**
 * Proves that negative coin balances and coin deltas round-trip through PostgreSQL,
 * pinning ADR-0014's signed balance at the schema level.
 */
class CoinBalanceIsSignedTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun aBalanceOfMinusOneRoundTripsThroughTheDatabase() {
        val playerId = insertPlayer("device-1", -1)

        val readBalance = readPlayerCoinBalance(playerId)

        assertEquals(-1, readBalance)
    }

    @Test
    fun aCoinDeltaOfMinusOneRoundTripsThroughTheDatabase() {
        val playerId = insertPlayer("device-2", 0)
        val duelId = insertDuel()

        insertDuelResult(duelId, playerId, -1)

        val readDelta = readDuelResultCoinDelta(duelId, playerId)

        assertEquals(-1, readDelta)
    }

    @Test
    fun theBalanceIsNotFlooredAtZero() {
        val playerId = insertPlayer("device-3", 0)

        updatePlayerCoinBalance(playerId, -10)

        val readBalance = readPlayerCoinBalance(playerId)

        assertEquals(-10, readBalance)
    }

    private fun insertPlayer(deviceId: String, coinBalance: Int): UUID {
        val playerId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, device_id, coin_balance) VALUES (?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.setString(2, deviceId)
                statement.setInt(3, coinBalance)
                statement.executeUpdate()
            }
        }
        return playerId
    }

    private fun insertDuel(): UUID {
        val duelId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO duel (id, format, started_at, finished_at) VALUES (?, 'FREEZEOUT', now(), now())",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.executeUpdate()
            }
        }
        return duelId
    }

    private fun insertDuelResult(duelId: UUID, playerId: UUID, coinDelta: Int) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO duel_result (duel_id, player_id, coin_delta) VALUES (?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.setObject(2, playerId)
                statement.setInt(3, coinDelta)
                statement.executeUpdate()
            }
        }
    }

    private fun readPlayerCoinBalance(playerId: UUID): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT coin_balance FROM player WHERE id = ?",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
    }

    private fun readDuelResultCoinDelta(duelId: UUID, playerId: UUID): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT coin_delta FROM duel_result WHERE duel_id = ? AND player_id = ?",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.setObject(2, playerId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
    }

    private fun updatePlayerCoinBalance(playerId: UUID, coinBalance: Int) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE player SET coin_balance = ? WHERE id = ?",
            ).use { statement ->
                statement.setInt(1, coinBalance)
                statement.setObject(2, playerId)
                statement.executeUpdate()
            }
        }
    }
}
