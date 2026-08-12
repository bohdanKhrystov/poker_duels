package duels.poker.engine.card

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CardSerializerTest {
    @Test
    fun encodesACardAsItsNotation() {
        val card = Card.parse("As")
        val encoded = Json.encodeToString(CardSerializer, card)
        assertEquals("\"As\"", encoded)
    }

    @Test
    fun roundTripsEveryOneOfTheFiftyTwoCards() {
        for (card in Card.all) {
            val encoded = Json.encodeToString(CardSerializer, card)
            val decoded = Json.decodeFromString(CardSerializer, encoded)
            assertEquals(card, decoded)
        }
    }

    @Test
    fun roundTripsInsideAList() {
        val cards = listOf(Card.parse("7d"), Card.parse("Ah"))
        val encoded = Json.encodeToString(ListSerializer(CardSerializer), cards)
        val decoded = Json.decodeFromString(ListSerializer(CardSerializer), encoded)
        assertEquals(cards, decoded)
        assertEquals("[\"7d\",\"Ah\"]", encoded)
    }

    @Test
    fun rejectsNotationThatIsNotACard() {
        val json = "\"Zz\""
        assertThrows(IllegalArgumentException::class.java) {
            Json.decodeFromString(CardSerializer, json)
        }
    }
}
