package duels.poker.server.db

import duels.poker.server.auth.SessionToken
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

/**
 * Computes the SHA-256 digest of a session token for storage and lookup.
 *
 * A fresh [MessageDigest] is created per call — instances are not thread-safe, and this function
 * is called from `Dispatchers.IO`.
 */
internal fun sessionTokenDigest(token: SessionToken): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(token.value.toByteArray(UTF_8))
