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

    @Test
    fun aSignInRequestPrintsNeitherField() {
        val request = SignInRequest("alice", "hunter2")
        val handle = "alice"
        val password = "hunter2"

        // Test that neither field appears in the string representation
        val stringForm = request.toString()
        assertTrue(!stringForm.contains(handle), "toString() should not contain handle '$handle', but was: $stringForm")
        assertTrue(!stringForm.contains(password), "toString() should not contain password '$password', but was: $stringForm")
    }

    @Test
    fun aSignInRequestStillRoundTrips() {
        val decoded = protocolJson.decodeFromString(SignInRequest.serializer(), """{"handle":"alice","password":"hunter2"}""")
        assertEquals(SignInRequest("alice", "hunter2"), decoded)
    }

    @Test
    fun aSignInRequestRefusesAnUnknownField() {
        assertThrows<IllegalArgumentException> {
            protocolJson.decodeFromString(SignInRequest.serializer(), """{"handle":"alice","password":"hunter2","deviceId":"d-1"}""")
        }
    }

    @Test
    fun aSignInResponseCarriesOnlyTheToken() {
        val response = SignInResponse("t")
        val encoded = protocolJson.encodeToString(SignInResponse.serializer(), response)
        assertEquals("""{"sessionToken":"t"}""", encoded)
    }

    @Test
    fun aSignInResponsePrintsNoToken() {
        val response = SignInResponse("supersecret")
        val stringForm = response.toString()
        assertTrue(!stringForm.contains("supersecret"), "toString() should not contain token, but was: $stringForm")
    }
}
