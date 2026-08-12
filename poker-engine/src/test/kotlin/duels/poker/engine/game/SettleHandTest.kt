package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** [settleHand] closes a swept hand to a single winner. */
internal class SettleHandTest {

    @Test
    fun theWinnerTakesThePot() {
        val before = handState().copy(
            pot = 600,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 300),
                Seat(index = 1, stack = START_STACK, committedThisHand = 300),
            ),
        )

        val result = settleHand(before, listOf(1))

        val awarded = result.events.filterIsInstance<PotAwarded>().single()
        assertEquals(1, awarded.seat)
        assertEquals(600, awarded.amount)
        assertEquals(before.seat(1).stack + 600, result.newState.seat(1).stack)
        assertEquals(0, result.newState.pot)
    }

    @Test
    fun anUncalledBetComesBackBeforeTheAward() {
        val before = handState().copy(
            pot = 800,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 300),
                Seat(index = 1, stack = START_STACK, committedThisHand = 500),
            ),
        )

        val result = settleHand(before, listOf(1))

        val returned = result.events.filterIsInstance<UncalledBetReturned>().single()
        val awarded = result.events.filterIsInstance<PotAwarded>().single()
        assertEquals(1, returned.seat)
        assertEquals(200, returned.amount)
        assertEquals(600, awarded.amount)
        assertTrue(result.events.indexOf(returned) < result.events.indexOf(awarded))
    }

    @Test
    fun aPotOfZeroEmitsNoAward() {
        val before = handState().copy(
            pot = 450,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 450),
                Seat(index = 1, stack = START_STACK, committedThisHand = 0),
            ),
        )

        val result = settleHand(before, listOf(1))

        assertEquals(2, result.events.size)
        val returned = result.events[0]
        val finished = result.events[1]
        assertTrue(returned is UncalledBetReturned)
        returned as UncalledBetReturned
        assertEquals(0, returned.seat)
        assertEquals(450, returned.amount)
        assertTrue(finished is HandFinished)
        assertTrue(result.events.none { it is PotAwarded })
    }

    @Test
    fun theHandFinishesComplete() {
        val before = handState().copy(
            pot = 600,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 300),
                Seat(index = 1, stack = START_STACK, committedThisHand = 300),
            ),
        )

        val result = settleHand(before, listOf(0))

        assertTrue(result.events.last() is HandFinished)
        assertEquals(Street.COMPLETE, result.newState.street)
        assertNull(result.newState.seatToAct)
        assertTrue(result.newState.isHandOver)
    }

    @Test
    fun chipsAreConservedBySettlement() {
        val positions = listOf(
            handState().copy(
                pot = 600,
                seats = listOf(
                    Seat(index = 0, stack = START_STACK, committedThisHand = 300),
                    Seat(index = 1, stack = START_STACK, committedThisHand = 300),
                ),
            ) to listOf(1),
            handState().copy(
                pot = 800,
                seats = listOf(
                    Seat(index = 0, stack = START_STACK, committedThisHand = 300),
                    Seat(index = 1, stack = START_STACK, committedThisHand = 500),
                ),
            ) to listOf(1),
            handState().copy(
                pot = 450,
                seats = listOf(
                    Seat(index = 0, stack = START_STACK, committedThisHand = 450),
                    Seat(index = 1, stack = START_STACK, committedThisHand = 0),
                ),
            ) to listOf(1),
        )

        positions.forEach { (before, winners) ->
            val result = settleHand(before, winners)
            assertEquals(before.chipsInPlay, result.newState.chipsInPlay)
        }
    }

    @Test
    fun theSequencesStayDense() {
        val before = handState().copy(
            eventCount = 9,
            pot = 800,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 300),
                Seat(index = 1, stack = START_STACK, committedThisHand = 500),
            ),
        )

        val result = settleHand(before, listOf(1))

        assertEquals(listOf(9, 10, 11), result.events.map { it.sequence })
        assertEquals(12, result.newState.eventCount)
    }

    @Test
    fun theEventsDescribeTheTransition() {
        val before = handState().copy(
            pot = 800,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisHand = 300),
                Seat(index = 1, stack = START_STACK, committedThisHand = 500),
            ),
        )

        val result = settleHand(before, listOf(1))

        assertEventsDescribeTheTransition(before, result)
    }

    @Test
    fun anUnsweptStateIsRefused() {
        val before = handState().copy(
            pot = 600,
            seats = listOf(
                Seat(index = 0, stack = START_STACK, committedThisStreet = 100, committedThisHand = 300),
                Seat(index = 1, stack = START_STACK, committedThisHand = 300),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) { settleHand(before, listOf(0)) }
    }
}
