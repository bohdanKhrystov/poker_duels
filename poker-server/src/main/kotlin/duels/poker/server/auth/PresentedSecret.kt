package duels.poker.server.auth

/**
 * A bearer secret presented at sign-in or sign-up.
 *
 * The `toString()` method returns a fixed redaction, because a bearer secret in a log line is the
 * leak that no amount of endpoint care can repair. Access the raw secret via [value].
 */
@JvmInline
public value class PresentedSecret(public val value: String) {
    override fun toString(): String = REDACTION

    public companion object {
        /**
         * The fixed string returned by `toString()` to prevent accidental leaks into logs or
         * exception messages.
         */
        public const val REDACTION: String = "PresentedSecret(redacted)"
    }
}

/**
 * An opaque session token that authenticates a request to a game server.
 *
 * The `toString()` method returns a fixed redaction, because a bearer token in a log line is the
 * leak that no amount of endpoint care can repair. Access the raw token via [value].
 */
@JvmInline
public value class SessionToken(public val value: String) {
    override fun toString(): String = REDACTION

    public companion object {
        /**
         * The fixed string returned by `toString()` to prevent accidental leaks into logs or
         * exception messages.
         */
        public const val REDACTION: String = "SessionToken(redacted)"
    }
}
