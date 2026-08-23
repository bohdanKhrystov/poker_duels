package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.PlayerAction
import duels.poker.server.duel.Addressed
import duels.poker.server.duel.DuelResult
import duels.poker.server.duel.DuelResultSink
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.duel.decisionPointOf
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.SeatPresence
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

private fun newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())

private fun codeSource(vararg codes: String): RoomCodeSource {
    val iterator = codes.iterator()
    return RoomCodeSource { RoomCode(iterator.next()) }
}

private val TEST_TIMEOUTS = RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000, disconnectGraceMillis = 30_000)

private val fixedSeeds = HandSeedSource { 7L }

private val threeHands = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(3))

private val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))

/** The player seated at [seat]: the host for `0`, the guest for `1`. */
private fun seatedPlayer(seat: Int, host: PlayerId, guest: PlayerId): PlayerId = if (seat == 0) host else guest

/**
 * Builds the [Act] frame for whoever is on turn in [room]'s live hand, shoving all-in — the one
 * action that guarantees the seat behind it faces a bet, so a give-up on that next turn is read
 * off [duels.poker.engine.game.legalActions] as `Fold` (`ADR-0023`), never the `Check` a plain
 * call would leave legal instead.
 */
private fun allInFrame(room: Room): Act {
    val hand = room.runner!!.hand!!
    return Act(
        handNumber = hand.state.handNumber,
        actionSequence = decisionPointOf(hand.log.events)!!.sequence,
        action = PlayerAction.AllIn(hand.state.seatToAct!!),
    )
}

internal class GraceExpiryTest {

    @Test
    fun nothingExpiresWhileTheWindowIsStillRunning() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        registry.disconnect(room.code, seatedPlayer(onTurn, host, guest))

        clock.advance(29_999)
        val runnerBefore = registry.get(room.code)!!.runner

        val expiries = registry.expireGracePeriods()

