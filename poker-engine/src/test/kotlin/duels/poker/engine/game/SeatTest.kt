package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SeatTest {
    @Test
    fun buildsASeatWithDefaults() {
        val seat = Seat(index = 0, stack = 10_000)
        assertEquals(0, seat.index)
        assertEquals(10_000, seat.stack)
        assertEquals(emptyList<Any>(), seat.holeCards)
        assertEquals(0, seat.committedThisStreet)
        assertEquals(0, seat.committedThisHand)
        assertFalse(seat.hasFolded)
        assertFalse(seat.isAllIn)
    }

    @Test
    fun holdsExactlyTwoHoleCards() {
        val originalHoleCards = cards("As Kd")
        val seat = Seat(index = 0, stack = 10_000, holeCards = originalHoleCards)
        val copiedSeat = seat.copy()
        assertEquals(originalHoleCards, copiedSeat.holeCards)
    }

    @Test
    fun rejectsASeatIndexOutsideZeroAndOne() {
        assertThrows<IllegalArgumentException> {
            Seat(index = -1, stack = 10_000)
        }
        assertThrows<IllegalArgumentException> {
            Seat(index = 2, stack = 10_000)
        }
    }

    @Test
    fun rejectsANegativeStack() {
        assertThrows<IllegalArgumentException> {
            Seat(index = 0, stack = -1)
        }
    }

    @Test
    fun rejectsAnyHoleCardCountButZeroOrTwo() {
        assertThrows<IllegalArgumentException> {
            Seat(index = 0, stack = 10_000, holeCards = cards("As"))
        }
        assertThrows<IllegalArgumentException> {
            Seat(index = 0, stack = 10_000, holeCards = cards("As Kd Qh"))
        }
    }

    @Test
    fun rejectsDuplicateHoleCards() {
        assertThrows<IllegalArgumentException> {
            Seat(index = 0, stack = 10_000, holeCards = listOf(cards("As")[0], cards("As")[0]))
        }
    }

    @Test
    fun rejectsStreetCommitmentAboveHandCommitment() {
        assertThrows<IllegalArgumentException> {
            Seat(index = 0, stack = 10_000, committedThisStreet = 100, committedThisHand = 50)
        }
    }

    @Test
    fun equalSeatsAreEqual() {
        val seat1 = Seat(index = 0, stack = 10_000)
        val seat2 = Seat(index = 0, stack = 10_000)
        assertEquals(seat1, seat2)
        assertEquals(seat1.hashCode(), seat2.hashCode())
    }
}
