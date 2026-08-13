package duels.poker.server.protocol

import duels.poker.engine.game.PlayerAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

internal class ProtocolCodecTest {
    @Test
    fun helloSurvivesEncodeThenDecode() {
        val hello = Hello("d1")
        val decoded = ProtocolCodec.decodeClient(ProtocolCodec.encode(hello))
        val message = assertInstanceOf(Decoded.Message::class.java, decoded)
        assertEquals(hello, message.message)
    }

    @Test
    fun actSurvivesEncodeThenDecode() {
        val act = Act(3, 7, PlayerAction.Raise(0, 300))
        val decoded = ProtocolCodec.decodeClient(ProtocolCodec.encode(act))
        val message = assertInstanceOf(Decoded.Message::class.java, decoded)
        assertEquals(act, message.message)
    }

    @Test
    fun anEncodedServerMessageCarriesItsDiscriminator() {
        val frame = ProtocolCodec.encode(ServerMessage.Snapshot(sampleView()))
        assertEquals(true, frame.contains("\"type\":\"Snapshot\""))
    }

    @Test
    fun anEncodedWelcomeCarriesItsDefaultedVersion() {
        val frame = ProtocolCodec.encode(ServerMessage.Welcome("d1"))
        assertEquals(true, frame.contains("\"protocolVersion\":$PROTOCOL_VERSION"))
    }

    @Test
    fun anUnknownDiscriminatorIsRefusedAsUnknownMessage() {
        val decoded = ProtocolCodec.decodeClient("""{"type":"Surrender"}""")
        assertEquals(Decoded.Refused(ProtocolError.UNKNOWN_MESSAGE), decoded)
    }

    @Test
    fun nonJsonIsRefusedAsMalformed() {
        val decoded = ProtocolCodec.decodeClient("not json at all")
        assertEquals(Decoded.Refused(ProtocolError.MALFORMED_MESSAGE), decoded)
    }

    @Test
    fun theKnownNamesComeFromTheDescriptor() {
        val decoded = ProtocolCodec.decodeClient("""{"type":"Hello"}""")
        assertInstanceOf(Decoded.Message::class.java, decoded)
    }
}
