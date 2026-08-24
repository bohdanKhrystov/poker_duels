package duels.poker.server.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeviceBindingFinalityTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun revokingALiveBindingSucceedsOnce() {
        val playerId = bind("device-${UUID.randomUUID()}")

        val updated = dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE device_binding SET revoked_at = now() WHERE player_id = ? AND revoked_at IS NULL",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeUpdate()
            }
        }

        assertEquals(1, updated, "Revocation should update exactly one row")

        // Verify revoked_at is now set to a non-null value
        val revokedAt = dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT revoked_at FROM device_binding WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getObject(1, OffsetDateTime::class.java)
                }
            }
        }
        assertNotNull(revokedAt, "revoked_at should be set after revocation")
    }

    @Test
    fun unRevokingIsRefused() {
        val playerId = bind("device-${UUID.randomUUID()}")

        // First, revoke the binding
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE device_binding SET revoked_at = now() WHERE player_id = ? AND revoked_at IS NULL",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeUpdate()
            }
        }

        // Try to un-revoke (set revoked_at back to NULL)
        val exception = assertFailsWith<SQLException> {
            dataSource.connection.use { connection ->
                connection.prepareStatement(
                    "UPDATE device_binding SET revoked_at = NULL WHERE player_id = ?",
                ).use { statement ->
                    statement.setObject(1, playerId)
                    statement.executeUpdate()
                }
            }
        }

        assertEquals("23001", exception.sqlState, "Un-revoking should fail with restrict_violation")
        assertTrue(
            exception.message?.contains("a revoked device binding is final") ?: false,
            "Exception message should contain 'a revoked device binding is final'",
        )
    }

    @Test
    fun movingARevocationToADifferentTimestampIsRefused() {
        val playerId = bind("device-${UUID.randomUUID()}")

        // First, revoke the binding
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE device_binding SET revoked_at = now() WHERE player_id = ? AND revoked_at IS NULL",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeUpdate()
            }
        }

        // Try to change revoked_at to a different timestamp
        val exception = assertFailsWith<SQLException> {
            dataSource.connection.use { connection ->
                connection.prepareStatement(
                    "UPDATE device_binding SET revoked_at = now() + interval '1 hour' WHERE player_id = ?",
                ).use { statement ->
                    statement.setObject(1, playerId)
                    statement.executeUpdate()
                }
            }
        }

        assertEquals("23001", exception.sqlState, "Changing revocation timestamp should fail with restrict_violation")
    }

    @Test
    fun writingTheSameRevocationTimestampAgainIsAllowed() {
        val playerId = bind("device-${UUID.randomUUID()}")

        // First, revoke the binding
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE device_binding SET revoked_at = now() WHERE player_id = ? AND revoked_at IS NULL",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeUpdate()
            }
        }

        // Read the current revoked_at value
        val revokedAt = dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT revoked_at FROM device_binding WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getObject(1, OffsetDateTime::class.java)
                }
            }
        }

        // Write the same revoked_at value back
        val updated = dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE device_binding SET revoked_at = ? WHERE player_id = ?",
            ).use { statement ->
                statement.setObject(1, revokedAt)
                statement.setObject(2, playerId)
                statement.executeUpdate()
            }
        }

        assertEquals(1, updated, "Idempotent revocation update should succeed")
    }

    @Test
    fun theTriggerIsScopedToTheRevokedAtColumn() {
        // Query the pg_trigger catalog joined to pg_attribute through unnest(tgattr) for
        // device_binding's non-internal triggers. Should return exactly one row:
        // (device_binding_revocation_final, revoked_at)
        val triggerInfo = dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT t.tgname, a.attname
                FROM pg_trigger t
                JOIN pg_class c ON t.tgrelid = c.oid
                JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(t.tgattr)
                WHERE c.relname = 'device_binding' AND NOT t.tgisinternal
                ORDER BY t.tgname, a.attname
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val rows = mutableListOf<Pair<String, String>>()
                    while (resultSet.next()) {
                        rows.add(
                            resultSet.getString(1) to resultSet.getString(2),
                        )
                    }
                    rows
                }
            }
        }

        assertEquals(1, triggerInfo.size, "Should find exactly one trigger attribute")
        val (triggerName, attributeName) = triggerInfo.first()
        assertEquals("device_binding_revocation_final", triggerName, "Trigger name should be device_binding_revocation_final")
        assertEquals("revoked_at", attributeName, "Trigger should be scoped to revoked_at column")
    }

    private fun bind(deviceId: String): UUID {
        val playerId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, coin_balance) VALUES (?, 0)",
            ).use { statement ->
                statement.setObject(1, playerId)
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
}
