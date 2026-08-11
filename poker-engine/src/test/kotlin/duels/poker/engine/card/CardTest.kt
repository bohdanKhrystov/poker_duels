package duels.poker.engine.card

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class CardTest {
    @Test
    fun allHoldsFiftyTwoDistinctCards() {
        assertEquals(52, Card.all.size)
        assertEquals(52, Card.all.toSet().size)
    }

    @Test
    fun everyRankAndSuitRoundTrips() {
        for (rank in Rank.entries) {
            for (suit in Suit.entries) {
                val card = Card.of(rank, suit)
                assertEquals(rank, card.rank)
                assertEquals(suit, card.suit)
            }
        }
    }

    @Test
    fun allIsOrderedByRankThenSuit() {
        for (i in 0..51) {
            val expected = Card.of(Rank.entries[i / 4], Suit.entries[i % 4])
            assertEquals(expected, Card.all[i])
        }
    }

    @Test
    fun cardsWithTheSameRankAndSuitAreEqual() {
        val card1 = Card.of(Rank.ACE, Suit.SPADES)
        val card2 = Card.of(Rank.ACE, Suit.SPADES)
        assertEquals(card1, card2)
        assertEquals(card1.hashCode(), card2.hashCode())

        val card3 = Card.of(Rank.ACE, Suit.HEARTS)
        assertNotEquals(card1, card3)
    }
}
