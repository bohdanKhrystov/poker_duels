package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** [settleHand] splits a swept hand between two equal winners. */
internal class SplitPotTest {

    @Test
    fun anEvenPotSplitsInHalf() {
        val before = handState().copy(
            pot = 600,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 300),
                Seat(index = 1, stack = START_STACK, committedThisHand = 300),
            ),
        )

        val result = settleHand(before, listOf(0, 1))

        val awards = result.events.filterIsInstance<PotAwarded>()
        assertEquals(2, awards.size)
        assertEquals(300, awards[0].amount)
        assertEquals(300, awards[1].amount)
        assertEquals(before.seat(0).stack + 300, result.newState.seat(0).stack)
        assertEquals(before.seat(1).stack + 300, result.newState.seat(1).stack)
    }

    @Test
    fun theOddChipGoesToTheSeatOutOfPosition() {
        val before = handState().copy(
            buttonSeat = 0,
            pot = 601,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 301),
                Seat(index = 1, stack = START_STACK, committedThisHand = 301),
            ),
        )

        val result = settleHand(before, listOf(0, 1))

        val awards = result.events.filterIsInstance<PotAwarded>().associateBy { it.seat }
        assertEquals(300, awards.getValue(0).amount)
        assertEquals(301, awards.getValue(1).amount)
    }

    @Test
    fun theOddChipFollowsTheButton() {
        val before = handState().copy(
            buttonSeat = 1,
            pot = 601,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 301),
                Seat(index = 1, stack = START_STACK, committedThisHand = 301),
            ),
        )

        val result = settleHand(before, listOf(0, 1))

        val awards = result.events.filterIsInstance<PotAwarded>().associateBy { it.seat }
        assertEquals(301, awards.getValue(0).amount)
        assertEquals(300, awards.getValue(1).amount)
    }

    @Test
    fun theAwardsComeInSeatOrder() {
        val before = handState().copy(
            buttonSeat = 0,
            pot = 601,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 301),
                Seat(index = 1, stack = START_STACK, committedThisHand = 301),
            ),
        )

        val result = settleHand(before, listOf(0, 1))

        val awards = result.events.filterIsInstance<PotAwarded>()
        assertEquals(0, awards[0].seat)
        assertEquals(1, awards[1].seat)
    }

    @Test
    fun anUncalledBetIsReturnedBeforeASplit() {
        val before = handState().copy(
            pot = 800,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 300),
                Seat(index = 1, stack = START_STACK, committedThisHand = 500),
            ),
        )

        val result = settleHand(before, listOf(0, 1))

        val returned = result.events.filterIsInstance<UncalledBetReturned>().single()
        val awards = result.events.filterIsInstance<PotAwarded>()
        assertEquals(1, returned.seat)
        assertEquals(200, returned.amount)
        assertEquals(2, awards.size)
        assertEquals(300, awards[0].amount)
        assertEquals(300, awards[1].amount)
        assertTrue(result.events.indexOf(returned) < result.events.indexOf(awards[0]))
        assertTrue(result.events.indexOf(returned) < result.events.indexOf(awards[1]))
    }

    @Test
    fun aSplitConservesEveryChip() {
        val positions = listOf(
            handState().copy(
                pot = 600,
                seats = listOf(
                    Seat(index = 0, stack = START_STACK, committedThisHand = 300),
                    Seat(index = 1, stack = START_STACK, committedThisHand = 300),
                ),
            ),
            handState().copy(
                buttonSeat = 0,
                pot = 601,
                seats = listOf(
                    Seat(index = 0, stack = START_STACK, committedThisHand = 301),
                    Seat(index = 1, stack = START_STACK, committedThisHand = 301),
                ),
            ),
            handState().copy(
                buttonSeat = 1,
                pot = 601,
                seats = listOf(
                    Seat(index = 0, stack = START_STACK, committedThisHand = 301),
                    Seat(index = 1, stack = START_STACK, committedThisHand = 301),
                ),
            ),
            handState().copy(
                pot = 800,
                seats = listOf(
                    Seat(index = 0, stack = START_STACK, committedThisHand = 300),
                    Seat(index = 1, stack = START_STACK, committedThisHand = 500),
                ),
            ),
        )

        positions.forEach { before ->
            val result = settleHand(before, listOf(0, 1))
            assertEquals(before.chipsInPlay, result.newState.chipsInPlay)
            assertEquals(0, result.newState.pot)
        }
    }

    @Test
    fun aWinnerCannotWinTwice() {
        val before = handState().copy(
            pot = 600,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 300),
                Seat(index = 1, stack = START_STACK, committedThisHand = 300),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            settleHand(before, listOf(1, 1))
        }
    }

    @Test
    fun theEventsDescribeTheTransition() {
        val before = handState().copy(
            buttonSeat = 0,
            pot = 601,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 301),
                Seat(index = 1, stack = START_STACK, committedThisHand = 301),
            ),
        )

        val result = settleHand(before, listOf(0, 1))

        assertEventsDescribeTheTransition(before, result)
    }

    @Test
    fun theUncalledBetIsReturnedBeforeTheSplit() {
        // Seat 0 commits 150, Seat 1 commits 201: odd pot of 351, uncalled 51 to seat 1, leaving 300 to split
        val before = handState().copy(
            pot = 351,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 150),
                Seat(index = 1, stack = START_STACK, committedThisHand = 201),
            ),
        )

        val result = settleHand(before, listOf(0, 1))

        val returned = result.events.filterIsInstance<UncalledBetReturned>().single()
        val awards = result.events.filterIsInstance<PotAwarded>()

        // Uncalled bet should be returned first
        assertEquals(1, returned.seat)
        assertEquals(51, returned.amount)
        assertEquals(2, awards.size)

        // Both PotAwarded events should come after UncalledBetReturned
        assertTrue(result.events.indexOf(returned) < result.events.indexOf(awards[0]))
        assertTrue(result.events.indexOf(returned) < result.events.indexOf(awards[1]))

        // Split should be on pot after return (300), not original pot (351)
        assertEquals(150, awards[0].amount)
        assertEquals(150, awards[1].amount)
    }

    @Test
    fun anOddSplitAfterAnUncalledReturnStillConservesEveryChip() {
        // Seat 0 commits 150, Seat 1 commits 201: odd pot of 351, uncalled 51 to seat 1, leaving 300 to split
        val before = handState().copy(
            pot = 351,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 150),
                Seat(index = 1, stack = START_STACK, committedThisHand = 201),
            ),
        )

        val result = settleHand(before, listOf(0, 1))

        // Verify chip conservation: initial stacks + pot == final stacks + pot
        assertEquals(before.chipsInPlay, result.newState.chipsInPlay)
        assertEquals(0, result.newState.pot)
    }

    @Test
    fun anUncalledReturnAlwaysLeavesAnEvenPot() {
        // Seat 0 commits 200, Seat 1 commits 251: odd pot of 451
        // Uncalled 51 to seat 1, leaving 400 to split
        // The remaining pot after an uncalled return is always even because:
        // remaining = C0 + C1 - |C0 - C1| = 2 * min(C0, C1), which is always even.
        val before = handState().copy(
            buttonSeat = 0,
            pot = 451,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 200),
                Seat(index = 1, stack = START_STACK, committedThisHand = 251),
            ),
        )

        val result = settleHand(before, listOf(0, 1))

        val returned = result.events.filterIsInstance<UncalledBetReturned>().single()
        val awards = result.events.filterIsInstance<PotAwarded>().associateBy { it.seat }

        // Uncalled return goes to seat 1
        assertEquals(1, returned.seat)
        assertEquals(51, returned.amount)

        // After uncalled return, pot is 400 (even), so split is 200 each with no odd chip
        assertEquals(200, awards.getValue(0).amount)
        assertEquals(200, awards.getValue(1).amount)
    }
}
