// File named for its intended contents; a later route ticket in this recovery-route chain adds
// the second DTO and can delete this suppression, the same pattern AuthDtos.kt started from.
@file:Suppress("ktlint:standard:filename")

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
