package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FoldEndsTheHandTest {
    @Test
    fun aFoldEmitsPlayerFoldedThenBettingRoundEnded() {
        val state = handState().copy(betToMatch = 300)
        val action = PlayerAction.Fold(0)

        val result = DefaultPokerEngine.handle(state, action)

        assertFalse(result.isRejected)
        assertEquals(2, result.events.size)
        assertEquals(PlayerFolded(sequence = 0, seat = 0), result.events[0])
        assertEquals(BettingRoundEnded(sequence = 1, street = Street.PREFLOP), result.events[1])
    }

    @Test
    fun aFoldSweepsEveryCommitmentIntoThePot() {
        val committedSeats = listOf(
            Seat(index = 0, stack = 9_700, committedThisStreet = 100, committedThisHand = 100),
            Seat(index = 1, stack = 9_700, committedThisStreet = 300, committedThisHand = 300),
        )
        val state = handState(committedSeats).copy(
            street = Street.FLOP,
            board = Board(cards("2c 3c 4c")),
            betToMatch = 300,
            seatToAct = 0,
            pot = 0,
        )
        val action = PlayerAction.Fold(0)

        val result = DefaultPokerEngine.handle(state, action)

        assertFalse(result.isRejected)
        assertEquals(400, result.newState.pot)
        assertEquals(0, result.newState.seat(0).committedThisStreet)
        assertEquals(0, result.newState.seat(1).committedThisStreet)
    }

    @Test
    fun aFoldLeavesNobodyToAct() {
        val committedSeats = listOf(
            Seat(index = 0, stack = 9_900, committedThisStreet = 100, committedThisHand = 100),
            Seat(index = 1, stack = 9_700, committedThisStreet = 300, committedThisHand = 300),
        )
        val state = handState(committedSeats).copy(betToMatch = 300, seatToAct = 0)
        val action = PlayerAction.Fold(0)

        val result = DefaultPokerEngine.handle(state, action)

        assertFalse(result.isRejected)
        assertNull(result.newState.seatToAct)
        assertTrue(result.newState.seat(0).hasFolded)
    }

    @Test
    fun aFoldDealsNoCards() {
        val state = handState()
        val action = PlayerAction.Fold(0)

        val before = state.copy()
        val result = DefaultPokerEngine.handle(state, action)

        assertEquals(before.board, result.newState.board)
        assertEquals(before.deck.remaining, result.newState.deck.remaining)
    }

    @Test
    fun chipsAreConservedByAFold() {
        val committedSeats = listOf(
            Seat(index = 0, stack = 9_900, committedThisStreet = 100, committedThisHand = 100),
            Seat(index = 1, stack = 9_700, committedThisStreet = 300, committedThisHand = 300),
        )
        val state = handState(committedSeats).copy(betToMatch = 300, seatToAct = 0, pot = 0)
        val action = PlayerAction.Fold(0)

        val beforeChips = state.chipsInPlay
        val result = DefaultPokerEngine.handle(state, action)
        val afterChips = result.newState.chipsInPlay

        assertEquals(beforeChips, afterChips)
    }

    @Test
    fun theUncalledBetIsStillRecoverable() {
        val committedSeats = listOf(
            Seat(index = 0, stack = 9_700, committedThisStreet = 0, committedThisHand = 300),
            Seat(index = 1, stack = 9_600, committedThisStreet = 200, committedThisHand = 500),
        )
        val state = handState(committedSeats).copy(
            street = Street.FLOP,
            board = Board(cards("2c 3c 4c")),
            betToMatch = 200,
            seatToAct = 0,
            pot = 400,
        )
        val action = PlayerAction.Fold(0)

        val result = DefaultPokerEngine.handle(state, action)

        assertEquals(200, result.newState.seat(1).committedThisHand - result.newState.seat(0).committedThisHand)
    }

    @Test
    fun noFurtherActionIsAcceptedAfterAFold() {
        val committedSeats = listOf(
            Seat(index = 0, stack = 9_900, committedThisStreet = 100, committedThisHand = 100),
            Seat(index = 1, stack = 9_700, committedThisStreet = 300, committedThisHand = 300),
        )
        val state = handState(committedSeats).copy(betToMatch = 300, seatToAct = 0)
        val foldAction = PlayerAction.Fold(0)

        val afterFold = DefaultPokerEngine.handle(state, foldAction).newState
        val checkAction = PlayerAction.Check(1)
        val result = DefaultPokerEngine.handle(afterFold, checkAction)

        assertTrue(result.isRejected)
        assertEquals(Rejection.NotYourTurn(null), result.rejection)
    }
}
