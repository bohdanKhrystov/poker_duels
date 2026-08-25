package duels.poker.server.auth

/**
 * An email address provided by a player for password recovery.
 *
 * The `toString()` method returns a fixed redaction, because an email address in a log line is the
 * leak that no amount of endpoint care can repair. Access the raw address via [value].
 * Per [ADR-0031](../adr/ADR-0031-an-optional-verified-recovery-email.md) §6.3, the address
 * appears in exactly one flow, and a redacting `toString()` is what makes a `"$address"` in a log
 * line print the redaction rather than the address itself.
 */
@JvmInline
public value class EmailAddress(public val value: String) {
    override fun toString(): String = REDACTION

    public companion object {
        /**
         * The fixed string returned by `toString()` to prevent accidental leaks into logs or
         * exception messages.
         */
        public const val REDACTION: String = "EmailAddress(redacted)"
    }
}
