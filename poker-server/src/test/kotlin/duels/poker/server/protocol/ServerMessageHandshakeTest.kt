package duels.poker.server.protocol

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerMessageHandshakeTest {
    @Test
    fun welcomeRoundTrips() {
        val original = ServerMessage.Welcome("player-1", "device-1")
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), original)
        val decoded = protocolJson.decodeFromString(ServerMessage.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun welcomeCarriesItsVersionEvenWhenDefaulted() {
        val message = ServerMessage.Welcome("player-1", "device-1")
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), message)
        assertTrue(encoded.contains("\"protocolVersion\":$PROTOCOL_VERSION"), "Encoded message should contain protocolVersion: $encoded")
    }

    @Test
    fun failureRoundTrips() {
        val original = ServerMessage.Failure(ProtocolError.VERSION_MISMATCH)
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), original)
        val decoded = protocolJson.decodeFromString(ServerMessage.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun theDiscriminatorsAreExplicit() {
        val welcome = ServerMessage.Welcome("player-1", "device-1")
        val welcomeEncoded = protocolJson.encodeToString(ServerMessage.serializer(), welcome)
        assertTrue(welcomeEncoded.contains("\"type\":\"Welcome\""), "Welcome should have explicit type discriminator: $welcomeEncoded")

        val failure = ServerMessage.Failure(ProtocolError.VERSION_MISMATCH)
        val failureEncoded = protocolJson.encodeToString(ServerMessage.serializer(), failure)
        assertTrue(failureEncoded.contains("\"type\":\"Failure\""), "Failure should have explicit type discriminator: $failureEncoded")
    }

    @Test
    fun theErrorSetIsExactlyWhatIsDeclared() {
        val expectedNames = listOf(
            "UNKNOWN_MESSAGE",
            "MALFORMED_MESSAGE",
            "VERSION_MISMATCH",
            "NOT_YOUR_TURN",
            "UNKNOWN_ROOM",
            "ROOM_FULL",
            "NOT_IN_DUEL",
            "FRAME_LIMIT_EXCEEDED",
            "INVALID_SESSION",
            "REMATCH_UNAVAILABLE",
        )
        val actualNames = ProtocolError.entries.map { it.name }
        assertEquals(expectedNames, actualNames)
    }
}
