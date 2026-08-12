package duels.poker.engine.game

import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultPokerEngineTest {
    @Test
    fun anIllegalActionComesBackRejectedAndUnchanged() {
        val state = handState().copy(betToMatch = 300)
        val action = PlayerAction.Check(0)

        val result = DefaultPokerEngine.handle(state, action)

        assertTrue(result.isRejected)
        assertEquals(state, result.newState)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun anOutOfTurnActionIsRejected() {
        val state = handState()
        val action = PlayerAction.Fold(1)

        val result = DefaultPokerEngine.handle(state, action)

        assertEquals(Rejection.NotYourTurn(0), result.rejection)
    }

    @Test
    fun aLegalBetEmitsOnePlayerBetAndMovesTheChips() {
        val state = handState()
        val action = PlayerAction.Bet(0, to = 300)

        val result = DefaultPokerEngine.handle(state, action)

        assertFalse(result.isRejected)
        assertEquals(
            listOf(PlayerBet(sequence = 0, seat = 0, to = 300), ActionOn(sequence = 1, seat = 1)),
            result.events,
        )
        assertEquals(9_700, result.newState.seat(0).stack)
        assertEquals(300, result.newState.betToMatch)
    }

    @Test
    fun aCallCommitsUpToTheBar() {
        val state = committedFacingABet()
        val action = PlayerAction.Call(0)

        val result = DefaultPokerEngine.handle(state, action)

        assertFalse(result.isRejected)
        assertEquals(300, result.newState.seat(0).committedThisStreet)
    }

    @Test
    fun chipsAreConservedByAnAcceptedAction() {
        acceptedCases().forEach { (state, action) ->
            val result = DefaultPokerEngine.handle(state, action)
            assertFalse(result.isRejected)
            assertEquals(state.chipsInPlay, result.newState.chipsInPlay)
        }
    }

    @Test
    fun theEventsDescribeTheTransition() {
        acceptedCases().forEach { (state, action) ->
            val result = DefaultPokerEngine.handle(state, action)
            assertEventsDescribeTheTransition(state, result)
        }
    }

    @Test
    fun theSequenceContinuesFromEventCount() {
        val state = handState().copy(eventCount = 5)
        val action = PlayerAction.Check(0)

        val result = DefaultPokerEngine.handle(state, action)

        assertEquals(2, result.events.size)
        assertEquals(listOf(5, 6), result.events.map { it.sequence })
        assertEquals(7, result.newState.eventCount)
    }

    @Test
    fun theSmallBlindActsFirstPreflop() {
        val state = openedHand()

        assertEquals(0, state.seatToAct)
    }

    @Test
    fun aLimpPassesTheOptionToTheBigBlind() {
        val state = openedHand()

        val result = DefaultPokerEngine.handle(state, PlayerAction.Call(0))

        assertEquals(ActionOn(sequence = 7, seat = 1), result.events.last())
        assertEquals(1, result.newState.seatToAct)
    }

    @Test
    fun aRaisePassesTheActionBack() {
        val state = openedHand()
        val afterCall = DefaultPokerEngine.handle(state, PlayerAction.Call(0)).newState

        val result = DefaultPokerEngine.handle(afterCall, PlayerAction.Raise(1, to = 300))

        assertEquals(0, result.newState.seatToAct)
    }

    @Test
    fun theActionAlternatesUntilTheRoundEnds() {
        val state = openedHand()

        val afterCall = DefaultPokerEngine.handle(state, PlayerAction.Call(0))
        val afterRaise = DefaultPokerEngine.handle(afterCall.newState, PlayerAction.Raise(1, to = 300))
        val afterFinalCall = DefaultPokerEngine.handle(afterRaise.newState, PlayerAction.Call(0))

        assertEquals(1, afterCall.newState.seatToAct)
        assertEquals(0, afterRaise.newState.seatToAct)
        assertNull(afterFinalCall.newState.seatToAct)
    }

    @Test
    fun aCompletedRoundNamesNoNewActor() {
        val state = openedHand()
        val afterCall = DefaultPokerEngine.handle(state, PlayerAction.Call(0)).newState

        val result = DefaultPokerEngine.handle(afterCall, PlayerAction.Check(1))

        assertTrue(result.events.none { it is ActionOn })
        assertNull(result.newState.seatToAct)
    }

    @Test
    fun theAlternationEventsDescribeTheTransition() {
        val state = openedHand()

        val afterCall = DefaultPokerEngine.handle(state, PlayerAction.Call(0))
        assertEventsDescribeTheTransition(state, afterCall)

        val afterRaise = DefaultPokerEngine.handle(afterCall.newState, PlayerAction.Raise(1, to = 300))
        assertEventsDescribeTheTransition(afterCall.newState, afterRaise)

        val afterFinalCall = DefaultPokerEngine.handle(afterRaise.newState, PlayerAction.Call(0))
        assertEventsDescribeTheTransition(afterRaise.newState, afterFinalCall)
    }

    @Test
    fun theSequencesStayDense() {
        val state = openedHand()

        val afterCall = DefaultPokerEngine.handle(state, PlayerAction.Call(0))
        val afterRaise = DefaultPokerEngine.handle(afterCall.newState, PlayerAction.Raise(1, to = 300))
        val afterFinalCall = DefaultPokerEngine.handle(afterRaise.newState, PlayerAction.Call(0))

        val sequences = (afterCall.events + afterRaise.events + afterFinalCall.events).map { it.sequence }

        assertEquals((state.eventCount until state.eventCount + sequences.size).toList(), sequences)
    }

    /** Hand 1, button on seat 0, both seats stacked 10,000, blinds 50/100, opened for action. */
    private fun openedHand(): GameState =
        startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L)).newState

    /** Seat 0 has 100 already down this street, facing a bet of 300. */
    private fun committedFacingABet(): GameState {
        val committedSeats = listOf(
            Seat(index = 0, stack = 9_900, committedThisStreet = 100, committedThisHand = 100),
            Seat(index = 1, stack = 9_700, committedThisStreet = 300, committedThisHand = 300),
        )
        return handState(committedSeats).copy(betToMatch = 300, seatToAct = 0)
    }

    /** A bet, a call, a fold and an all-in, each legal for the state it pairs with. */
    private fun acceptedCases(): List<Pair<GameState, PlayerAction>> {
        val betState = handState()
        val callState = committedFacingABet()
        val foldState = committedFacingABet()
        val allInState = handState()

        return listOf(
            betState to PlayerAction.Bet(0, to = 300),
            callState to PlayerAction.Call(0),
            foldState to PlayerAction.Fold(0),
            allInState to PlayerAction.AllIn(0),
        )
    }
}
