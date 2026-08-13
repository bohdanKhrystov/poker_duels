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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

private val fixedSeeds = HandSeedSource { 7L }

private fun newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())

private fun scriptedRegistry(
    seeds: HandSeedSource = fixedSeeds,
    sink: DuelResultSink = DuelResultSink { },
): RoomRegistry {
    val codes = object : RoomCodeSource {
        override fun newRoomCode(): RoomCode = RoomCode("2B7KMNPQ")
    }
    return RoomRegistry(codes, MutableClock(), seeds = seeds, sink = sink)
}

/** Builds the [Act] frame for whoever is on turn in [room]'s live hand. */
private fun callFrame(room: Room): Act {
    val hand = room.runner!!.hand!!
    return Act(
        handNumber = hand.state.handNumber,
        actionSequence = decisionPointOf(hand.log.events)!!.sequence,
        action = PlayerAction.Call(hand.state.seatToAct!!),
    )
}

internal class RoomDuelTest {

    @Test
    fun theSecondJoinerStartsALiveDuel() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)

        val result = registry.join(room.code, guest) as JoinResult.Seated

        val runner = result.room.runner
        assertNotNull(runner)
        assertNotNull(runner!!.hand)
        assertNull(runner.outcome)
        assertEquals(runner, registry.get(room.code)!!.runner)
    }

    @Test
    fun anActRoutedThroughTheRoomMovesTheDuelAndReturnsTheRunnersFrames() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        val seated = (registry.join(room.code, guest) as JoinResult.Seated).room
        val message = callFrame(seated)
        val expected = seated.act(message.action.seat, message, fixedSeeds)!!

        val step = registry.act(room.code) { r -> r.act(message.action.seat, message, fixedSeeds) }

        assertNotNull(step)
        assertEquals(expected.outbound, step!!.outbound)
        assertEquals(expected.runner, step.runner)
        assertEquals(step.runner, registry.get(room.code)!!.runner)
    }

    @Test
    fun actOnARoomWithNoLiveDuelReturnsNull() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val room = registry.create(host)

        val step = registry.act(room.code) { r -> r.act(0, Act(1, 0, PlayerAction.Check(0)), fixedSeeds) }

        assertNull(step)
    }

    @Test
    @Timeout(60)
    fun concurrentActsOnOneRoomNeverInterleave() = runBlocking(Dispatchers.Default) {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        val seated = (registry.join(room.code, guest) as JoinResult.Seated).room
        val message = callFrame(seated)
        val gate = CompletableDeferred<Unit>()
        val callers = 50

        val jobs = (0 until callers).map {
            async {
                gate.await()
                registry.act(room.code) { r -> r.act(message.action.seat, message, fixedSeeds) }
            }
        }
        gate.complete(Unit)
        val results = jobs.awaitAll()

        // A replayed frame is dropped with an empty outbound (`ActRefusal.STALE_FRAME`), so
        // exactly one of these racing, identical frames may have actually moved the duel.
        val moved = results.filterNotNull().filter { it.outbound.isNotEmpty() }
        assertEquals(1, moved.size)
        assertEquals(moved.single().runner, registry.get(room.code)!!.runner)
    }

    @Test
    fun aFinishedDuelRecordsTheResultExactlyOnceWithTheWinner() = runBlocking {
        val recorded = CopyOnWriteArrayList<DuelResult>()
        val registry = scriptedRegistry(sink = DuelResultSink { recorded.add(it) })
        val host = newPlayerId()
        val guest = newPlayerId()
        val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
        val room = registry.create(host, oneHand)
        val seated = (registry.join(room.code, guest) as JoinResult.Seated).room
        val hand = seated.runner!!.hand!!
        val toActSeat = hand.state.seatToAct!!
        val fold = Act(
            handNumber = hand.state.handNumber,
            actionSequence = decisionPointOf(hand.log.events)!!.sequence,
            action = PlayerAction.Fold(toActSeat),
        )

        val step = registry.act(room.code) { r -> r.act(toActSeat, fold, fixedSeeds) }

        assertNotNull(step)
        val outcome = step!!.runner.outcome
        assertNotNull(outcome)
        val winnerSeat = outcome!!.winner!!
        val expectedWinner = if (winnerSeat == 0) host else guest

        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)
        assertEquals(1, recorded.size)
        assertEquals(expectedWinner, recorded.single().winner)

        // A retry of the very frame that finished the duel must not record a second result.
        val retry = registry.act(room.code) { r -> r.act(toActSeat, fold, fixedSeeds) }
        assertNull(retry)
        assertEquals(1, recorded.size)
    }

    @Test
    fun aRematchStartsAFreshDuel() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
        val room = registry.create(host, oneHand)
        val seated = (registry.join(room.code, guest) as JoinResult.Seated).room
        val hand = seated.runner!!.hand!!
        val fold = Act(
            handNumber = hand.state.handNumber,
            actionSequence = decisionPointOf(hand.log.events)!!.sequence,
            action = PlayerAction.Fold(hand.state.seatToAct!!),
        )
        registry.act(room.code) { r -> r.act(hand.state.seatToAct!!, fold, fixedSeeds) }
        val finishedRunner = registry.get(room.code)!!.runner!!

        registry.offerRematch(room.code, host)
        val agreed = registry.offerRematch(room.code, guest) as RematchResult.Agreed

        val freshRunner = agreed.room.runner!!
        assertNotNull(freshRunner.hand)
        assertNull(freshRunner.outcome)
        assertTrue(freshRunner != finishedRunner)
        assertEquals(freshRunner, registry.get(room.code)!!.runner)
    }
}
