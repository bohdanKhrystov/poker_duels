package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BettingProjectionTest {

    @Test
    fun foldMarksTheSeatAndClearsTheActor() {
        val state = handState()

        val result = applyBetting(state, PlayerFolded(1, 0))

        assertTrue(result.seat(0).hasFolded)
        assertNull(result.seatToAct)
        assertEquals(state.chipsInPlay, result.chipsInPlay)
        assertEquals(state.seat(0).stack, result.seat(0).stack)
        assertEquals(state.seat(1).stack, result.seat(1).stack)
        assertEquals(2, result.eventCount)
    }

    @Test
    fun checkClearsOnlyTheActor() {
        val state = handState()

        val result = applyBetting(state, PlayerChecked(1, 0))

        assertEquals(state.copy(seatToAct = null, eventCount = 2), result)
    }

    @Test
    fun callMovesChipsUpToTheBar() {
        val state = handState()
            .copy(seats = listOf(seats()[0].commit(100), seats()[1]), betToMatch = 300)

        val result = applyBetting(state, PlayerCalled(1, 0, 300))

        assertEquals(START_STACK - 300, result.seat(0).stack)
        assertEquals(300, result.seat(0).committedThisStreet)
        assertEquals(300, result.betToMatch)
    }

    @Test
    fun callAllInForLessLeavesTheBarWhereItIs() {
        val state = handState(seats(stack0 = 150)).copy(betToMatch = 300)

        val result = applyBetting(state, PlayerCalled(1, 0, 150))

        assertTrue(result.seat(0).isAllIn)
        assertEquals(0, result.seat(0).stack)
        assertEquals(300, result.betToMatch)
    }

    @Test
    fun betSetsTheBarAndDoublesTheMinimumRaise() {
        val state = handState()

        val result = applyBetting(state, PlayerBet(1, 0, 300))

        assertEquals(300, result.betToMatch)
        assertEquals(600, result.minRaiseTo)
    }

    @Test
    fun raiseAddsItsOwnIncrementToTheMinimum() {
        val state = handState().copy(betToMatch = 300, minRaiseTo = 600)

        val result = applyBetting(state, PlayerRaised(1, 1, 900))

        assertEquals(900, result.betToMatch)
        assertEquals(1_500, result.minRaiseTo)
    }

    @Test
    fun aFullAllInRaisesTheMinimum() {
        val state = handState().copy(betToMatch = 300, minRaiseTo = 600)

        val result = applyBetting(state, PlayerAllIn(1, 1, 900))

        assertEquals(900, result.betToMatch)
        assertEquals(1_500, result.minRaiseTo)
        assertTrue(result.seat(1).isAllIn)
    }

    @Test
    fun aShortAllInDoesNotRaiseTheMinimum() {
        val state = handState().copy(betToMatch = 300, minRaiseTo = 600)

        val result = applyBetting(state, PlayerAllIn(1, 1, 450))

        assertEquals(450, result.betToMatch)
        assertEquals(600, result.minRaiseTo)
    }

    @Test
    fun chipsAreConservedByEveryBettingEvent() {
        val betState = handState()
        assertEquals(betState.chipsInPlay, applyBetting(betState, PlayerBet(1, 0, 300)).chipsInPlay)

        val raiseState = handState().copy(betToMatch = 300, minRaiseTo = 600)
        assertEquals(raiseState.chipsInPlay, applyBetting(raiseState, PlayerRaised(1, 1, 900)).chipsInPlay)

        val callState = handState().copy(betToMatch = 300)
        assertEquals(callState.chipsInPlay, applyBetting(callState, PlayerCalled(1, 0, 300)).chipsInPlay)

        val allInState = handState(seats(stack0 = 150)).copy(betToMatch = 300)
        assertEquals(allInState.chipsInPlay, applyBetting(allInState, PlayerAllIn(1, 0, 150)).chipsInPlay)

        val foldState = handState()
        assertEquals(foldState.chipsInPlay, applyBetting(foldState, PlayerFolded(1, 0)).chipsInPlay)
    }

    @Test
    fun rejectsAnEventThatWouldOverdrawAStack() {
        val state = handState(seats(stack0 = 10_000))

        assertThrows(IllegalArgumentException::class.java) {
            applyBetting(state, PlayerBet(1, 0, 20_000))
        }
    }

    @Test
    fun aBetRecordsTheBettorAsTheLastAggressor() {
        val state = handState()

        val result = applyBetting(state, PlayerBet(1, 0, 300))

        assertEquals(0, result.lastAggressor)
    }

    @Test
    fun aRaiseRecordsTheRaiserAsTheLastAggressor() {
        val state = handState().copy(betToMatch = 300, minRaiseTo = 600)

        val result = applyBetting(state, PlayerRaised(1, 1, 900))

        assertEquals(1, result.lastAggressor)
    }

    @Test
    fun aFullAllInRecordsTheAggressor() {
        val state = handState().copy(betToMatch = 300, minRaiseTo = 600)

        val result = applyBetting(state, PlayerAllIn(1, 1, 900))

        assertEquals(1, result.lastAggressor)
    }

    @Test
    fun aShortAllInIsNotAggression() {
        val state = handState(seats(stack0 = 150)).copy(betToMatch = 300, lastAggressor = 1)

        val result = applyBetting(state, PlayerAllIn(1, 0, 150))

        assertEquals(1, result.lastAggressor)
    }

    @Test
    fun callingAndCheckingAreNotAggression() {
        val state = handState().copy(betToMatch = 300, lastAggressor = 1)

        val calledResult = applyBetting(state, PlayerCalled(1, 0, 300))
        assertEquals(1, calledResult.lastAggressor)

        val checkedResult = applyBetting(state.copy(betToMatch = 0), PlayerChecked(1, 0))
        assertEquals(1, checkedResult.lastAggressor)
    }
}
