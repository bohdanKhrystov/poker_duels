package duels.poker.server.protocol

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HandshakeTest {
    @Test
    fun aMatchingVersionIsWelcomed() {
        val result = handshake(Hello("d1", PROTOCOL_VERSION), "d1")
        assertEquals(ServerMessage.Welcome("d1"), result)
    }

    @Test
    fun aDefaultedHelloIsWelcomed() {
        val result = handshake(Hello(), "d1")
        assertEquals(ServerMessage.Welcome("d1"), result)
    }

    @Test
    fun anOlderVersionIsRefused() {
        val result = handshake(Hello("d1", PROTOCOL_VERSION - 1), "d1")
        assertEquals(ServerMessage.Failure(ProtocolError.VERSION_MISMATCH), result)
    }

    @Test
    fun aNewerVersionIsRefused() {
        val result = handshake(Hello("d1", PROTOCOL_VERSION + 1), "d1")
        assertEquals(ServerMessage.Failure(ProtocolError.VERSION_MISMATCH), result)
    }

    @Test
    fun aRefusalCarriesNoDeviceId() {
        val result = handshake(Hello("d1", PROTOCOL_VERSION - 1), "d1")
        assertInstanceOf(ServerMessage.Failure::class.java, result)
    }
}
