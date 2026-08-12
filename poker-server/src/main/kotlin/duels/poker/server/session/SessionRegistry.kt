package duels.poker.server.session

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * A unique identifier for a live connection's session.
 *
 * @param value The session identifier string.
 */
@JvmInline
public value class SessionId(public val value: String)

/**
 * A live session binding a connection to the player behind it.
 *
 * @param id The unique session identifier.
 * @param player The player profile this session belongs to.
 */
public data class Session(val id: SessionId, val player: Player)

/**
 * A concurrent registry of live sessions, keyed by [SessionId].
 *
 * The registry holds sessions and nothing else: no game state, no room, no channel, no clock,
 * and no opinion on the socket behind a session. Every method is non-suspending, so [remove] can
 * be called from a connection's `finally` block without risking that the cleanup never runs.
 *
 * [remove] is the exactly-once primitive: it returns the removed [Session] the first time it is
 * called for a given id, and `null` on every call after — whichever caller wins the race to
 * remove an id is the one, and only one, that observes a non-null result.
 *
 * [sessionsOf] is a query, not a policy: whether a player may hold more than one live session at
 * once is undecided (`DEC-011`), and this class neither permits nor forbids it — it only reports
 * what sessions currently exist.
 */
public class SessionRegistry {
    private val sessions = ConcurrentHashMap<SessionId, Session>()

    /**
     * Register a session, making it visible to [get], [sessionsOf] and [size].
     *
     * @param session The session to register.
     */
    public fun register(session: Session) {
        sessions[session.id] = session
    }

    /**
     * Remove a session by id.
     *
     * Returns the removed [Session] on the call that actually removes it, and `null` on every
     * subsequent call for that id — including when the id was never registered. This makes
     * "closed exactly once" observable: only one caller, ever, sees a non-null result for a
     * given id.
     *
     * @param id The session id to remove.
     * @return The removed session, or `null` if it was already gone.
     */
    public fun remove(id: SessionId): Session? = sessions.remove(id)

    /**
     * Look up a session by id.
     *
     * @param id The session id to look up.
     * @return The session, or `null` if no session with this id is registered.
     */
    public fun get(id: SessionId): Session? = sessions[id]

    /**
     * List every currently registered session for a player.
     *
     * This is a snapshot query: it reports what sessions exist right now and enforces nothing
     * about how many a player may hold.
     *
     * @param playerId The player id to list sessions for.
     * @return The sessions currently registered for this player, in no particular order.
     */
    public fun sessionsOf(playerId: PlayerId): List<Session> =
        sessions.values.filter { it.player.id == playerId }

    /** The number of currently registered sessions. */
    public val size: Int
        get() = sessions.size

    public companion object {
        /** Generate a fresh, effectively unique [SessionId]. */
        public fun newSessionId(): SessionId = SessionId(UUID.randomUUID().toString())
    }
}
