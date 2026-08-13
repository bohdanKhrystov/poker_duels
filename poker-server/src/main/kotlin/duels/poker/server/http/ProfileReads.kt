package duels.poker.server.http

import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId

/**
 * A port for reading player profiles, their coin balances and their duel history.
 *
 * This port exists so the routes can be tested without a database, and so no route ever holds
 * a `DataSource` (`ADR-0011`). It returns the response type rather than a parallel domain type
 * because the answer's shape *is* the wire's shape; a second identical type would be a copy nobody
 * reads. **Nothing on this port creates anything** — an unknown device is `null`, and profile
 * creation happens on the socket handshake only (`ADR-0012`), so a crawler cannot mint rows.
 */
public interface ProfileReads {
    /**
     * Read a device's profile and balance.
     *
     * @param deviceId The device identifier to look up.
     * @return A profile response if the device is known, `null` otherwise. Does not create
     *   anything on an unknown device.
     */
    public suspend fun profileOf(deviceId: DeviceId): ProfileResponse?

    /**
     * Read a player's most recent completed duels, newest first.
     *
     * @param playerId The player whose duels to read. Only duels this player sat in are
     *   returned — never another player's.
     * @param limit The maximum number of duels to return. The caller is expected to have
     *   already clamped this (see `duelLimitOrNull`); this port honours whatever value it is
     *   given.
     * @return The player's duels ordered by finish time, newest first, capped at [limit].
     */
    public suspend fun recentDuelsOf(playerId: PlayerId, limit: Int): List<DuelSummaryResponse>
}
