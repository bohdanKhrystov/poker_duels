package duels.poker.server.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SchemaConstraintsTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun theSchemaHasTheThreeTables() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
                ).use { resultSet ->
                    val tables = mutableListOf<String>()
                    while (resultSet.next()) {
                        tables.add(resultSet.getString(1))
                    }
                    // Expect player, duel, duel_result, and flyway_schema_history
                    assertTrue(tables.contains("player"), "Expected 'player' table")
                    assertTrue(tables.contains("duel"), "Expected 'duel' table")
                    assertTrue(tables.contains("duel_result"), "Expected 'duel_result' table")
                    assertTrue(
                        tables.contains("flyway_schema_history"),
                        "Expected 'flyway_schema_history' table",
                    )
                }
            }
        }
    }

    @Test
    fun aSecondProfileForOneDeviceIdIsRejected() {
        val deviceId = "device-${UUID.randomUUID()}"
        insertPlayer(deviceId, 100)

        val exception = assertFailsWith<SQLException> {
            insertPlayer(deviceId, 50)
        }

        assertEquals("23505", exception.sqlState)
        assertTrue(
            exception.message?.contains("player_device_id_unique") ?: false,
            "Exception message should contain constraint name 'player_device_id_unique', got: ${exception.message}",
        )
    }

    @Test
    fun aSecondResultRowForOneDuelAndPlayerIsRejected() {
        val duelId = insertDuel()
        val playerId = insertPlayer("device-${UUID.randomUUID()}", 100)

        // Insert first result row
        insertDuelResult(duelId, playerId, 1)

        // Try to insert a second result row for the same (duel_id, player_id)
        val exception = assertFailsWith<SQLException> {
            insertDuelResult(duelId, playerId, -1)
        }

        assertEquals("23505", exception.sqlState)
        assertTrue(
            exception.message?.contains("duel_result_pkey") ?: false,
            "Exception message should contain constraint name 'duel_result_pkey', got: ${exception.message}",
        )
    }

    @Test
    fun aResultRowForAnUnknownDuelIsRejected() {
        val unknownDuelId = UUID.randomUUID()
        val playerId = insertPlayer("device-${UUID.randomUUID()}", 100)

        val exception = assertFailsWith<SQLException> {
            insertDuelResult(unknownDuelId, playerId, 1)
        }

        assertEquals("23503", exception.sqlState)
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
                "INSERT INTO duel (id, format, started_at, finished_at, hands_played) VALUES (?, 'FREEZEOUT', now(), now(), 1)",
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
}
