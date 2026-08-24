package duels.poker.server.db

import duels.poker.server.session.DeviceId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// ADR-0049 §3: a revoked device may bind again, but only to a profile that does not yet exist --
// never back to the one it left, and never to any other pre-existing profile. TASK-040604 already
// proved a revoked row satisfies neither partial unique index, so it blocks nothing; this class
// proves the consequence -- what `resolve` and `findOrNull` actually read and write once one exists.
class PostgresPlayerDirectoryRevocationTest {
    private lateinit var dataSource: DataSource
    private lateinit var directory: PostgresPlayerDirectory

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        directory = PostgresPlayerDirectory(dataSource)
    }

    @Test
    fun findingARevokedDeviceIsNullWhileALiveOneIsFound() = runBlocking {
        val revokedDeviceId = DeviceId("d-revoked")
        val liveDeviceId = DeviceId("d-live")
        val revokedPlayer = directory.resolve(revokedDeviceId)
        val livePlayer = directory.resolve(liveDeviceId)

        revoke(UUID.fromString(revokedPlayer.id.value))

        assertNull(directory.findOrNull(revokedDeviceId))
        assertEquals(livePlayer.id, directory.findOrNull(liveDeviceId)?.id)
    }

    @Test
    fun resolvingARevokedDeviceMintsADifferentProfile() = runBlocking {
        val deviceId = DeviceId("d-revoked")
        val original = directory.resolve(deviceId)

        revoke(UUID.fromString(original.id.value))
        val reResolved = directory.resolve(deviceId)

        assertNotEquals(original.id, reResolved.id)
        assertEquals(2, playerRowCount())
        assertEquals(2, deviceBindingRowCountFor("d-revoked"))
    }

    @Test
    fun theAbandonedProfileKeepsItsCoins() = runBlocking {
        val deviceId = DeviceId("d-revoked")
        val original = directory.resolve(deviceId)
        val originalPlayerId = UUID.fromString(original.id.value)
        setBalance(originalPlayerId, 3)

        revoke(originalPlayerId)
        val reResolved = directory.resolve(deviceId)

        assertEquals(3, coinBalanceOf(originalPlayerId))
        assertEquals(0, coinBalanceOf(UUID.fromString(reResolved.id.value)))
    }

    @Test
    fun theRevokedBindingIsUntouchedByTheReResolve() = runBlocking {
        val deviceId = DeviceId("d-revoked")
        val original = directory.resolve(deviceId)
        val originalPlayerId = UUID.fromString(original.id.value)
        revoke(originalPlayerId)
        val boundAtBeforeReResolve = boundAtOf("d-revoked", originalPlayerId)

        directory.resolve(deviceId)

        assertNotNull(revokedAtOf("d-revoked", originalPlayerId))
        assertEquals(boundAtBeforeReResolve, boundAtOf("d-revoked", originalPlayerId))
    }

    @Test
    fun aSecondResolveOfTheRevokedDeviceIsIdempotent() = runBlocking {
        val deviceId = DeviceId("d-revoked")
        val original = directory.resolve(deviceId)
        revoke(UUID.fromString(original.id.value))
        val secondResolve = directory.resolve(deviceId)

        val thirdResolve = directory.resolve(deviceId)

        assertEquals(secondResolve.id, thirdResolve.id)
        assertEquals(2, playerRowCount())
    }

    // Raw SQL, deliberately: no Kotlin revoke path exists yet (TASK-040607 onward). This ticket is
    // about what `resolve` and `findOrNull` read, not about who writes `revoked_at`.
    private fun revoke(playerId: UUID) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE device_binding SET revoked_at = now() WHERE player_id = ? AND revoked_at IS NULL",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeUpdate()
            }
        }
    }

    private fun setBalance(playerId: UUID, balance: Int) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("UPDATE player SET coin_balance = ? WHERE id = ?").use { statement ->
                statement.setInt(1, balance)
                statement.setObject(2, playerId)
                statement.executeUpdate()
            }
        }
    }

    private fun playerRowCount(): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM player").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
    }

    private fun deviceBindingRowCountFor(deviceId: String): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT COUNT(*) FROM device_binding WHERE device_id = ?",
            ).use { statement ->
                statement.setString(1, deviceId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
    }

    private fun coinBalanceOf(playerId: UUID): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT coin_balance FROM player WHERE id = ?").use { statement ->
                statement.setObject(1, playerId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
    }

    private fun revokedAtOf(deviceId: String, playerId: UUID): Timestamp? {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT revoked_at FROM device_binding WHERE device_id = ? AND player_id = ?",
            ).use { statement ->
                statement.setString(1, deviceId)
                statement.setObject(2, playerId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getTimestamp(1)
                }
            }
        }
    }

    private fun boundAtOf(deviceId: String, playerId: UUID): Timestamp {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT bound_at FROM device_binding WHERE device_id = ? AND player_id = ?",
            ).use { statement ->
                statement.setString(1, deviceId)
                statement.setObject(2, playerId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getTimestamp(1)
                }
            }
        }
    }
}
