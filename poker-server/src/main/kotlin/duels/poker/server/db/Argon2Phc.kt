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
