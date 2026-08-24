package duels.poker.server.http

import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.session.PlayerId

/**
 * A port for reading player profiles, their coin balances and their duel history.
 *
 * This port exists so the routes can be tested without a database, and so no route ever holds
 * a `DataSource` (`ADR-0011`). It returns the response type rather than a parallel domain type
 * because the answer's shape *is* the wire's shape; a second identical type would be a copy nobody
 * reads. **Nothing on this port creates anything** — an unknown player is `null`, and profile
 * creation happens on the socket handshake only (`ADR-0012`), so a crawler cannot mint rows.
 *
 * Both functions are keyed by [PlayerId], and neither gains a device-keyed overload, now or later
 * (`ADR-0030` §4): a caller resolves identity — session token or device id — into a player through
 * `IdentityResolver` first, and only the resolved player ever reaches this port.
 */
public interface ProfileReads {
    /**
     * Read a player's profile and balance.
     *
     * @param playerId The player identifier to look up.
     * @return A profile response if the player is known, `null` otherwise. Does not create
     *   anything on an unknown player.
     */
    public suspend fun profileOf(playerId: PlayerId): ProfileResponse?

    /**
     * Read a player's most recent completed duels, newest first.
     *
     * @param playerId The player whose duels to read. Only duels this player sat in are
     *   returned — never another player's.
     * @param limit The maximum number of duels to return. The caller is expected to have
     *   already clamped this (see `duelLimitOrNull`); this port honours whatever value it is
     *   given.
     * @param after A position in the list to resume after. `null` reads the newest page.
     * @param filter Narrows which of the player's duels are returned. `DuelFilter.NONE` narrows
     *   nothing — the caller has already parsed and refused whatever it would not accept.
     * @return The player's duels ordered by finish time, newest first, capped at [limit].
     */
    public suspend fun recentDuelsOf(
        playerId: PlayerId,
        limit: Int,
        after: DuelCursor? = null,
        filter: DuelFilter = DuelFilter.NONE,
    ): List<DuelSummaryResponse>
}
