package duels.poker.server.auth

/**
 * A port to send the two mails a recovery email address is ever used for.
 *
 * `ADR-0031` §6.2 makes this a mechanism, not a sentence someone has to remember: **the interface
 * declares exactly two members, both named for the mail they send.** There is no
 * `send(to, subject, body)` anywhere in this codebase, and there never will be without a decision
 * that supersedes `ADR-0031` — adding a third member, of any kind, is that decision, made visible
 * as a diff a reviewer reads and caught structurally by a test asserting this interface's shape.
 *
 * `sendPasswordReset` takes `handle` as a plain `String`, not a `LoginHandle`. `ADR-0031` §6.2's
 * illustrative snippet writes `handle: LoginHandle`, but no such type exists in this codebase:
 * `loginHandleOrNull` returns `String?`, and `Credentials` itself takes `identifier: String`. The
 * mechanism the ADR specifies is the two member *names*, transcribed here exactly; the parameter
 * type follows the type that already exists rather than the illustrative snippet.
 *
 * What sends these mails, and what happens when no sender is configured, is `DEC-072`
 * (`TASK-041627`) — deliberately absent here. The subject lines, the bodies, and the reset link
 * are `ADR-0031`'s explicit deferral to `STORY-0412`.
 */
public interface RecoveryMailer {
    /**
     * Send a token proving the address belongs to the player who provided it.
     *
     * @param address The address to send to.
     * @param token The verification token the address must present to prove receipt.
     */
    public suspend fun sendVerification(address: EmailAddress, token: VerificationToken)

    /**
     * Send a token that lets its holder reset the account's password.
     *
     * @param address The address to send to.
     * @param token The reset token the holder must present to reset the password.
     * @param handle The player's login handle, included so a player who forgot it can still
     *   recover the account despite having opted in.
     */
    public suspend fun sendPasswordReset(address: EmailAddress, token: ResetToken, handle: String)
}
