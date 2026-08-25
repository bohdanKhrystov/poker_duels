package duels.poker.server.auth

import duels.poker.server.session.PlayerId

/**
 * Mints and consumes one-time password reset tokens (`ADR-0031` §4).
 *
 * [issue] is implemented by `TASK-041613`. [consume] is declared here and implemented by
 * `TASK-041614`, so the port is written once rather than widened later.
 */
public interface PasswordResets {
    /**
     * Issues [token] as [playerId]'s new live reset token, superseding any token the player
     * already held.
     *
     * Answers `false`, writing nothing, when [playerId] already holds a token issued less than
     * fifteen minutes ago (`ADR-0031` §5): the request is a complete no-op and the outstanding
     * token is left exactly as it was, so a double-click cannot destroy the link the player is
     * about to use.
     *
     * @return `true` if [token] is now the player's live reset token; `false` if the request was
     *   suppressed by the resend window.
     */
    public suspend fun issue(playerId: PlayerId, token: ResetToken): Boolean

    /**
     * Consumes [token], if it names a live, unexpired reset, and rewrites the password to
     * [secret] once it passes policy (`ADR-0031` §4).
     *
     * Implemented by `TASK-041614`.
     */
    public suspend fun consume(token: ResetToken, secret: PresentedSecret): Boolean
}
