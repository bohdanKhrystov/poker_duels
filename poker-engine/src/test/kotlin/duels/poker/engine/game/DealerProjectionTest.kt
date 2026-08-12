package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DealerProjectionTest {

    @Test
    fun endingARoundSweepsCommitmentsIntoThePot() {
        val state = handState(listOf(seats()[0].commit(300), seats()[1].commit(300)))

        val result = applyDealer(state, BettingRoundEnded(1, Street.PREFLOP))

        assertEquals(600, result.pot)
        assertEquals(0, result.seat(0).committedThisStreet)
        assertEquals(0, result.seat(1).committedThisStreet)
        assertEquals(state.seat(0).stack, result.seat(0).stack)
        assertEquals(state.seat(1).stack, result.seat(1).stack)
    }

    @Test
    fun endingARoundResetsTheBarAndTheMinimumRaise() {
        val state = handState(listOf(seats()[0].commit(300), seats()[1].commit(300)))
            .copy(betToMatch = 300, seatToAct = 0)

        val result = applyDealer(state, BettingRoundEnded(1, Street.PREFLOP))

        assertEquals(0, result.betToMatch)
        assertEquals(state.bigBlind, result.minRaiseTo)
        assertNull(result.seatToAct)
    }

    @Test
    fun chipsAreConservedWhenARoundEnds() {
        val state = handState(listOf(seats()[0].commit(300), seats()[1].commit(150)))
            .copy(betToMatch = 300)

        val result = applyDealer(state, BettingRoundEnded(1, Street.PREFLOP))

        assertEquals(state.chipsInPlay, result.chipsInPlay)
    }

    @Test
    fun dealingTheFlopAdvancesTheStreetAndTheBoard() {
        val state = handState()

        val result = applyDealer(state, StreetDealt(6, Street.FLOP, cards("As Kd 7c")))

        assertEquals(Street.FLOP, result.street)
        assertEquals(3, result.board.size)
        assertEquals(cards("As Kd 7c"), result.board.cards)
    }

    @Test
    fun dealingTheTurnAndRiverAddOneCardEach() {
        val flopState = handState().copy(street = Street.FLOP, board = Board(cards("2c 3c 4c")))

        val turnResult = applyDealer(flopState, StreetDealt(9, Street.TURN, cards("5c")))
        val riverResult = applyDealer(turnResult, StreetDealt(11, Street.RIVER, cards("6c")))

        assertEquals(Street.RIVER, riverResult.street)
        assertEquals(5, riverResult.board.size)
    }

    @Test
    fun showdownReachedSetsTheStreet() {
        val riverState = handState()
            .copy(street = Street.RIVER, board = Board(cards("2c 3c 4c 5c 6c")), seatToAct = 0)

        val result = applyDealer(riverState, ShowdownReached(15))

        assertEquals(Street.SHOWDOWN, result.street)
        assertNull(result.seatToAct)
    }

    @Test
    fun revealingAHandRecordsTheHoleCards() {
        val state = handState()

        val result = applyDealer(state, HandRevealed(9, 1, cards("As Kd")))

        assertEquals(cards("As Kd"), result.seat(1).holeCards)
        assertTrue(result.seat(0).holeCards.isEmpty())
    }

    @Test
    fun dealingANewStreetClearsTheLastAggressor() {
        val state = handState().copy(lastAggressor = 0)

        val result = applyDealer(state, StreetDealt(6, Street.FLOP, cards("As Kd 7c")))

        assertNull(result.lastAggressor)
    }

    @Test
    fun endingABettingRoundKeepsTheLastAggressor() {
        val state = handState().copy(lastAggressor = 1)

        val result = applyDealer(state, BettingRoundEnded(1, Street.RIVER))

        assertEquals(1, result.lastAggressor)
    }

    @Test
    fun reachingShowdownKeepsTheLastAggressor() {
        val riverState = handState()
            .copy(street = Street.RIVER, board = Board(cards("2c 3c 4c 5c 6c")), seatToAct = 0, lastAggressor = 1)

        val result = applyDealer(riverState, ShowdownReached(15))

        assertEquals(1, result.lastAggressor)
    }
}
