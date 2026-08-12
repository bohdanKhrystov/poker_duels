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

private fun codeSource(vararg codes: String): RoomCodeSource {
    val iterator = codes.iterator()
    return RoomCodeSource { RoomCode(iterator.next()) }
}

private val TEST_TIMEOUTS = RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000)

internal class RoomReapTest {

    @Test
    fun aWaitingRoomIsReapedAtItsTimeout() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val room = registry.create(newPlayerId())

        clock.advance(10_000)
        val reaped = registry.reap()

        assertEquals(listOf(room.code), reaped)
        assertNull(registry.get(room.code))
    }

    @Test
    fun aWaitingRoomOneMillisecondShortSurvives() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        registry.create(newPlayerId())

        clock.advance(9_999)
        val reaped = registry.reap()

        assertEquals(emptyList<RoomCode>(), reaped)
        assertEquals(1, registry.size)
    }

    @Test
    fun aFinishedRoomIsReapedAtTheFinishedTimeout() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.finish(room.code)

        clock.advance(3_999)
        assertEquals(emptyList<RoomCode>(), registry.reap())

        clock.advance(1)
        assertEquals(listOf(room.code), registry.reap())
    }

    @Test
    fun anAbandonedRoomIsReapedAtTheFinishedTimeout() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val room = registry.create(newPlayerId())
        registry.abandon(room.code)

        clock.advance(3_999)
        assertEquals(emptyList<RoomCode>(), registry.reap())

        clock.advance(1)
        assertEquals(listOf(room.code), registry.reap())
    }

    @Test
    fun aPlayingRoomIsNeverReaped() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        clock.advance(10_000_000)
        val reaped = registry.reap()

        assertEquals(emptyList<RoomCode>(), reaped)
        assertNotNull(registry.get(room.code))
    }

    @Test
    fun joiningResetsTheIdleClock() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)

        clock.advance(9_000)
        registry.join(room.code, guest)
        clock.advance(9_000)
        registry.finish(room.code)

        val reaped = registry.reap()

        assertEquals(emptyList<RoomCode>(), reaped)
    }

    @Test
    fun reapReturnsEveryCodeItRemoved() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ", "2B7KMNPR", "2B7KMNPS"), clock, TEST_TIMEOUTS)
        val roomA = registry.create(newPlayerId())
        val roomB = registry.create(newPlayerId())
        val roomC = registry.create(newPlayerId())

        clock.advance(10_000)
        val reaped = registry.reap()

        assertEquals(setOf(roomA.code, roomB.code, roomC.code), reaped.toSet())
        assertEquals(3, reaped.size)
        assertEquals(0, registry.size)
    }
}
