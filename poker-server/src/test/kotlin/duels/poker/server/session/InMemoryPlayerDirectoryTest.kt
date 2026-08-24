package duels.poker.server.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class InMemoryPlayerDirectoryTest {
    @Test
    fun resolvingANewDeviceCreatesOneProfile(): Unit = runBlocking {
        val directory = InMemoryPlayerDirectory()
        val deviceId = DeviceId("d1")

        val player = directory.resolve(deviceId)

        assertEquals(1, directory.profileCount)
        assertEquals(deviceId, player.deviceId)
    }

    @Test
    fun resolvingTheSameDeviceTwiceReturnsTheSameProfile(): Unit = runBlocking {
        val directory = InMemoryPlayerDirectory()
        val deviceId = DeviceId("d1")

        val player1 = directory.resolve(deviceId)
        val player2 = directory.resolve(deviceId)

        assertEquals(player1, player2)
        assertEquals(1, directory.profileCount)
    }

    @Test
    fun differentDevicesGetDifferentProfiles(): Unit = runBlocking {
        val directory = InMemoryPlayerDirectory()
        val d1 = DeviceId("d1")
        val d2 = DeviceId("d2")

        val player1 = directory.resolve(d1)
        val player2 = directory.resolve(d2)

        assertNotEquals(player1.id, player2.id)
        assertEquals(2, directory.profileCount)
    }

    @Test
    fun aBlankDeviceIdIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            DeviceId("")
        }

        assertFailsWith<IllegalArgumentException> {
            DeviceId("   ")
        }
    }

    @Test
    fun theDoubleFindsWhatItResolved(): Unit = runBlocking {
        val directory = InMemoryPlayerDirectory()
        val d1 = DeviceId("d1")
        val d2 = DeviceId("d2")
        val resolved1 = directory.resolve(d1)
        val resolved2 = directory.resolve(d2)

        val found1 = directory.findOrNull(d1)
        val found2 = directory.findOrNull(d2)

        assertEquals(resolved1, found1)
        assertEquals(resolved2, found2)
        assertNotEquals(found1?.id, found2?.id)
    }

    @Test
    fun theDoubleCreatesNothingOnAMiss(): Unit = runBlocking {
        val directory = InMemoryPlayerDirectory()
        val deviceId = DeviceId("ghost")
        val before = directory.profileCount

        assertNull(directory.findOrNull(deviceId))
        assertEquals(before, directory.profileCount)

        // A second lookup after the first proves the first call left nothing behind for the
        // second call to find — the property a single call cannot distinguish from a fluke.
        assertNull(directory.findOrNull(deviceId))
        assertEquals(before, directory.profileCount)
    }

    @Test
    fun concurrentResolvesOfOneDeviceCreateOneProfile(): Unit = runBlocking {
        val directory = InMemoryPlayerDirectory()
        val deviceId = DeviceId("d1")

        val results = (0 until 100).map {
            async(Dispatchers.Default) {
                val player = directory.resolve(deviceId)
                player.id
            }
        }.awaitAll()

        assertEquals(1, directory.profileCount)
        val firstPlayerId = results[0]
        results.forEach { assertEquals(firstPlayerId, it) }
    }
}
