package duels.poker.server.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DisplayNameUniquenessTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun aSecondPlayerCannotTakeAHeldName() {
        val name = "alice"
        val firstPlayerId = insertPlayerWithName(name)

        val exception = assertFailsWith<SQLException> {
            insertPlayerWithName(name)
        }

        assertEquals("23505", exception.sqlState)
        val storedName = readDisplayName(firstPlayerId)
        assertEquals(name, storedName, "First player's name should still be stored")
    }

    @Test
    fun theFoldIsCaseInsensitive() {
        val name1 = "Bob"
        val firstPlayerId = insertPlayerWithName(name1)

        val exception = assertFailsWith<SQLException> {
            insertPlayerWithName("bob")
        }

        assertEquals("23505", exception.sqlState)
        val storedName = readDisplayName(firstPlayerId)
        assertEquals(name1, storedName, "First player's name should be stored as typed")
    }

    @Test
    fun theFoldReachesBeyondAscii() {
        // Using Élodie (composed form with accented e) and élodie
        // A naive lower() without proper Unicode collation would not catch this collision
        val name1 = "Élodie"
        val firstPlayerId = insertPlayerWithName(name1)

        val exception = assertFailsWith<SQLException> {
            insertPlayerWithName("élodie")
        }

        assertEquals("23505", exception.sqlState)
        val storedName = readDisplayName(firstPlayerId)
        assertEquals(name1, storedName, "First player's name should be stored as typed")
    }

    @Test
    fun aDifferentNameIsAccepted() {
        val name1 = "Bob"
        val firstPlayerId = insertPlayerWithName(name1)

        val name2 = "Bobby"
        val secondPlayerId = insertPlayerWithName(name2)

        val storedName1 = readDisplayName(firstPlayerId)
        val storedName2 = readDisplayName(secondPlayerId)

        assertEquals(name1, storedName1, "First player's name should be stored")
        assertEquals(name2, storedName2, "Second player's different name should be stored")
    }

    @Test
    fun aHomoglyphIsADifferentName() {
        // Cyrillic 'а' (U+0430) versus Latin 'a' (U+0061) in names
        // These are visually similar but different characters and do not collide
        // See ADR-0038 — homoglyph impersonation is accepted as a residual,
        // not solved by the case-insensitive fold
        val latinName = "ace"
        val firstPlayerId = insertPlayerWithName(latinName)

        val cyrillicName = "асе" // Cyrillic а, Latin s and e
        val secondPlayerId = insertPlayerWithName(cyrillicName)

        val storedLatinName = readDisplayName(firstPlayerId)
        val storedCyrillicName = readDisplayName(secondPlayerId)

        assertEquals(latinName, storedLatinName, "Latin 'ace' should be stored")
        assertEquals(cyrillicName, storedCyrillicName, "Cyrillic variant should be stored as different name")
    }

    @Test
    fun anyNumberOfProfilesHaveNoName() {
        val firstPlayerId = insertPlayerWithoutName()
        val secondPlayerId = insertPlayerWithoutName()
        val thirdPlayerId = insertPlayerWithoutName()

        val firstName = readDisplayName(firstPlayerId)
        val secondName = readDisplayName(secondPlayerId)
        val thirdName = readDisplayName(thirdPlayerId)

        assertNull(firstName, "First unnamed player should have NULL display_name")
        assertNull(secondName, "Second unnamed player should have NULL display_name")
        assertNull(thirdName, "Third unnamed player should have NULL display_name")
    }

    @Test
    fun aRefusedNameLeavesTheLoserUnnamed() {
        val name = "champion"
        val firstPlayerId = insertPlayerWithName(name)

        val secondPlayerId = insertPlayerWithoutName()

        val exception = assertFailsWith<SQLException> {
            updatePlayerName(secondPlayerId, name)
        }

        assertEquals("23505", exception.sqlState)
        val loserName = readDisplayName(secondPlayerId)
        assertNull(loserName, "Loser's display_name should still be NULL after collision")

        // Verify the loser can now take a different name
        val differentName = "challenger"
        updatePlayerName(secondPlayerId, differentName)
        val updatedName = readDisplayName(secondPlayerId)
        assertEquals(differentName, updatedName, "Loser should be able to take a different name immediately")
    }

    private fun insertPlayerWithName(displayName: String): UUID {
        val playerId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')",
            ).use { statement ->
                statement.setString(1, displayName)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "INSERT INTO player (id, coin_balance, display_name) VALUES (?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.setInt(2, 100)
                statement.setString(3, displayName)
                statement.executeUpdate()
            }
        }
        return playerId
    }

    private fun insertPlayerWithoutName(): UUID {
        val playerId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, coin_balance) VALUES (?, ?)",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.setInt(2, 100)
                statement.executeUpdate()
            }
        }
        return playerId
    }

    private fun updatePlayerName(playerId: UUID, displayName: String) {
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
