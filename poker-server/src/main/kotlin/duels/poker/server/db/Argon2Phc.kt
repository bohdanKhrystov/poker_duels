package duels.poker.server.db

import java.util.Base64

// Argon2id parameters per OWASP baseline and ADR-0027 §1
internal const val ARGON2_VERSION = 19
internal const val ARGON2_MEMORY_KIB = 19456
internal const val ARGON2_ITERATIONS = 2
internal const val ARGON2_PARALLELISM = 1
internal const val ARGON2_SALT_BYTES = 16
internal const val ARGON2_TAG_BYTES = 32

/**
 * Encodes a salt and tag into the PHC string format required by `ADR-0027` §1:
 * `$argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>`.
 *
 * The parameters travel with every row so they can be raised independently on the next
 * successful verification, without requiring a migration.
 */
internal class Argon2Phc(val salt: ByteArray, val tag: ByteArray) {
    init {
        require(salt.size == ARGON2_SALT_BYTES) {
            "Salt must be exactly $ARGON2_SALT_BYTES bytes, got ${salt.size}"
        }
        require(tag.size == ARGON2_TAG_BYTES) {
            "Tag must be exactly $ARGON2_TAG_BYTES bytes, got ${tag.size}"
        }
    }

    fun encode(): String {
        val base64 = Base64.getEncoder().withoutPadding()
        val saltB64 = base64.encodeToString(salt)
        val tagB64 = base64.encodeToString(tag)
        return "${'$'}argon2id${'$'}v=$ARGON2_VERSION${'$'}m=$ARGON2_MEMORY_KIB,t=$ARGON2_ITERATIONS,p=$ARGON2_PARALLELISM${'$'}$saltB64${'$'}$tagB64"
    }
}

/**
 * Parses a PHC string that this project's own [Argon2Phc.encode] produced, returning `null` for
 * every other string: a different algorithm, version or cost parameters, a reshaped string, or
 * bytes that are not valid, unpadded, standard base64 of the right length.
 *
 * Never throws. [java.util.Base64]'s decoder signals invalid input with
 * `IllegalArgumentException`, which is caught here rather than left to reach a login attempt.
 * Never logs or returns any part of [encoded] — a refusal is a plain `null`, with no reason
 * attached for anything to leak through.
 */
internal fun parseArgon2PhcOrNull(encoded: String): Argon2Phc? {
    val parts = encoded.split('$')
    if (parts.size != 6 || parts[0].isNotEmpty()) return null

    // The three fixed sections are whole-string comparisons against the constants above, not a
    // parse of the numbers inside them — a reordering, an added space or a changed digit is
    // refused without a second rule, and "m=019456" is refused without a number parser to fool.
    if (parts[1] != "argon2id") return null
    if (parts[2] != "v=$ARGON2_VERSION") return null
    if (parts[3] != "m=$ARGON2_MEMORY_KIB,t=$ARGON2_ITERATIONS,p=$ARGON2_PARALLELISM") return null

    val saltB64 = parts[4]
    val tagB64 = parts[5]
    // java.util.Base64's decoder accepts a value with or without padding, so refusing '=' has to
    // be ours: without this line a padded salt or tag decodes to exactly the bytes we would have
    // emitted unpadded, and this function would wrongly accept it.
    if (saltB64.contains('=') || tagB64.contains('=')) return null

    val decoder = Base64.getDecoder()
    val salt = decoder.decodeOrNull(saltB64) ?: return null
    val tag = decoder.decodeOrNull(tagB64) ?: return null
    if (salt.size != ARGON2_SALT_BYTES || tag.size != ARGON2_TAG_BYTES) return null

    return Argon2Phc(salt, tag)
}

private fun Base64.Decoder.decodeOrNull(value: String): ByteArray? {
    return try {
        decode(value)
    } catch (ignored: IllegalArgumentException) {
        null
    }
}
