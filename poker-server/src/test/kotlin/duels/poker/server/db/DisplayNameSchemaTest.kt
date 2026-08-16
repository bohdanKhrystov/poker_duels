package duels.poker.server.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.text.Normalizer
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DisplayNameSchemaTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun aNameOfThirtyTwoCodePointsIsStored() {
        val name = "a".repeat(32)
        val playerId = insertPlayerWithName(name)

        val storedName = readDisplayName(playerId)
        assertEquals(name, storedName, "32-character name should be stored as-is")
    }

    @Test
    fun aNameOfThirtyThreeCodePointsIsRefused() {
        val name = "a".repeat(33)

        val exception = assertFailsWith<SQLException> {
            insertPlayerWithName(name)
        }

        assertEquals("23514", exception.sqlState)
        assertTrue(
            exception.message?.contains("player_display_name_length") ?: false,
            "Exception message should contain constraint name 'player_display_name_length', got: ${exception.message}",
        )
    }

    @Test
    fun theBoundIsCountedInCodePointsNotUtf16Units() {
        // U+1D504 (Mathematical Alphanumeric Symbol, requires 2 UTF-16 units) repeated 17 times
        // This is 17 code points but 34 UTF-16 units
        val name = "𝔄".repeat(17)
        val playerId = insertPlayerWithName(name)

        val storedName = readDisplayName(playerId)
        assertEquals(name, storedName, "17 astral characters should be stored")
    }

    @Test
    fun anEmptyNameIsRefused() {
        val exception = assertFailsWith<SQLException> {
            insertPlayerWithName("")
        }

        assertEquals("23514", exception.sqlState)
        assertTrue(
            exception.message?.contains("player_display_name_length") ?: false,
            "Exception message should contain constraint name 'player_display_name_length', got: ${exception.message}",
        )
    }

    @Test
    fun aNameWithLeadingOrTrailingSpaceIsRefused() {
        // Test leading space
        var exception = assertFailsWith<SQLException> {
            insertPlayerWithName(" bob")
        }

        assertEquals("23514", exception.sqlState)
        assertTrue(
            exception.message?.contains("player_display_name_trimmed") ?: false,
            "Exception message should contain constraint name 'player_display_name_trimmed', got: ${exception.message}",
        )

        // Test trailing space
        exception = assertFailsWith<SQLException> {
            insertPlayerWithName("bob ")
        }

        assertEquals("23514", exception.sqlState)
        assertTrue(
            exception.message?.contains("player_display_name_trimmed") ?: false,
            "Exception message should contain constraint name 'player_display_name_trimmed', got: ${exception.message}",
        )
    }

    @Test
    fun aDecomposedNameIsRefused() {
        // U+0065 (e) + U+0301 (combining acute accent) = decomposed form of é (NFD)
        val decomposedName = Normalizer.normalize("élodie", Normalizer.Form.NFD)

        val exception = assertFailsWith<SQLException> {
            insertPlayerWithName(decomposedName)
        }

        assertEquals("23514", exception.sqlState)
        assertTrue(
            exception.message?.contains("player_display_name_nfc") ?: false,
            "Exception message should contain constraint name 'player_display_name_nfc', got: ${exception.message}",
        )
    }

    @Test
    fun theComposedFormOfTheSameNameIsStored() {
        // U+00E9 (é) = composed form of é (NFC)
        val composedName = "élodie"
        val playerId = insertPlayerWithName(composedName)

        val storedName = readDisplayName(playerId)
        assertEquals(composedName, storedName, "Composed form should be stored as-is")
    }

    @Test
    fun manyProfilesWithNoNameCoexist() {
        // Insert three players without a display_name
        val playerId1 = insertPlayerWithoutName()
        val playerId2 = insertPlayerWithoutName()
        val playerId3 = insertPlayerWithoutName()

        // Verify all three can be retrieved
        val name1 = readDisplayName(playerId1)
        val name2 = readDisplayName(playerId2)
        val name3 = readDisplayName(playerId3)

        assertTrue(name1 == null, "First player should have NULL display_name")
        assertTrue(name2 == null, "Second player should have NULL display_name")
        assertTrue(name3 == null, "Third player should have NULL display_name")
    }

    private fun insertPlayerWithName(displayName: String): UUID {
        val playerId = UUID.randomUUID()
        val deviceId = "device-${UUID.randomUUID()}"
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, device_id, coin_balance, display_name) VALUES (?, ?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.setString(2, deviceId)
                statement.setInt(3, 100)
                statement.setString(4, displayName)
                statement.executeUpdate()
            }
        }
        return playerId
    }

    private fun insertPlayerWithoutName(): UUID {
        val playerId = UUID.randomUUID()
        val deviceId = "device-${UUID.randomUUID()}"
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, device_id, coin_balance) VALUES (?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.setString(2, deviceId)
                statement.setInt(3, 100)
                statement.executeUpdate()
            }
        }
        return playerId
    }

    private fun readDisplayName(playerId: UUID): String? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT display_name FROM player WHERE id = ?",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.getString(1)
                    }
                }
            }
        }
        return null
    }
}
