package duels.poker.server.http

import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.session.PlayerId

/**
 * A port for writing player profiles.
 *
 * This port exists so the routes can be tested without a database, and so no route ever holds
 * a `DataSource` (`ADR-0011`). **This port is separate from `ProfileReads`** because `ProfileReads`'s
 * contract is that nothing on it creates or mutates. Tests rely on that guarantee
 * (`ADR-0021`). The first HTTP write gets its own port rather than eroding it.
 *
 * This port exposes no function that takes a display name and returns a `PlayerId`, `DeviceId`
 * or `ProfileResponse` — §7 of `ADR-0029` makes that a structural guarantee on both ports.
 */
public interface ProfileWrites {
    /**
     * Set a player's display name.
     *
     * @param playerId The player whose name to set.
     * @param canonicalName The display name, already canonicalised by the caller. The caller
     *   has already invoked `canonicalDisplayNameOrNull` and passed the result. This port
     *   does not re-canonicalise; a port that did would be a second place the rule lives.
     * @return A [SetNameResult] describing what happened.
     */
    public suspend fun setDisplayName(playerId: PlayerId, canonicalName: String): SetNameResult
}

/**
 * The answer to a request to set a display name.
 *
 * A sealed type because callers must act on the two outcomes differently (`ADR-0134` §5):
 * the name was set successfully, or the folded name collides with an already-spent string.
 * These are distinct HTTP codes (200, 409) and distinct decisions a client must make.
 */
public sealed interface SetNameResult {
    /**
     * The display name was set successfully.
     *
     * @param profile The player's profile after the name was set, including the canonical
     *   `displayName`. The server trims and normalises; the client is **told** the exact
     *   string it now owns rather than assumed to get what it sent (`ADR-0029` §5).
     */
    public data class NameSet(val profile: ProfileResponse) : SetNameResult

    /**
     * The folded name collides with an already-spent string — held, blocked, retired, or
     * replaced by an earlier rename. The name is already taken.
     *
     * This answers `409 Conflict`. The fold uses `lower(name COLLATE "und-x-icu")`,
     * so `Bob` and `bob` collide (`ADR-0029` §1). The player may send a different name.
     */
    public object NameTaken : SetNameResult
}
