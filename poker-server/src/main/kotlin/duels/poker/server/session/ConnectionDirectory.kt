package duels.poker.server.session

import duels.poker.server.room.RoomCode
import java.util.concurrent.ConcurrentHashMap

/**
 * A concurrent directory of live [ConnectionWriter]s, keyed by the [PlayerId] behind them.
 *
 * [ConnectionDirectory] knows about players, writers and the one room each writer's connection is
 * currently in — no seat, no `ServerMessage`, no Ktor. Mapping a seat to a player, and finding the
 * writer for the *other* seat, is the room's job and happens elsewhere; this class only answers
 * "which writer belongs to this player, in this room, right now". A [RoomCode] is in scope because
 * [writerFor] cannot answer that question without one — `ADR-0104` §2.
 *
 * Every member is non-suspending, for the same reason [SessionRegistry]'s are: [forget] is called
 * from a connection's `finally` block, and a cleanup that can suspend is a cleanup that might not
 * run under cancellation.
 */
public class ConnectionDirectory {
    private val writers = ConcurrentHashMap<PlayerId, Registration>()

    /**
     * Register [writer] as the writer for [player], replacing whatever writer was registered for
     * this player before.
     *
     * Overwriting is deliberate, not accidental: the most recent socket for a player is the one
     * that holds the seat, which is `ADR-0018` restated for writers.
     *
     * [membership] is stored by reference, never copied: it is the very [RoomMembership] the
     * socket loop that owns [writer] writes to, so [writerFor] always reads the room [player]'s
     * connection is in *at the instant of the call*, never the room it was in when this method
     * ran (`ADR-0104` §2).
     *
     * @param player The player this writer belongs to.
     * @param writer The writer to register.
     * @param membership The connection's own record of which room, if any, it has entered.
     */
    public fun register(player: PlayerId, writer: ConnectionWriter, membership: RoomMembership) {
        writers[player] = Registration(writer, membership)
    }

    /**
     * Remove [writer] from the directory for [player], but only if it is still the writer
     * currently registered there.
     *
     * This takes the writer as well as the player, and must be called with the writer whose
     * cleanup is running. Under `ADR-0018` a second socket for one device adopts the seat and the
     * older one then closes; if this removed by key alone, the older socket's `finally` would
     * delete the newer socket's writer and the surviving connection would silently stop receiving
     * frames. Requiring the writer to match means the adopted socket's cleanup removes nothing,
     * and the returned `false` makes that observable.
     *
     * The stored value is a [Registration], not a bare writer, so the match and the removal are
     * kept as one atomic step against the same instance: the entry is read once, and *that*
     * instance — never one rebuilt from parts — is handed to [ConcurrentHashMap.remove]'s
     * key-and-value overload. Reading the entry, checking its writer, and then removing by key
     * alone would leave a window between the check and the removal for a concurrent [register] to
     * land in — reopening, one layer up, the exact adoption race the paragraph above exists to
     * close.
     *
     * @param player The player to remove the writer for.
     * @param writer The writer that must still be registered for the removal to happen.
     * @return `true` if [writer] was the registered writer and was removed, `false` otherwise —
     *   including when it had already been replaced or removed by an earlier call.
     */
    public fun forget(player: PlayerId, writer: ConnectionWriter): Boolean {
        val current = writers[player] ?: return false
        return current.writer == writer && writers.remove(player, current)
    }

    /**
     * Look up the writer currently registered for [player], but only if that connection is, at
     * this instant, in [room].
     *
     * The [RoomMembership] stored alongside the writer is read fresh on every call, never cached
     * here or by a caller, so a connection that has since moved to another room — or left every
     * room — answers `null` even though a writer is still registered for [player]. A connection
     * that has entered no room answers `null` for every room. `ADR-0104` §1.
     *
     * @param player The player to look up.
     * @param room The room the frame being delivered is about.
     * @return The registered writer if [player]'s connection is currently in [room], `null`
     *   otherwise — including when no writer is registered for [player] at all.
     */
    public fun writerFor(player: PlayerId, room: RoomCode): ConnectionWriter? =
        writers[player]?.takeIf { it.membership.code == room }?.writer

    /** The number of currently registered writers. */
    public val size: Int
        get() = writers.size
}

/**
 * What [ConnectionDirectory] stores per player: the [writer] a connection is using, and the
 * [membership] that says which room, if any, that same connection is currently in.
 *
 * Held as one pair, rather than two parallel maps, so a [ConnectionDirectory.register] and every
 * [ConnectionDirectory.writerFor] lookup it backs read one consistent value — `ADR-0104` §2.
 */
private data class Registration(val writer: ConnectionWriter, val membership: RoomMembership)
