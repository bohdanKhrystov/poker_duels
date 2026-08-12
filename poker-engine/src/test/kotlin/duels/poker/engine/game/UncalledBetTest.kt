package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UncalledBetTest {

    @Test
    fun equalCommitmentsLeaveNothingUncalled() {
        val state = handState(
            seats = seats().mapIndexed { i, seat ->
                seat.copy(committedThisHand = 300)
            },
        )
        assertNull(uncalledPortion(state))
    }

    @Test
    fun theOvercommittedSeatGetsTheDifference() {
        val state = handState(
            seats = listOf(
                Seat(index = 0, stack = 0, committedThisHand = 300),
                Seat(index = 1, stack = 0, committedThisHand = 500),
            ),
        )
        val result = uncalledPortion(state)
        assertEquals(UncalledPortion(1, 200), result)
    }

    @Test
    fun eitherSeatCanBeTheOneOwed() {
        val state = handState(
            seats = listOf(
                Seat(index = 0, stack = 0, committedThisHand = 900),
                Seat(index = 1, stack = 0, committedThisHand = 100),
            ),
        )
        val result = uncalledPortion(state)
        assertEquals(UncalledPortion(0, 800), result)
    }

    @Test
    fun itReadsTheHandTotalNotTheStreetTotal() {
        val state = handState(
            seats = listOf(
                Seat(index = 0, stack = 0, committedThisStreet = 0, committedThisHand = 300),
                Seat(index = 1, stack = 0, committedThisStreet = 0, committedThisHand = 500),
            ),
        )
        val result = uncalledPortion(state)
        assertEquals(UncalledPortion(1, 200), result)
    }

    @Test
    fun anEmptyPortionIsNotAValue() {
        assertThrows(IllegalArgumentException::class.java) {
            UncalledPortion(0, 0)
        }
    }

    @Test
    fun aPortionBelongsToARealSeat() {
        assertThrows(IllegalArgumentException::class.java) {
            UncalledPortion(2, 100)
        }
    }
}
