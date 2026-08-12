package duels.poker.engine.game

import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlayerActionSerializationTest {

    private val json = Json { prettyPrint = false }

    @Test
    fun everyActionRoundTripsThroughTheParentSerializer() {
        val actions = listOf(
            PlayerAction.Fold(0),
            PlayerAction.Check(1),
            PlayerAction.Call(0),
            PlayerAction.Bet(1, 300),
            PlayerAction.Raise(0, 500),
            PlayerAction.AllIn(1),
        )

        for (action in actions) {
            val encoded = json.encodeToString(PlayerAction.serializer(), action)
            val decoded = json.decodeFromString(PlayerAction.serializer(), encoded)
            assertEquals(action, decoded, "Action $action did not round-trip correctly")
        }
    }

    @Test
    fun betEncodesItsSeatAndAmount() {
        val bet = PlayerAction.Bet(0, 300)
        val encoded = json.encodeToString(PlayerAction.serializer(), bet)

        // Verify the exact structure
        assertEquals("""{"type":"Bet","seat":0,"to":300}""", encoded)
    }

    @Test
    fun theDiscriminatorIsTheShortName() {
        val allIn = PlayerAction.AllIn(1)
        val encoded = json.encodeToString(PlayerAction.serializer(), allIn)

        // Should contain the short name
        assert(encoded.contains(""""type":"AllIn"""")) {
            "Encoded should contain short discriminator \"AllIn\", but got: $encoded"
        }

        // Should not contain the fully qualified class name
        assert(!encoded.contains("duels.poker.engine")) {
            "Encoded should not contain fully qualified class name, but got: $encoded"
        }
    }

    @Test
    fun aListOfActionsRoundTrips() {
        val actions = listOf(
            PlayerAction.Fold(0),
            PlayerAction.Bet(1, 100),
            PlayerAction.Raise(0, 200),
            PlayerAction.AllIn(1),
        )
        val serializer = ListSerializer(PlayerAction.serializer())

        val encoded = json.encodeToString(serializer, actions)
        val decoded = json.decodeFromString(serializer, encoded)

        assertEquals(actions, decoded)
    }

    @Test
    fun decodingAnUnknownActionTypeFails() {
        val unknownJson = """{"type":"Shove","seat":0}"""

        assertThrows<SerializationException> {
            json.decodeFromString(PlayerAction.serializer(), unknownJson)
        }
    }

    @Test
    fun decodingANonPositiveBetFails() {
        val nonPositiveBetJson = """{"type":"Bet","seat":0,"to":0}"""

        assertThrows<IllegalArgumentException> {
            json.decodeFromString(PlayerAction.serializer(), nonPositiveBetJson)
        }
    }
}
