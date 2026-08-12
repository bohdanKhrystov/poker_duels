package duels.poker.engine.game

import duels.poker.engine.card.Card
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EventSerializationTest {

    private val json = Json { prettyPrint = false }

    @Test
    fun everyBettingEventRoundTrips() {
        val events = listOf(
            PlayerFolded(0, 0),
            PlayerChecked(1, 1),
            PlayerCalled(2, 0, 100),
            PlayerBet(3, 1, 200),
            PlayerRaised(4, 0, 400),
            PlayerAllIn(5, 1, 1000),
        )

        for (event in events) {
            val encoded = json.encodeToString(BettingEvent.serializer(), event)
            val decoded = json.decodeFromString(BettingEvent.serializer(), encoded)
            assertEquals(event, decoded, "Event $event did not round-trip correctly")
        }
    }

    @Test
    fun everyDealerEventRoundTrips() {
        val events = listOf(
            BettingRoundEnded(0, Street.PREFLOP),
            StreetDealt(1, Street.FLOP, listOf(Card.parse("Ah"), Card.parse("Kd"), Card.parse("2c"))),
            ShowdownReached(2),
            HandRevealed(3, 0, listOf(Card.parse("As"), Card.parse("Ks"))),
            UncalledBetReturned(4, 1, 50),
            PotAwarded(5, 0, 1000),
            HandFinished(6),
        )

        for (event in events) {
            val encoded = json.encodeToString(DealerEvent.serializer(), event)
            val decoded = json.decodeFromString(DealerEvent.serializer(), encoded)
            assertEquals(event, decoded, "Event $event did not round-trip correctly")
        }
    }

    @Test
    fun cardsInsideAnEventAreWrittenAsNotation() {
        val event = StreetDealt(4, Street.FLOP, listOf(Card.parse("Ah"), Card.parse("Kd"), Card.parse("2c")))
        val encoded = json.encodeToString(DealerEvent.serializer(), event)

        // Verify the cards are encoded as notation
        assert(encoded.contains(""""Ah"""")) {
            "Encoded should contain card notation \"Ah\", but got: $encoded"
        }
        assert(encoded.contains(""""Kd"""")) {
            "Encoded should contain card notation \"Kd\", but got: $encoded"
        }
        assert(encoded.contains(""""2c"""")) {
            "Encoded should contain card notation \"2c\", but got: $encoded"
        }
    }

    @Test
    fun theDiscriminatorIsTheShortName() {
        val event = PotAwarded(5, 0, 1000)
        val encoded = json.encodeToString(DealerEvent.serializer(), event)

        // Should contain the short name as discriminator
        assert(encoded.contains(""""type":"PotAwarded"""")) {
            "Encoded should contain short discriminator \"type\":\"PotAwarded\", but got: $encoded"
        }

        // Should not contain the fully qualified class name
        assert(!encoded.contains("duels.poker.engine")) {
            "Encoded should not contain fully qualified class name, but got: $encoded"
        }
    }

    @Test
    fun decodingAnEventWithABadFieldFails() {
        val badJson = """{"type":"PlayerBet","sequence":3,"seat":5,"to":200}"""

        assertThrows<IllegalArgumentException> {
            json.decodeFromString(BettingEvent.serializer(), badJson)
        }
    }
}
