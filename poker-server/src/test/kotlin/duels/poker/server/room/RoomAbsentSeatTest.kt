package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.PlayerAction
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.duel.act
import duels.poker.server.duel.decisionPointOf
import duels.poker.server.duel.startDuel
import duels.poker.server.protocol.Act
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

// FixedHands(3), not 1: a folded hand 1 must have somewhere to go, so a duel that stalled on an
// absent seat and a duel that correctly kept moving are told apart by which hand they land on.
private val threeHands = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(3))
private val seeds = HandSeedSource { 7L }

// Every fixture below that carries no turnDeadline at all is judged against this instant, so the
// absent-seat tests keep asking exactly what they asked before the clock became an input: no
// deadline can have expired by it.
private val beforeAnyDeadline = 3_000L

// The instant every deadline below runs out at, and the one every timed-out call is made at: the
// comparison is `>=`, so this exact instant is already too late.
private val deadlineAt = 10_000L

/**
 * This room with a [TurnDeadline] governing its live decision, running out at [expiresAt].
 *
 * The seat, hand and sequence are read off the live hand exactly as [Room.clocked] would derive
 * them, so no test has to know which seat opens a hand to time it out.
 */
private fun Room.withDeadlineAt(expiresAt: Long): Room {
    val hand = runner!!.hand!!
    return copy(
        turnDeadline = TurnDeadline(
            seat = hand.state.seatToAct!!,
            handNumber = hand.state.handNumber,
            actionSequence = decisionPointOf(hand.log.events)!!.sequence,
            bankBeginsAt = expiresAt,
            expiresAt = expiresAt,
        ),
    )
}

