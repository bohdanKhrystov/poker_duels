package duels.poker.server.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.sql.Types
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AuthSessionSchemaTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun aSecondRowWithTheSameTokenHashIsRefused() {
        val playerId = insertPlayer("device-${UUID.randomUUID()}")
        val tokenHash = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plusSeconds(3600)

        insertAuthSession(tokenHash, playerId, issuedAt, expiresAt)

        val exception = assertFailsWith<SQLException> {
            insertAuthSession(tokenHash, playerId, issuedAt, expiresAt)
        }

        assertEquals("23505", exception.sqlState)
        assertTrue(
            exception.message?.contains("auth_session_pkey") ?: false,
            "Exception message should contain constraint name 'auth_session_pkey', got: ${exception.message}",
        )
    }

    @Test
    fun twoRowsWithDifferentTokenHashesForOnePlayerAreAccepted() {
        val playerId = insertPlayer("device-${UUID.randomUUID()}")
        val tokenHash1 = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val tokenHash2 = byteArrayOf(0x05, 0x06, 0x07, 0x08)
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plusSeconds(3600)

        // Insert first session with the same player_id
        insertAuthSession(tokenHash1, playerId, issuedAt, expiresAt)

        // Insert second session with the same player_id but different token_hash
        insertAuthSession(tokenHash2, playerId, issuedAt, expiresAt)

        // Verify both rows with same player_id exist and have correct token hashes
        dataSource.connection.use { connection ->
            // First verify the count
            connection.prepareStatement(
                "SELECT COUNT(*) FROM auth_session WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    assertEquals(2, resultSet.getInt(1), "Expected 2 sessions for the same player")
                }
            }

            // Then verify the token hashes are correctly stored
            connection.prepareStatement(
                "SELECT token_hash FROM auth_session WHERE player_id = ? ORDER BY issued_at",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeQuery().use { resultSet ->
                    assertTrue(resultSet.next(), "First session should exist")
                    val retrievedHash1 = resultSet.getBytes(1)
                    assertContentEquals(
                        tokenHash1,
                        retrievedHash1,
                        "First session's token_hash should match what was written",
                    )

                    assertTrue(resultSet.next(), "Second session should exist")
                    val retrievedHash2 = resultSet.getBytes(1)
                    assertContentEquals(
                        tokenHash2,
                        retrievedHash2,
                        "Second session's token_hash should match what was written",
                    )
                }
            }
        }
    }

    @Test
    fun anAuthSessionForAPlayerThatDoesNotExistIsRefused() {
        val nonExistentPlayerId = UUID.randomUUID()
        val tokenHash = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plusSeconds(3600)

        val exception = assertFailsWith<SQLException> {
            insertAuthSession(tokenHash, nonExistentPlayerId, issuedAt, expiresAt)
        }

        assertEquals("23503", exception.sqlState)
    }

    @Test
    fun deletingAPlayerThatHoldsAnAuthSessionIsRefused() {
        val playerId = insertPlayer("device-${UUID.randomUUID()}")
        val tokenHash = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plusSeconds(3600)

        insertAuthSession(tokenHash, playerId, issuedAt, expiresAt)

        val exception = assertFailsWith<SQLException> {
            deletePlayer(playerId)
        }

        assertEquals("23503", exception.sqlState)
    }

    @Test
    fun noForeignKeyOnTheTwoNewTablesHasAnOnDeleteAction() {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT c.conname, c.confdeltype
                FROM pg_constraint c
                JOIN pg_class t ON t.oid = c.conrelid
                WHERE c.contype = 'f' AND t.relname IN ('credential', 'auth_session')
                ORDER BY c.conname
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val constraints = mutableListOf<Pair<String, Char>>()
                    while (resultSet.next()) {
                        val conname = resultSet.getString(1)
                        val confdeltype = resultSet.getString(2)[0]
                        constraints.add(conname to confdeltype)
                    }

                    assertEquals(2, constraints.size, "Expected exactly 2 foreign keys, got ${constraints.size}")

                    // Verify every constraint has NO ACTION ('a')
                    for ((conname, confdeltype) in constraints) {
                        assertEquals(
                            'a',
                            confdeltype,
                            "Foreign key '$conname' should have NO ACTION ('a'), got '$confdeltype'",
                        )
                    }

                    // Verify the constraint names are the expected ones
                    val expectedNames = setOf(
                        "credential_player_id_fkey",
                        "auth_session_player_id_fkey",
                    )
                    val actualNames = constraints.map { it.first }.toSet()
                    assertEquals(
                        expectedNames,
                        actualNames,
                        "Expected constraint names $expectedNames, got $actualNames",
                    )
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

    private fun insertAuthSession(
        tokenHash: ByteArray,
        playerId: UUID,
        issuedAt: Instant,
        expiresAt: Instant,
    ) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO auth_session (token_hash, player_id, issued_at, expires_at) VALUES (?, ?, ?, ?)",
            ).use { statement ->
                statement.setBytes(1, tokenHash)
                statement.setObject(2, playerId)
                statement.setObject(3, issuedAt.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
                statement.setObject(4, expiresAt.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
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
