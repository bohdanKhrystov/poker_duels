package duels.poker.engine.duel

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DuelFormatSerializationTest {
    @Test
    fun `theDefaultFormatRoundTrips`() {
        val original = DuelFormat.DEFAULT
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<DuelFormat>(json)
        assertEquals(original, decoded)
        // Verify all five levels are preserved
        for (hand in listOf(1, 11, 21, 31, 41)) {
            assertEquals(
                original.blinds.blindsFor(hand),
                decoded.blinds.blindsFor(hand),
            )
        }
    }

    @Test
    fun `freezeoutEncodesAsItsShortName`() {
        val original = EndCondition.Freezeout
        val json = Json.encodeToString<EndCondition>(original)
        assertTrue(json.contains("\"type\":\"Freezeout\""), "JSON should contain type discriminator")
        assertFalse(json.contains("EndConditionKt"), "JSON should not contain class name")
        assertFalse(json.contains("duels.poker.engine.duel"), "JSON should not contain package path")
    }

    @Test
    fun `fixedHandsCarriesItsHandCount`() {
        val original = EndCondition.FixedHands(40)
        val json = Json.encodeToString<EndCondition>(original)
        assertTrue(json.contains("\"hands\":40"), "JSON should contain hands field")
        val decoded = Json.decodeFromString<EndCondition>(json)
        assertEquals(original, decoded)
        assertEquals(40, (decoded as EndCondition.FixedHands).hands)
    }

    @Test
    fun `aFixedLengthFormatRoundTrips`() {
        val original = DuelFormat(
            startingStack = 10_000,
            blinds = BlindSchedule(
                levels = listOf(
                    BlindLevel(50, 100),
                    BlindLevel(75, 150),
                    BlindLevel(100, 200),
                    BlindLevel(150, 300),
                    BlindLevel(200, 400),
                ),
                handsPerLevel = 10,
            ),
            endCondition = EndCondition.FixedHands(20),
        )
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<DuelFormat>(json)
        assertEquals(original, decoded)
        assertEquals(EndCondition.FixedHands(20), decoded.endCondition)
    }

    @Test
    fun `decodingAFormatWhoseStackIsBelowTheBigBlindFails`() {
        val invalidJson = """{"startingStack":50,"blinds":{"levels":[{"smallBlind":50,"bigBlind":100},{"smallBlind":75,"bigBlind":150}],"handsPerLevel":10},"endCondition":{"type":"Freezeout"}}"""
        assertThrows<IllegalArgumentException> {
            Json.decodeFromString<DuelFormat>(invalidJson)
        }
    }
}
