package duels.poker.server.protocol

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HandshakeTest {
    @Test
    fun aMatchingVersionIsNotRefused() {
        val result = versionRefusalOrNull(Hello(deviceId = "d1", protocolVersion = PROTOCOL_VERSION))
        assertNull(result)
    }

    @Test
    fun aDefaultedHelloIsNotRefused() {
        val result = versionRefusalOrNull(Hello())
        assertNull(result)
    }

    @Test
    fun anOlderVersionIsRefused() {
        val result = versionRefusalOrNull(Hello(deviceId = "d1", protocolVersion = PROTOCOL_VERSION - 1))
        assertEquals(ServerMessage.Failure(ProtocolError.VERSION_MISMATCH), result)
    }

    @Test
    fun aNewerVersionIsRefused() {
        val result = versionRefusalOrNull(Hello(deviceId = "d1", protocolVersion = PROTOCOL_VERSION + 1))
        assertEquals(ServerMessage.Failure(ProtocolError.VERSION_MISMATCH), result)
    }
}
