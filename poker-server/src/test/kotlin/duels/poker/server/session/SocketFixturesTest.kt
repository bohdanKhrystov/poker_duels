package duels.poker.server.session

import duels.poker.server.auth.Identity
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SocketFixturesTest {
    @Test
    fun eachCallGetsItsOwnRegistry() {
        val deps1 = testDeps()
        val deps2 = testDeps()

        // They should be different instances
        assert(deps1.sessions !== deps2.sessions)
        assert(deps1.rooms !== deps2.rooms)
        assert(deps1.connections !== deps2.connections)
        // Both should start empty
        assertEquals(0, deps1.sessions.size)
        assertEquals(0, deps2.sessions.size)
    }

    @Test
    fun fixedDeviceIdsIssuesInOrder() {
        val source = fixedDeviceIds("a", "b")

        val id1 = source.newDeviceId()
        val id2 = source.newDeviceId()

        assertEquals(DeviceId("a"), id1)
        assertEquals(DeviceId("b"), id2)
    }

    @Test
    fun fixedDeviceIdsFailsWhenExhausted() {
        val source = fixedDeviceIds("a", "b")

        source.newDeviceId() // "a"
        source.newDeviceId() // "b"

        assertFailsWith<IllegalStateException> {
            source.newDeviceId() // Should throw
        }
    }

    @Test
    fun theDefaultRoomRegistryStartsEmpty() {
        val deps = testDeps()
        assertEquals(0, deps.rooms.size)
    }

    @Test
    fun theDefaultConnectionDirectoryStartsEmpty() {
        val deps = testDeps()
        assertEquals(0, deps.connections.size)
    }

    @Test
    fun testDepsDefaultsAResolverOverItsOwnDirectory() {
        val directory = InMemoryPlayerDirectory()
        val deviceId = DeviceId("x")

        val deps = testDeps(directory = directory)

        // Before adding the device, resolve should answer UnknownDevice
        val beforeResolve = runBlocking {
            deps.identities.resolve(null, deviceId)
        }
        assert(beforeResolve is Identity.UnknownDevice)

        // Add the device to the directory
        runBlocking {
            directory.resolve(deviceId)
        }

        // After adding, resolve should answer Device
        val afterResolve = runBlocking {
            deps.identities.resolve(null, deviceId)
        }
        assert(afterResolve is Identity.Device)
    }
}