private fun playingRoom(): Room {
    val opened = Room.open(RoomCode("2B7KMNPQ"), PlayerId("host"), threeHands, now = 1_000L)
    val seated = (opened.join(PlayerId("guest"), now = 2_000L) as JoinResult.Seated).room
    val started = startDuel(seated.format, seated.openingButtonSeat, seed = 7L)
    return seated.copy(runner = started.runner, duelId = UUID.randomUUID())
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

/**
 * Builds a minimum-raise [Act] frame for whoever is on turn in [room]'s live hand.
 *
 * At the button's first decision, the big blind is already an outstanding wager, so the least
 * commital way to leave the opponent facing a real bet is a `Raise` to `state.minRaiseTo`, not a
 * `Bet` — `BET` is not even in the legal set once a blind is posted.
 */
private fun raiseFrame(room: Room): Act {
    val hand = room.runner!!.hand!!
    return Act(
        handNumber = hand.state.handNumber,
        actionSequence = decisionPointOf(hand.log.events)!!.sequence,
        action = PlayerAction.Raise(hand.state.seatToAct!!, to = hand.state.minRaiseTo),
    )
}

internal class RoomAbsentSeatTest {

    // A `Call` from `onTurn`, the button, is the heads-up big blind's option: the absent seat on
    // the other side of it owes nothing, in any hand, under any format, so `FOLD` is never legal
    // there and this fixture can never end in a fold. `ADR-0023` makes the give-up action `Check`
    // instead — these two cases prove the anti-stall property that still holds: the turn reaches
    // the absent seat, an action is appended, and the turn comes back to the present player with
    // the hand still live. The hand-ending path needs the other fixture, below, where the present
    // seat leaves the absent one facing a real bet.

    @Test
    fun aCallFromThePresentSeatIsFollowedByTheAbsentSeatsGiveUp() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val absent = room.copy(absentSeats = setOf(1 - onTurn))
        val frame = callFrame(room)

        val step = absent.act(onTurn, frame, seeds)

        assertNotNull(step)
        assertEquals(PlayerAction.Check(1 - onTurn), step!!.runner.hand!!.log.actions.last())
    }

    @Test
    fun theTurnReturnsToThePresentSeatAfterwards() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val absent = room.copy(absentSeats = setOf(1 - onTurn))
        val frame = callFrame(room)

        val step = absent.act(onTurn, frame, seeds)

        assertNotNull(step)
        assertEquals(onTurn, step!!.runner.hand!!.state.seatToAct)
        assertNull(step.runner.outcome)
    }

    @Test
    fun aBetFromThePresentSeatIsFollowedByTheAbsentSeatsFold() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val absent = room.copy(absentSeats = setOf(1 - onTurn))
        val frame = raiseFrame(room)

        val step = absent.act(onTurn, frame, seeds)

        assertNotNull(step)
        assertEquals(PlayerAction.Fold(1 - onTurn), step!!.runner.log.hands[0].actions.last())
    }

    @Test
    fun theAbsentSeatKeepsFoldingAndTheDuelAdvances() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val absent = room.copy(absentSeats = setOf(1 - onTurn))
        val frame = raiseFrame(room)

        val step = absent.act(onTurn, frame, seeds)

        assertNotNull(step)
        // Three, not two, and deliberately so: absentSeats persists across hands and heads-up
        // alternates the button, so the seat that folded hand one holds hand two's button — whose
        // opening decision owes the blind gap, making FOLD legal there too. It folds again before
        // anyone present acts, and hand three is the first live one.
        assertEquals(3, step!!.runner.hand!!.state.handNumber)
        assertNull(step.runner.outcome)
        // The hand number alone is the weaker form of this claim; hand two's own log is reachable
        // from the runner this ticket already touches, so assert the stronger one too: hand two
        // is exactly that seat folding, nothing else.
        assertEquals(listOf(PlayerAction.Fold(1 - onTurn)), step.runner.log.hands[1].actions)
    }

    @Test
    fun oneStepCarriesBothTheActionAndTheFold() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val absent = room.copy(absentSeats = setOf(1 - onTurn))
        val nobodyAbsent = room.copy(absentSeats = emptySet())
        val frame = callFrame(room)

        val absentStep = absent.act(onTurn, frame, seeds)
        val plainStep = nobodyAbsent.act(onTurn, frame, seeds)

        assertNotNull(absentStep)
        assertNotNull(plainStep)
        // The comparison, not the raw count, is the point: without it, a fold-through that never
        // ran would still leave `absentStep` looking green on its own.
        assertTrue(plainStep!!.outbound.size < absentStep!!.outbound.size)
        assertEquals(1, plainStep.runner.hand!!.state.handNumber)
    }

    @Test
    fun aRoomWithNobodyAbsentIsUnchanged() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val nobodyAbsent = room.copy(absentSeats = emptySet())
        val frame = callFrame(room)

        val step = nobodyAbsent.act(onTurn, frame, seeds)
        val expected = act(room.runner!!, onTurn, frame, seeds)

        assertNotNull(step)
        assertEquals(expected.runner.hand!!.state.handNumber, step!!.runner.hand!!.state.handNumber)
        assertEquals(expected.runner.hand!!.state.seatToAct, step.runner.hand!!.state.seatToAct)
        assertTrue(step.runner.hand!!.log.actions.none { it is PlayerAction.Fold })
    }

    @Test
    fun giveUpTurnFoldsTheSeatOnTurn() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val absent = room.copy(absentSeats = setOf(onTurn))

        val step = absent.giveUpTurn(beforeAnyDeadline, seeds)

        assertNotNull(step)
        assertEquals(PlayerAction.Fold(onTurn), step!!.step.runner.log.hands[0].actions.last())
    }

    @Test
    fun giveUpTurnAnswersNullWhenTheTurnIsNotTheirs() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val absent = room.copy(absentSeats = setOf(1 - onTurn))

        val step = absent.giveUpTurn(beforeAnyDeadline, seeds)

        assertNull(step)
    }

    @Test
    fun giveUpTurnAnswersNullWithNobodyAbsent() {
        val room = playingRoom()

        val step = room.giveUpTurn(beforeAnyDeadline, seeds)

        assertNull(step)
    }

    @Test
    fun giveUpTurnAnswersNullForARoomThatIsNotPlaying() {
        val waiting = Room.open(RoomCode("2B7KMNPQ"), PlayerId("host"), threeHands, now = 1_000L)
        val finished = playingRoom().copy(state = RoomState.FINISHED, absentSeats = setOf(0))

        assertNull(waiting.giveUpTurn(beforeAnyDeadline, seeds))
        assertNull(finished.giveUpTurn(beforeAnyDeadline, seeds))
    }

    @Test
    fun giveUpTurnIsNotSwallowedByARoomAwayOnAnotherSeat() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        // onTurn has already run out into absentSeats, but the opponent seat is separately away
        // on its own, unrelated socket. A giveUpTurn that (wrongly) deferred to any seat being
        // away would answer null here, and since nothing but this expiry path ever asks again,
        // the duel would stall forever.
        val awayAndAbsent = room.copy(
            awaySeats = setOf(1 - onTurn),
            absentSeats = setOf(onTurn),
        )
        assertTrue(awayAndAbsent.awaySeats.isNotEmpty())

        val step = awayAndAbsent.giveUpTurn(beforeAnyDeadline, seeds)

        assertNotNull(step)
        assertEquals(PlayerAction.Fold(onTurn), step!!.step.runner.log.hands[0].actions.last())
    }

    @Test
    fun aSeatOutOfTimeIsPlayedThoughItIsPresent() {
        val room = playingRoom().withDeadlineAt(deadlineAt)
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        // Nobody is gone: absence cannot be what moves this turn, only the clock.
        assertTrue(room.absentSeats.isEmpty() && room.awaySeats.isEmpty())

        val given = room.giveUpTurn(deadlineAt, seeds)

        assertNotNull(given)
        assertEquals(PlayerAction.Fold(onTurn), given!!.step.runner.log.hands[0].actions.last())
    }

    @Test
    fun aSeatInsideItsDeadlineIsNotPlayed() {
        val room = playingRoom().withDeadlineAt(deadlineAt)
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val runnerBefore = room.runner

        val inside = room.giveUpTurn(deadlineAt - 1, seeds)
        val expired = room.giveUpTurn(deadlineAt, seeds)

        assertNull(inside)
        assertSame(runnerBefore, room.runner)
        // One millisecond later the very same room does move: the null above is the deadline
        // talking, not a fixture that could never give up a turn at all.
        assertNotNull(expired)
        assertEquals(PlayerAction.Fold(onTurn), expired!!.step.runner.log.hands[0].actions.last())
    }

    // The next two share one fixture, and differ in one bit: the seat that runs out of time is
    // connected in the first and away in the second. Heads-up alternates the button, so the seat
    // that folds hand one holds hand two's button and is on turn again immediately — which is what
    // makes "exactly one decision" and "the next decision too" tell each other apart at all.

    @Test
    fun aTimedOutSeatGivesUpExactlyOneDecision() {
        val room = playingRoom()
        val opener = room.runner!!.hand!!.state.seatToAct!!
        val raised = room.act(opener, raiseFrame(room), seeds)!!
        val facing = room.copy(runner = raised.runner).withDeadlineAt(deadlineAt)
        val late = facing.runner!!.hand!!.state.seatToAct!!

        val given = facing.giveUpTurn(deadlineAt, seeds)

        assertNotNull(given)
        assertEquals(PlayerAction.Fold(late), given!!.step.runner.log.hands[0].actions.last())
        assertEquals(2, given.step.runner.hand!!.state.handNumber)
        assertEquals(late, given.step.runner.hand!!.state.seatToAct)
        assertEquals(emptyList<PlayerAction>(), given.step.runner.hand!!.log.actions)
    }

    @Test
    fun aTimedOutConnectedSeatIsNeverLatchedAbsent() {
        val room = playingRoom().withDeadlineAt(deadlineAt)
        val onTurn = room.runner!!.hand!!.state.seatToAct!!

        val given = room.giveUpTurn(deadlineAt, seeds)

        assertNotNull(given)
        // The give-up happened — without this the claim below holds for a room nothing touched.
        assertEquals(PlayerAction.Fold(onTurn), given!!.step.runner.log.hands[0].actions.last())
        assertEquals(emptySet<Int>(), given.room.absentSeats)
    }

    @Test
    fun aTimedOutAwaySeatIsLatchedAbsent() {
        val room = playingRoom().withDeadlineAt(deadlineAt)
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        // Its socket is still down at deadlineAt — the turn clock is what runs out here, so
        // nothing but presence deciding the latch can put this seat in absentSeats.
        val away = room.copy(awaySeats = setOf(onTurn))

        val given = away.giveUpTurn(deadlineAt, seeds)

        assertNotNull(given)
        assertEquals(setOf(onTurn), given!!.room.absentSeats)
        assertEquals(emptySet<Int>(), given.room.awaySeats)
    }

    @Test
    fun aLatchedSeatsNextDecisionIsPlayedAtTheSameInstant() {
        val room = playingRoom()
        val opener = room.runner!!.hand!!.state.seatToAct!!
        val raised = room.act(opener, raiseFrame(room), seeds)!!
        val facing = room.copy(runner = raised.runner).withDeadlineAt(deadlineAt)
        val late = facing.runner!!.hand!!.state.seatToAct!!
        val away = facing.copy(awaySeats = setOf(late))

        val given = away.giveUpTurn(deadlineAt, seeds)

        assertNotNull(given)
        assertEquals(setOf(late), given!!.room.absentSeats)
        // Hand three, where the connected twin of this fixture stopped at hand two: hand two's
        // button is this same latched seat, and its decision falls in this very call, with no
        // second deadline armed in between.
        assertEquals(3, given.step.runner.hand!!.state.handNumber)
        assertEquals(listOf(PlayerAction.Fold(late)), given.step.runner.log.hands[1].actions)
    }

    @Test
    fun bothSeatsGoneAbandonsInsteadOfPlayingItOut() {
        val room = playingRoom().withDeadlineAt(deadlineAt)
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val bothGone = room.copy(
            awaySeats = setOf(onTurn),
            absentSeats = setOf(1 - onTurn),
        )

        val given = bothGone.giveUpTurn(deadlineAt, seeds)

        assertNotNull(given)
        assertEquals(RoomState.ABANDONED, given!!.room.state)
        assertTrue(given.step.outbound.isEmpty())
        // Nothing was played out for nobody: the duel is exactly where it stood.
        assertSame(bothGone.runner, given.step.runner)
    }
}
