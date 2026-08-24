package duels.poker.server.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlayerNameIsRegisteredTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun anUnregisteredNameCannotBeHeld() {
        val playerId = insertPlayerWithoutName()

        val exception = assertFailsWith<SQLException> {
            setDisplayName(playerId, "Ghost")
        }

        assertEquals("23503", exception.sqlState)
        assertTrue(
            exception.message?.contains("player_display_name_registered") ?: false,
            "Exception message should contain constraint name 'player_display_name_registered', got: ${exception.message}",
        )
    }

    // Without this, anUnregisteredNameCannotBeHeld passes against a database in which every write
    // fails -- the one wrong implementation a foreign-key test cannot otherwise tell apart.
    @Test
    fun aRegisteredNameCanBeHeld() {
        val playerId = insertPlayerWithoutName()
        registerName("Ghost", "TAKEN")

        setDisplayName(playerId, "Ghost")

        assertEquals("Ghost", readDisplayName(playerId))
    }

    // The key constrains the string, not the reason: a BLOCKED row satisfies it exactly as a TAKEN
    // one does. Screening is the write path's (ADR-0051 §2), not the schema's.
    @Test
    fun aBlockedNameIsHoldableAtTheSchemaLevel() {
        val playerId = insertPlayerWithoutName()
        registerName("Slur", "BLOCKED")

        setDisplayName(playerId, "Slur")

        assertEquals("Slur", readDisplayName(playerId))
    }

    @Test
    fun theKeyTakesNoActionOnDelete() {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT confdeltype FROM pg_constraint WHERE conname = 'player_display_name_registered'",
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    assertTrue(resultSet.next(), "player_display_name_registered constraint should exist")
                    assertEquals("a", resultSet.getString(1))
                }
            }
        }
    }

    private fun insertPlayerWithoutName(): UUID {
        val playerId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, coin_balance) VALUES (?, 0)",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeUpdate()
            }
        }
        return playerId
    }

    private fun registerName(name: String, reason: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO name_registry (name, reason) VALUES (?, ?)",
            ).use { statement ->
                statement.setString(1, name)
                statement.setString(2, reason)
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
