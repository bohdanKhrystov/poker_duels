package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.PlayerAction
import duels.poker.server.duel.DuelResult
import duels.poker.server.duel.DuelResultSink
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.duel.decisionPointOf
import duels.poker.server.protocol.Act
import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

private fun newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())

private fun codeSource(vararg codes: String): RoomCodeSource {
    val iterator = codes.iterator()
    return RoomCodeSource { RoomCode(iterator.next()) }
}

private val TEST_TIMEOUTS = RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000)

private val fixedSeeds = HandSeedSource { 7L }

/** Builds the finishing [Act] frame — a fold on hand one — for a room seated on [oneHand]. */
private fun foldFrame(room: Room): Act {
    val hand = room.runner!!.hand!!
    return Act(
        handNumber = hand.state.handNumber,
        actionSequence = decisionPointOf(hand.log.events)!!.sequence,
        action = PlayerAction.Fold(hand.state.seatToAct!!),
    )
}

private val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))

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

    @Test
    @Timeout(60)
    fun aRoomWithRecordingOutstandingSurvivesAReapPastTheFinishedTimeout() = runBlocking(Dispatchers.Default) {
        val clock = MutableClock()
        val enteredRecord = CompletableDeferred<Unit>()
        val releaseRecord = CompletableDeferred<Unit>()
        // `record` never returns until released below: this stands in for a hung DB call, the
        // ticket's own motivating example of what outlasts `timeouts.finishedMillis`.
        val sink = DuelResultSink {
            enteredRecord.complete(Unit)
            releaseRecord.await()
        }
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds, sink = sink)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host, oneHand)
        val seated = (registry.join(room.code, guest) as JoinResult.Seated).room
        val fold = foldFrame(seated)

        val finishing = async {
            registry.act(room.code) { r -> r.act(fold.action.seat, fold, fixedSeeds) }
        }
        enteredRecord.await()
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)

        // Well past the FINISHED idle limit, and the room reads as idle by `lastActivityAt` alone
        // — but its result is still mid-flight, so `reap` must leave it exactly where it is.
        clock.advance(TEST_TIMEOUTS.finishedMillis + 1)
        val reaped = registry.reap()

        assertEquals(emptyList<RoomCode>(), reaped)
        assertNotNull(registry.get(room.code))
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)

        releaseRecord.complete(Unit)
        // Captured so this function's inferred return type stays `Unit`, not `DuelStep?` — a
        // non-`Unit` test method silently confused Gradle's `--tests` filter into reporting "no
        // tests found" for this exact method during development of this test.
        val finishedStep = finishing.await()
        assertNotNull(finishedStep)
    }

    @Test
    @Timeout(60)
    fun onceRecordingCompletesTheRoomBecomesReapableAgain() = runBlocking(Dispatchers.Default) {
        val clock = MutableClock()
        val enteredRecord = CompletableDeferred<Unit>()
        val releaseRecord = CompletableDeferred<Unit>()
        val recorded = CopyOnWriteArrayList<DuelResult>()
        val sink = DuelResultSink { result ->
            enteredRecord.complete(Unit)
            releaseRecord.await()
            recorded.add(result)
        }
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds, sink = sink)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host, oneHand)
        val seated = (registry.join(room.code, guest) as JoinResult.Seated).room
        val fold = foldFrame(seated)

        val finishing = async {
            registry.act(room.code) { r -> r.act(fold.action.seat, fold, fixedSeeds) }
        }
        enteredRecord.await()
        clock.advance(TEST_TIMEOUTS.finishedMillis + 1)
        assertEquals(emptyList<RoomCode>(), registry.reap())

        releaseRecord.complete(Unit)
        finishing.await()
        assertEquals(1, recorded.size)

        // Recording finished, so the `recording` entry is gone and the room's idle clock is
        // already past the limit: a fresh reap must collect it exactly as any other long-finished
        // room. Anything else would trade the lost-duel bug for a permanent leak on the success
        // path — a different, unacceptable failure this fix must not introduce.
        val reaped = registry.reap()

        assertEquals(listOf(room.code), reaped)
        assertNull(registry.get(room.code))
    }

    @Test
    fun aFinishedRoomRecordedThroughActIsStillReapedNormally() = runBlocking {
        val clock = MutableClock()
        val recorded = CopyOnWriteArrayList<DuelResult>()
        val registry = RoomRegistry(
            codeSource("2B7KMNPQ"),
            clock,
            TEST_TIMEOUTS,
            seeds = fixedSeeds,
            sink = DuelResultSink { recorded.add(it) },
        )
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host, oneHand)
        val seated = (registry.join(room.code, guest) as JoinResult.Seated).room
        val fold = foldFrame(seated)

        registry.act(room.code) { r -> r.act(fold.action.seat, fold, fixedSeeds) }
        assertEquals(1, recorded.size)
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)

        // A room finished and successfully recorded through `act` (not `Room.finish` called
        // directly, as the other FINISHED-reap test above does) is reaped exactly as before —
        // this fix must not have simply disabled reaping for FINISHED rooms.
        clock.advance(TEST_TIMEOUTS.finishedMillis)
        val reaped = registry.reap()

        assertEquals(listOf(room.code), reaped)
        assertNull(registry.get(room.code))
    }
}
