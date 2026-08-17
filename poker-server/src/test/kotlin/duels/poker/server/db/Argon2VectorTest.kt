package duels.poker.server.db

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * Known-answer tests for the Bouncy Castle Argon2id implementation.
 *
 * Each test verifies that Argon2id is correctly configured by comparing computed hashes against
 * published vectors. The RFC 9106 vector proves Argon2id at specific parameters; the salt variance
 * test proves the computation is a function of its inputs and not a constant; the production
 * parameters test proves the chosen memory and time costs are allocatable on a JVM.
 */
internal class Argon2VectorTest {
    /**
     * RFC 9106 §5.3 published test vector.
     *
     * Verifies Argon2id with version 19, time cost t = 3, memory cost m = 32 KiB, parallelism p = 4,
     * password 32 bytes of 0x01, salt 16 bytes of 0x02, secret 8 bytes of 0x03, associated data
     * 12 bytes of 0x04, outputs a 32-byte tag matching the vector exactly.
     */
    @Test
    fun theRfc9106Argon2idVectorReproduces() {
        val password = ByteArray(32) { 0x01 }
        val salt = ByteArray(16) { 0x02 }
        val secret = ByteArray(8) { 0x03 }
        val associatedData = ByteArray(12) { 0x04 }

        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(19)
            .withMemoryAsKB(32)
            .withIterations(3)
            .withParallelism(4)
            .withSalt(salt)
            .withSecret(secret)
            .withAdditional(associatedData)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(params)
        val tag = ByteArray(32)
        generator.generateBytes(password, tag, 0, tag.size)

        // Expected digest from RFC 9106 §5.3
        val expected = byteArrayOf(
            0x0d, 0x64, 0x0d.toByte(), 0xf5.toByte(), 0x8d.toByte(), 0x78, 0x76, 0x6c.toByte(),
            0x08.toByte(), 0xc0.toByte(), 0x37, 0xa3.toByte(), 0x4a.toByte(), 0x8b.toByte(), 0x53.toByte(), 0xc9.toByte(),
            0xd0.toByte(), 0x1e.toByte(), 0xf0.toByte(), 0x45.toByte(), 0x2d, 0x75.toByte(), 0xb6.toByte(), 0x5e.toByte(),
            0xb5.toByte(), 0x25, 0x20, 0xe9.toByte(), 0x6b.toByte(), 0x01.toByte(), 0xe6.toByte(), 0x59.toByte(),
        )
        assertArrayEquals(expected, tag)
    }

    /**
     * Proves that Argon2id is a function of its inputs: changing a single salt byte changes the output.
     *
     * Uses the same parameters as [theRfc9106Argon2idVectorReproduces], but changes the salt's last
     * byte from 0x02 to 0x03.
     */
    @Test
    fun oneDifferentSaltByteChangesTheTag() {
        val password = ByteArray(32) { 0x01 }
        val salt = ByteArray(16) { 0x02 }
        salt[15] = 0x03 // Change the last byte

        val secret = ByteArray(8) { 0x03 }
        val associatedData = ByteArray(12) { 0x04 }

        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(19)
            .withMemoryAsKB(32)
            .withIterations(3)
            .withParallelism(4)
            .withSalt(salt)
            .withSecret(secret)
            .withAdditional(associatedData)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(params)
        val tag = ByteArray(32)
        generator.generateBytes(password, tag, 0, tag.size)

        // Expected digest from RFC 9106 §5.3 (this should NOT match when salt is changed)
        val rfcVector = byteArrayOf(
            0x0d, 0x64, 0x0d.toByte(), 0xf5.toByte(), 0x8d.toByte(), 0x78, 0x76, 0x6c.toByte(),
            0x08.toByte(), 0xc0.toByte(), 0x37, 0xa3.toByte(), 0x4a.toByte(), 0x8b.toByte(), 0x53.toByte(), 0xc9.toByte(),
            0xd0.toByte(), 0x1e.toByte(), 0xf0.toByte(), 0x45.toByte(), 0x2d, 0x75.toByte(), 0xb6.toByte(), 0x5e.toByte(),
            0xb5.toByte(), 0x25, 0x20, 0xe9.toByte(), 0x6b.toByte(), 0x01.toByte(), 0xe6.toByte(), 0x59.toByte(),
        )
        assertNotEquals(rfcVector.contentToString(), tag.contentToString())
    }

    /**
     * Production parameters test.
     *
     * Proves that the chosen production parameters (m = 19456 KiB, t = 2, p = 1) are allocatable
     * on this JVM and produce a 32-byte output without error.
     *
     * Parameters from ADR-0027 §1: m = 19456 KiB (roughly 19 MiB per verification), t = 2 iterations,
     * p = 1 parallelism lane.
     */
    @Test
    fun theProductionParametersProduceAThirtyTwoByteTag() {
        val password = ByteArray(32) { 0x01 }
        val salt = ByteArray(16) { 0x02 }
        val secret = ByteArray(8) { 0x03 }
        val associatedData = ByteArray(12) { 0x04 }

        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(19)
            .withMemoryAsKB(19456)
            .withIterations(2)
            .withParallelism(1)
            .withSalt(salt)
            .withSecret(secret)
            .withAdditional(associatedData)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(params)
        val tag = ByteArray(32)
        generator.generateBytes(password, tag, 0, tag.size)

        // Just verify we got 32 bytes and they are not all zeros
        assert(tag.size == 32)
        assert(tag.any { it != 0.toByte() })
    }
}
