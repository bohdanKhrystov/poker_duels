package duels.poker.engine.log

import duels.poker.engine.game.PlayedHand
import duels.poker.engine.game.playRandomHand
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * `HandLog` round-trips through JSON, preserving every value seed through event, over fifty
 * hands nobody hand-picked.
 */
internal class HandLogSerializationTest {

    @Test
    @Timeout(60)
    fun aHandLogRoundTripsOverFiftyRandomHands() {
        for (seed in 1L..50L) {
            val played = playRandomHand(seed)
            val log = logOf(seed, played)

            val encoded = Json.encodeToString(HandLog.serializer(), log)
            val decoded = Json.decodeFromString(HandLog.serializer(), encoded)

            assertEquals(log, decoded, "seed $seed: round-trip failed")
        }
    }

    @Test
    @Timeout(60)
    fun theSameLogAlwaysEncodesToTheSameText() {
        for (seed in 1L..50L) {
            val played = playRandomHand(seed)
            val log = logOf(seed, played)

            val encoded1 = Json.encodeToString(HandLog.serializer(), log)
            val encoded2 = Json.encodeToString(HandLog.serializer(), log)

            assertEquals(encoded1, encoded2, "seed $seed: encoding is not deterministic")
        }
    }

    @Test
    fun theVersionIsOmittedUnlessDefaultsAreEncoded() {
        val played = playRandomHand(1)
        val log = logOf(1, played)

        // Plain Json omits defaults
        val plainJson = Json
        val encoded = plainJson.encodeToString(HandLog.serializer(), log)
        assertFalse(encoded.contains("\"version\""), "plain Json should omit version")

        // Json with encodeDefaults includes them
        val withDefaults = Json { encodeDefaults = true }
        val encodedWithDefaults = withDefaults.encodeToString(HandLog.serializer(), log)
        assertTrue(encodedWithDefaults.contains("\"version\":1"), "Json with encodeDefaults should include version")
    }

    @Test
    fun decodingALogWithAGapInEventSequencesFails() {
        val gapJson = """{"seed":1,"handNumber":1,"buttonSeat":0,"stacks":[1000,1000],"smallBlind":50,"bigBlind":100,"actions":[],"events":[{"type":"HandStarted","sequence":0,"handNumber":1,"buttonSeat":0,"smallBlind":50,"bigBlind":100,"stacks":[1000,1000]},{"type":"ActionOn","sequence":2,"seat":0}],"version":1}"""

        assertThrows(IllegalArgumentException::class.java) {
            Json.decodeFromString(HandLog.serializer(), gapJson)
        }
    }

    @Test
    fun decodingALogWhoseFirstEventIsNotHandStartedFails() {
        val wrongStartJson = """{"seed":1,"handNumber":1,"buttonSeat":0,"stacks":[1000,1000],"smallBlind":50,"bigBlind":100,"actions":[],"events":[{"type":"ActionOn","sequence":0,"seat":0}],"version":1}"""

        assertThrows(IllegalArgumentException::class.java) {
            Json.decodeFromString(HandLog.serializer(), wrongStartJson)
        }
    }
}

private fun logOf(seed: Long, played: PlayedHand) = HandLog(
    seed = seed,
    handNumber = played.opening.handNumber,
    buttonSeat = played.opening.buttonSeat,
    stacks = played.opening.seats.map { it.stack },
    smallBlind = played.opening.smallBlind,
    bigBlind = played.opening.bigBlind,
    actions = played.actions,
    events = played.events,
)
