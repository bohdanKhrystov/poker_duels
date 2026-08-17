package duels.poker.server.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RedactedValuesTest {
    @Test
    fun aPresentedSecretPrintsItsRedaction() {
        val secret = PresentedSecret("correct-horse-battery-staple")
        assertEquals(PresentedSecret.REDACTION, secret.toString())
    }

    @Test
    fun aSessionTokenPrintsItsRedaction() {
        val token = SessionToken("correct-horse-battery-staple")
        assertEquals(SessionToken.REDACTION, token.toString())
    }

    @Test
    fun twoDifferentSecretsPrintTheSameThing() {
        val secret1 = PresentedSecret("alpha-bravo-charlie-delta")
        val secret2 = PresentedSecret("echo-foxtrot-golf-hotel")
        assertEquals(secret1.toString(), secret2.toString())
    }

    @Test
    fun theSecretReachesNoStringByAnyOfTheUsualRoutes() {
        val secret = PresentedSecret("correct-horse-battery-staple")
        val rawValue = "correct-horse-battery-staple"

        // Test seven routes and collect results
        val routes: List<Pair<String, String>> = listOf(
            "string interpolation" to "$secret",
            "direct toString()" to secret.toString(),
            "listOf().toString()" to listOf(secret).toString(),
            "mapOf().toString()" to mapOf("k" to secret).toString(),
            "String.format" to String.format("%s", secret),
            "StringBuilder.append" to StringBuilder().append(secret).toString(),
            "exception message" to (IllegalStateException("secret was $secret").message ?: ""),
        )

        // Assert no route contains raw value and all contain redaction
        for ((routeName, result) in routes) {
            assertFalse(
                result.contains(rawValue),
                "Route '$routeName' leaked the raw value:\n$result",
            )
            assertTrue(
                result.contains(PresentedSecret.REDACTION),
                "Route '$routeName' did not contain redaction:\n$result",
            )
        }
    }

    @Test
    fun theValueIsStillReadableToCodeThatAsksForIt() {
        val rawValue = "correct-horse-battery-staple"
        val secret = PresentedSecret(rawValue)
        assertEquals(rawValue, secret.value)
    }
}
