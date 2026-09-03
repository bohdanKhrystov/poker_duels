package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.PlayerAction
import duels.poker.server.duel.Addressed
import duels.poker.server.duel.DuelResult
import duels.poker.server.duel.DuelResultSink
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.duel.decisionPointOf
import duels.poker.server.duel.startDuel
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.ServerMessage
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
        // The room's very first clocked decision: nobody has answered one yet, so the seat now on
        // turn is owed the whole fresh allowance and both banks read as the configured full bank
        // (`ADR-0113` §§1, 3) — read off the resulting hand and the configured defaults, not off
        // the clock mechanism this registry call is what actually exercises.
        val expectedHand = expected.runner.hand!!
        val expectedClock = ServerMessage.TurnClock(
            seat = expectedHand.state.seatToAct!!,
            handNumber = expectedHand.state.handNumber,
            actionSequence = decisionPointOf(expectedHand.log.events)!!.sequence,
            turnRemainingMillis = RoomTimeouts.DEFAULT_TURN_MILLIS,
            bankRemainingMillis = listOf(
                RoomTimeouts.DEFAULT_TIMEBANK_MILLIS,
                RoomTimeouts.DEFAULT_TIMEBANK_MILLIS,
            ),
        )
        val expectedOutbound = expected.outbound +
            listOf(Addressed(0, expectedClock), Addressed(1, expectedClock))

        val step = registry.act(room.code) { r -> r.act(message.action.seat, message, fixedSeeds) }

        assertNotNull(step)
        assertEquals(expectedOutbound, step!!.outbound)
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
    fun afailingSinkLeavesTheDuelRecordableAgain() = runBlocking {
        val recorded = CopyOnWriteArrayList<DuelResult>()
        var attempts = 0
        val sink = DuelResultSink {
            attempts++
            if (attempts == 1) throw RuntimeException("store unavailable")
            recorded.add(it)
        }
        val registry = scriptedRegistry(sink = sink)
        val host = newPlayerId()
        val guest = newPlayerId()
        val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
        val room = registry.create(host, oneHand)
        val seated = (registry.join(room.code, guest) as JoinResult.Seated).room
        val duelIdAtStart = seated.duelId
        val hand = seated.runner!!.hand!!
        val toActSeat = hand.state.seatToAct!!
        val fold = Act(
            handNumber = hand.state.handNumber,
            actionSequence = decisionPointOf(hand.log.events)!!.sequence,
            action = PlayerAction.Fold(toActSeat),
        )

        var threw = false
        try {
            registry.act(room.code) { r -> r.act(toActSeat, fold, fixedSeeds) }
        } catch (expected: RuntimeException) {
            threw = true
        }

        // A sink failure must not strand the room: it stays PLAYING, with nothing recorded yet,
        // so the very same finishing frame can be replayed.
        assertTrue(threw)
        assertEquals(0, recorded.size)
        assertEquals(RoomState.PLAYING, registry.get(room.code)!!.state)
        assertEquals(duelIdAtStart, registry.get(room.code)!!.duelId)

        val retryStep = registry.act(room.code) { r -> r.act(toActSeat, fold, fixedSeeds) }

        assertNotNull(retryStep)
        assertEquals(1, recorded.size)
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)
        assertEquals(duelIdAtStart, registry.get(room.code)!!.duelId)
    }

    @Test
    fun aretriedFinishRecordsUnderTheSameDuelId() = runBlocking {
        val seenDuelIds = CopyOnWriteArrayList<UUID>()
        var attempts = 0
        val sink = DuelResultSink { result ->
            attempts++
            // Read the id the sink was actually handed, not one re-derived from the room: a sink
            // that minted its own id per call — the bug this test guards against — would still
            // pass if this asserted against `Room.duelId`, because that field never changes
            // across a retry regardless of what the sink does with it.
            seenDuelIds.add(result.duelId)
            if (attempts == 1) throw RuntimeException("store unavailable")
        }
        val registry = scriptedRegistry(sink = sink)
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

        var threw = false
        try {
            registry.act(room.code) { r -> r.act(toActSeat, fold, fixedSeeds) }
        } catch (expected: RuntimeException) {
            threw = true
        }
        assertTrue(threw)

        registry.act(room.code) { r -> r.act(toActSeat, fold, fixedSeeds) }

        assertEquals(2, seenDuelIds.size)
        assertEquals(seenDuelIds[0], seenDuelIds[1])
    }

    @Test
    @Timeout(60)
    fun arematchAgreedWhileRecordingIsInFlightCannotLoseTheOriginalDuel() = runBlocking {
        val recorded = CopyOnWriteArrayList<DuelResult>()
        var attempts = 0
        val enteredRecord = CompletableDeferred<Unit>()
        val releaseRecord = CompletableDeferred<Unit>()
        val sink = DuelResultSink { result ->
            attempts++
            if (attempts == 1) {
                // The room's write-back to FINISHED has already happened by the time `record` is
                // called — this is the exact window a concurrent rematch could race. Signal the
                // test that the window is open, then wait to be released before failing, so the
                // rematch attempt below runs entirely inside it.
                enteredRecord.complete(Unit)
                releaseRecord.await()
                throw RuntimeException("store unavailable")
            }
            recorded.add(result)
        }
        val registry = scriptedRegistry(sink = sink)
        val host = newPlayerId()
        val guest = newPlayerId()
        val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
        val room = registry.create(host, oneHand)
        val seated = (registry.join(room.code, guest) as JoinResult.Seated).room
        val originalDuelId = seated.duelId
        val hand = seated.runner!!.hand!!
        val toActSeat = hand.state.seatToAct!!
        val fold = Act(
            handNumber = hand.state.handNumber,
            actionSequence = decisionPointOf(hand.log.events)!!.sequence,
            action = PlayerAction.Fold(toActSeat),
        )

        val finishing = async(Dispatchers.Default) {
            var threw = false
            try {
                registry.act(room.code) { r -> r.act(toActSeat, fold, fixedSeeds) }
            } catch (expected: RuntimeException) {
                threw = true
            }
            threw
        }

        enteredRecord.await()
        // The room is FINISHED here, but its result is not yet confirmed recorded: both offers
        // must be refused exactly as they would be for a duel still in progress, never agreed.
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)
        val hostOffer = registry.offerRematch(room.code, host)
        val guestOffer = registry.offerRematch(room.code, guest)
        assertTrue(hostOffer is RematchResult.Refused)
        assertTrue(guestOffer is RematchResult.Refused)
        assertEquals(RematchRefusal.NOT_FINISHED, (hostOffer as RematchResult.Refused).reason)
        assertEquals(RematchRefusal.NOT_FINISHED, (guestOffer as RematchResult.Refused).reason)

        releaseRecord.complete(Unit)
        assertTrue(finishing.await())

        // The very frame that finished the duel is still replayable, and its retry finally
        // records the original duel under its original id — nothing was lost to the race.
        val retryStep = registry.act(room.code) { r -> r.act(toActSeat, fold, fixedSeeds) }
        assertNotNull(retryStep)
        assertEquals(1, recorded.size)
        assertEquals(originalDuelId, recorded.single().duelId)

        // Now that the duel is actually recorded, a rematch is allowed again.
        registry.offerRematch(room.code, host)
        val agreed = registry.offerRematch(room.code, guest)
        assertTrue(agreed is RematchResult.Agreed)
    }

    @Test
    @Timeout(60)
    fun concurrentFinishingFramesRecordExactlyOnce() = runBlocking(Dispatchers.Default) {
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
        val gate = CompletableDeferred<Unit>()
        val callers = 50

        // The existing `concurrentActsOnOneRoomNeverInterleave` test races a mid-hand action and
        // only retries the finish sequentially; this races the finishing frame itself on real
        // threads, which is the case the mutex's exactly-once guarantee has never been checked
        // against.
        val jobs = (0 until callers).map {
            async {
                gate.await()
                registry.act(room.code) { r -> r.act(toActSeat, fold, fixedSeeds) }
            }
        }
        gate.complete(Unit)
        jobs.awaitAll()

        assertEquals(1, recorded.size)
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)
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

    @Test
    fun seatingTheGuestHandsBackTheOpeningFrames() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)

        val result = registry.join(room.code, guest) as JoinResult.Seated

        assertTrue(result.outbound.isNotEmpty())
        assertTrue(result.outbound.any { it.seat == 0 && it.message is ServerMessage.Snapshot })
        assertTrue(result.outbound.any { it.seat == 1 && it.message is ServerMessage.Snapshot })
    }

    @Test
    fun theOpeningFramesAreTheOnesTheRunnerProduced() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)

        val result = registry.join(room.code, guest) as JoinResult.Seated

        val expected = startDuel(room.format, room.openingButtonSeat, fixedSeeds.newHandSeed())
        assertEquals(expected.outbound, result.outbound)
    }

    @Test
    fun exactlyOneSeatIsToldItIsItsTurn() = runBlocking {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)

        val result = registry.join(room.code, guest) as JoinResult.Seated

        val toActSeat = result.room.runner!!.hand!!.state.seatToAct!!
        val yourTurnFrames = result.outbound.filter { it.message is ServerMessage.YourTurn }

        assertEquals(1, yourTurnFrames.size)
        assertEquals(toActSeat, yourTurnFrames.single().seat)
    }

    @Test
    fun apureRoomJoinYieldsNoFrames() {
        val registry = scriptedRegistry()
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)

        val result = room.join(guest, 0L) as JoinResult.Seated

        assertTrue(result.outbound.isEmpty())
    }
}
