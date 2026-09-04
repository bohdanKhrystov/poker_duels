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
import org.junit.jupiter.api.Assertions.assertNotSame
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

private val TEST_TIMEOUTS = RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000)

/**
 * A short, known turn clock. The tests below need a clock they can run out on its own — either a
 * connected seat with nobody disconnecting at all, or an away seat whose own decision comes due —
 * rather than [TEST_TIMEOUTS]' own `turnMillis`/`timebankMillis`, whatever [RoomTimeouts]'
 * constructor defaults to and deliberately large enough that no test above times a seat out by
 * accident.
 */
private val SHORT_CLOCK_TIMEOUTS = RoomTimeouts(
    waitingMillis = TEST_TIMEOUTS.waitingMillis,
    finishedMillis = TEST_TIMEOUTS.finishedMillis,
    turnMillis = 1_000L,
    timebankMillis = 2_000L,
)

private val fixedSeeds = HandSeedSource { 7L }

private val threeHands = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(3))

private val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))

/** The player seated at [seat]: the host for `0`, the guest for `1`. */
private fun seatedPlayer(seat: Int, host: PlayerId, guest: PlayerId): PlayerId = if (seat == 0) host else guest

/** The live decision's own deadline, read back rather than re-derived, so a probe either side of it never guesses. */
private suspend fun liveDeadline(registry: RoomRegistry, code: RoomCode): Long =
    registry.get(code)!!.turnDeadline!!.expiresAt

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

/** Builds the finishing [Act] frame — a fold on the seat currently on turn. */
private fun foldFrame(room: Room): Act {
    val hand = room.runner!!.hand!!
    return Act(
        handNumber = hand.state.handNumber,
        actionSequence = decisionPointOf(hand.log.events)!!.sequence,
        action = PlayerAction.Fold(hand.state.seatToAct!!),
    )
}

/** Builds the [Act] frame for whoever is on turn in [room]'s live hand, calling whatever bet stands. */
private fun callFrame(room: Room): Act {
    val hand = room.runner!!.hand!!
    return Act(
        handNumber = hand.state.handNumber,
        actionSequence = decisionPointOf(hand.log.events)!!.sequence,
        action = PlayerAction.Call(hand.state.seatToAct!!),
    )
}

internal class TurnClockExpiryTest {

    @Test
    fun nothingExpiresBeforeTheDeadline() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        registry.disconnect(room.code, seatedPlayer(onTurn, host, guest))
        val deadline = liveDeadline(registry, room.code)

        clock.advance(deadline - clock.nowMillis() - 1)
        val runnerBefore = registry.get(room.code)!!.runner

        val expiries = registry.expireTurnClocks()

