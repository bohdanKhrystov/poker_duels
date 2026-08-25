package duels.poker.server.protocol.http

import kotlinx.serialization.Serializable

/**
 * The body of `POST /api/auth/verify-email`, carrying the token mailed to the address being
 * proven.
 *
 * No default value: a missing field must be refused with `400` rather than silently becoming
 * `""`, which would ask [duels.poker.server.auth.RecoveryEmails.verifyPending] to consume the
 * empty string as a token instead of the request never reaching the port at all.
 *
 * The `toString()` method returns a fixed redaction, because a verification token in a log line
 * or exception message is the leak that no amount of endpoint care can repair — the same reason
 * [duels.poker.server.auth.VerificationToken] itself redacts.
 *
 * @property token The verification token from the mailed link.
 */
@Serializable
public data class VerifyEmailRequest(val token: String) {
    override fun toString(): String = REDACTION

    public companion object {
        /**
         * The fixed string returned by `toString()` to prevent accidental leaks into logs or
         * exception messages.
         */
        public const val REDACTION: String = "VerifyEmailRequest(redacted)"
    }
}

/**
 * The body of `POST /api/auth/reset-password`, carrying the one-time token mailed to the player
 * and the new password to rewrite the credential to.
 *
 * Neither field is defaulted: a missing field must be refused with `400` rather than silently
 * becoming `""`, which would ask [duels.poker.server.auth.PasswordResets.consume] to spend the
 * empty string as a token, or set the empty string as a password, instead of the request never
 * reaching the port at all.
 *
 * The `toString()` method returns a fixed redaction, because a reset token and a plaintext
 * password in a log line or exception message are exactly the leaks no amount of endpoint care
 * can repair afterward — the same reason [VerifyEmailRequest] and
 * [duels.poker.server.auth.VerificationToken] redact.
 *
 * @property token The reset token from the mailed link.
 * @property newPassword The password to rewrite the credential to, once [token] is spent.
 */
@Serializable
public data class ResetPasswordRequest(val token: String, val newPassword: String) {
    override fun toString(): String = REDACTION

    public companion object {
        /**
         * The fixed string returned by `toString()` to prevent accidental leaks into logs or
         * exception messages.
         */
        public const val REDACTION: String = "ResetPasswordRequest(redacted)"
    }
}

/**
 * The body of `DELETE /api/auth/recovery-email`, carrying the caller's current password so the
 * erase can be gated on it.
 *
 * No default value: a missing field must be refused with `400` rather than silently becoming
 * `""`, which would ask [duels.poker.server.auth.Credentials.verifyCurrent] to check the empty
 * string against a live credential instead of the request never reaching the port at all.
 *
 * The `toString()` method returns a fixed redaction, because a plaintext password in a log line
 * or exception message is exactly the leak no amount of endpoint care can repair afterward — the
 * same reason [ResetPasswordRequest] and [VerifyEmailRequest] redact.
 *
 * @property currentPassword The password to verify against the caller's current credential before
 *   the erase runs.
 */
@Serializable
public data class DetachRecoveryEmailRequest(val currentPassword: String) {
    override fun toString(): String = REDACTION

    public companion object {
        /**
         * The fixed string returned by `toString()` to prevent accidental leaks into logs or
         * exception messages.
         */
        public const val REDACTION: String = "DetachRecoveryEmailRequest(redacted)"
    }
}
