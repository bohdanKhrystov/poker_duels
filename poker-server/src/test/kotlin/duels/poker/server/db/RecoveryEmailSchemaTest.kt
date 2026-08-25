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
import kotlin.test.assertNull
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

    @Test
    fun theAddressIndexIsPinnedToTheIcuRootCollation() {
        // Query pg_index joined by the *table*, never the index name: one row per index key,
        // via unnest(indcollation) WITH ORDINALITY, LEFT JOINed to pg_collation so a
        // non-collatable key (collation OID 0, recovery_email_pkey's UUID) stays a row instead
        // of being dropped by an INNER JOIN and silently corrupting the count below.
        val indexKeys =
            dataSource.connection.use { connection ->
                connection.prepareStatement(
                    """
                    SELECT ic.relname AS index_name,
                           k.ord AS key_position,
                           coll.collname,
                           coll.collprovider::text AS coll_provider
                    FROM pg_index i
                    JOIN pg_class ic ON ic.oid = i.indexrelid
                    JOIN pg_class tc ON tc.oid = i.indrelid
                    CROSS JOIN LATERAL unnest(i.indcollation) WITH ORDINALITY AS k(collation_oid, ord)
                    LEFT JOIN pg_collation coll ON coll.oid = k.collation_oid
                    WHERE tc.relname = 'recovery_email'
                    ORDER BY ic.relname, k.ord
                    """.trimIndent(),
                ).use { statement ->
                    statement.executeQuery().use { resultSet ->
                        val rows = mutableListOf<IndexKeyCollation>()
                        while (resultSet.next()) {
                            rows.add(
                                IndexKeyCollation(
                                    indexName = resultSet.getString("index_name"),
                                    keyPosition = resultSet.getInt("key_position"),
                                    collationName = resultSet.getString("collname"),
                                    collationProvider = resultSet.getString("coll_provider"),
                                ),
                            )
                        }
                        rows
                    }
                }
            }

        // Count and names, read from the catalog, before any collation is examined: the
        // vacuity guard. A filter that matched nothing would satisfy neither of these.
        assertEquals(
            2,
            indexKeys.size,
            "Expected exactly one collation-bearing key per index on recovery_email, found $indexKeys",
        )
        assertEquals(
            setOf("recovery_email_address_unique", "recovery_email_pkey"),
            indexKeys.map { it.indexName }.toSet(),
        )

        val addressUniqueKey = indexKeys.single { it.indexName == "recovery_email_address_unique" }
        assertEquals(
            "und-x-icu",
            addressUniqueKey.collationName,
            "recovery_email_address_unique should be pinned to the ICU root collation",
        )
        assertEquals(
            "i",
            addressUniqueKey.collationProvider,
            "recovery_email_address_unique's collation provider should be ICU ('i')",
        )

        val primaryKeyKey = indexKeys.single { it.indexName == "recovery_email_pkey" }
        assertNull(primaryKeyKey.collationName, "recovery_email_pkey is on a UUID and should carry no collation")
        assertNull(primaryKeyKey.collationProvider, "recovery_email_pkey is on a UUID and should carry no collation")
    }

    @Test
    fun twoSpellingsOnlyIcuFoldsTogetherAreOneAddress() {
        val player1Id = insertPlayer()
        val player2Id = insertPlayer()

        // U+0130 LATIN CAPITAL LETTER I WITH DOT ABOVE, i.e. "\u0130@example.com". Written as an
        // escape, not the literal glyph, so the fixture survives any editor.
        insertRecoveryEmail(player1Id, "\u0130@example.com")

        // "i" (U+0069) followed by U+0307 COMBINING DOT ABOVE, i.e. "i\u0307@example.com" -- the
        // exact sequence "und-x-icu" folds U+0130 to. The container's default fold (musl libc)
        // instead folds U+0130 to a bare "i", so only the pinned collation collides this pair.
        val exception =
            assertFailsWith<SQLException> {
                insertRecoveryEmail(player2Id, "i\u0307@example.com")
            }

        assertEquals("23505", exception.sqlState)
        assertTrue(
            exception.message?.contains("recovery_email_address_unique") ?: false,
            "Exception message should contain constraint name 'recovery_email_address_unique', got: ${exception.message}",
        )
    }

    private data class IndexKeyCollation(
        val indexName: String,
        val keyPosition: Int,
        val collationName: String?,
        val collationProvider: String?,
    )

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
