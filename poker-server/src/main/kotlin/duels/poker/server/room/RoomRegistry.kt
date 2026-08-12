package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.session.PlayerId
import duels.poker.server.time.ServerClock
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

/**
 * A concurrent registry of live rooms, keyed by [RoomCode].
 *
 * Each room lives behind its own [kotlinx.coroutines.sync.Mutex]: this is the single-writer rule
 * of `STORY-0206`, so that a later frame taking one room's mutex around a read-modify-write never
 * races another frame mutating the same room. `DEC-013` asks whether `STORY-0207` needs to
 * promote this to a channel-fed actor; a per-room mutex is what that decision is measured
 * against, and it changes no signature here.
 *
 * The registry knows no JSON, no `WebSocketSession`, no `ProtocolError` — nothing here reaches
 * outside the engine and the room model.
 *
 * @param codes The source of new, unique-among-live-rooms codes.
 * @param clock The clock used to stamp a newly opened room's activity.
 * @param timeouts The idle limits a later ticket reaps rooms against. Declared now so that a
 *   constructor growing a parameter later does not break every test that already built a
 *   registry.
 */
public class RoomRegistry(
    private val codes: RoomCodeSource,
    private val clock: ServerClock,
    private val timeouts: RoomTimeouts = RoomTimeouts.DEFAULT,
) {
    private val rooms = ConcurrentHashMap<RoomCode, Holder>()

    /**
     * Open a fresh, [RoomState.WAITING] room for [host], under a code no other live room holds.
     *
     * Mints a code from [codes] and stores the new room with `putIfAbsent`, which is what makes
     * the code unique: a `containsKey` check followed by a `put` would race another `create`
     * minting the same code. A non-null result from `putIfAbsent` means the code collided with a
     * room already live; the mint is retried up to [MAX_CODE_ATTEMPTS] times before giving up.
     *
     * @param host The player opening the room.
     * @param format The duel's configuration.
     * @return The newly created, stored room.
     * @throws IllegalStateException if [MAX_CODE_ATTEMPTS] consecutive codes all collided with a
     *   live room.
     */
    public fun create(host: PlayerId, format: DuelFormat = DuelFormat.DEFAULT): Room {
        repeat(MAX_CODE_ATTEMPTS) {
            val code = codes.newRoomCode()
            val room = Room.open(code, host, format, clock.nowMillis())
            if (rooms.putIfAbsent(code, Holder(room)) == null) {
                return room
            }
        }
        throw IllegalStateException("failed to mint a unique room code after $MAX_CODE_ATTEMPTS attempts")
    }

    /**
     * Look up a room by its code.
     *
     * A non-suspending snapshot read: it reports the room as it stood at the moment of the call,
     * taking no lock.
     *
     * @param code The room code to look up.
     * @return The room, or `null` if no room with this code is registered.
     */
    public fun get(code: RoomCode): Room? = rooms[code]?.room

    /** The number of currently registered rooms. */
    public val size: Int
        get() = rooms.size

    /**
     * One room plus the mutex that serialises every read-modify-write against it.
     *
     * `room` is `@Volatile` so a snapshot read in [get] always sees the latest published value
     * without itself taking [mutex].
     */
    private class Holder(@Volatile var room: Room) {
        val mutex = Mutex()
    }

    public companion object {
        /** The number of times [create] retries a colliding code before giving up. */
        public const val MAX_CODE_ATTEMPTS: Int = 10
    }
}
