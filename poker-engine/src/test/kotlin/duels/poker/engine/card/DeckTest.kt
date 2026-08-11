package duels.poker.engine.card

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DeckTest {
    @Test
    fun fullDeckHoldsEveryCardOnce() {
        val deck = Deck.full()
        assertEquals(52, deck.remaining)
        val deal = deck.deal(52)
        assertEquals(Card.all, deal.cards)
    }

    @Test
    fun dealTakesFromTheTopAndLeavesTheRest() {
        val deck = Deck.full()
        val deal1 = deck.deal(2)
        assertEquals(Card.all.take(2), deal1.cards)
        assertEquals(50, deal1.deck.remaining)
        val deal2 = deal1.deck.deal(1)
        assertEquals(listOf(Card.all[2]), deal2.cards)
    }

    @Test
    fun dealingEveryCardEmptiesTheDeck() {
        val deck = Deck.full()
        val deal = deck.deal(52)
        assertEquals(0, deal.deck.remaining)
        val emptyDeal = deal.deck.deal(0)
        assertEquals(emptyList<Card>(), emptyDeal.cards)
    }

    @Test
    fun dealingMoreThanRemainsIsRejected() {
        val deck = Deck.full()
        assertThrows(IllegalArgumentException::class.java) {
            deck.deal(53)
        }
        assertThrows(IllegalArgumentException::class.java) {
            deck.deal(-1)
        }
    }

    @Test
    fun dealDoesNotMutateTheReceiver() {
        val deck = Deck.full()
        val deal1 = deck.deal(5)
        val deal2 = deck.deal(5)
        assertEquals(deal1.cards, deal2.cards)
    }

    @Test
    fun toStringDoesNotRevealTheCards() {
        val deck = Deck.full()
        val str = deck.toString()
        assertEquals("Deck(remaining=52)", str)
        assertFalse(str.contains("As"))
    }
}
