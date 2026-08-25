package duels.poker.server.db

import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.VerificationToken
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

/**
 * Computes the SHA-256 digest of a verification token for storage and lookup.
 *
 * A fresh [MessageDigest] is created per call — instances are not thread-safe, and this function
 * is called from `Dispatchers.IO`.
 */
internal fun recoveryTokenDigest(token: VerificationToken): ByteArray =
    digestOf(token.value)

/**
 * Computes the SHA-256 digest of a reset token for storage and lookup.
 *
 * A fresh [MessageDigest] is created per call — instances are not thread-safe, and this function
 * is called from `Dispatchers.IO`.
 */
internal fun recoveryTokenDigest(token: ResetToken): ByteArray =
    digestOf(token.value)

private fun digestOf(value: String): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray(UTF_8))
