package duels.poker.server.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.sql.Types
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class RecoveryEmailSchemaTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun oneAddressBelongsToOnePlayerWhateverItsCase() {
        val player1Id = insertPlayer()
        val player2Id = insertPlayer()

        insertRecoveryEmail(player1Id, "Bob@example.com")

        val exception = assertFailsWith<SQLException> {
            insertRecoveryEmail(player2Id, "bob@EXAMPLE.com")
        }

        assertEquals("23505", exception.sqlState)
        assertTrue(
            exception.message?.contains("recovery_email_address_unique") ?: false,
            "Exception message should contain constraint name 'recovery_email_address_unique', got: ${exception.message}",
        )
    }

    @Test
    fun theStoredAddressKeepsTheCaseThePlayerTyped() {
        val playerId = insertPlayer()

        insertRecoveryEmail(playerId, "Bob@Example.com")

        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT address FROM recovery_email WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeQuery().use { resultSet ->
                    assertTrue(resultSet.next(), "Recovery email should exist")
                    val storedAddress = resultSet.getString(1)
                    assertEquals(
                        "Bob@Example.com",
                        storedAddress,
                        "Stored address should preserve the case the player typed",
                    )
                }
            }
        }
    }

    @Test
    fun aPlayerHoldsOnePendingAddressAtATime() {
        val playerId = insertPlayer()
        val tokenHash1 = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val tokenHash2 = byteArrayOf(0x05, 0x06, 0x07, 0x08)
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plusSeconds(86400)

        insertEmailVerification(tokenHash1, playerId, "test1@example.com", issuedAt, expiresAt)

        val exception = assertFailsWith<SQLException> {
            insertEmailVerification(tokenHash2, playerId, "test2@example.com", issuedAt, expiresAt)
        }

        assertEquals("23505", exception.sqlState)
        assertTrue(
            exception.message?.contains("email_verification_one_per_player") ?: false,
            "Exception message should contain constraint name 'email_verification_one_per_player', got: ${exception.message}",
        )
    }

    @Test
    fun aPlayerHoldsOneLiveResetTokenAtATime() {
        val playerId = insertPlayer()
        val tokenHash1 = byteArrayOf(0x09, 0x0a, 0x0b, 0x0c)
        val tokenHash2 = byteArrayOf(0x0d, 0x0e, 0x0f, 0x10)
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plusSeconds(3600)

        insertPasswordReset(tokenHash1, playerId, issuedAt, expiresAt)

        val exception = assertFailsWith<SQLException> {
            insertPasswordReset(tokenHash2, playerId, issuedAt, expiresAt)
        }

        assertEquals("23505", exception.sqlState)
        assertTrue(
            exception.message?.contains("password_reset_one_per_player") ?: false,
            "Exception message should contain constraint name 'password_reset_one_per_player', got: ${exception.message}",
        )
    }

    private fun insertPlayer(): UUID {
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

    private fun insertRecoveryEmail(
        playerId: UUID,
        address: String,
    ) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO recovery_email (player_id, address, verified_at) VALUES (?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.setString(2, address)
                statement.setObject(3, Instant.now().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                statement.executeUpdate()
            }
        }
    }

    private fun insertEmailVerification(
        tokenHash: ByteArray,
        playerId: UUID,
        address: String,
        issuedAt: Instant,
        expiresAt: Instant,
    ) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO email_verification (token_hash, player_id, address, issued_at, expires_at) VALUES (?, ?, ?, ?, ?)",
            ).use { statement ->
                statement.setBytes(1, tokenHash)
                statement.setObject(2, playerId)
                statement.setString(3, address)
                statement.setObject(4, issuedAt.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                statement.setObject(5, expiresAt.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                statement.executeUpdate()
            }
        }
    }

    private fun insertPasswordReset(
        tokenHash: ByteArray,
        playerId: UUID,
        issuedAt: Instant,
        expiresAt: Instant,
    ) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO password_reset (token_hash, player_id, issued_at, expires_at) VALUES (?, ?, ?, ?)",
            ).use { statement ->
                statement.setBytes(1, tokenHash)
                statement.setObject(2, playerId)
                statement.setObject(3, issuedAt.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                statement.setObject(4, expiresAt.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                statement.executeUpdate()
            }
        }
    }
}
