package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BettingRulesAllInTest {

    @Test
    fun facingAnAllInOpponentLeavesOnlyFoldAndCall() {
        val state = handState(
            seats = listOf(
                seats()[0],
                seats()[1].copy(stack = 0, committedThisStreet = 450, committedThisHand = 450, isAllIn = true),
            ),
        ).copy(betToMatch = 450, minRaiseTo = 900)

        val legal = legalActions(state)

        assertEquals(setOf(ActionType.FOLD, ActionType.CALL), legal.allowed)
    }

    @Test
    fun aShortAllInDoesNotReopenTheBetting() {
        val state = handState(
            seats = listOf(
                seats()[0].copy(committedThisStreet = 300, committedThisHand = 300),
                seats()[1].copy(stack = 0, committedThisStreet = 450, committedThisHand = 450, isAllIn = true),
            ),
        ).copy(betToMatch = 450, minRaiseTo = 600)

        val legal = legalActions(state)

        assertEquals(setOf(ActionType.FOLD, ActionType.CALL), legal.allowed)
        assertEquals(450, legal.callTo)
    }

    @Test
    fun aSeatThatCannotCoverTheBetMayNotRaise() {
        val state = handState(seats = listOf(seats(stack0 = 150)[0], seats()[1]))
            .copy(betToMatch = 300, minRaiseTo = 600)

        val legal = legalActions(state)

        assertEquals(setOf(ActionType.FOLD, ActionType.CALL, ActionType.ALL_IN), legal.allowed)
        assertEquals(150, legal.callTo)
    }

    @Test
    fun aShortStackCallsAllInForLess() {
        val state = handState(seats = listOf(seats(stack0 = 150)[0], seats()[1]))
            .copy(betToMatch = 300, minRaiseTo = 600)

        val legal = legalActions(state)

        assertEquals(legal.allInTo, legal.callTo)
    }

    @Test
    fun aStackThatReachesExactlyTheBarCannotRaise() {
        val state = handState(seats = listOf(seats(stack0 = 900)[0], seats()[1]))
            .copy(betToMatch = 900, minRaiseTo = 1_800)

        val legal = legalActions(state)

        assertEquals(setOf(ActionType.FOLD, ActionType.CALL, ActionType.ALL_IN), legal.allowed)
        assertEquals(900, legal.callTo)
        assertEquals(100, legal.minBetTo)
        assertEquals(900, legal.minRaiseTo)
        assertEquals(900, legal.allInTo)
    }

    @Test
    fun aShortStackMayBetLessThanABigBlind() {
        val state = handState(seats = listOf(seats(stack0 = 60)[0], seats()[1]))

        val legal = legalActions(state)

        assertEquals(true, legal.allows(ActionType.BET))
        assertEquals(60, legal.minBetTo)
    }

    @Test
    fun aShortStackMayRaiseForLessThanTheMinimum() {
        val state = handState(seats = listOf(seats(stack0 = 500)[0], seats()[1]))
            .copy(betToMatch = 300, minRaiseTo = 600)

        val legal = legalActions(state)

        assertEquals(true, legal.allows(ActionType.RAISE))
        assertEquals(500, legal.minRaiseTo)
    }

    @Test
    fun anAllInOpponentCannotBeBetInto() {
        val state = handState(
            seats = listOf(
                seats()[0],
                seats()[1].copy(stack = 0, committedThisStreet = 0, isAllIn = true),
            ),
        )

        val legal = legalActions(state)

        assertEquals(setOf(ActionType.CHECK), legal.allowed)
    }

    @Test
    fun aLiveOpponentStillAllowsTheFullSet() {
        val state = handState().copy(betToMatch = 300, minRaiseTo = 600)

        val legal = legalActions(state)

        assertEquals(
            setOf(ActionType.FOLD, ActionType.CALL, ActionType.RAISE, ActionType.ALL_IN),
            legal.allowed,
        )
    }
}
