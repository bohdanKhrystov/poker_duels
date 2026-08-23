package duels.poker.server.auth

import org.junit.jupiter.api.Test
import java.security.SecureRandom
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SessionTokensTest {
    @Test
    fun aTokenIsFortyThreeCharacters() {
        val tokens = SessionTokens()
        val token = tokens.newToken()
        assertTrue(token.value.length == 43, "Token should be exactly 43 characters, got ${token.value.length}")
    }

    @Test
    fun aTokenUsesOnlyTheUrlSafeAlphabet() {
        val tokens = SessionTokens()
        val urlSafePattern = Regex("^[A-Za-z0-9_-]+$")

        // Generate 200+ tokens to statistically verify URL-safe alphabet
        repeat(250) {
            val token = tokens.newToken()
            assertTrue(urlSafePattern.matches(token.value), "Token contains non-URL-safe characters: ${token.value}")
        }
    }

    @Test
    fun twoTokensDiffer() {
        val tokens = SessionTokens()
        val token1 = tokens.newToken()
        val token2 = tokens.newToken()
        assertNotEquals(token1, token2, "Two tokens from the same instance should differ")
    }

    @Test
    fun theTokenIsExactlyWhatTheRandomGave() {
        // Test with bytes 0x00..0x1F
        val stubRandom1 = object : SecureRandom() {
            override fun nextBytes(bytes: ByteArray) {
                for (i in bytes.indices) {
                    bytes[i] = i.toByte()
                }
            }
        }

        val tokens1 = SessionTokens(stubRandom1)
        val token1 = tokens1.newToken()

        // Calculate expected value
        val expectedBytes1 = ByteArray(32) { it.toByte() }
        val expectedToken1 = Base64.getUrlEncoder().withoutPadding().encodeToString(expectedBytes1)

        assertEquals(expectedToken1, token1.value, "Token with bytes 0x00..0x1F should match expected value")

        // Test with bytes all 0xFF
        val stubRandom2 = object : SecureRandom() {
            override fun nextBytes(bytes: ByteArray) {
                for (i in bytes.indices) {
                    bytes[i] = 0xFF.toByte()
                }
            }
        }

        val tokens2 = SessionTokens(stubRandom2)
        val token2 = tokens2.newToken()

        // Calculate expected value
        val expectedBytes2 = ByteArray(32) { 0xFF.toByte() }
        val expectedToken2 = Base64.getUrlEncoder().withoutPadding().encodeToString(expectedBytes2)

        assertEquals(expectedToken2, token2.value, "Token with bytes 0xFF should match expected value")

        // Verify the two tokens are different (with two different inputs)
        assertNotEquals(token1.value, token2.value, "Tokens from different random sources should differ")
    }

    @Test
    fun nothingIsDrawnUntilAskedTwice() {
        var callCount = 0

        val countingRandom = object : SecureRandom() {
            override fun nextBytes(bytes: ByteArray) {
                callCount++
                // Fill with some value so the token is valid
                for (i in bytes.indices) {
                    bytes[i] = 0x00
                }
            }
        }

        val tokens = SessionTokens(countingRandom)

        // First call to newToken should call nextBytes once
        tokens.newToken()
        assertEquals(1, callCount, "First newToken call should invoke nextBytes once")

        // Second call to newToken should call nextBytes again (total 2)
        tokens.newToken()
        assertEquals(2, callCount, "Two newToken calls should invoke nextBytes twice (no caching)")
    }
}
