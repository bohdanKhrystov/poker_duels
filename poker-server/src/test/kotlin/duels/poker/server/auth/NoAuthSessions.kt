package duels.poker.server.auth

import duels.poker.server.session.PlayerId

/**
 * A test double for [AuthSessions] that models the "no session was ever issued" case.
 *
 * This double is used in tests that run before sign-in, when no player has authenticated yet.
 * Any code that attempts to `issue` a token through this double is using the wrong double,
 * and will fail fast. All other operations return the neutral outcome for an unauthenticated
 * context.
 */
internal object NoAuthSessions : AuthSessions {
    override suspend fun issue(playerId: PlayerId): SessionToken {
        throw UnsupportedOperationException(
            "NoAuthSessions is a pre-authentication double; it does not issue tokens",
        )
    }

    override suspend fun playerOf(token: SessionToken): PlayerId? = null

    override suspend fun delete(token: SessionToken) {
        // No operation; this double holds no tokens to delete.
    }
}
