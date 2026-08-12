package duels.poker.engine.duel

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class BlindSerializationTest {
    @Test
    fun `aBlindLevelRoundTrips`() {
        val original = BlindLevel(50, 100)
        val json = Json.encodeToString(original)
        assertEquals("""{"smallBlind":50,"bigBlind":100}""", json)
        val decoded = Json.decodeFromString<BlindLevel>(json)
        assertEquals(original, decoded)
    }

    @Test
    fun `aScheduleRoundTripsWithItsLevels`() {
        val original = BlindSchedule(
            levels = listOf(
                BlindLevel(10, 20),
                BlindLevel(25, 50),
                BlindLevel(50, 100),
            ),
            handsPerLevel = 10,
        )
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<BlindSchedule>(json)
        assertEquals(original, decoded)
    }

    @Test
    fun `decodingADescendingLadderFails`() {
        val descendingJson = """{"levels":[{"smallBlind":50,"bigBlind":100},{"smallBlind":25,"bigBlind":50}],"handsPerLevel":10}"""
        assertThrows(IllegalArgumentException::class.java) {
            Json.decodeFromString<BlindSchedule>(descendingJson)
        }
    }

    @Test
    fun `decodingABlindLevelWithABigBlindBelowTheSmallFails`() {
        val invalidJson = """{"smallBlind":100,"bigBlind":50}"""
        assertThrows(IllegalArgumentException::class.java) {
            Json.decodeFromString<BlindLevel>(invalidJson)
        }
    }
}
