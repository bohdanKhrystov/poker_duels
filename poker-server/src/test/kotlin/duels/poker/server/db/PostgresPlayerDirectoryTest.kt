package duels.poker.server.db

import duels.poker.server.session.DeviceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostgresPlayerDirectoryTest {
    private lateinit var dataSource: DataSource
    private lateinit var directory: PostgresPlayerDirectory

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        directory = PostgresPlayerDirectory(dataSource)
    }

    @Test
    fun resolvingANewDeviceCreatesExactlyOneRowWithThatId() = runBlocking {
        val deviceId = DeviceId("d1")

        val player = directory.resolve(deviceId)

        assertEquals(1, playerRowCount())
        assertEquals("d1", deviceRowDeviceId(UUID.fromString(player.id.value)))
        assertEquals(UUID.fromString(player.id.value), deviceRowUuid("d1"))
    }

    @Test
    fun resolvingTheSameDeviceTwiceReturnsTheSamePlayerId() = runBlocking {
        val deviceId = DeviceId("d1")

        val player1 = directory.resolve(deviceId)
        val player2 = directory.resolve(deviceId)

        assertEquals(player1.id, player2.id)
        assertEquals(1, playerRowCount())
    }

    @Test
    fun differentDevicesGetDifferentProfiles() = runBlocking {
        val deviceId1 = DeviceId("d1")
        val deviceId2 = DeviceId("d2")

        val player1 = directory.resolve(deviceId1)
        val player2 = directory.resolve(deviceId2)

        assertEquals(2, playerRowCount())
        assert(player1.id != player2.id)
    }

    @Test
    fun aNewProfileStartsAtAZeroBalance() = runBlocking {
        val deviceId = DeviceId("d1")

        val player = directory.resolve(deviceId)

        val balance = coinBalanceOf(UUID.fromString(player.id.value))
        assertEquals(0, balance)
    }

    @Test
    fun concurrentFirstContactFromManyConnectionsCreatesOneProfile() = runBlocking {
        // ADR-0012 puts one-profile-per-device in UNIQUE (device_id), so 16 simultaneous
        // inserts produce 15 conflicts that the ON CONFLICT clause resolves to the surviving row.
        // A directory that checked-then-inserted would create a second profile here.
        val deviceId = DeviceId("shared-device")
        val players = (1..16).map {
            async(Dispatchers.IO) {
                directory.resolve(deviceId)
            }
        }.awaitAll()

        assertEquals(1, playerRowCount())
        assertEquals(1, deviceBindingRowCount())
        val firstPlayerId = players.first().id
        players.forEach { player ->
            assertEquals(firstPlayerId, player.id)
        }
    }

    @Test
    fun findingAKnownDeviceAnswersTheSamePlayerResolveDid() = runBlocking {
        val deviceId1 = DeviceId("d1")
        val deviceId2 = DeviceId("d2")
        val resolved1 = directory.resolve(deviceId1)
        val resolved2 = directory.resolve(deviceId2)

        val found1 = directory.findOrNull(deviceId1)
        val found2 = directory.findOrNull(deviceId2)

        assertEquals(resolved1.id, found1?.id)
        assertEquals(resolved2.id, found2?.id)
        assert(found1?.id != found2?.id)
    }

    @Test
    fun findingAnUnknownDeviceIsNull() = runBlocking {
        val found = directory.findOrNull(DeviceId("ghost"))

        assertNull(found)
    }

    @Test
    fun findingAnUnknownDeviceCreatesNothing() = runBlocking {
        val deviceId = DeviceId("ghost")
        val before = playerRowCount()

        assertNull(directory.findOrNull(deviceId))
        assertEquals(before, playerRowCount())

        // A second lookup after the first proves the first call left no row behind for the
        // second call to find — the property a single call cannot distinguish from a fluke.
        assertNull(directory.findOrNull(deviceId))
        assertEquals(before, playerRowCount())
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

    private fun deviceBindingRowCount(): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM device_binding").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
    }

    private fun deviceRowDeviceId(playerId: UUID): String {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT device_id FROM device_binding WHERE player_id = ? AND revoked_at IS NULL",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getString(1)
                }
            }
        }
    }

    private fun deviceRowUuid(deviceId: String): UUID {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT player_id FROM device_binding WHERE device_id = ? AND revoked_at IS NULL",
            ).use { statement ->
                statement.setString(1, deviceId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getObject(1, UUID::class.java)
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
}
