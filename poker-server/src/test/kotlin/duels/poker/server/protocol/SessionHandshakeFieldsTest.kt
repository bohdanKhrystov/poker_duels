package duels.poker.server.protocol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `Hello.sessionToken`, `Welcome.playerId`, `Welcome`'s newly nullable `deviceId` and
 * `ProtocolError.INVALID_SESSION` — the whole of this ticket's wire shape. Nothing reads a
 * token and nothing resolves a session yet (`TASK-040518`), so these tests only construct and
 * round-trip frames; they emit nothing.
 */
internal class SessionHandshakeFieldsTest {
    @Test
    fun aHelloWithNoTokenRoundTripsWithANullToken() {
        val hello = Hello(deviceId = "d1")
        val decoded = ProtocolCodec.decodeClient(ProtocolCodec.encode(hello))
        val message = assertInstanceOf(Decoded.Message::class.java, decoded).message
        assertEquals(hello, message)
        assertEquals(null, (message as Hello).sessionToken)
    }

    @Test
    fun aHelloCarriesItsTokenAcrossTheWire() {
        // Paired with the null case above on purpose: a token the encoder silently dropped
        // would still leave that test green, since a dropped null is still null.
        val hello = Hello(deviceId = "d1", sessionToken = "t")
        val decoded = ProtocolCodec.decodeClient(ProtocolCodec.encode(hello))
        val message = assertInstanceOf(Decoded.Message::class.java, decoded).message
        assertEquals(hello, message)
        assertEquals("t", (message as Hello).sessionToken)
    }

    @Test
    fun aWelcomeNamesThePlayerAndTheDevice() {
        val welcome = ServerMessage.Welcome("p1", "d1")
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), welcome)
        val decoded = protocolJson.decodeFromString(ServerMessage.serializer(), encoded)
        assertEquals(welcome, decoded)
        assertTrue(encoded.contains("\"playerId\":\"p1\""), "Encoded message should contain playerId: $encoded")
        assertTrue(encoded.contains("\"deviceId\":\"d1\""), "Encoded message should contain deviceId: $encoded")
    }

    @Test
    fun aWelcomeMayNameNoDevice() {
        val welcome = ServerMessage.Welcome("p1", null)
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), welcome)
        val decoded = protocolJson.decodeFromString(ServerMessage.serializer(), encoded)
        assertEquals(welcome, decoded)
        // Present and null, not absent: encodeDefaults alone would not catch a field that had
        // been dropped from the class entirely, but it does catch one silently omitted only
        // when null.
        assertTrue(encoded.contains("\"deviceId\":null"), "Encoded message should contain a null deviceId: $encoded")
    }

    @Test
    fun theTwoWelcomesAreNotEqual() {
        // Two fixtures differing in one field each, so neither field can be a constant the
        // generated equals() ignores.
        assertNotEquals(ServerMessage.Welcome("p1", "d1"), ServerMessage.Welcome("p2", "d1"))
        assertNotEquals(ServerMessage.Welcome("p1", "d1"), ServerMessage.Welcome("p1", "d2"))
    }

    @Test
    fun invalidSessionIsOnTheWire() {
        val failure = ServerMessage.Failure(ProtocolError.INVALID_SESSION)
        val encoded = protocolJson.encodeToString(ServerMessage.serializer(), failure)
        val decoded = protocolJson.decodeFromString(ServerMessage.serializer(), encoded)
        assertEquals(failure, decoded)
        assertTrue(encoded.contains("\"error\":\"INVALID_SESSION\""), "Encoded message should name the error: $encoded")
    }
}
