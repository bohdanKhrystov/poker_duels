package duels.poker.server.protocol.http

import duels.poker.server.protocol.protocolJson
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

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
        val rawPassword = "hunter2222"

        // Test four routes and collect results
        val routes: List<Pair<String, String>> = listOf(
            "direct toString()" to request.toString(),
            "string interpolation" to "$request",
            "listOf().toString()" to listOf(request).toString(),
            "exception message" to (IllegalStateException("body was $request").message ?: ""),
        )

        // Collect all failures: each route can have two failures (leaked password, missing redaction)
        val failures: MutableList<String> = mutableListOf()
        for ((routeName, result) in routes) {
            if (result.contains(rawPassword)) {
                failures.add("Route '$routeName' leaked the password: $result")
            }
            if (!result.contains(SignUpRequest.REDACTION)) {
                failures.add("Route '$routeName' did not contain redaction: $result")
            }
        }

        // Assert all routes passed, reporting all failures together
        assertTrue(
            failures.isEmpty(),
            "Redaction failures:\n${failures.joinToString("\n")}",
        )
    }
}
