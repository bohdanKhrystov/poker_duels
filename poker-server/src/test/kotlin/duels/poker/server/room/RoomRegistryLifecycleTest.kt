package duels.poker.server.room

import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

private fun newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())

private fun scriptedRegistry(code: String = "2B7KMNPQ"): RoomRegistry {
    val codes = object : RoomCodeSource {
        override fun newRoomCode(): RoomCode = RoomCode(code)
    }
    return RoomRegistry(codes, MutableClock())
}

internal class RoomRegistryLifecycleTest {

    @Test
    fun finishUpdatesTheStoredRoom() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        val finished = registry.finish(room.code)

        assertEquals(RoomState.FINISHED, finished!!.state)
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)
        assertEquals(finished, registry.get(room.code))
    }

    @Test
    fun finishingARoomThatIsNotPlayingThrows() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val room = registry.create(host)

        assertThrows<IllegalStateException> {
            registry.finish(room.code)
        }
    }

    @Test
    fun abandonUpdatesTheStoredRoom() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        val abandoned = registry.abandon(room.code)

        assertEquals(RoomState.ABANDONED, abandoned!!.state)
        assertEquals(RoomState.ABANDONED, registry.get(room.code)!!.state)
    }

    @Test
    fun finishAndAbandonReturnNullForAnUnknownCode() = runBlocking {
        val registry = scriptedRegistry()

        val finishResult = registry.finish(RoomCode("ZZZZZZZZ"))
        val abandonResult = registry.abandon(RoomCode("ZZZZZZZZ"))

        assertNull(finishResult)
        assertNull(abandonResult)
        assertEquals(0, registry.size)
    }

    @Test
    fun oneOfferLeavesTheStoredRoomFinished() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.finish(room.code)

        val result = registry.offerRematch(room.code, host)

        assertEquals(host, room.host)
        val offered = (result as RematchResult.Offered).room
        assertEquals(RoomState.FINISHED, offered.state)
        assertEquals(setOf(host), offered.rematchOffers)
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)
        assertEquals(setOf(host), registry.get(room.code)!!.rematchOffers)
    }

    @Test
    fun bothOffersReturnTheStoredRoomToPlaying() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.finish(room.code)

        registry.offerRematch(room.code, host)
        val result = registry.offerRematch(room.code, guest)

        val agreed = (result as RematchResult.Agreed).room
        assertEquals(RoomState.PLAYING, agreed.state)
        assertEquals(1, agreed.openingButtonSeat)
        assertEquals(emptySet<PlayerId>(), agreed.rematchOffers)
        assertEquals(RoomState.PLAYING, registry.get(room.code)!!.state)
    }

    @Test
    fun offeringInAnUnknownRoomIsRefusedUnknownRoom() = runBlocking {
        val registry = scriptedRegistry()
        val player = newPlayerId()

        val result = registry.offerRematch(RoomCode("ZZZZZZZZ"), player)

        assertEquals(RematchResult.Refused(RematchRefusal.UNKNOWN_ROOM), result)
    }
}
