package duels.poker.server.db

import duels.poker.server.auth.PresentedSecret
import kotlinx.coroutines.runBlocking
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.junit.jupiter.api.Test
import java.security.SecureRandom
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Two secrets, genuinely different from each other, used throughout rather than one string
// reused on both sides of a comparison.
private val SECRET_A = PresentedSecret("correct horse battery staple")
private val SECRET_B = PresentedSecret("Tr0ub4dor&3")

/** A [SecureRandom] whose [nextBytes] always fills the destination with [salt], for pinning a test's salt. */
private fun fixedSaltRandom(salt: ByteArray): SecureRandom =
    object : SecureRandom() {
        override fun nextBytes(bytes: ByteArray) {
            salt.copyInto(bytes)
        }
    }

/**
 * Tests for [Argon2Hasher].
 *
 * The cheapest wrong `matches` parses the stored string and never looks at the secret at all — a
 * hash of the *right* secret is a valid PHC string too, so only a *different* secret checked
 * against it tells the two apart; `aWrongSecretDoesNotVerify` is that case.
 * `aTagDifferingOnlyInItsFirstByteDoesNotVerify` and its sibling on the last byte are the pair
 * that rules out a comparison stopping at the first mismatching byte rather than examining the
 * whole tag.
 */
class Argon2HasherTest {
    @Test
    fun aHashedSecretVerifiesAgainstItsOwnString() {
        runBlocking {
            val hasher = Argon2Hasher()

            val phc = hasher.hash(SECRET_A)

            assertTrue(hasher.matches(SECRET_A, phc))
        }
    }

    @Test
    fun theSameSecretHashedTwiceGivesTwoStringsAndBothVerify() {
        runBlocking {
            val hasher = Argon2Hasher()

            val first = hasher.hash(SECRET_A)
            val second = hasher.hash(SECRET_A)

            // Either assertion alone would let a fixed-salt defect through: equal strings with
            // both verifying, or different strings where only one verifies.
            assertNotEquals(first, second)
            assertTrue(hasher.matches(SECRET_A, first))
            assertTrue(hasher.matches(SECRET_A, second))
        }
    }

    @Test
    fun theHashIsAStringTheProjectsOwnParserAccepts() {
        runBlocking {
            val hasher = Argon2Hasher()

            val phc = hasher.hash(SECRET_A)

            assertNotNull(parseArgon2PhcOrNull(phc))
        }
    }

    @Test
    fun aWrongSecretDoesNotVerify() {
        runBlocking {
            val hasher = Argon2Hasher()
            val phcOfSecretA = hasher.hash(SECRET_A)

            // A different secret against that same string...
            assertFalse(hasher.matches(SECRET_B, phcOfSecretA))
            // ...and the right secret against the hash of a different one — both directions,
            // since a defect could fail to check either.
            assertFalse(hasher.matches(SECRET_A, hasher.hash(SECRET_B)))
        }
    }

    @Test
    fun aTagDifferingOnlyInItsLastByteDoesNotVerify() {
        runBlocking {
            val hasher = Argon2Hasher()
            val parsed = assertNotNull(parseArgon2PhcOrNull(hasher.hash(SECRET_A)))

            val flippedTag = parsed.tag.copyOf()
            val lastIndex = flippedTag.lastIndex
            flippedTag[lastIndex] = (flippedTag[lastIndex].toInt() xor 0x01).toByte()
            val tampered = Argon2Phc(parsed.salt, flippedTag).encode()

            assertFalse(hasher.matches(SECRET_A, tampered))
        }
    }

    @Test
    fun aTagDifferingOnlyInItsFirstByteDoesNotVerify() {
        runBlocking {
            val hasher = Argon2Hasher()
            val parsed = assertNotNull(parseArgon2PhcOrNull(hasher.hash(SECRET_A)))

            val flippedTag = parsed.tag.copyOf()
            flippedTag[0] = (flippedTag[0].toInt() xor 0x01).toByte()
            val tampered = Argon2Phc(parsed.salt, flippedTag).encode()

            assertFalse(hasher.matches(SECRET_A, tampered))
        }
    }

