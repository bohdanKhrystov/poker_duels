package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class HeadsUpOrderTest {
    @Test
    fun otherSeatFlipsTheIndex() {
        assertEquals(1, otherSeat(0))
        assertEquals(0, otherSeat(1))
    }

    @Test
    fun theButtonPostsTheSmallBlind() {
        assertEquals(0, smallBlindSeat(0))
        assertEquals(1, smallBlindSeat(1))
    }

    @Test
    fun theNonButtonPostsTheBigBlind() {
        assertEquals(1, bigBlindSeat(0))
        assertEquals(0, bigBlindSeat(1))
    }

    @Test
    fun theButtonActsFirstPreflop() {
        assertEquals(0, firstToActOn(Street.PREFLOP, 0))
        assertEquals(1, firstToActOn(Street.PREFLOP, 1))
    }

    @Test
    fun theNonButtonActsFirstOnEveryLaterStreet() {
        for (buttonSeat in 0..1) {
            assertEquals(otherSeat(buttonSeat), firstToActOn(Street.FLOP, buttonSeat))
            assertEquals(otherSeat(buttonSeat), firstToActOn(Street.TURN, buttonSeat))
            assertEquals(otherSeat(buttonSeat), firstToActOn(Street.RIVER, buttonSeat))
        }
    }

    @Test
    fun thereIsNoActionOrderAfterTheRiver() {
        assertThrows(IllegalArgumentException::class.java) {
            firstToActOn(Street.SHOWDOWN, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            firstToActOn(Street.COMPLETE, 0)
        }
    }

    @Test
    fun rejectsASeatOutsideTheTable() {
        assertThrows(IllegalArgumentException::class.java) {
            otherSeat(2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            firstToActOn(Street.PREFLOP, -1)
        }
    }
}
