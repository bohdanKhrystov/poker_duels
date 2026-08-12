package duels.poker.engine.game

import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A seat whose stack is at most its own blind posts all-in and is then the seat to act with no
 * legal action — without a fix the hand hangs forever waiting for it. `startHand` instead sweeps
 * the blinds and runs the board out to [ShowdownReached], reusing [endBettingRound] rather than
 * leaving an [ActionOn] nobody can satisfy. `TASK-010519`.
 */
class OpeningRunOutTest {
    @Test
    fun aButtonAllInOnItsBlindStillReachesShowdown() {
        val result = startHand(1, 0, listOf(50, 10_000), 50, 100, SplitMix64Rng(1L))

        assertEquals(Street.COMPLETE, result.newState.street)
        assertTrue(result.events.any { it is ShowdownReached })
        assertEquals(5, result.newState.board.size)
        assertNull(result.newState.seatToAct)
    }

    @Test
    fun bothBlindsAllInReachShowdown() {
        val result = startHand(1, 0, listOf(50, 80), 50, 100, SplitMix64Rng(1L))

        assertEquals(Street.COMPLETE, result.newState.street)
        assertTrue(result.newState.seat(0).isAllIn)
        assertTrue(result.newState.seat(1).isAllIn)
    }

    @Test
    fun anOpeningRunOutEmitsNoActionOn() {
        val result = startHand(1, 0, listOf(50, 10_000), 50, 100, SplitMix64Rng(1L))

        assertTrue(result.events.none { it is ActionOn })
    }

    @Test
    fun anOrdinaryHandStillGetsItsActionOn() {
        val result = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L))

        assertTrue(result.events.last() is ActionOn)
        assertEquals(Street.PREFLOP, result.newState.street)
    }

    @Test
    fun chipsAreConservedInAnOpeningRunOut() {
        val result = startHand(1, 0, listOf(50, 10_000), 50, 100, SplitMix64Rng(1L))

        assertEquals(10_050, result.newState.chipsInPlay)
        assertEquals(0, result.newState.potTotal)

        val returned = result.events.filterIsInstance<UncalledBetReturned>().single()
        assertEquals(1, returned.seat)
        assertEquals(50, returned.amount)

        assertEquals(0, result.newState.seat(0).stack)
        assertEquals(10_050, result.newState.seat(1).stack)
    }

    @Test
    fun theEventsDescribeTheState() {
        val result = startHand(1, 0, listOf(50, 10_000), 50, 100, SplitMix64Rng(1L))

        val opening = GameState(
            handNumber = 1,
            buttonSeat = 0,
            street = Street.PREFLOP,
            seats = listOf(Seat(index = 0, stack = 50), Seat(index = 1, stack = 10_000)),
            board = Board.EMPTY,
            pot = 0,
            betToMatch = 0,
            minRaiseTo = 100,
            seatToAct = null,
            smallBlind = 50,
            bigBlind = 100,
            eventCount = 0,
            deck = result.newState.deck,
            rng = result.newState.rng,
        )

        val folded = StateProjection.fold(opening, result.events)

        assertEquals(folded, result.newState)
    }
}