        assertEquals(emptyList<TurnClockExpiry>(), expiries)
        assertTrue(registry.get(room.code)!!.awaySeats.isNotEmpty())
        assertSame(runnerBefore, registry.get(room.code)!!.runner)
    }

    @Test
    fun theDeadlineRunningOutFoldsTheAwaySeatOnTurn() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        registry.disconnect(room.code, seatedPlayer(onTurn, host, guest))
        val deadline = liveDeadline(registry, room.code)

        clock.advance(deadline - clock.nowMillis())
        val expiries = registry.expireTurnClocks()

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
        val deadline = liveDeadline(registry, room.code)

        clock.advance(deadline - clock.nowMillis())
        registry.expireTurnClocks()

        val after = registry.get(room.code)!!
        assertEquals(RoomState.PLAYING, after.state)
        assertFalse(after.awaySeats.isNotEmpty())
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
        val deadline = liveDeadline(registry, room.code)

        clock.advance(deadline - clock.nowMillis())
        val firstSweep = registry.expireTurnClocks()

        // "Does not fire twice" is only proved once the first sweep is shown to have actually
        // fired: a non-empty result, and the fold it produced visible in the hand log.
        assertEquals(1, firstSweep.size)
        val handOneActionsAfterFirstSweep = registry.get(room.code)!!.runner!!.log.hands.first().actions
        assertEquals(PlayerAction.Fold(onTurn), handOneActionsAfterFirstSweep.last())
        val roomAfterFirstSweep = registry.get(room.code)!!

        val secondSweep = registry.expireTurnClocks()

        assertEquals(emptyList<TurnClockExpiry>(), secondSweep)
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

        // Nothing is foldable yet: only the seat actually on turn owns a deadline (Room.clocked),
        // and that seat — connected — is not the one that just went away.
        val beforeTurnReaches = registry.get(room.code)!!
        assertTrue(beforeTurnReaches.absentSeats.isEmpty())

        // The turn reaches the away seat once the present seat acts, arming that seat's own
        // deadline for the first time — an away seat off turn owns no deadline of its own to run
        // out on its own schedule; TASK-130810 removed the standalone timer that once let it.
        val message = allInFrame(beforeTurnReaches)
        registry.act(room.code) { r -> r.act(message.action.seat, message, fixedSeeds) }
        val deadline = liveDeadline(registry, room.code)

        clock.advance(deadline - clock.nowMillis())
        val expiries = registry.expireTurnClocks()

        assertEquals(1, expiries.size)
        val handOneActions = registry.get(room.code)!!.runner!!.log.hands.first().actions
        assertEquals(PlayerAction.Fold(offTurn), handOneActions.last())
        assertEquals(setOf(offTurn), registry.get(room.code)!!.absentSeats)
    }

    @Test
    fun aPlayerWhoCameBackInTimeIsNotLatchedAbsent() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        val onTurnPlayer = seatedPlayer(onTurn, host, guest)
        registry.disconnect(room.code, onTurnPlayer)
        val deadline = liveDeadline(registry, room.code)
        val runnerBefore = registry.get(room.code)!!.runner

        clock.advance(deadline - clock.nowMillis() - 1_000)
        registry.resume(room.code, onTurnPlayer)
        clock.advance(1_000)

        val expiries = registry.expireTurnClocks()

        // Reconnecting cannot undo the ordinary per-decision allowance once it runs out — that
        // clock runs whether or not the seat is connected, exactly as for a seat that was never
        // away at all (aSeatOutOfTimeIsPlayedThoughItIsPresent) — it only prevents the latch a
        // still-away seat's own expiry would add. The give-up may end hand 1 outright (an
        // opening fold wins it uncontested), so the runner's own identity — not the live hand's
        // action count, which a fresh hand 2 would reset to zero — is what proves it happened.
        assertEquals(1, expiries.size)
        assertNotSame(runnerBefore, registry.get(room.code)!!.runner)
        assertTrue(registry.get(room.code)!!.absentSeats.isEmpty())
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

        // Only the seat actually on turn owns a live deadline (Room.clocked). The first sweep
        // latches that one and hands the turn to the other, still-away seat with a fresh deadline
        // of its own; both gone is reached only on the sweep that finds that second deadline too.
        val firstDeadline = liveDeadline(registry, room.code)
        clock.advance(firstDeadline - clock.nowMillis())
        val firstSweep = registry.expireTurnClocks()
        assertEquals(1, firstSweep.size)
        assertEquals(RoomState.PLAYING, registry.get(room.code)!!.state)

        val secondDeadline = liveDeadline(registry, room.code)
        clock.advance(secondDeadline - clock.nowMillis())
        val expiries = registry.expireTurnClocks()

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
        val deadline = liveDeadline(registry, room.code)

        clock.advance(deadline - clock.nowMillis())
        val expiries = registry.expireTurnClocks()

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

        // Both rooms were seated back to back with no clock advance in between, so their opening
        // decisions were clocked at the same instant and share the same deadline.
        val deadline = liveDeadline(registry, okRoom.code)
        clock.advance(deadline - clock.nowMillis())
        // Before the fix this threw the sink's exception straight out of `expireTurnClocks()`,
        // losing `okRoom`'s already-computed fold along with it.
        val expiries = registry.expireTurnClocks()

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

        // The turn reaches the away seat once the present seat acts, arming that seat's own
        // deadline for the first time — an away seat off turn owns no deadline of its own to run
        // out on its own schedule.
        val opening = registry.get(room.code)!!
        registry.act(room.code) { r -> r.act(onTurn, callFrame(opening), fixedSeeds) }
        val deadline = liveDeadline(registry, room.code)

        clock.advance(deadline - clock.nowMillis())
        val expiries = registry.expireTurnClocks()

        assertEquals(1, expiries.size)
        val presenceFrame = Addressed(onTurn, ServerMessage.OpponentPresence(SeatPresence.ABSENT))
        assertEquals(presenceFrame, expiries.single().outbound.first())
    }

    /**
     * The pairing half of [noFrameGoesToTheSeatThatExpired]: there, the seat that expires is on
     * turn; here it is off turn, so between the two, "no presence reaches the seat that just
     * expired" is proved for both concrete seat numbers, not just whichever the seed puts on turn.
     */
    @Test
    fun theSeatOffTurnExpiringTellsTheSeatOnTurn() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val onTurn = registry.get(room.code)!!.runner!!.hand!!.state.seatToAct!!
        val offTurn = 1 - onTurn
        registry.disconnect(room.code, seatedPlayer(offTurn, host, guest))

        // The turn reaches the away seat once the present seat acts, arming that seat's own
        // deadline for the first time.
        val opening = registry.get(room.code)!!
        registry.act(room.code) { r -> r.act(onTurn, callFrame(opening), fixedSeeds) }
        val deadline = liveDeadline(registry, room.code)

        clock.advance(deadline - clock.nowMillis())
        val expiries = registry.expireTurnClocks()

        assertEquals(1, expiries.size)
        val outbound = expiries.single().outbound
        assertTrue(outbound.contains(Addressed(onTurn, ServerMessage.OpponentPresence(SeatPresence.ABSENT))))
        assertTrue(outbound.none { (seat, message) -> seat == offTurn && message is ServerMessage.OpponentPresence })
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
        val deadline = liveDeadline(registry, room.code)

        clock.advance(deadline - clock.nowMillis())
        val expiries = registry.expireTurnClocks()

        assertEquals(1, expiries.size)
        val outbound = expiries.single().outbound
        val presenceFrame = Addressed(1 - onTurn, ServerMessage.OpponentPresence(SeatPresence.ABSENT))
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
        val deadline = liveDeadline(registry, room.code)

        clock.advance(deadline - clock.nowMillis())
        val expiries = registry.expireTurnClocks()

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
        val deadline = liveDeadline(registry, room.code)

        clock.advance(deadline - clock.nowMillis() - 1)
        val expiries = registry.expireTurnClocks()

        assertEquals(emptyList<TurnClockExpiry>(), expiries)
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

        // Only the seat actually on turn owns a live deadline; the first sweep latches that one
        // and hands the turn to the other, still-away seat with a fresh deadline of its own.
        val firstDeadline = liveDeadline(registry, room.code)
        clock.advance(firstDeadline - clock.nowMillis())
        registry.expireTurnClocks()

        val secondDeadline = liveDeadline(registry, room.code)
        clock.advance(secondDeadline - clock.nowMillis())
        val expiries = registry.expireTurnClocks()

        assertEquals(1, expiries.size)
        assertEquals(RoomState.ABANDONED, expiries.single().room.state)
        assertTrue(expiries.single().outbound.none { (_, message) -> message is ServerMessage.OpponentPresence })
    }

    @Test
    fun aSeatWhoseClockRanOutIsPlayed() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, SHORT_CLOCK_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        val actionsBeforeSweep = registry.get(room.code)!!.runner!!.hand!!.log.actions

        // Nobody disconnects: the connected seat on turn simply outlasts its own clock.
        clock.advance(SHORT_CLOCK_TIMEOUTS.turnMillis + SHORT_CLOCK_TIMEOUTS.timebankMillis)
        val expiries = registry.expireTurnClocks()

        assertEquals(1, expiries.size)
        val handOneActions = registry.get(room.code)!!.runner!!.log.hands.first().actions
        assertTrue(handOneActions.size > actionsBeforeSweep.size)
        // Presence, not the clock, decides the latch: a seat that ran out of time while
        // connected is late, not gone.
        assertTrue(registry.get(room.code)!!.absentSeats.isEmpty())
    }

    @Test
    fun nowIsReadOnceSoTheExpiryNeverPrecedesTheDeadline() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ", "V215DE1C"), clock, SHORT_CLOCK_TIMEOUTS, seeds = fixedSeeds)
        val deadlineAllowance = SHORT_CLOCK_TIMEOUTS.turnMillis + SHORT_CLOCK_TIMEOUTS.timebankMillis

        // The early room's opening decision is clocked at t=0, so its deadline sits exactly
        // deadlineAllowance away. The late room's is clocked one millisecond later, at t=1, so
        // its own deadline is one millisecond after the early room's — the two straddle
        // whatever single instant this pass reads `now` as.
        val earlyRoom = registry.create(newPlayerId())
        registry.join(earlyRoom.code, newPlayerId())

        clock.advance(1)

        val lateRoom = registry.create(newPlayerId())
        registry.join(lateRoom.code, newPlayerId())

        val earlyActionsBefore = registry.get(earlyRoom.code)!!.runner!!.hand!!.log.actions
        val lateActionsBefore = registry.get(lateRoom.code)!!.runner!!.hand!!.log.actions

        // From t=1 to t=deadlineAllowance: the early room's deadline is now reached exactly;
        // the late room's is still one instant away.
        clock.advance(deadlineAllowance - 1)
        val expiries = registry.expireTurnClocks()

        assertEquals(1, expiries.size)
        assertEquals(earlyRoom.code, expiries.single().room.code)
        val earlyActionsAfter = registry.get(earlyRoom.code)!!.runner!!.log.hands.first().actions
        assertTrue(earlyActionsAfter.size > earlyActionsBefore.size)
        val lateActionsAfter = registry.get(lateRoom.code)!!.runner!!.hand!!.log.actions
        assertEquals(lateActionsBefore, lateActionsAfter)
    }

    @Test
    fun anActAtTheDeadlineMinusOneMovesTheDuelAndTheNextSweepExpiresNothing() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, SHORT_CLOCK_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        val seated = (registry.join(room.code, guest) as JoinResult.Seated).room
        val deadline = seated.turnDeadline!!.expiresAt
        val frame = callFrame(seated)

        clock.advance(deadline - 1)
        val step = registry.act(room.code) { r -> r.act(frame.action.seat, frame, fixedSeeds) }

        assertNotNull(step)
        // The call does not end the hand, so hand 1 is still live: `log.hands` only ever
        // carries a *completed* hand, which this one is not.
        val handOneActionsAfterAct = registry.get(room.code)!!.runner!!.hand!!.log.actions
        assertEquals(1, handOneActionsAfterAct.size)

        val expiries = registry.expireTurnClocks()

        assertEquals(emptyList<TurnClockExpiry>(), expiries)
        val handOneActionsAfterSweep = registry.get(room.code)!!.runner!!.hand!!.log.actions
        assertEquals(handOneActionsAfterAct, handOneActionsAfterSweep)
    }

    @Test
    fun aSweepThatWinsMakesTheStaleActComeBackRejected() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, SHORT_CLOCK_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        val seated = (registry.join(room.code, guest) as JoinResult.Seated).room
        val staleFrame = callFrame(seated)

        clock.advance(SHORT_CLOCK_TIMEOUTS.turnMillis + SHORT_CLOCK_TIMEOUTS.timebankMillis)
        val expiries = registry.expireTurnClocks()

        assertEquals(1, expiries.size)
        val handOneActionsAfterSweep = registry.get(room.code)!!.runner!!.log.hands.first().actions
        assertEquals(1, handOneActionsAfterSweep.size)

        val staleStep = registry.act(room.code) { r -> r.act(staleFrame.action.seat, staleFrame, fixedSeeds) }

        assertNotNull(staleStep)
        val handOneActionsAfterStaleAct = registry.get(room.code)!!.runner!!.log.hands.first().actions
        assertEquals(handOneActionsAfterSweep, handOneActionsAfterStaleAct)
    }

    @Test
    fun aFinishedRoomsLateDisconnectDoesNotLeakTheRecordingClaim() = runBlocking {
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

        // The duel finishes normally, through act: the claim it takes in `recording` is
        // claimed and cleared correctly here, exactly as aFoldThatEndsTheDuelIsRecordedOnce
        // proves — this test is about what a *later*, unrelated sweep does to a room already
        // past that point.
        val finishFrame = foldFrame(registry.get(room.code)!!)
        registry.act(room.code) { r -> r.act(finishFrame.action.seat, finishFrame, fixedSeeds) }
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)
        assertEquals(1, recorded.size)

        // The remaining seat closes its tab well after the duel is over, and a later sweep finds
        // it away — the room's runner is finished, not live, but still not null. Room.giveUpTurn
        // refuses any room that is not PLAYING before it ever touches that runner, so this stale
        // one is never reached.
        val remainingPlayer = if (finishFrame.action.seat == 0) guest else host
        registry.disconnect(room.code, remainingPlayer)
        clock.advance(60_000)

        val expiries = registry.expireTurnClocks()

        // No throw reaching this line is necessary but not sufficient: a leaked `recording`
        // claim throws nowhere on its own, it just sits there. What it silently breaks is
        // what the two assertions below check directly — neither is satisfied by "it didn't
        // throw" alone.
        assertEquals(emptyList<TurnClockExpiry>(), expiries)
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)

        // RoomRegistry.offerRematch refuses as NOT_FINISHED whenever `recording[code]` still
        // names this room's current duel id — exactly the state a leaked claim leaves behind.
        val rematchResult = registry.offerRematch(room.code, host)
        assertTrue(rematchResult is RematchResult.Offered)

        // isReapable refuses unconditionally, regardless of how idle the room is, for the same
        // reason: a leaked claim makes `recording[code] == room.duelId` forever, and this room
        // would never appear in a reap() result again.
        clock.advance(TEST_TIMEOUTS.finishedMillis)
        val reaped = registry.reap()
        assertEquals(listOf(room.code), reaped)
    }

    /**
     * A `WAITING` room's lone host may disconnect before a guest ever joins
     * (`RoomRegistry.disconnect` allows it), and this documents what the sweep does about it:
     * nothing, on purpose — the room is left exactly as it was, still `WAITING`, and its own
     * idle timeout in [RoomRegistry.reap] is what eventually removes it.
     *
     * [Room.giveUpTurn]'s own first check refuses any room that is not [RoomState.PLAYING] before
     * touching anything else — a `WAITING` room, carrying no [Room.runner] at all, is refused by
     * that same line, before `giveUpTurn` ever reads a runner to be `null` on.
     */
    @Test
    fun aWaitingRoomsDisconnectedHostIsLeftForTheIdleTimeout() = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS, seeds = fixedSeeds)
        val host = newPlayerId()
        val room = registry.create(host)
        registry.disconnect(room.code, host)

        clock.advance(30_000)
        val expiries = registry.expireTurnClocks()

        assertEquals(emptyList<TurnClockExpiry>(), expiries)
        assertEquals(RoomState.WAITING, registry.get(room.code)!!.state)

        // Left alone by the sweep, but not forgotten: the ordinary WAITING idle timeout —
        // unrelated to the disconnect grace window this pass never touched — still reaps it.
        val reaped = registry.reap()
        assertEquals(listOf(room.code), reaped)
    }
}
