package duels.poker.engine.card

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CardsTest {
    @Test
    fun parsesAWhitespaceSeparatedList() {
        val result = cards("As Kd 7h")
        assertEquals(3, result.size)
        assertEquals(Card.of(Rank.ACE, Suit.SPADES), result[0])
        assertEquals(Card.of(Rank.KING, Suit.DIAMONDS), result[1])
        assertEquals(Card.of(Rank.SEVEN, Suit.HEARTS), result[2])
    }

    @Test
    fun toleratesExtraWhitespace() {
        val result = cards("  As   Kd  7h  ")
        assertEquals(3, result.size)
        assertEquals(Card.of(Rank.ACE, Suit.SPADES), result[0])
        assertEquals(Card.of(Rank.KING, Suit.DIAMONDS), result[1])
        assertEquals(Card.of(Rank.SEVEN, Suit.HEARTS), result[2])
    }

    @Test
    fun rejectsADuplicateCard() {
        assertThrows(IllegalArgumentException::class.java) {
            cards("As Kd As")
        }
    }

    @Test
    fun rejectsBlankNotation() {
        assertThrows(IllegalArgumentException::class.java) {
            cards("   ")
        }
    }

    @Test
    fun rejectsAnInvalidCard() {
        assertThrows(IllegalArgumentException::class.java) {
            cards("As 10d")
        }
    }
}
