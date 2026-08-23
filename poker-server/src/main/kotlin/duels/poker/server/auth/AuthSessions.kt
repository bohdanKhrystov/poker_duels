package duels.poker.server.auth

import duels.poker.server.session.PlayerId

/**
 * A port for issuing and verifying bearer session tokens that authenticate requests to a game
 * server.
 *
 * **Nothing on this port returns a stored form.** `issue` hands back the plaintext token
 * exactly once, because that is the only moment it exists. The storage layer hashes the token
 * before persistence; this port sees only the plaintext.
 *
 * **`playerOf` answers `null` for both an expired row and an unknown one**, deliberately and
 * indistinguishably. Expiry is enforced in the read, not by a sweep, so an expired row is
 * garbage rather than a hole. There is no separate error condition for a stale token.
 */
public interface AuthSessions {
    /**
     * Issue a new session token for a player.
     *
     * Returns the plaintext token exactly once. Once returned from this function, the token is
     * never readable in plaintext again — it is hashed before storage, and all future operations
     * see only the stored form.
     *
     * @param playerId The player to issue a session for.
     * @return The plaintext session token for this player.
     */
    public suspend fun issue(playerId: PlayerId): SessionToken

    /**
     * Retrieve the player a session token names, or null.
     *
     * Returns `null` for both an unknown token and an expired session, indistinguishably.
     * An expired row is not swept up by background cleanup; it is treated as unknown at read
     * time.
     *
     * @param token The session token to verify.
     * @return The `PlayerId` this token names, or `null` if the token is unknown or expired.
     */
    public suspend fun playerOf(token: SessionToken): PlayerId?

    /**
     * Revoke a session token.
     *
     * Deletes the given token from the session store. If the token is unknown or already
     * deleted, this operation returns normally without error.
     *
     * @param token The session token to revoke.
     */
    public suspend fun delete(token: SessionToken)
}
