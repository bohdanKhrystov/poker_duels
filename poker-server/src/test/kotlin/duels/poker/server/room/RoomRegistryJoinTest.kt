package duels.poker.server.room

import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.UUID

private fun newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())

private fun scriptedRegistry(code: String = "2B7KMNPQ"): RoomRegistry {
    val codes = object : RoomCodeSource {
        override fun newRoomCode(): RoomCode = RoomCode(code)
    }
    return RoomRegistry(codes, MutableClock())
}

internal class RoomRegistryJoinTest {

    @Test
    fun joiningAnUnknownCodeIsRefusedUnknownRoom() = runBlocking {
        val registry = scriptedRegistry()

        val result = registry.join(RoomCode("ZZZZZZZZ"), newPlayerId())

        assertEquals(JoinResult.Refused(RoomRefusal.UNKNOWN_ROOM), result)
        assertEquals(0, registry.size)
    }

    @Test
    fun aSuccessfulJoinIsStoredInTheRegistry() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)

        val result = registry.join(room.code, guest)

        assertTrue(result is JoinResult.Seated)
        val stored = registry.get(room.code)!!
        assertEquals(RoomState.PLAYING, stored.state)
        assertEquals(guest, stored.guest)
    }

    @Test
    fun aRefusedJoinLeavesTheStoredRoomUntouched() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val room = registry.create(host)

        val result = registry.join(room.code, host)

        assertEquals(JoinResult.Refused(RoomRefusal.ALREADY_SEATED), result)
        val stored = registry.get(room.code)!!
        assertEquals(RoomState.WAITING, stored.state)
        assertNull(stored.guest)
    }

    @Test
    fun aThirdJoinerIsRefusedRoomFullAndTheGuestKeepsTheSeat() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        val result = registry.join(room.code, newPlayerId())

        assertEquals(JoinResult.Refused(RoomRefusal.ROOM_FULL), result)
        assertEquals(guest, registry.get(room.code)!!.guest)
    }

    @Test
    @Timeout(60)
    fun oneHundredConcurrentJoinersProduceExactlyOneGuest() = runBlocking(Dispatchers.Default) {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val room = registry.create(host)
        val gate = CompletableDeferred<Unit>()
        val players = (0 until 100).map { PlayerId("p$it") }

        val jobs = players.map { player ->
            async {
                gate.await()
                registry.join(room.code, player)
            }
        }
        gate.complete(Unit)
        val results = jobs.awaitAll()

        val seated = results.filterIsInstance<JoinResult.Seated>()
        val refused = results.filterIsInstance<JoinResult.Refused>()
        assertEquals(1, seated.size)
        assertEquals(99, refused.size)
        assertTrue(refused.all { it.reason == RoomRefusal.ROOM_FULL })
        assertEquals(seated.single().room.guest, registry.get(room.code)!!.guest)
    }
}
