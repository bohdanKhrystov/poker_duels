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

/**
 * Pins `ADR-0049` §1's two partial unique indexes and §3's primary key: at most one live binding
 * per device (`device_binding_live_device`), at most one live binding per player
 * (`device_binding_live_player`), and a `(device_id, player_id)` pair that a revocation makes
 * permanent (`device_binding_pkey`). `TASK-040603`'s [DeviceBindingFinalityTest] owns the trigger
 * that makes a revocation itself irreversible; this class owns the three refusals a live `INSERT`
 * can meet, and proves each partial index blocks a live row without blocking a revoked one.
 */
class DeviceBindingUniquenessTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun aSecondLiveBindingForOneDeviceIsRefused() {
        val playerA = newPlayer()
        val playerB = newPlayer()
        bind("d-shared", playerA)

        val exception = assertFailsWith<SQLException> { bind("d-shared", playerB) }

        assertEquals("23505", exception.sqlState)
        assertTrue(
            exception.message?.contains("device_binding_live_device") ?: false,
            "Expected the message to name device_binding_live_device: ${exception.message}",
        )
    }

    @Test
    fun aSecondLiveBindingForOnePlayerIsRefused() {
        val player = newPlayer()
        bind("d-first", player)

        val exception = assertFailsWith<SQLException> { bind("d-second", player) }

        assertEquals("23505", exception.sqlState)
        assertTrue(
            exception.message?.contains("device_binding_live_player") ?: false,
            "Expected the message to name device_binding_live_player: ${exception.message}",
        )
    }

    @Test
    fun aRevokedBindingBlocksNeitherIndex() {
        val playerA = newPlayer()
        val playerB = newPlayer()
        bind("d-shared", playerA)
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE device_binding SET revoked_at = now() WHERE device_id = ? AND player_id = ?",
            ).use { statement ->
                statement.setString(1, "d-shared")
                statement.setObject(2, playerA)
                statement.executeUpdate()
            }
        }

        // Two distinct device ids and two distinct players, so neither insert can be explained
        // by the other's key: the first proves the device index is partial, the second proves
        // the player index is partial, and both proving it at once rules out one index covering
        // for a missing other.
        bind("d-shared", playerB)
        bind("d-second", playerA)

        val liveCount = dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT count(*) FROM device_binding WHERE revoked_at IS NULL",
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getLong(1)
                }
            }
        }
        assertEquals(2L, liveCount)
    }

    @Test
    fun theSamePairNeverBindsAgain() {
        val player = newPlayer()
        bind("d-a", player)
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE device_binding SET revoked_at = now() WHERE device_id = ? AND player_id = ?",
            ).use { statement ->
                statement.setString(1, "d-a")
                statement.setObject(2, player)
                statement.executeUpdate()
            }
        }

        val exception = assertFailsWith<SQLException> { bind("d-a", player) }

        assertEquals("23505", exception.sqlState)
        assertTrue(
            exception.message?.contains("device_binding_pkey") ?: false,
            "Expected the message to name device_binding_pkey: ${exception.message}",
        )
    }

    @Test
    fun aRevokedRowSurvivesTheRefusal() {
        val player = newPlayer()
        bind("d-a", player)
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE device_binding SET revoked_at = now() WHERE device_id = ? AND player_id = ?",
            ).use { statement ->
                statement.setString(1, "d-a")
                statement.setObject(2, player)
                statement.executeUpdate()
            }
        }

        assertFailsWith<SQLException> { bind("d-a", player) }

        val rowCount = dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT count(*) FROM device_binding WHERE device_id = ?",
            ).use { statement ->
                statement.setString(1, "d-a")
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getLong(1)
                }
            }
        }
        assertEquals(1L, rowCount, "The refused insert should not have written a second row")

        val revokedAt = dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT revoked_at FROM device_binding WHERE device_id = ?",
            ).use { statement ->
                statement.setString(1, "d-a")
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getObject(1, OffsetDateTime::class.java)
                }
            }
        }
        assertNotNull(revokedAt, "The surviving row should still be revoked")
    }

    private fun newPlayer(): UUID {
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

    private fun bind(deviceId: String, playerId: UUID) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO device_binding (device_id, player_id) VALUES (?, ?)",
            ).use { statement ->
                statement.setString(1, deviceId)
                statement.setObject(2, playerId)
                statement.executeUpdate()
            }
        }
    }
}