        assertEquals(emptyList<GraceExpiry>(), expiries)
        assertTrue(registry.get(room.code)!!.isPaused)
        assertSame(runnerBefore, registry.get(room.code)!!.runner)
    }

    @Test
    fun theWindowRunningOutFoldsTheSeatOnTurn() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        registry.disconnect(room.code, seatedPlayer(onTurn, host, guest))

        clock.advance(30_000)
        val expiries = registry.expireGracePeriods()

        assertEquals(1, expiries.size)
        val handOneActions = registry.get(room.code)!!.runner!!.log.hands.first().actions
        assertEquals(PlayerAction.Fold(onTurn), handOneActions.last())
    }

    @Test
    fun theDuelCarriesOnWithoutTheAbsentPlayer() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host, threeHands)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        registry.disconnect(room.code, seatedPlayer(onTurn, host, guest))

        clock.advance(30_000)
        registry.expireGracePeriods()

        val after = registry.get(room.code)!!
        assertEquals(RoomState.PLAYING, after.state)
        assertFalse(after.isPaused)
        assertEquals(2, after.runner!!.hand!!.state.handNumber)
    }

    @Test
    fun theSweepDoesNotFireTwice() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        registry.disconnect(room.code, seatedPlayer(onTurn, host, guest))

        clock.advance(30_000)
        val firstSweep = registry.expireGracePeriods()

        // "Does not fire twice" is only proved once the first sweep is shown to have actually
        // fired: a non-empty result, and the fold it produced visible in the hand log.
        assertEquals(1, firstSweep.size)
        val handOneActionsAfterFirstSweep = registry.get(room.code)!!.runner!!.log.hands.first().actions
        assertEquals(PlayerAction.Fold(onTurn), handOneActionsAfterFirstSweep.last())
        val roomAfterFirstSweep = registry.get(room.code)!!

        val secondSweep = registry.expireGracePeriods()

        assertEquals(emptyList<GraceExpiry>(), secondSweep)
        assertEquals(roomAfterFirstSweep, registry.get(room.code))
    }

    @Test
    fun aSeatOffTurnIsFoldedWhenTheTurnReachesIt() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        val offTurn = 1 - onTurn
        registry.disconnect(room.code, seatedPlayer(offTurn, host, guest))
        val actionsBeforeSweep = registry.get(room.code)!!.runner!!.hand!!.log.actions

        clock.advance(30_000)
        val expiries = registry.expireGracePeriods()

        assertEquals(1, expiries.size)
        val afterSweep = registry.get(room.code)!!
        assertFalse(afterSweep.isPaused)
        // Nothing was foldable yet: the seat that ran out was not on turn, so the hand's own log
        // of actions must read exactly as it did before the sweep.
        assertEquals(actionsBeforeSweep, afterSweep.runner!!.hand!!.log.actions)

        val message = allInFrame(afterSweep)
        val step = registry.act(room.code) { r -> r.act(message.action.seat, message, fixedSeeds) }

        assertNotNull(step)
        val handOneActions = registry.get(room.code)!!.runner!!.log.hands.first().actions
        assertEquals(PlayerAction.Fold(offTurn), handOneActions.last())
    }

    @Test
    fun aPlayerWhoCameBackInTimeIsNotFolded() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        val onTurnPlayer = seatedPlayer(onTurn, host, guest)
        registry.disconnect(room.code, onTurnPlayer)
        val actionsBeforeSweep = registry.get(room.code)!!.runner!!.hand!!.log.actions

        clock.advance(29_000)
        registry.resume(room.code, onTurnPlayer)
        clock.advance(1_000)

        val expiries = registry.expireGracePeriods()

        assertEquals(emptyList<GraceExpiry>(), expiries)
        assertEquals(actionsBeforeSweep, registry.get(room.code)!!.runner!!.hand!!.log.actions)
    }

    @Test
    fun bothSeatsGoneEndsTheRoom() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, host)
        registry.disconnect(room.code, guest)

        clock.advance(30_000)
        val expiries = registry.expireGracePeriods()

        assertEquals(1, expiries.size)
        assertEquals(RoomState.ABANDONED, expiries.single().room.state)
        assertTrue(expiries.single().outbound.isEmpty())
        assertEquals(RoomState.ABANDONED, registry.get(room.code)!!.state)

        clock.advance(TEST_TIMEOUTS.finishedMillis)
        val reaped = registry.reap()

        assertEquals(listOf(room.code), reaped)
        assertNull(registry.get(room.code))
    }

    @Test
    fun aFoldThatEndsTheDuelIsRecordedOnce() = runBlocking {
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
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        val duelId = registry.get(room.code)!!.duelId
        registry.disconnect(room.code, seatedPlayer(onTurn, host, guest))

        clock.advance(30_000)
        val expiries = registry.expireGracePeriods()

        assertEquals(1, expiries.size)
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)
        assertEquals(1, recorded.size)
        assertEquals(duelId, recorded.single().duelId)
    }

    @Test
    fun aRoomWhoseFoldFailsToRecordDoesNotStopTheRestOfTheSweep() = runBlocking {
        val clock = MutableClock()
        // These two codes are not interchangeable with any other pair: `rooms` is a
        // `ConcurrentHashMap`, iterated in hash-bucket order, and this specific pair is the one
        // that places the failing room's code ahead of the survivor's. A test that instead
        // happened to iterate the survivor first would still pass even if pass 2 aborted on the
        // first exception instead of isolating it — there would be no later room left to orphan.
        val registry = RoomRegistry(
            codeSource("2B7KMNPQ", "V215DE1C"),
            clock,
            TEST_TIMEOUTS,
            seeds = fixedSeeds,
            sink = DuelResultSink { throw RuntimeException("simulated sink outage") },
        )

        // A duel-ending fold: its expiry drives the fold all the way through `act`'s sink call,
        // which this registry's sink always fails.
        val failingHost = newPlayerId()
        val failingGuest = newPlayerId()
        val failingRoom = registry.create(failingHost, oneHand)
        registry.join(failingRoom.code, failingGuest)
        val failingOnTurn = registry.get(failingRoom.code)!!.runner!!.hand!!.state.seatToAct!!
        registry.disconnect(failingRoom.code, seatedPlayer(failingOnTurn, failingHost, failingGuest))

        // An unrelated room whose fold does not end its duel, so it never reaches the sink at
        // all — its own `act` call has nothing to fail on.
        val okHost = newPlayerId()
        val okGuest = newPlayerId()
        val okRoom = registry.create(okHost)
        registry.join(okRoom.code, okGuest)
        val okOnTurn = registry.get(okRoom.code)!!.runner!!.hand!!.state.seatToAct!!
        registry.disconnect(okRoom.code, seatedPlayer(okOnTurn, okHost, okGuest))

        clock.advance(30_000)
        // Before the fix this threw the sink's exception straight out of `expireGracePeriods()`,
        // losing `okRoom`'s already-computed fold along with it.
        val expiries = registry.expireGracePeriods()

        assertEquals(1, expiries.size)
        assertEquals(okRoom.code, expiries.single().room.code)
        val okHandOneActions = registry.get(okRoom.code)!!.runner!!.log.hands.first().actions
        assertEquals(PlayerAction.Fold(okOnTurn), okHandOneActions.last())
    }

    @Test
    fun theSeatThatStayedIsToldTheOtherIsAbsent() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        // The seat that runs out here is off turn, on purpose: this is the pairing half of
        // `theAbsentMarkPrecedesTheFramesTheFoldProduced`'s on-turn seat, so between the two,
        // both concrete seat numbers are exercised as both "the seat that expired" and "the seat
        // that stayed" — not just whichever one the seed happens to put on turn.
        val offTurn = 1 - onTurn
        registry.disconnect(room.code, seatedPlayer(offTurn, host, guest))

        clock.advance(30_000)
        val expiries = registry.expireGracePeriods()

        assertEquals(1, expiries.size)
        val presenceFrame = Addressed(onTurn, ServerMessage.OpponentPresence(SeatPresence.ABSENT, null))
        assertEquals(presenceFrame, expiries.single().outbound.first())
    }

    @Test
    fun theAbsentMarkPrecedesTheFramesTheFoldProduced() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host, oneHand)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        registry.disconnect(room.code, seatedPlayer(onTurn, host, guest))

        clock.advance(30_000)
        val expiries = registry.expireGracePeriods()

        assertEquals(1, expiries.size)
        val outbound = expiries.single().outbound
        val presenceFrame = Addressed(1 - onTurn, ServerMessage.OpponentPresence(SeatPresence.ABSENT, null))
        // "Both arrived" is true of the wrong order too: a duel-ending fold produces frames of
        // its own (`Events`, `Snapshot`), so only the position of the presence frame among them —
        // not merely its presence in the list — says whether it came first.
        assertTrue(outbound.size > 1)
        assertEquals(0, outbound.indexOf(presenceFrame))
    }

    @Test
    fun noFrameGoesToTheSeatThatExpired() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host, oneHand)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        registry.disconnect(room.code, seatedPlayer(onTurn, host, guest))

        clock.advance(30_000)
        val expiries = registry.expireGracePeriods()

        assertEquals(1, expiries.size)
        val outbound = expiries.single().outbound
        // The expired seat is not left out of the fold's own frames — it still gets `Events` and
        // `Snapshot` like any other recipient. It is only ever left out of the one frame that
        // reports a seat's presence, because that frame is recipient-relative and the expired
        // seat has no opponent of its own to be told about.
        assertTrue(outbound.any { (seat, _) -> seat == onTurn })
        assertTrue(outbound.none { (seat, message) -> seat == onTurn && message is ServerMessage.OpponentPresence })
    }

    @Test
    fun aWindowStillRunningProducesNothing() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        registry.disconnect(room.code, seatedPlayer(onTurn, host, guest))

        clock.advance(29_999)
        val expiries = registry.expireGracePeriods()

        assertEquals(emptyList<GraceExpiry>(), expiries)
    }

    @Test
    fun bothSeatsGoneSendNoPresence() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, host)
        registry.disconnect(room.code, guest)

        clock.advance(30_000)
        val expiries = registry.expireGracePeriods()

        assertEquals(1, expiries.size)
        assertTrue(expiries.single().outbound.none { (_, message) -> message is ServerMessage.OpponentPresence })
    }
}
