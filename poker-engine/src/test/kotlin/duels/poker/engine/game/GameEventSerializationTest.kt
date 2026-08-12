package duels.poker.engine.game

import duels.poker.engine.card.Card
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameEventSerializationTest {
    private val json = Json { prettyPrint = false }

    @Test
    fun everySubtypeRoundTripsThroughTheParentSerializer() {
        val events = listOf(
            HandStarted(0, 1, 0, 1, 2, listOf(1000, 1000)),
            BlindPosted(1, 0, 1, false),
            HoleCardsDealt(2, 0, listOf(Card.parse("As"), Card.parse("Kh"))),
            ActionOn(3, 0),
            PlayerFolded(4, 1),
            PlayerChecked(5, 0),
            PlayerCalled(6, 1, 4),
            PlayerBet(7, 0, 10),
            PlayerRaised(8, 1, 30),
            PlayerAllIn(9, 0, 100),
            BettingRoundEnded(10, Street.PREFLOP),
            StreetDealt(11, Street.FLOP, listOf(Card.parse("2s"), Card.parse("3h"), Card.parse("4d"))),
            ShowdownReached(12),
            HandRevealed(13, 0, listOf(Card.parse("As"), Card.parse("Kh"))),
            UncalledBetReturned(14, 1, 50),
            PotAwarded(15, 0, 150),
            HandFinished(16),
        )

        val serialized = json.encodeToString(ListSerializer(GameEvent.serializer()), events)
        val deserialized = json.decodeFromString(ListSerializer(GameEvent.serializer()), serialized)

        assertEquals(events, deserialized)
    }

    @Test
    fun theListIsNotShorterThanTheHierarchy() {
        val events = listOf(
            HandStarted(0, 1, 0, 1, 2, listOf(1000, 1000)),
            BlindPosted(1, 0, 1, false),
            HoleCardsDealt(2, 0, listOf(Card.parse("As"), Card.parse("Kh"))),
            ActionOn(3, 0),
            PlayerFolded(4, 1),
            PlayerChecked(5, 0),
            PlayerCalled(6, 1, 4),
            PlayerBet(7, 0, 10),
            PlayerRaised(8, 1, 30),
            PlayerAllIn(9, 0, 100),
            BettingRoundEnded(10, Street.PREFLOP),
            StreetDealt(11, Street.FLOP, listOf(Card.parse("2s"), Card.parse("3h"), Card.parse("4d"))),
            ShowdownReached(12),
            HandRevealed(13, 0, listOf(Card.parse("As"), Card.parse("Kh"))),
            UncalledBetReturned(14, 1, 50),
            PotAwarded(15, 0, 150),
            HandFinished(16),
        )

        assertEquals(17, events.size)
        assertEquals(17, events.map { it::class }.distinct().size)
    }

    @Test
    fun theSchemaVersionIsNotWrittenToTheWire() {
        val event = ActionOn(3, 1)
        val serialized = json.encodeToString(GameEvent.serializer(), event)

        assertEquals("""{"type":"ActionOn","sequence":3,"seat":1}""", serialized)
    }

    @Test
    fun decodingAnUnknownEventTypeFails() {
        val jsonString = """{"type":"HandExploded","sequence":0}"""

        assertThrows<SerializationException> {
            json.decodeFromString(GameEvent.serializer(), jsonString)
        }
    }
}
