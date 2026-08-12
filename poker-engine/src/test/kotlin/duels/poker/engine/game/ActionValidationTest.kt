package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class ActionValidationTest {

    @Test
    fun aCompleteHandRejectsEveryAction() {
        val state = handState().copy(street = Street.COMPLETE)

        val rejection = rejectionFor(state, PlayerAction.Fold(0))

        assertEquals(Rejection.HandComplete, rejection)
    }

    @Test
    fun actingOutOfTurnIsRejected() {
        val state = handState().copy(seatToAct = 0)

        val rejection = rejectionFor(state, PlayerAction.Check(1))

        assertEquals(Rejection.NotYourTurn(0), rejection)
    }

    @Test
    fun checkingWhileFacingABetIsNotAllowed() {
        val state = handState().copy(betToMatch = 300)

        val rejection = rejectionFor(state, PlayerAction.Check(0))

        assertEquals(
            Rejection.ActionNotAllowed(ActionType.CHECK, legalActions(state).allowed),
            rejection,
        )
    }

    @Test
    fun foldingWithNothingToCallIsNotAllowed() {
        val state = handState()

        val rejection = rejectionFor(state, PlayerAction.Fold(0))

        assertEquals(
            Rejection.ActionNotAllowed(ActionType.FOLD, legalActions(state).allowed),
            rejection,
        )
    }

    @Test
    fun aBetBelowOneBigBlindNamesTheMinimum() {
        val state = handState()

        val rejection = rejectionFor(state, PlayerAction.Bet(0, 50))

        assertEquals(Rejection.AmountTooSmall(50, 100), rejection)
    }

    @Test
    fun aRaiseBelowTheMinimumNamesTheMinimum() {
        val state = handState().copy(betToMatch = 300, minRaiseTo = 600)

        val rejection = rejectionFor(state, PlayerAction.Raise(0, 400))

        assertEquals(Rejection.AmountTooSmall(400, 600), rejection)
    }

    @Test
    fun aRaiseAboveTheStackNamesTheMaximum() {
        val state = handState().copy(betToMatch = 300, minRaiseTo = 600)

        val rejection = rejectionFor(state, PlayerAction.Raise(0, 20_000))

        assertEquals(Rejection.AmountTooLarge(20_000, 10_000), rejection)
    }

    @Test
    fun aLegalActionIsNotRejected() {
        val state = handState()

        assertNull(rejectionFor(state, PlayerAction.Check(0)))
        assertNull(rejectionFor(state, PlayerAction.Bet(0, 100)))
        assertNull(rejectionFor(state, PlayerAction.AllIn(0)))
    }

    @Test
    fun theMinimumItselfIsLegal() {
        val state = handState().copy(betToMatch = 300, minRaiseTo = 600)

        val rejection = rejectionFor(state, PlayerAction.Raise(0, 600))

        assertNull(rejection)
    }
}
