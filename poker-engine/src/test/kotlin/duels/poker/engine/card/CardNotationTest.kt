package duels.poker.engine.card

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CardNotationTest {
    @Test
    fun formatsEveryCardAsRankThenSuit() {
        // Check all 52 cards
        for (card in Card.all) {
            val expected = "${card.rank.symbol}${card.suit.symbol}"
            assertEquals(expected, card.toString())
        }

        // Spot checks
        assertEquals("As", Card.of(Rank.ACE, Suit.SPADES).toString())
        assertEquals("Th", Card.of(Rank.TEN, Suit.HEARTS).toString())
        assertEquals("2c", Card.of(Rank.TWO, Suit.CLUBS).toString())
        assertEquals("Kd", Card.of(Rank.KING, Suit.DIAMONDS).toString())
    }

    @Test
    fun parsesEveryCardItFormats() {
        for (card in Card.all) {
            val formatted = card.toString()
            val parsed = Card.parse(formatted)
            assertEquals(card, parsed)
        }
    }

    @Test
    fun parsesTheRankCharacterCaseInsensitively() {
        val aceOfSpadesUppercase = Card.parse("As")
        val aceOfSpadesLowercase = Card.parse("as")
        assertEquals(aceOfSpadesUppercase, aceOfSpadesLowercase)
        assertEquals(Rank.ACE, aceOfSpadesLowercase.rank)
        assertEquals(Suit.SPADES, aceOfSpadesLowercase.suit)
    }

    @Test
    fun rejectsMalformedNotation() {
        val invalidInputs = listOf(
            "", // empty
            "A", // too short
            "As ", // trailing space
            " As", // leading space
            "10s", // two-digit rank
            "Xs", // invalid rank
            "AS", // suit must be lowercase
            "sA", // wrong order
            "1c", // invalid rank
            "AsKd", // too long
        )

        for (input in invalidInputs) {
            val exception = assertThrows(
                IllegalArgumentException::class.java,
            ) {
                Card.parse(input)
            }
            // Message must contain the offending input
            assertEquals(
                true,
                exception.message?.contains(input) ?: false,
            )
        }
    }

    @Test
    fun parseOrNullReturnsNullForMalformedNotation() {
        val invalidInputs = listOf(
            "", // empty
            "A", // too short
            "As ", // trailing space
            " As", // leading space
            "10s", // two-digit rank
            "Xs", // invalid rank
            "AS", // suit must be lowercase
            "sA", // wrong order
            "1c", // invalid rank
            "AsKd", // too long
        )

        for (input in invalidInputs) {
            val result = Card.parseOrNull(input)
            assertNull(result)
        }
    }
}
