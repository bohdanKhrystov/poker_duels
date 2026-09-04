package duels.poker.server.room

import duels.poker.server.duel.Addressed
import duels.poker.server.protocol.SeatPresence
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

private fun newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())

private fun codeSource(vararg codes: String): RoomCodeSource {
    val iterator = codes.iterator()
    return RoomCodeSource { RoomCode(iterator.next()) }
}

private val TEST_TIMEOUTS = RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000)

internal class RoomDisconnectTest {

    @Test
    fun aDisconnectMarksTheSeatAway(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        assertEquals(emptySet<Int>(), registry.get(room.code)!!.awaySeats)

        val disconnected = registry.disconnect(room.code, guest)

        assertEquals(setOf(1), disconnected!!.room.awaySeats)
    }

    @Test
    fun theStoredRoomHasAnAwaySeat(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        assertFalse(registry.get(room.code)!!.awaySeats.isNotEmpty())

        registry.disconnect(room.code, guest)

        assertTrue(registry.get(room.code)!!.awaySeats.isNotEmpty())
    }

    @Test
    fun theHostAndTheGuestAreTrackedSeparately(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        val afterHost = registry.disconnect(room.code, host)
        assertEquals(setOf(0), afterHost!!.room.awaySeats)

        val afterGuest = registry.disconnect(room.code, guest)

        assertEquals(setOf(0, 1), afterGuest!!.room.awaySeats)
    }

    @Test
    fun somebodyWhoIsNotSeatedChangesNothing(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val before = registry.get(room.code)

        val result = registry.disconnect(room.code, PlayerId("stranger"))

        assertNull(result)
        assertEquals(before, registry.get(room.code))
    }

    @Test
    fun anUnknownCodeAnswersNull(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        val result = registry.disconnect(RoomCode("ZZZZZZZZ"), host)

        assertNull(result)
    }

    @Test
    fun aDropTellsTheOtherSeatItIsAway(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        val disconnected = registry.disconnect(room.code, guest)

        assertEquals(
            listOf(Addressed(0, ServerMessage.OpponentPresence(SeatPresence.AWAY))),
            disconnected!!.outbound,
        )
    }

    @Test
    fun theDropNamesTheSeatThatStayed(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        val afterHost = registry.disconnect(room.code, host)
        assertEquals(
            listOf(Addressed(1, ServerMessage.OpponentPresence(SeatPresence.AWAY))),
            afterHost!!.outbound,
        )

        val afterGuest = registry.disconnect(room.code, guest)
        assertEquals(
            listOf(Addressed(0, ServerMessage.OpponentPresence(SeatPresence.AWAY))),
            afterGuest!!.outbound,
        )
    }

    @Test
    fun aRoomWithNobodyElseSeatedProducesNoFrame(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val room = registry.create(host)

        val disconnected = registry.disconnect(room.code, host)

        assertEquals(setOf(0), disconnected!!.room.awaySeats)
        assertEquals(emptyList<Any>(), disconnected.outbound)
    }

    @Test
    fun anUnseatedPlayerProducesNoFrame(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        val result = registry.disconnect(room.code, PlayerId("stranger"))

        assertNull(result)
        assertEquals(emptySet<Int>(), registry.get(room.code)!!.awaySeats)
    }
}
