// File named for its intended contents; STORY-0405 adds the second DTO and can delete this suppression.
@file:Suppress("ktlint:standard:filename")

package duels.poker.server.protocol.http

import kotlinx.serialization.Serializable

/**
 * The body of `POST /api/auth/sign-up`, carrying a handle and a password.
 *
 * The server canonicalises what it receives via fold and length policy. The DTO is a shape, not a
 * rule.
 *
 * The `toString()` method returns a fixed redaction, because a plaintext password in a log line,
 * exception message or debugger frame is the leak that no amount of endpoint care can repair.
 *
 * @property handle The player's chosen handle. No default value: a missing field must be refused
 *   rather than silently becoming `""`.
 * @property password The player's chosen password. No default value: a missing field must be
 *   refused rather than silently becoming `""`, which would sign somebody up with an empty
 *   password.
 */
@Serializable
public data class SignUpRequest(val handle: String, val password: String) {
    override fun toString(): String = REDACTION

    public companion object {
        /**
         * The fixed string returned by `toString()` to prevent accidental leaks into logs or
         * exception messages.
         */
        public const val REDACTION: String = "SignUpRequest(redacted)"
    }
}

/**
 * The body of `POST /api/auth/sign-in`, carrying a handle and a password.
 *
 * The server canonicalises what it receives via fold and length policy. The DTO is a shape, not a
 * rule.
 *
 * The `toString()` method returns a fixed redaction, because a plaintext password in a log line,
 * exception message or debugger frame is the leak that no amount of endpoint care can repair.
 *
 * @property handle The player's handle.
 * @property password The player's password.
 */
@Serializable
public data class SignInRequest(val handle: String, val password: String) {
    override fun toString(): String = REDACTION

    public companion object {
        /**
         * The fixed string returned by `toString()` to prevent accidental leaks into logs or
         * exception messages.
         */
        public const val REDACTION: String = "SignInRequest(redacted)"
    }
}

/**
 * The response body of `POST /api/auth/sign-in`, carrying a session token.
 *
 * This is the only response body in the codebase that carries a live session token in plaintext.
 * It is returned exactly once at token issue, and nothing reads it back. A client learns who it
 * is from `GET /api/me`, which is one round trip and one source of truth.
 *
 * The `toString()` method returns a fixed redaction to prevent the token from leaking into logs
 * or exception messages.
 *
 * @property sessionToken The session token, valid for thirty days.
 */
@Serializable
public data class SignInResponse(val sessionToken: String) {
    override fun toString(): String = REDACTION

    public companion object {
        /**
         * The fixed string returned by `toString()` to prevent accidental leaks into logs or
         * exception messages.
         */
        public const val REDACTION: String = "SignInResponse(redacted)"
    }
}