    @Test
    fun aStoredStringTheParserRefusesAnswersFalse() {
        runBlocking {
            val hasher = Argon2Hasher()
            // Shaped like a real PHC string — right algorithm, right salt and tag lengths — but
            // for a cost parameter (m=8) this project never writes, so the parser refuses it too.
            val realShapedButWrongCost =
                "\$argon2id\$v=19\$m=8,t=2,p=1\$AgICAgICAgICAgICAgICAg\$AwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwM"

            assertFalse(hasher.matches(SECRET_A, "not-a-phc-string"))
            assertFalse(hasher.matches(SECRET_A, realShapedButWrongCost))
        }
    }

    @Test
    fun aNonAsciiSecretVerifies() {
        runBlocking {
            val hasher = Argon2Hasher()
            val secret = PresentedSecret("café 🎰")

            val phc = hasher.hash(secret)

            assertTrue(hasher.matches(secret, phc))
        }
    }

    @Test
    fun aSecretHashedComposedVerifiesWhenPresentedDecomposed() {
        runBlocking {
            val hasher = Argon2Hasher()
            // U+00E9 LATIN SMALL LETTER E WITH ACUTE — the single precomposed code point.
            val composed = PresentedSecret("caf\u00E9")
            // "e" followed by U+0301 COMBINING ACUTE ACCENT — the same visible string as two code
            // points, and a different UTF-8 byte sequence without the fold.
            val decomposed = PresentedSecret("cafe\u0301")

            val phc = hasher.hash(composed)

            assertTrue(hasher.matches(decomposed, phc))
        }
    }

    @Test
    fun aSecretHashedDecomposedVerifiesWhenPresentedComposed() {
        runBlocking {
            val hasher = Argon2Hasher()
            val decomposed = PresentedSecret("cafe\u0301")
            val composed = PresentedSecret("caf\u00E9")

            val phc = hasher.hash(decomposed)

            assertTrue(hasher.matches(composed, phc))
        }
    }

    @Test
    fun aCompatibilityEquivalentSecretDoesNotVerify() {
        runBlocking {
            val hasher = Argon2Hasher()
            // U+FB01 LATIN SMALL LIGATURE FI: an NFKC compatibility equivalent of "fi", but not an
            // NFC one — NFC only recomposes canonical decompositions, so this ligature is left
            // alone and the two secrets below must stay two different secrets.
            val ligature = PresentedSecret("\uFB01le1234")

            val phc = hasher.hash(ligature)

            assertFalse(hasher.matches(PresentedSecret("file1234"), phc))
        }
    }

    @Test
    fun hashingWithAFixedRandomSourceProducesAnExactPhcString() {
        runBlocking {
            val fixedSalt = ByteArray(ARGON2_SALT_BYTES) { (it + 1).toByte() }
            val hasher = Argon2Hasher(fixedSaltRandom(fixedSalt))

            val phc = hasher.hash(SECRET_A)

            // Recomputed independently of Argon2Hasher, from the same salt, so this pins the
            // salt, the tag and the encoding together rather than only checking self-consistency.
            val parameters =
                Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(ARGON2_VERSION)
                    .withIterations(ARGON2_ITERATIONS)
                    .withMemoryAsKB(ARGON2_MEMORY_KIB)
                    .withParallelism(ARGON2_PARALLELISM)
                    .withSalt(fixedSalt)
                    .build()
            val generator = Argon2BytesGenerator()
            generator.init(parameters)
            val expectedTag = ByteArray(ARGON2_TAG_BYTES)
            generator.generateBytes(SECRET_A.value.toByteArray(Charsets.UTF_8), expectedTag)
            val expected = Argon2Phc(fixedSalt, expectedTag).encode()

            assertEquals(expected, phc)
        }
    }
}
