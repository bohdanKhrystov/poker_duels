package duels.poker.server.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CredentialSchemaTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun aSecondCredentialWithTheSameKindAndIdentifierIsRefused() {
        val playerId = insertPlayer("device-${UUID.randomUUID()}")
        insertCredential(playerId, "passkey", "user@example.com", "hash123")

        val exception = assertFailsWith<SQLException> {
            insertCredential(playerId, "passkey", "user@example.com", "hash456")
        }

        assertEquals("23505", exception.sqlState)
        assertTrue(
            exception.message?.contains("credential_kind_identifier_unique") ?: false,
            "Exception message should contain constraint name 'credential_kind_identifier_unique', got: ${exception.message}",
        )
    }

    @Test
    fun theSameIdentifierUnderADifferentKindIsAccepted() {
        val playerId = insertPlayer("device-${UUID.randomUUID()}")
        val identifier = "user@example.com"

        // First insert with kind = 'passkey'
        insertCredential(playerId, "passkey", identifier, "hash123")

        // Second insert with the same identifier but different kind = 'email' should succeed
        insertCredential(playerId, "email", identifier, "hash456")

        // Verify both rows exist
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT COUNT(*) FROM credential WHERE player_id = ? AND identifier = ?",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.setString(2, identifier)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    assertEquals(2, resultSet.getInt(1), "Expected 2 credentials with the same identifier")
                }
            }
        }
    }

    @Test
    fun aCredentialForAPlayerThatDoesNotExistIsRefused() {
        val nonExistentPlayerId = UUID.randomUUID()

        val exception = assertFailsWith<SQLException> {
            insertCredential(nonExistentPlayerId, "passkey", "user@example.com", "hash123")
        }

        assertEquals("23503", exception.sqlState)
    }

    @Test
    fun deletingAPlayerThatHoldsACredentialIsRefused() {
        val playerId = insertPlayer("device-${UUID.randomUUID()}")
        insertCredential(playerId, "passkey", "user@example.com", "hash123")

        val exception = assertFailsWith<SQLException> {
            deletePlayer(playerId)
        }

        assertEquals("23503", exception.sqlState)
    }

    @Test
    fun aCredentialRowMayHaveNoSecretHash() {
        val playerId = insertPlayer("device-${UUID.randomUUID()}")
        insertCredential(playerId, "passkey", "user@example.com", null)

        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT secret_hash FROM credential WHERE player_id = ? AND identifier = ?",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.setString(2, "user@example.com")
                statement.executeQuery().use { resultSet ->
                    assertTrue(resultSet.next(), "Credential row should exist")
                    assertNull(resultSet.getString(1), "secret_hash should be NULL")
                }
            }
        }
    }

    private fun insertPlayer(deviceId: String): UUID {
        val playerId = UUID.randomUUID()
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

    private fun insertCredential(
        playerId: UUID,
        kind: String,
        identifier: String,
        secretHash: String?,
    ) {
        val credentialId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO credential (id, player_id, kind, identifier, secret_hash) VALUES (?, ?, ?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, credentialId)
                statement.setObject(2, playerId)
                statement.setString(3, kind)
                statement.setString(4, identifier)
                if (secretHash != null) {
                    statement.setString(5, secretHash)
                } else {
                    statement.setNull(5, java.sql.Types.VARCHAR)
                }
                statement.executeUpdate()
            }
        }
    }

    private fun deletePlayer(playerId: UUID) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "DELETE FROM player WHERE id = ?",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeUpdate()
            }
        }
    }
}
