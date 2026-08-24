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
            exception.message?.contains("device_binding_live_device") ?: false,
            "Exception message should contain constraint name 'device_binding_live_device', got: ${exception.message}",
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

    @Test
    fun aDisplayNameLongerThan32CharactersIsRejected() {
        val deviceId = "device-${UUID.randomUUID()}"
        val playerId = insertPlayer(deviceId, 100)

        val tooLongName = "a".repeat(33)

        val exception = assertFailsWith<SQLException> {
            setDisplayName(playerId, tooLongName)
        }

        assertEquals("23514", exception.sqlState)
        assertTrue(
            exception.message?.contains("player_display_name_length") ?: false,
            "Exception message should contain constraint name 'player_display_name_length', got: ${exception.message}",
        )
    }

    @Test
    fun anEmptyDisplayNameIsRejected() {
        val deviceId = "device-${UUID.randomUUID()}"
        val playerId = insertPlayer(deviceId, 100)

        val exception = assertFailsWith<SQLException> {
            setDisplayName(playerId, "")
        }

        assertEquals("23514", exception.sqlState)
        assertTrue(
            exception.message?.contains("player_display_name_length") ?: false,
            "Exception message should contain constraint name 'player_display_name_length', got: ${exception.message}",
        )
    }

    @Test
    fun aDisplayNameWithLeadingWhitespaceIsRejected() {
        val deviceId = "device-${UUID.randomUUID()}"
        val playerId = insertPlayer(deviceId, 100)

        val exception = assertFailsWith<SQLException> {
            setDisplayName(playerId, "  Bob")
        }

        assertEquals("23514", exception.sqlState)
        assertTrue(
            exception.message?.contains("player_display_name_trimmed") ?: false,
            "Exception message should contain constraint name 'player_display_name_trimmed', got: ${exception.message}",
        )
    }

    @Test
    fun aDisplayNameWithTrailingWhitespaceIsRejected() {
        val deviceId = "device-${UUID.randomUUID()}"
        val playerId = insertPlayer(deviceId, 100)

        val exception = assertFailsWith<SQLException> {
            setDisplayName(playerId, "Bob  ")
        }

        assertEquals("23514", exception.sqlState)
        assertTrue(
            exception.message?.contains("player_display_name_trimmed") ?: false,
            "Exception message should contain constraint name 'player_display_name_trimmed', got: ${exception.message}",
        )
    }

    @Test
    fun aNonNFCNormalizedDisplayNameIsRejected() {
        val deviceId = "device-${UUID.randomUUID()}"
        val playerId = insertPlayer(deviceId, 100)

        // U+0065 (e) + U+0301 (combining acute accent) = decomposed form of é (NFD)
        val nonNFCNormalizedName = "élodie"

        val exception = assertFailsWith<SQLException> {
            setDisplayName(playerId, nonNFCNormalizedName)
        }

        assertEquals("23514", exception.sqlState)
        assertTrue(
            exception.message?.contains("player_display_name_nfc") ?: false,
            "Exception message should contain constraint name 'player_display_name_nfc', got: ${exception.message}",
        )
    }

    @Test
    fun twoDisplayNamesDifferingOnlyByCaseAreRefusedByTheRegistry() {
        val deviceId1 = "device-${UUID.randomUUID()}"
        val deviceId2 = "device-${UUID.randomUUID()}"

        val playerId1 = insertPlayer(deviceId1, 100)
        val playerId2 = insertPlayer(deviceId2, 100)

        // Register and hand over the first name
        registerAndSetDisplayName(playerId1, "Bob")

        // Try to register a second name differing only by case — should be refused by the registry
        val exception = assertFailsWith<SQLException> {
            registerAndSetDisplayName(playerId2, "bob")
        }

        assertEquals("23505", exception.sqlState)
        assertTrue(
            exception.message?.contains("name_registry_folded") ?: false,
            "Exception message should contain constraint name 'name_registry_folded', got: ${exception.message}",
        )

        val holderName = dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT display_name FROM player WHERE id = ?",
            ).use { statement ->
                statement.setObject(1, playerId1)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getString(1)
                }
            }
        }
        assertEquals(
            "Bob",
            holderName,
            "Player 2's refused claim must not have disturbed the name player 1 already holds",
        )
    }

    @Test
    fun anUpdateToAnAlreadySetDisplayNameIsRejected() {
        val deviceId = "device-${UUID.randomUUID()}"
        val playerId = insertPlayer(deviceId, 100)

        // Register and hand over the initial name
        registerAndSetDisplayName(playerId, "Bob")

        // Try to update to a different name — should be rejected by trigger
        val exception = assertFailsWith<SQLException> {
            setDisplayName(playerId, "Alice")
        }

        assertEquals("23001", exception.sqlState)
        assertTrue(
            exception.message?.contains("display_name is permanent once set") ?: false,
            "Exception message should contain trigger message 'display_name is permanent once set', got: ${exception.message}",
        )
    }

    // Two statements, not one: the schema no longer has anywhere on `player` to name a device, so
    // the live binding this test collides on lives in `device_binding`, one insert down.
    private fun insertPlayer(deviceId: String, coinBalance: Int): UUID {
        val playerId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, coin_balance) VALUES (?, ?)",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.setInt(2, coinBalance)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "INSERT INTO device_binding (device_id, player_id) VALUES (?, ?)",
            ).use { statement ->
                statement.setString(1, deviceId)
                statement.setObject(2, playerId)
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

    private fun setDisplayName(playerId: UUID, displayName: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE player SET display_name = ? WHERE id = ?",
            ).use { statement ->
                statement.setString(1, displayName)
                statement.setObject(2, playerId)
                statement.executeUpdate()
            }
        }
    }

    // ADR-0051 §2: the registry spends the string before the player is handed the name — no
    // ON CONFLICT, so a collision on name_registry_folded raises rather than being swallowed.
    private fun registerAndSetDisplayName(playerId: UUID, displayName: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')",
            ).use { statement ->
                statement.setString(1, displayName)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "UPDATE player SET display_name = ? WHERE id = ?",
            ).use { statement ->
                statement.setString(1, displayName)
                statement.setObject(2, playerId)
                statement.executeUpdate()
            }
        }
    }
}
