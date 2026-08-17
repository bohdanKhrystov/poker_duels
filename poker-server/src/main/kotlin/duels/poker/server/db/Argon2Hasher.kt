package duels.poker.server.db

import duels.poker.server.auth.PresentedSecret
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Hashes a [PresentedSecret] to a PHC string and verifies one against a stored PHC string.
 *
 * Declared as an interface only so `TASK-040313` can count calls made through it; there is no
 * second implementation, and none is planned.
 */
internal interface SecretHasher {
    /** Hashes [secret] with a fresh random salt, returning a string [parseArgon2PhcOrNull] accepts. */
    suspend fun hash(secret: PresentedSecret): String

    /**
     * Recomputes the tag for [secret] over the salt parsed out of [storedPhc] and compares the two
     * tags without revealing where, or whether, they differ.
     *
     * A [storedPhc] the parser refuses — the wrong shape, algorithm, version or cost parameters —
     * answers `false`. It never throws.
     */
    suspend fun matches(secret: PresentedSecret, storedPhc: String): Boolean
}

/**
 * The project's only [SecretHasher]: Argon2id via Bouncy Castle, at the cost parameters fixed in
 * [Argon2Phc], per `ADR-0027` §1.
 *
 * [random] is a constructor parameter — not a default read at call time — so a test can inject a
 * source whose output it controls and pin the exact string [hash] produces, while production keeps
 * a fresh [SecureRandom] per instance.
 */
internal class Argon2Hasher(private val random: SecureRandom = SecureRandom()) : SecretHasher {

    override suspend fun hash(secret: PresentedSecret): String =
        withContext(Dispatchers.IO) {
            val salt = ByteArray(ARGON2_SALT_BYTES)
            random.nextBytes(salt)
            val tag = tagFor(secret, salt)
            Argon2Phc(salt, tag).encode()
        }

    override suspend fun matches(secret: PresentedSecret, storedPhc: String): Boolean =
        withContext(Dispatchers.IO) {
            val parsed = parseArgon2PhcOrNull(storedPhc) ?: return@withContext false
            // The salt below is the one this call parsed out of storedPhc, never a fresh one — a
            // fresh salt would make every call answer false, right secret or not.
            val recomputed = tagFor(secret, parsed.salt)
            // Bytewise equality alone would return as soon as it finds the first differing byte,
            // handing an attacker a timing signal for how many leading bytes of a guess were
            // right. This call examines the whole array in constant time regardless.
            MessageDigest.isEqual(recomputed, parsed.tag)
        }

    private fun tagFor(secret: PresentedSecret, salt: ByteArray): ByteArray {
        val parameters =
            Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(ARGON2_VERSION)
                .withIterations(ARGON2_ITERATIONS)
                .withMemoryAsKB(ARGON2_MEMORY_KIB)
                .withParallelism(ARGON2_PARALLELISM)
                .withSalt(salt)
                .build()

        val generator = Argon2BytesGenerator()
        generator.init(parameters)

        val tag = ByteArray(ARGON2_TAG_BYTES)
        // Written out ourselves rather than left to Bouncy Castle's char[] overload, so this
        // project owns the encoding and a future change to it breaks a test rather than silently
        // invalidating every stored hash.
        generator.generateBytes(secret.value.toByteArray(Charsets.UTF_8), tag)
        return tag
    }
}
