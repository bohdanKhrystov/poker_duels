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

/** A [RoomCodeSource] that hands out [codes] in order, once each. */
private class HeldRoomCodes(vararg codes: String) : RoomCodeSource {
    private val queue = ArrayDeque(codes.map { RoomCode(it) })

    override fun newRoomCode(): RoomCode = queue.removeFirst()
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
    fun aPlayingRoomOutranksAWaitingOneWhicheverOpenedFirst() = runBlocking {
        // WAITING opened at 1_000, PLAYING opened and joined at 2_000: the stamp key would pick
        // the WAITING room, but the state key outranks it.
        val clockA = MutableClock(1_000)
        val registryA = RoomRegistry(HeldRoomCodes("AAAAAAAA", "BBBBBBBB"), clockA)
        val hostA = newPlayerId()
        registryA.create(hostA)
        clockA.set(2_000)
        val playingCodeA = registryA.create(hostA).code
        registryA.join(playingCodeA, newPlayerId())

        val resultA = registryA.heldRoom(hostA)
        assertEquals(playingCodeA, resultA?.code)
        assertEquals(RoomState.PLAYING, resultA?.state)

        // Swapped: PLAYING opened and joined at 1_000, WAITING opened at 2_000. Still the PLAYING
        // room, so the answer is not simply "the older room" either.
        val clockB = MutableClock(1_000)
        val registryB = RoomRegistry(HeldRoomCodes("AAAAAAAA", "BBBBBBBB"), clockB)
        val hostB = newPlayerId()
        val playingCodeB = registryB.create(hostB).code
        registryB.join(playingCodeB, newPlayerId())
        clockB.set(2_000)
        registryB.create(hostB)

        val resultB = registryB.heldRoom(hostB)
        assertEquals(playingCodeB, resultB?.code)
        assertEquals(RoomState.PLAYING, resultB?.state)
    }

    @Test
    fun theOlderWaitingRoomWinsEvenWhenItCarriesTheHigherCode() {
        // "ZZZZZZZZ" minted at 1_000, "AAAAAAAA" minted at 2_000: the code key would pick
        // "AAAAAAAA", but the older stamp outranks it.
        val clockA = MutableClock(1_000)
        val registryA = RoomRegistry(HeldRoomCodes("ZZZZZZZZ", "AAAAAAAA"), clockA)
        val hostA = newPlayerId()
        registryA.create(hostA)
        clockA.set(2_000)
        registryA.create(hostA)

        val resultA = registryA.heldRoom(hostA)
        assertEquals(RoomCode("ZZZZZZZZ"), resultA?.code)

        // "AAAAAAAA" minted at 1_000, "ZZZZZZZZ" minted at 2_000: the older room still wins, and
        // this time it also carries the lower code, so the first half is not passing by accident.
        val clockB = MutableClock(1_000)
        val registryB = RoomRegistry(HeldRoomCodes("AAAAAAAA", "ZZZZZZZZ"), clockB)
        val hostB = newPlayerId()
        registryB.create(hostB)
        clockB.set(2_000)
        registryB.create(hostB)

        val resultB = registryB.heldRoom(hostB)
        assertEquals(RoomCode("AAAAAAAA"), resultB?.code)
    }

    @Test
    fun theLowerCodeBreaksATieOnTheStamp() {
        // A clock that never advances: both rooms carry the identical stamp, so the first two
        // keys tie and the code alone decides.
        val registryA = RoomRegistry(HeldRoomCodes("ZZZZZZZZ", "AAAAAAAA"), MutableClock(1_000))
        val hostA = newPlayerId()
        registryA.create(hostA)
        registryA.create(hostA)

        val resultA = registryA.heldRoom(hostA)
        assertEquals(RoomCode("AAAAAAAA"), resultA?.code)

        // Minted in the opposite order: the answer is still "AAAAAAAA", so it is not the order
        // the rooms were inserted in.
        val registryB = RoomRegistry(HeldRoomCodes("AAAAAAAA", "ZZZZZZZZ"), MutableClock(1_000))
        val hostB = newPlayerId()
        registryB.create(hostB)
        registryB.create(hostB)

        val resultB = registryB.heldRoom(hostB)
        assertEquals(RoomCode("AAAAAAAA"), resultB?.code)
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
