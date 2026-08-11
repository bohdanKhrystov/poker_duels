package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test

class GameStatesTest {
    @Test
    fun handStateIsAFreshPreflopPosition() {
        val state = handState()
        assertEquals(Street.PREFLOP, state.street)
        assertEquals(Board.EMPTY, state.board)
        assertEquals(0, state.pot)
        assertEquals(0, state.betToMatch)
        assertEquals(BIG_BLIND, state.minRaiseTo)
        assertEquals(0, state.seatToAct)
        assertEquals(0, state.buttonSeat)
        assertEquals(0, state.eventCount)
    }

    @Test
    fun handStateStartsWithEqualStacks() {
        val state = handState()
        assertEquals(START_STACK, state.seats[0].stack)
        assertEquals(START_STACK, state.seats[1].stack)
        assertEquals(2 * START_STACK, state.chipsInPlay)
    }

    @Test
    fun seatsAcceptDifferentStacks() {
        val state = handState(seats(500, 1_500))
        assertEquals(500, state.seats[0].stack)
        assertEquals(1_500, state.seats[1].stack)
    }

    @Test
    fun handStateIsCopyable() {
        val original = handState()
        val modified = original.copy(pot = 300)
        assertEquals(300, modified.pot)
        assertEquals(0, original.pot)
        assertNotSame(original, modified)
    }
}
