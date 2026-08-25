package duels.poker.server.auth

import org.junit.jupiter.api.Test
import java.security.SecureRandom
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RecoveryTokensTest {
    @Test
    fun aVerificationTokenIsTwoHundredAndFiftySixBitsOfUrlSafeBase64() {
        // Stub that fills 32 bytes with 0xFC, which produces '/' characters in standard base64
        // but '_' in URL-safe base64. This distinguishes the two encoders.
        val stubRandom = object : SecureRandom() {
            override fun nextBytes(bytes: ByteArray) {
                for (i in bytes.indices) {
                    bytes[i] = 0xFC.toByte()
                }
            }
        }

        val tokens = RecoveryTokens(stubRandom)
        val token = tokens.newVerificationToken()

        // 32 bytes of 0xFC encodes to exactly this string in URL-safe base64 without padding
        val expectedValue = "_Pz8_Pz8_Pz8_Pz8_Pz8_Pz8_Pz8_Pz8_Pz8_Pz8_Pw"

        assertEquals(expectedValue, token.value, "Verification token should be 43-character URL-safe base64")
        assertEquals(43, token.value.length, "Token should be exactly 43 characters")
        assertTrue(!token.value.contains("+"), "Token must not contain standard base64 '+' character")
        assertTrue(!token.value.contains("/"), "Token must not contain standard base64 '/' character")
        assertTrue(!token.value.contains("="), "Token must not contain padding '='")
    }

    @Test
    fun aResetTokenIsMintedTheSameWay() {
        // Same stub as the verification token test, to ensure both token types are minted identically
        val stubRandom = object : SecureRandom() {
            override fun nextBytes(bytes: ByteArray) {
                for (i in bytes.indices) {
                    bytes[i] = 0xFC.toByte()
                }
            }
        }

        val tokens = RecoveryTokens(stubRandom)
        val token = tokens.newResetToken()

        // Same expected value as verification token, but wrapped in ResetToken
        val expectedValue = "_Pz8_Pz8_Pz8_Pz8_Pz8_Pz8_Pz8_Pz8_Pz8_Pz8_Pw"

        assertEquals(expectedValue, token.value, "Reset token should be 43-character URL-safe base64")
        assertEquals(43, token.value.length, "Token should be exactly 43 characters")
        assertTrue(!token.value.contains("+"), "Token must not contain standard base64 '+' character")
        assertTrue(!token.value.contains("/"), "Token must not contain standard base64 '/' character")
        assertTrue(!token.value.contains("="), "Token must not contain padding '='")
    }

    @Test
    fun twoCallsOnARealSecureRandomDiffer() {
        val tokens = RecoveryTokens()
        val token1 = tokens.newVerificationToken()
        val token2 = tokens.newVerificationToken()

        assertEquals(43, token1.value.length, "First token should be 43 characters")
        assertEquals(43, token2.value.length, "Second token should be 43 characters")
        assertNotEquals(token1.value, token2.value, "Two tokens from the same instance should differ")
    }

    @Test
    fun printingEitherTokenRevealsNothing() {
        val verificationToken = VerificationToken("secret-verification-value")
        val resetToken = ResetToken("secret-reset-value")

        assertEquals(VerificationToken.REDACTION, "$verificationToken", "Verification token toString should be redacted")
        assertEquals(ResetToken.REDACTION, "$resetToken", "Reset token toString should be redacted")

        assertTrue(!VerificationToken.REDACTION.contains("secret"), "Redaction should not contain secret verification value")
        assertTrue(!ResetToken.REDACTION.contains("secret"), "Redaction should not contain secret reset value")

        assertNotEquals(VerificationToken.REDACTION, ResetToken.REDACTION, "The two redactions must be different strings")
        assertTrue(VerificationToken.REDACTION.contains("Verification"), "Verification redaction should mention its type")
        assertTrue(ResetToken.REDACTION.contains("Reset"), "Reset redaction should mention its type")
    }
}
