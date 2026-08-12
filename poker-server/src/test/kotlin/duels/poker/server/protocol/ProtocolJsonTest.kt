package duels.poker.server.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
private data class Probe(val a: Int = 7)

class ProtocolJsonTest {
    @Test
    fun theProtocolVersionIsOne() {
        assertEquals(1, PROTOCOL_VERSION)
    }

    @Test
    fun defaultValuesReachTheWire() {
        val json = protocolJson.encodeToString(Probe.serializer(), Probe())
        assertEquals("""{"a":7}""", json)
    }

    @Test
    fun unknownKeysAreRefused() {
        assertFailsWith<SerializationException> {
            protocolJson.decodeFromString(Probe.serializer(), """{"a":1,"b":2}""")
        }
    }
}
