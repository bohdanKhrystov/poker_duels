package duels.poker.server.auth

import duels.poker.server.session.PlayerId

/**
 * A port for revoking device bindings and signing out everywhere but the revoking session.
 *
 * A device binding is the link between a `device_id` and a `player_id`. Revoking a binding marks
 * it as revoked in the database, prevents the device from authenticating as that player again,
 * and optionally signs out all other sessions for that player (per ADR-0050), while preserving
 * the revoking session.
 *
 * **The statement names the player, never a device**, because there is at most one live binding
 * per player and a client asserts no fact (ADR-0002). A parameter carrying a device id would be
 * the shape in which one player revokes another's.
 *
 * **`keeping` is the caller's own token, and it is not optional.** ADR-0049 §5 requires a session,
 * so there is always exactly one row to exclude from the sweep, and ADR-0037's *"revocation does
 * not kill the revoking session"* holds by construction rather than by care. A nullable parameter
 * would make "sweep everything, including the caller" reachable.
 */
public interface DeviceBindings {
    /**
     * Revoke this player's live device binding and delete all other sessions for this player.
     *
     * Marks this player's live binding as revoked if one is live, in the `device_binding` table.
     * In the same transaction, deletes every `auth_session` row for this player except the one
     * named by `keeping`. Both operations are performed atomically.
     *
     * This operation is a no-op with respect to the `player`, `credential`, `duel` and
     * `duel_result` tables — only `device_binding` and `auth_session` are touched.
     *
     * A player who was never bound and a player whose binding was live are told apart by
     * nothing: this function returns `Unit` in both cases. A player who holds no session
     * (device-authenticated only) cannot call this function; the HTTP layer enforces this by
     * returning `401 Unauthorized` to device-authenticated requests to the revocation route.
     *
     * @param playerId The player whose binding is to be revoked.
     * @param keeping The session token of the revoking request, which will be preserved.
     */
    public suspend fun revoke(playerId: PlayerId, keeping: SessionToken)
}
