package duels.poker.server.room

import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

private fun newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())

private fun scriptedRegistry(code: String = "2B7KMNPQ"): RoomRegistry {
    val codes = object : RoomCodeSource {
        override fun newRoomCode(): RoomCode = RoomCode(code)
    }
    return RoomRegistry(codes, MutableClock())
}

internal class RoomRegistryHeldRoomTest {

    @Test
    fun heldRoomIsNullForAPlayerHoldingNothing() {
        val registry = scriptedRegistry()
        val player = newPlayerId()

        // Empty registry
        var result = registry.heldRoom(player)
        assertNull(result)

        // Registry holding one room belonging to another player
        val otherPlayer = newPlayerId()
        registry.create(otherPlayer)
        result = registry.heldRoom(player)
        assertNull(result)
    }

    @Test
    fun heldRoomFindsTheWaitingRoomItsHostOpened() {
        val registry = scriptedRegistry()
        val host = newPlayerId()

        val created = registry.create(host)
        val result = registry.heldRoom(host)

        assertNotNull(result)
        assertEquals(created, result)
        assertEquals(created.code, result!!.code)
        assertEquals(RoomState.WAITING, result.state)
    }

    @Test
    fun heldRoomFindsThePlayingRoomTheGuestSitsIn() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()

        val created = registry.create(host)
        val joinResult = registry.join(created.code, guest) as JoinResult.Seated
        val room = joinResult.room

        val hostResult = registry.heldRoom(host)
        assertEquals(room, hostResult)

        val guestResult = registry.heldRoom(guest)
        assertEquals(room, guestResult)
        assertEquals(1, room.seatOf(guest))
    }

    @Test
    fun heldRoomIgnoresAFinishedRoom() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()

        val created = registry.create(host)
        registry.join(created.code, guest)
        registry.finish(created.code)

        val hostResult = registry.heldRoom(host)
        assertNull(hostResult)

        val guestResult = registry.heldRoom(guest)
        assertNull(guestResult)

        // But get(code) still returns the room
        val room = registry.get(created.code)
        assertNotNull(room)
        assertEquals(RoomState.FINISHED, room!!.state)
        assertEquals(0, room.seatOf(host))
    }

    @Test
    fun heldRoomIgnoresAnAbandonedRoom() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()

        val created = registry.create(host)
        registry.abandon(created.code)

        val result = registry.heldRoom(host)
        assertNull(result)

        // But get(code) still returns the room
        val room = registry.get(created.code)
        assertNotNull(room)
        assertEquals(RoomState.ABANDONED, room!!.state)
        assertEquals(0, room.seatOf(host))
    }

    @Test
    fun heldRoomIsDeclaredNonSuspendingSoItCanTakeNoMutex() {
        val source = java.io.File("src/main/kotlin/duels/poker/server/room/RoomRegistry.kt").readText()

        // Asserts it contains `public fun heldRoom(`
        assert(source.contains("public fun heldRoom(")) { "heldRoom must be declared as public fun" }

        // does not contain `suspend fun heldRoom`
        assert(!source.contains("suspend fun heldRoom")) { "heldRoom must not be declared as suspend fun" }

        // contains no `tryLock`
        assert(!source.contains("tryLock")) { "heldRoom must not use tryLock" }
    }
}
