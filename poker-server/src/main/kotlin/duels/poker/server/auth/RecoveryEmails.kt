package duels.poker.server.auth

import duels.poker.server.session.PlayerId

/**
 * A port for the two recovery tables `ADR-0031` builds: the proven `recovery_email` row a player
 * holds once verified, and the `email_verification` row a claim writes before it is proven.
 *
 * **The address never leaves this package except into `RecoveryMailer`.** No member returns an
 * [EmailAddress] to a caller, and no member returns a `String` that could be one —
 * [ResetRecipient.handle] is a login handle, and `loginHandleOrNull` permits only `[a-z0-9._-]`,
 * so it structurally cannot be an address.
 *
 * **[verifiedOwnerOf] is the only read that returns an owner and nothing else** — it keeps its
 * second caller, the attach path's already-proven-elsewhere check, which must not receive a
 * handle. It returns a [PlayerId], never the address itself; the address needed to actually send
 * mail is read inside the storage layer, at send time.
 *
 * **Expiry is enforced in every read**, by `WHERE expires_at > now()`. A missed sweep is a
 * retention defect and never a security hole — [deleteExpiredVerifications] only ever deletes rows
 * a read would already have refused.
 *
 * **[claimPending] answering [ClaimPendingResult.Suppressed] is `ADR-0031` §5's fifteen-minute
 * rule, applied to this table.** It is the same rule `PasswordResets.issue` answers `false` for,
 * and it is the only per-account cap on the mail a successful attach causes. An implementation
 * that always answers [ClaimPendingResult.Claimed] satisfies this type and breaks the design.
 */
public interface RecoveryEmails {
    /**
     * Write a pending verification row for [playerId], replacing any the player already holds —
     * unless the player already holds one issued less than fifteen minutes ago, in which case
     * nothing is written at all.
     *
     * `ADR-0031` §5 answers `202` to the caller whatever this returns, so the result is never a
     * status code and no caller branches a *response* on it. It answers a narrower question this
     * ticket exists for — *did this request cause a mail* — that nothing else in the system can
     * answer.
     *
     * @param playerId The player attaching an address.
     * @param address The address to verify.
     * @param token The verification token minted for this attempt. Stored only if this call
     *   returns [ClaimPendingResult.Claimed]; discarded if it returns
     *   [ClaimPendingResult.Suppressed].
     * @return [ClaimPendingResult.Claimed] if a row was written, [ClaimPendingResult.Suppressed]
     *   if the fifteen-minute window suppressed the write.
     */
    public suspend fun claimPending(
        playerId: PlayerId,
        address: EmailAddress,
        token: VerificationToken,
    ): ClaimPendingResult

    /**
     * Consume a verification token and, if it names a live pending row, prove the address.
     *
     * @param token The token presented by the caller.
     * @return A [VerifyEmailResult] describing the outcome.
     */
    public suspend fun verifyPending(token: VerificationToken): VerifyEmailResult

    /**
     * Ask whether a player has a proven recovery email attached.
     *
     * @param playerId The player to check.
     * @return `true` if the player has a verified `recovery_email` row, `false` otherwise.
     */
    public suspend fun hasRecoveryEmail(playerId: PlayerId): Boolean

    /**
     * Look up the player who owns a verified address, for `forgot-password`.
     *
     * Returns `null` for an address that is unknown and for one that is only pending,
     * indistinguishably — `ADR-0031` §3 treats the two states as the same state as far as the
     * account is concerned.
     *
     * @param address The address to look up.
     * @return The [PlayerId] that owns this address once verified, or `null`.
     */
    public suspend fun verifiedOwnerOf(address: EmailAddress): PlayerId?

    /**
     * Look up the player and login handle behind a verified recovery address, for
     * `forgot-password`'s mail — the third argument `RecoveryMailer.sendPasswordReset` requires
     * and that [verifiedOwnerOf] alone cannot supply.
     *
     * Answers `null` for exactly the states [verifiedOwnerOf] does — an address that is unknown
     * and one that is only pending, indistinguishably (`ADR-0031` §3) — and for a third: a
     * verified address whose owner holds no `password` credential. All three are one outcome to
     * the caller (`ADR-0082` §1).
     *
     * **There is no [PlayerId] overload of this member, and there must never be one.** Obtaining a
     * handle requires already holding a *proven* recovery address, which is the exact secret
     * `forgot-password` exists to refuse to disclose (`ADR-0082` §1).
     *
     * @param address The address to look up.
     * @return The [PlayerId] and login handle behind [address] once verified, or `null`.
     */
    public suspend fun resetRecipientOf(address: EmailAddress): ResetRecipient?

