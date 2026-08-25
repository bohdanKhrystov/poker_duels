package duels.poker.server.auth

import java.security.SecureRandom
import java.util.Base64

private const val RECOVERY_TOKEN_BYTES = 32

/**
 * Mints opaque recovery tokens for email verification and password reset.
 *
 * Both token types are generated from 256 bits of entropy, encoded as URL-safe base64 without
 * padding, identical to the session token generation in `SessionTokens` per `ADR-0031` §4.
 * The tokens themselves are not hashed here; the stored forms live in `duels.poker.server.db`.
 * This class owns only the generation of fresh tokens from entropy.
 *
 * [random] is a constructor parameter — not a default read at call time — so a test can inject a
 * source whose output it controls and pin the exact strings, while production keeps a fresh
 * [SecureRandom] per instance.
 */
public class RecoveryTokens(private val random: SecureRandom = SecureRandom()) {
    /**
     * Generates a new verification token: 256 bits of entropy (32 bytes), encoded as URL-safe
     * base64 with no padding.
     *
     * Produces exactly 43 characters per invocation.
     */
    public fun newVerificationToken(): VerificationToken {
        val bytes = ByteArray(RECOVERY_TOKEN_BYTES)
        random.nextBytes(bytes)
        return VerificationToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
    }

    /**
     * Generates a new reset token: 256 bits of entropy (32 bytes), encoded as URL-safe base64
     * with no padding.
     *
     * Produces exactly 43 characters per invocation.
     */
    public fun newResetToken(): ResetToken {
        val bytes = ByteArray(RECOVERY_TOKEN_BYTES)
        random.nextBytes(bytes)
        return ResetToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
    }
}

/**
 * A token sent in an email to verify the address belongs to the player.
 *
 * The `toString()` method returns a fixed redaction, because a bearer token in a log line is the
 * leak that no amount of endpoint care can repair. Access the raw token via [value].
 */
@JvmInline
public value class VerificationToken(public val value: String) {
    override fun toString(): String = REDACTION

    public companion object {
        /**
         * The fixed string returned by `toString()` to prevent accidental leaks into logs or
         * exception messages.
         */
        public const val REDACTION: String = "VerificationToken(redacted)"
    }
}

/**
 * A token sent in an email to reset the player's password.
 *
 * The `toString()` method returns a fixed redaction, because a bearer token in a log line is the
 * leak that no amount of endpoint care can repair. Access the raw token via [value].
 */
@JvmInline
public value class ResetToken(public val value: String) {
    override fun toString(): String = REDACTION

    public companion object {
        /**
         * The fixed string returned by `toString()` to prevent accidental leaks into logs or
         * exception messages.
         */
        public const val REDACTION: String = "ResetToken(redacted)"
    }
}
