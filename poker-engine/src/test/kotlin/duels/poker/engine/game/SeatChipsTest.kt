package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeatChipsTest {

    @Test
    fun commitMovesChipsFromStackToCommitment() {
        val seat = Seat(0, 1_000).commit(300)

        assertEquals(700, seat.stack)
        assertEquals(300, seat.committedThisStreet)
        assertEquals(300, seat.committedThisHand)
        assertFalse(seat.isAllIn)
    }

    @Test
    fun commitAccumulatesWithinAStreet() {
        val seat = Seat(0, 1_000).commit(100).commit(200)

        assertEquals(700, seat.stack)
        assertEquals(300, seat.committedThisStreet)
        assertEquals(300, seat.committedThisHand)
    }

    @Test
    fun commitOfTheWholeStackIsAllIn() {
        val seat = Seat(0, 500).commit(500)

        assertEquals(0, seat.stack)
        assertTrue(seat.isAllIn)
    }

    @Test
    fun commitOfZeroFromAnEmptyStackIsNotAllIn() {
        val seat = Seat(0, 0).commit(0)

        assertFalse(seat.isAllIn)
    }

    @Test
    fun commitRejectsMoreThanTheStack() {
        val seat = Seat(0, 500)

        assertThrows(IllegalArgumentException::class.java) { seat.commit(501) }
    }

    @Test
    fun commitRejectsANegativeAmount() {
        val seat = Seat(0, 500)

        assertThrows(IllegalArgumentException::class.java) { seat.commit(-1) }
    }

    @Test
    fun awardAddsToTheStackOnly() {
        val seat = Seat(0, 0, committedThisHand = 500, isAllIn = true).award(1_000)

        assertEquals(1_000, seat.stack)
        assertEquals(500, seat.committedThisHand)
        assertTrue(seat.isAllIn)
    }

    @Test
    fun awardRejectsANegativeAmount() {
        val seat = Seat(0, 0)

        assertThrows(IllegalArgumentException::class.java) { seat.award(-1) }
    }

    @Test
    fun collectedClearsOnlyTheStreetCommitment() {
        val seat = Seat(0, 1_000).commit(300).collected()

        assertEquals(0, seat.committedThisStreet)
        assertEquals(300, seat.committedThisHand)
        assertEquals(700, seat.stack)
    }

    @Test
    fun chipsAreConservedByCommitAndAward() {
        var seat = Seat(0, 1_000)
        seat = seat.commit(100)
        seat = seat.commit(150)
        seat = seat.commit(50)

        assertEquals(1_000, seat.stack + seat.committedThisStreet)

        // Fresh seat: the 300 goes to the pot and comes straight back, so nothing
        // is created or destroyed.
        val roundTripped = Seat(0, 1_000).commit(300).collected().award(300)

        assertEquals(1_000, roundTripped.stack)
    }
}
