package duels.poker.server.protocol.http

import duels.poker.server.protocol.protocolJson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AuthDtosTest {
    @Test
    fun bothFieldsDecodeFromAJsonObject() {
        val decoded = protocolJson.decodeFromString(SignUpRequest.serializer(), """{"handle":"bob","password":"hunter2222"}""")
        assertEquals(SignUpRequest("bob", "hunter2222"), decoded)
    }

    @Test
    fun aMissingPasswordIsRefused() {
        assertThrows<IllegalArgumentException> {
            protocolJson.decodeFromString(SignUpRequest.serializer(), """{"handle":"bob"}""")
        }
    }

    @Test
    fun aMissingHandleIsRefused() {
        assertThrows<IllegalArgumentException> {
            protocolJson.decodeFromString(SignUpRequest.serializer(), """{"password":"hunter2222"}""")
        }
    }

    @Test
    fun anUnrecognisedFieldIsRefused() {
        assertThrows<IllegalArgumentException> {
            protocolJson.decodeFromString(SignUpRequest.serializer(), """{"handle":"bob","password":"hunter2222","playerId":"p-1"}""")
        }
    }

    @Test
    fun printingTheRequestPrintsNeitherField() {
        val request = SignUpRequest("bob", "hunter2222")
        val printed = request.toString()
        assertEquals(SignUpRequest.REDACTION, printed)
        assertFalse(printed.contains("hunter2222"))
        assertFalse(printed.contains("bob"))
    }
}
