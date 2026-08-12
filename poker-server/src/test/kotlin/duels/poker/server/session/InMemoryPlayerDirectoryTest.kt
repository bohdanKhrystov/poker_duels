package duels.poker.server.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

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
    fun concurrentResolvesOfOneDeviceCreateOneProfile(): Unit = runBlocking {
        val directory = InMemoryPlayerDirectory()
        val deviceId = DeviceId("d1")
        val results = mutableListOf<PlayerId>()

        val jobs = (0 until 100).map {
            launch(Dispatchers.Default) {
                val player = directory.resolve(deviceId)
                results.add(player.id)
            }
        }

        jobs.forEach { it.join() }

        assertEquals(1, directory.profileCount)
        val firstPlayerId = results[0]
        results.forEach { assertEquals(firstPlayerId, it) }
    }
}
