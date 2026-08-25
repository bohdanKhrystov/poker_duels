package duels.poker.server.mail

import duels.poker.server.auth.EmailAddress
import duels.poker.server.auth.RecoveryMailer
import duels.poker.server.auth.ResetToken
import duels.poker.server.auth.VerificationToken

/**
 * A recovery mailer that does nothing.
 *
 * This is what the wiring binds when no mail transport is configured, which is every developer
 * machine and every CI run. `ADR-0031` §7 makes a build with no sender configured a valid state
 * in development and tests. `ADR-0077` §1 makes this object the seam that implements that state.
 * A route calls this exactly as it calls a real transport, receives a completed suspend call
 * immediately, and nothing is sent. The absence of a sender is a property of the implementation,
 * not of the type, so no call site branches on it.
 */
public object NoRecoveryMailer : RecoveryMailer {
    override suspend fun sendVerification(address: EmailAddress, token: VerificationToken) {}

    override suspend fun sendPasswordReset(
        address: EmailAddress,
        token: ResetToken,
        handle: String,
    ) {}
}