    /**
     * Remove a player's proven recovery email, if one exists.
     *
     * `ADR-0031` §5's `DELETE` answers `204` whether or not a row existed, so this operation
     * returns normally in both cases.
     *
     * @param playerId The player to detach.
     */
    public suspend fun detach(playerId: PlayerId)

    /**
     * Delete every expired `email_verification` row.
     *
     * The sweep statement `ADR-0031` §3 requires: a pending row holds an unproven address, which
     * is personal data this system has not yet been able to use for its one purpose, so the
     * delete is not optional.
     *
     * @return The number of rows deleted, for a log line and for a test to assert on.
     */
    public suspend fun deleteExpiredVerifications(): Int
}

/**
 * The player id and login handle behind a verified recovery address, answered together because
 * `RecoveryMailer.sendPasswordReset` needs both from the one proof of possession
 * [RecoveryEmails.resetRecipientOf] requires (`ADR-0082` §1).
 *
 * An ordinary `data class` — deliberately not given [EmailAddress]'s redacting `toString()`.
 * Nothing on the reset path logs anything today; the trigger for revisiting that is the first log
 * line anywhere on this path (`ADR-0082` §Consequences).
 */
public data class ResetRecipient(val playerId: PlayerId, val handle: String)

/**
 * The answer to a request to verify a pending email address.
 *
 * A sealed type because callers must act on the outcomes differently: the address is now proven,
 * the token did not name a live pending row, or the address is already proven for another player.
 */
public sealed interface VerifyEmailResult {
    /**
     * The token named a live pending row, and the address is now proven for that player.
     */
    public object Verified : VerifyEmailResult

    /**
     * The token is unknown, expired, or already consumed.
     *
     * `ADR-0031` §5 treats the three as one case — its `400` does not distinguish them — so they
     * are one value here, and a caller cannot accidentally tell them apart.
     */
    public object Refused : VerifyEmailResult

    /**
     * The token was live, but the address it names is already verified for another player.
     *
     * This answers `ADR-0031` §5's `409`. It is not an enumeration oracle: the caller has already
     * proven possession of the mailbox by holding a token that was mailed to it.
     */
    public object AddressTaken : VerifyEmailResult
}

/**
 * The answer to a request to claim a pending email address.
 *
 * A sealed type, not a `Boolean`, though `PasswordResets.issue` answers the identical
 * fifteen-minute rule with one. Recorded so it is chosen rather than fallen into: inside this
 * file the idiom is already that a command which can go more than one way gets a sealed result
 * ([VerifyEmailResult]) and a query returns a plain value ([RecoveryEmails.hasRecoveryEmail],
 * [RecoveryEmails.verifiedOwnerOf], [RecoveryEmails.deleteExpiredVerifications]);
 * [RecoveryEmails.claimPending] is a command with two outcomes. `issue` has two outcomes and will
 * never have more, but the attach path already has a **second** mail-suppressing condition in
 * flight — the address-already-verified-elsewhere skip — so its caller decides *send or not* from
 * more than one input, and an exhaustive `when` is what stops a third condition being absorbed
 * silently by an `if`. If the two ports are ever made symmetric, the cheap direction is to give
 * `issue` a sealed type, not to take this one away.
 *
 * Neither object declares a property: no window remaining, no retry-after, no token, no address.
 * `ADR-0031` §5 answers `202` identically in every case, and a value the caller could surface here
 * is the oracle the whole endpoint is built to avoid.
 */
public sealed interface ClaimPendingResult {
    /**
     * A pending row was written, and the token the caller passed to
     * [RecoveryEmails.claimPending] is the one now stored.
     *
     * This is the only value on which a verification mail may be sent.
     */
    public object Claimed : ClaimPendingResult

    /**
     * The player already held a pending row issued inside the fifteen-minute window.
     *
     * Nothing was written: the token the caller passed in was discarded, and the outstanding one
     * is still live. `ADR-0031` §5: inside that window the request is a complete no-op, and
     * "crucially the outstanding token is not invalidated, so a double-click does not destroy the
     * link the player is about to use."
     */
    public object Suppressed : ClaimPendingResult
}
