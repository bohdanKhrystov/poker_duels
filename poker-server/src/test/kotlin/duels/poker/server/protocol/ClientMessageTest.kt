package duels.poker.server.protocol

import duels.poker.engine.game.PlayerAction
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContains
import kotlin.test.assertEquals

internal class ClientMessageTest {
    @Test
    fun helloRoundTrips() {
        val original = Hello("device-1", PROTOCOL_VERSION)
        val encoded = protocolJson.encodeToString(ClientMessage.serializer(), original)
        val decoded = protocolJson.decodeFromString(ClientMessage.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun helloWithNoDeviceIdRoundTrips() {
        val original = Hello()
        val encoded = protocolJson.encodeToString(ClientMessage.serializer(), original)
        val decoded = protocolJson.decodeFromString(ClientMessage.serializer(), encoded)
        assertEquals(original, decoded)
        assertEquals(null, (decoded as Hello).deviceId)
    }

    @Test
    fun helloCarriesItsVersionEvenWhenDefaulted() {
        val original = Hello()
        val encoded = protocolJson.encodeToString(ClientMessage.serializer(), original)
        assertContains(encoded, "\"protocolVersion\":$PROTOCOL_VERSION")
    }

    @Test
    fun actRoundTrips() {
        val original = Act(3, 7, PlayerAction.Raise(0, 300))
        val encoded = protocolJson.encodeToString(ClientMessage.serializer(), original)
        val decoded = protocolJson.decodeFromString(ClientMessage.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun theDiscriminatorsAreExplicit() {
        val hello = Hello("device-1", PROTOCOL_VERSION)
        val helloEncoded = protocolJson.encodeToString(ClientMessage.serializer(), hello)
        assertContains(helloEncoded, "\"type\":\"Hello\"")

        val act = Act(3, 7, PlayerAction.Raise(0, 300))
        val actEncoded = protocolJson.encodeToString(ClientMessage.serializer(), act)
        assertContains(actEncoded, "\"type\":\"Act\"")
    }

    @Test
    fun actRejectsAHandNumberBelowOne() {
        assertThrows<IllegalArgumentException> { Act(0, 0, PlayerAction.Fold(0)) }
    }

    @Test
    fun actRejectsANegativeActionSequence() {
        assertThrows<IllegalArgumentException> { Act(1, -1, PlayerAction.Fold(0)) }
    }
}
