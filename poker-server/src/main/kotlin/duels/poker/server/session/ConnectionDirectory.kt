package duels.poker.server.session

import java.util.concurrent.ConcurrentHashMap

/**
 * A concurrent directory of live [ConnectionWriter]s, keyed by the [PlayerId] behind them.
 *
 * [ConnectionDirectory] knows about players and writers and nothing else — no room, no seat, no
 * `ServerMessage`, no Ktor. Mapping a seat to a player, and finding the writer for the *other*
 * seat, is the room's job and happens elsewhere; this class only answers "which writer belongs to
 * this player right now".
 *
 * Every member is non-suspending, for the same reason [SessionRegistry]'s are: [forget] is called
 * from a connection's `finally` block, and a cleanup that can suspend is a cleanup that might not
 * run under cancellation.
 */
public class ConnectionDirectory {
    private val writers = ConcurrentHashMap<PlayerId, ConnectionWriter>()

    /**
     * Register [writer] as the writer for [player], replacing whatever writer was registered for
     * this player before.
     *
     * Overwriting is deliberate, not accidental: the most recent socket for a player is the one
     * that holds the seat, which is `ADR-0018` restated for writers.
     *
     * @param player The player this writer belongs to.
     * @param writer The writer to register.
     */
    public fun register(player: PlayerId, writer: ConnectionWriter) {
        writers[player] = writer
    }

    /**
     * Remove [writer] from the directory for [player], but only if it is still the writer
     * currently registered there — [ConcurrentHashMap.remove] with both key and value.
     *
     * This takes the writer as well as the player, and must be called with the writer whose
     * cleanup is running. Under `ADR-0018` a second socket for one device adopts the seat and the
     * older one then closes; if this removed by key alone, the older socket's `finally` would
     * delete the newer socket's writer and the surviving connection would silently stop receiving
     * frames. Requiring the writer to match means the adopted socket's cleanup removes nothing,
     * and the returned `false` makes that observable.
     *
     * @param player The player to remove the writer for.
     * @param writer The writer that must still be registered for the removal to happen.
     * @return `true` if [writer] was the registered writer and was removed, `false` otherwise —
     *   including when it had already been replaced or removed by an earlier call.
     */
    public fun forget(player: PlayerId, writer: ConnectionWriter): Boolean = writers.remove(player, writer)

    /**
     * Look up the writer currently registered for [player].
     *
     * @param player The player to look up.
     * @return The registered writer, or `null` if none is registered.
     */
    public fun writerFor(player: PlayerId): ConnectionWriter? = writers[player]

    /** The number of currently registered writers. */
    public val size: Int
        get() = writers.size
}
