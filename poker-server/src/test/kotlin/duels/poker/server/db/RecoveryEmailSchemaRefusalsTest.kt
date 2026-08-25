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

/**
 * `V8` deliberately withholds two things: a unique index on a pending address, and any
 * `ON DELETE` clause on the three recovery tables. Both are refusals, and a refusal produces no
 * assertion unless something holds it — these tests hold them, so adding either back later fails
 * a build instead of reading as a tidy-up.
 */
internal class RecoveryEmailSchemaRefusalsTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun twoPlayersMayBothHoldAPendingClaimOnOneAddress() {
        val player1Id = insertPlayer()
        val player2Id = insertPlayer()
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plusSeconds(86400)
        val address = "squatted@example.com"

        insertEmailVerification(byteArrayOf(0x01, 0x02), player1Id, address, issuedAt, expiresAt)
        insertEmailVerification(byteArrayOf(0x03, 0x04), player2Id, address, issuedAt, expiresAt)

        assertEquals(setOf(player1Id, player2Id), pendingClaimants(address))
    }

    @Test
    fun oneAddressIsStillOwnedByOnlyOneVerifiedPlayer() {
        val player1Id = insertPlayer()
        val player2Id = insertPlayer()
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plusSeconds(86400)
        val address = "squatted@example.com"

        insertEmailVerification(byteArrayOf(0x01, 0x02), player1Id, address, issuedAt, expiresAt)
        insertEmailVerification(byteArrayOf(0x03, 0x04), player2Id, address, issuedAt, expiresAt)

        insertRecoveryEmail(player1Id, address)

        val exception =
            assertFailsWith<SQLException> {
                insertRecoveryEmail(player2Id, address)
            }

        assertEquals("23505", exception.sqlState)
    }

    @Test
    fun noRecoveryTableCascadesOnDelete() {
        val recoveryTables = setOf("recovery_email", "email_verification", "password_reset")
        val foreignKeys = referentialConstraintsOn(recoveryTables)

        assertEquals(
            3,
            foreignKeys.size,
            "Expected exactly one foreign key per table in $recoveryTables, found $foreignKeys",
        )
        assertEquals(
            setOf(
                "recovery_email_player_id_fkey",
                "email_verification_player_id_fkey",
                "password_reset_player_id_fkey",
            ),
            foreignKeys.map { it.constraintName }.toSet(),
        )

        for (foreignKey in foreignKeys) {
            assertEquals(
                "NO ACTION",
                foreignKey.deleteRule,
                "Expected NO ACTION for ${foreignKey.constraintName} on ${foreignKey.tableName}, " +
                    "got ${foreignKey.deleteRule}",
            )
        }
    }

    private data class ForeignKey(
        val tableName: String,
        val constraintName: String,
        val deleteRule: String,
    )

    private fun referentialConstraintsOn(tableNames: Set<String>): List<ForeignKey> {
        val foreignKeys = mutableListOf<ForeignKey>()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT tc.table_name, tc.constraint_name, rc.delete_rule
                  FROM information_schema.referential_constraints rc
                  JOIN information_schema.table_constraints tc
                    ON tc.constraint_name = rc.constraint_name
                   AND tc.constraint_schema = rc.constraint_schema
                 WHERE tc.table_name = ANY (?)
                """.trimIndent(),
            ).use { statement ->
                statement.setArray(1, connection.createArrayOf("text", tableNames.toTypedArray()))
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        foreignKeys.add(
                            ForeignKey(
                                tableName = resultSet.getString("table_name"),
                                constraintName = resultSet.getString("constraint_name"),
                                deleteRule = resultSet.getString("delete_rule"),
                            ),
                        )
                    }
                }
            }
        }
        return foreignKeys
    }

    private fun pendingClaimants(address: String): Set<UUID> {
        val claimants = mutableSetOf<UUID>()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT player_id FROM email_verification WHERE address = ?",
            ).use { statement ->
                statement.setString(1, address)
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        claimants.add(resultSet.getObject(1, UUID::class.java))
                    }
                }
            }
        }
        return claimants
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
                "INSERT INTO email_verification (token_hash, player_id, address, issued_at, expires_at) " +
                    "VALUES (?, ?, ?, ?, ?)",
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
}
