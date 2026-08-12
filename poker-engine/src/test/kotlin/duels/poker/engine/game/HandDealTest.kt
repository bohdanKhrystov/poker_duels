package duels.poker.engine.game

import duels.poker.engine.card.Deck
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/** Tests for [startHand]'s deal: hole cards and the opening action, as events and as state. */
class HandDealTest {
    @Test
    fun eachSeatHoldsTwoCards() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        assertEquals(2, result.newState.seat(0).holeCards.size)
        assertEquals(2, result.newState.seat(1).holeCards.size)
    }

    @Test
    fun noCardIsDealtTwice() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        val cards = result.newState.seat(0).holeCards + result.newState.seat(1).holeCards
        assertEquals(4, cards.toSet().size)
    }

    @Test
    fun theDeckHasFortyEightLeft() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        assertEquals(48, result.newState.deck.remaining)
    }

    @Test
    fun theBigBlindIsDealtFirstAndEveryOtherCard() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        val dealt = Deck.full().shuffled(SplitMix64Rng(1L)).deck.deal(4).cards

        assertEquals(listOf(dealt[0], dealt[2]), result.newState.seat(1).holeCards)
        assertEquals(listOf(dealt[1], dealt[3]), result.newState.seat(0).holeCards)
    }

    @Test
    fun theButtonIsOnTurnPreflop() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))
        assertEquals(0, result.newState.seatToAct)

        val otherResult = startHand(1, buttonSeat = 1, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))
        assertEquals(1, otherResult.newState.seatToAct)
    }

    @Test
    fun theOpeningSequenceRunsToFive() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        assertEquals(6, result.events.size)
        result.events.forEachIndexed { index, event -> assertEquals(index, event.sequence) }

        assertEquals(HoleCardsDealt::class, result.events[3]::class)
        assertEquals(HoleCardsDealt::class, result.events[4]::class)
        assertEquals(ActionOn::class, result.events[5]::class)
    }

    @Test
    fun theSameSeedDealsTheSameCards() {
        val first = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(7L))
        val second = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(7L))

        assertEquals(first.newState.seat(0).holeCards, second.newState.seat(0).holeCards)
        assertEquals(first.newState.seat(1).holeCards, second.newState.seat(1).holeCards)

        val different = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(8L))

        assertNotEquals(first.newState.seat(0).holeCards, different.newState.seat(0).holeCards)
    }

    @Test
    fun theEventsDescribeTheState() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        val opening = GameState(
            handNumber = 1,
            buttonSeat = 0,
            street = Street.PREFLOP,
            seats = listOf(Seat(index = 0, stack = 10_000), Seat(index = 1, stack = 10_000)),
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
