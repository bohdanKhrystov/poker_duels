package duels.poker.engine.duel

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MatchEventSerializationTest {
    private val json = Json

    @Test
    fun aDuelOutcomeRoundTrips() {
        val original = DuelOutcome(1, 12, listOf(0, 20000))
        val encoded = json.encodeToString(DuelOutcome.serializer(), original)
        val decoded = json.decodeFromString(DuelOutcome.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun aDrawRoundTripsWithANullWinner() {
        val original = DuelOutcome(null, 40, listOf(500, 500))
        val encoded = json.encodeToString(DuelOutcome.serializer(), original)
        val decoded = json.decodeFromString(DuelOutcome.serializer(), encoded)
        assertEquals(original, decoded)
        assertTrue(decoded.isDraw)
    }

    @Test
    fun matchFinishedRoundTripsThroughTheParentSerializer() {
        val outcome = DuelOutcome(0, 25, listOf(5000, 0))
        val original = MatchFinished(0, outcome)
        val encoded = json.encodeToString(MatchEvent.serializer(), original as MatchEvent)
        val decoded = json.decodeFromString(MatchEvent.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun theDiscriminatorIsMatchFinished() {
        val outcome = DuelOutcome(1, 10, listOf(10000, 5000))
        val event = MatchFinished(2, outcome)
        val encoded = json.encodeToString(MatchEvent.serializer(), event as MatchEvent)
        assertTrue(encoded.contains("\"type\":\"MatchFinished\""))
        assertFalse(encoded.contains("MatchFinishedKt"))
        assertFalse(encoded.contains("duels.poker.engine.duel"))
    }

    @Test
    fun theSchemaVersionIsNotWrittenToTheWire() {
        val outcome = DuelOutcome(null, 5, listOf(2500, 2500))
        val event = MatchFinished(1, outcome)
        val encoded = json.encodeToString(MatchEvent.serializer(), event as MatchEvent)
        assertFalse(encoded.contains("\"version\""))
    }

    @Test
    fun decodingAnOutcomeWithThreeStacksFails() {
        val jsonString = """{"winner":1,"handsPlayed":10,"finalStacks":[1,2,3]}"""
        assertThrows<IllegalArgumentException> {
            json.decodeFromString(DuelOutcome.serializer(), jsonString)
        }
    }
}
