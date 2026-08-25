package duels.poker.server.db

import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.VerificationToken
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse

class RecoveryTokenDigestTest {
    @Test
    fun aVerificationTokenHashesToItsPublishedSha256() {
        val token = VerificationToken("abc")
        val expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
        val actual = recoveryTokenDigest(token)
        assertContentEquals(expected, actual)
    }

    @Test
    fun aResetTokenHashesToTheSameBytes() {
        val verificationToken = VerificationToken("abc")
        val resetToken = ResetToken("abc")
        val verificationDigest = recoveryTokenDigest(verificationToken)
        val resetDigest = recoveryTokenDigest(resetToken)
        assertContentEquals(verificationDigest, resetDigest)
    }

    @Test
    fun aDifferentTokenHashesDifferently() {
        val token1 = VerificationToken("abc")
        val token2 = VerificationToken("abd")
        val digest1 = recoveryTokenDigest(token1)
        val digest2 = recoveryTokenDigest(token2)
        assertFalse(digest1.contentEquals(digest2))
    }

    @Test
    fun aNonAsciiTokenIsHashedAsUtf8() {
        val token = VerificationToken("é")
        val expected = "4a99557e4033c3539de2eb65472017cad5f9557f7a0625a09f1c3f6e2ba69c4c"
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
        val actual = recoveryTokenDigest(token)
        assertContentEquals(expected, actual)
    }
}
