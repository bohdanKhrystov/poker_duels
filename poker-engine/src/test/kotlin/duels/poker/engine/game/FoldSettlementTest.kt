package duels.poker.engine.game

import duels.poker.engine.card.Card
import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A fold on [Street.FLOP], with real hole cards on both seats, proves that folding both awards
 * the pot to the survivor and never leaks the folder's cards through any event.
 */
class FoldSettlementTest {

    private val folderCards = cards("As Ks")
    private val survivorCards = cards("2c 3d")

    /**
     * Seat 0 faces a bet from seat 1 with unequal hand totals: the uncalled part of seat 1's bet
     * comes back before the rest of the swept pot is awarded.
     */
    private fun facingABet(): GameState {
        val committedSeats = listOf(
            Seat(index = 0, stack = START_STACK, holeCards = folderCards, committedThisHand = 300),
            Seat(index = 1, stack = START_STACK, holeCards = survivorCards, committedThisStreet = 300, committedThisHand = 600),
        )
        return handState(committedSeats).copy(
            street = Street.FLOP,
            board = Board(cards("7h 8h 9h")),
            betToMatch = 300,
            seatToAct = 0,
            pot = 300,
        )
    }

    /** Same shape, but the two hand totals already match at the point of the fold. */
    private fun facingABetWithMatchedTotals(): GameState {
        val committedSeats = listOf(
            Seat(index = 0, stack = START_STACK, holeCards = folderCards, committedThisHand = 600),
            Seat(index = 1, stack = START_STACK, holeCards = survivorCards, committedThisStreet = 300, committedThisHand = 600),
        )
        return handState(committedSeats).copy(
            street = Street.FLOP,
            board = Board(cards("7h 8h 9h")),
            betToMatch = 300,
            seatToAct = 0,
            pot = 300,
        )
    }

    private fun sweptPot(state: GameState): Int =
        state.pot + state.seat(0).committedThisStreet + state.seat(1).committedThisStreet

    @Test
    fun theSurvivorTakesThePot() {
        val state = facingABet()
        val action = PlayerAction.Fold(0)
        val swept = sweptPot(state)
        val beforeStack = state.seat(1).stack

        val result = DefaultPokerEngine.handle(state, action)

        assertFalse(result.isRejected)
        assertEquals(beforeStack + swept, result.newState.seat(1).stack)
        assertEquals(0, result.newState.pot)
    }

    @Test
    fun theHandEndsComplete() {
        val state = facingABet()
        val action = PlayerAction.Fold(0)

        val result = DefaultPokerEngine.handle(state, action)

        assertFalse(result.isRejected)
        assertEquals(Street.COMPLETE, result.newState.street)
        assertNull(result.newState.seatToAct)
        assertTrue(result.newState.isHandOver)
        assertTrue(result.events.last() is HandFinished)
    }

    @Test
    fun theFoldersCardsAppearInNoEvent() {
        val state = facingABet()
        val action = PlayerAction.Fold(0)

        val result = DefaultPokerEngine.handle(state, action)

        val cardsCarried: List<Card> = result.events.flatMap { event ->
            when (event) {
                is StreetDealt -> event.cards
                is HandRevealed -> event.cards
                else -> emptyList()
            }
        }
        assertTrue(folderCards.none { it in cardsCarried })
    }

    @Test
    fun nothingIsReturnedWhenBothSeatsMatched() {
        val state = facingABetWithMatchedTotals()
        val action = PlayerAction.Fold(0)
        val swept = sweptPot(state)

        val result = DefaultPokerEngine.handle(state, action)

        assertFalse(result.isRejected)
        assertTrue(result.events.none { it is UncalledBetReturned })
        val awarded = result.events.filterIsInstance<PotAwarded>().single()
        assertEquals(1, awarded.seat)
        assertEquals(swept, awarded.amount)
    }

    @Test
    fun chipsAreConservedByTheFoldSettlement() {
        val state = facingABet()
        val action = PlayerAction.Fold(0)

        val beforeChips = state.chipsInPlay
        val result = DefaultPokerEngine.handle(state, action)

        assertEquals(beforeChips, result.newState.chipsInPlay)
    }

    @Test
    fun theEventsDescribeTheTransition() {
        val state = facingABet()
        val action = PlayerAction.Fold(0)

        val result = DefaultPokerEngine.handle(state, action)

        assertEventsDescribeTheTransition(state, result)
    }
}
