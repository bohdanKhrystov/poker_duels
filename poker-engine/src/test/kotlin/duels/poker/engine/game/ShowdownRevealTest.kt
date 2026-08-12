package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class ShowdownRevealTest {
    @Test
    fun onlyTheWinnerShows() {
        val state = handState()

        assertEquals(listOf(0), revealOrder(state, listOf(0)))
    }

    @Test
    fun theLosingAggressorStillMucks() {
        val state = handState().copy(lastAggressor = 1)

        assertEquals(listOf(0), revealOrder(state, listOf(0)))
    }

    @Test
    fun aTieShowsBothHands() {
        val state = handState()

        val result = revealOrder(state, listOf(0, 1))

        assertEquals(2, result.size)
        assertEquals(setOf(0, 1), result.toSet())
    }

    @Test
    fun theLastAggressorShowsFirstInATie() {
        val aggressorIsSeatOne = handState().copy(lastAggressor = 1)
        val aggressorIsSeatZero = handState().copy(lastAggressor = 0)

        assertEquals(listOf(1, 0), revealOrder(aggressorIsSeatOne, listOf(0, 1)))
        assertEquals(listOf(0, 1), revealOrder(aggressorIsSeatZero, listOf(0, 1)))
    }

    @Test
    fun aCheckedFinalStreetShowsTheSeatOutOfPositionFirst() {
        val buttonOnZero = handState().copy(lastAggressor = null, buttonSeat = 0)
        val buttonOnOne = handState().copy(lastAggressor = null, buttonSeat = 1)

        assertEquals(listOf(1, 0), revealOrder(buttonOnZero, listOf(0, 1)))
        assertEquals(listOf(0, 1), revealOrder(buttonOnOne, listOf(0, 1)))
    }

    @Test
    fun rejectsAWinnerListThatIsNotOneOrTwoDistinctSeats() {
        val state = handState()

        assertThrows(IllegalArgumentException::class.java) { revealOrder(state, emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { revealOrder(state, listOf(0, 0)) }
    }
}
