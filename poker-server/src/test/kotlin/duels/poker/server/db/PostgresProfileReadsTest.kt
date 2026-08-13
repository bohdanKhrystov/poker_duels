package duels.poker.server.db

import duels.poker.server.session.DeviceId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class PostgresProfileReadsTest {
    private lateinit var dataSource: DataSource
    private lateinit var playerDirectory: PostgresPlayerDirectory
    private lateinit var profileReads: PostgresProfileReads

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        playerDirectory = PostgresPlayerDirectory(dataSource)
        profileReads = PostgresProfileReads(dataSource)
    }

    @Test
    fun aKnownDeviceReadsBackItsProfileAtZero() = runBlocking {
        val deviceId = DeviceId("alice")
        val player = playerDirectory.resolve(deviceId)

        val profile = profileReads.profileOf(deviceId)

        assertEquals(player.id.value, profile?.playerId)
        assertEquals(0, profile?.coinBalance)
    }

    @Test
    fun anUnknownDeviceReadsBackNull() = runBlocking {
        val profile = profileReads.profileOf(DeviceId("ghost"))

        assertNull(profile)
    }

    @Test
    fun readingAnUnknownDeviceCreatesNoProfile() = runBlocking {
        val countBefore = playerRowCount()

        profileReads.profileOf(DeviceId("ghost"))

        val countAfter = playerRowCount()
        assertEquals(countBefore, countAfter)
    }

    @Test
    fun twoDevicesReadBackTheirOwnProfiles() = runBlocking {
        val aliceDeviceId = DeviceId("alice")
        val bobDeviceId = DeviceId("bob")
        val alice = playerDirectory.resolve(aliceDeviceId)
        val bob = playerDirectory.resolve(bobDeviceId)

        val aliceProfile = profileReads.profileOf(aliceDeviceId)
        val bobProfile = profileReads.profileOf(bobDeviceId)

        assertEquals(alice.id.value, aliceProfile?.playerId)
        assertEquals(bob.id.value, bobProfile?.playerId)
        assertNotEquals(aliceProfile?.playerId, bobProfile?.playerId)
    }

    private fun playerRowCount(): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM player").use { statement ->
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }
    }
}
