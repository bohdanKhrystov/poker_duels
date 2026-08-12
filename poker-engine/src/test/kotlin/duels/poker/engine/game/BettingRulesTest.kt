package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BettingRulesTest {

    @Test
    fun aFreshStreetAllowsCheckAndBet() {
        val state = handState()

        val legal = legalActions(state)

        assertEquals(setOf(ActionType.CHECK, ActionType.BET, ActionType.ALL_IN), legal.allowed)
        assertEquals(100, legal.minBetTo)
    }

    @Test
    fun facingABetAllowsFoldCallAndRaise() {
        val state = handState().copy(betToMatch = 300, minRaiseTo = 600)

        val legal = legalActions(state)

        assertEquals(
            setOf(ActionType.FOLD, ActionType.CALL, ActionType.RAISE, ActionType.ALL_IN),
            legal.allowed,
        )
    }

    @Test
    fun callToIsAStreetTotalNotAnIncrement() {
        val state = handState(seats = listOf(seats()[0].commit(100), seats()[1]))
            .copy(betToMatch = 300, minRaiseTo = 600)

        val legal = legalActions(state)

        assertEquals(300, legal.callTo)
    }

    @Test
    fun theBigBlindKeepsItsOptionPreflop() {
        val state = handState(seats = listOf(seats()[0].commit(100), seats()[1]))
            .copy(betToMatch = 100, minRaiseTo = 200)

        val legal = legalActions(state)

        assertTrue(legal.allows(ActionType.CHECK))
        assertTrue(legal.allows(ActionType.RAISE))
        assertTrue(!legal.allows(ActionType.BET))
        assertTrue(!legal.allows(ActionType.FOLD))
    }

    @Test
    fun theMinimumRaiseComesFromTheState() {
        val state = handState().copy(betToMatch = 40, minRaiseTo = 70)

        val legal = legalActions(state)

        assertEquals(70, legal.minRaiseTo)
    }

    @Test
    fun theMinimumBetIsOneBigBlind() {
        val state = handState()

        val legal = legalActions(state)

        assertEquals(state.bigBlind, legal.minBetTo)
    }

    @Test
    fun allInToIsTheWholeStackPlusWhatIsAlreadyIn() {
        val state = handState(seats = listOf(seats()[0].commit(100).copy(stack = 900), seats()[1]))

        val legal = legalActions(state)

        assertEquals(1_000, legal.allInTo)
    }

    @Test
    fun noSeatToActMeansNoLegalActions() {
        val state = handState().copy(seatToAct = null)

        val legal = legalActions(state)

        assertEquals(emptySet<ActionType>(), legal.allowed)
    }

    @Test
    fun aCompleteHandHasNoLegalActions() {
        val state = handState().copy(street = Street.COMPLETE)

        val legal = legalActions(state)

        assertEquals(emptySet<ActionType>(), legal.allowed)
    }

    @Test
    fun aFoldedSeatHasNoLegalActions() {
        val state = handState(seats = listOf(seats()[0].copy(hasFolded = true), seats()[1]))

        val legal = legalActions(state)

        assertEquals(emptySet<ActionType>(), legal.allowed)
    }
}
