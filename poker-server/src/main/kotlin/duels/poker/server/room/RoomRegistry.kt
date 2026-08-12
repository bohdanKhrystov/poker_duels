package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.session.PlayerId
import duels.poker.server.time.ServerClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    /**
     * Seat [player] into the room at [code], applying [Room.join] under that room's mutex.
     *
     * The lookup, the call to [Room.join] and the write-back of a [JoinResult.Seated] room all
     * happen inside the one critical section: reading [Holder.room] outside the lock would let two
     * simultaneous joins both decide against the same stale room and seat two guests in one seat.
     * The holder is re-checked against the map once the lock is held, so a room reaped between the
     * lookup and the lock refuses with [RoomRefusal.UNKNOWN_ROOM] instead of seating a player into
     * a room nobody can find.
     *
     * @param code The room to join.
     * @param player The player attempting to join.
     * @return The result of [Room.join]; a [JoinResult.Refused] room is left exactly as it was.
     */
    public suspend fun join(code: RoomCode, player: PlayerId): JoinResult {
        return mutate(
            code,
            absent = { JoinResult.Refused(RoomRefusal.UNKNOWN_ROOM) },
            block = { room ->
                when (val result = room.join(player, clock.nowMillis())) {
                    is JoinResult.Seated -> Pair(result.room, result)
                    is JoinResult.Refused -> Pair(null, result)
                }
            },
        )
    }

    /**
     * Finish a [RoomState.PLAYING] room when the duel concludes.
     *
     * Applies [Room.finish] under that room's mutex, storing the resulting [RoomState.FINISHED]
     * room. The finish operation and the write-back happen inside the one critical section so that
     * a concurrent finish request on the same room always finds the authoritative state.
     *
     * @param code The room to finish.
     * @return The room after the transition, or `null` for a code with no live room.
     * @throws IllegalStateException if the room is not [RoomState.PLAYING].
     */
    public suspend fun finish(code: RoomCode): Room? {
        return mutate(
            code,
            absent = { null },
            block = { room ->
                val finished = room.finish(clock.nowMillis())
                Pair(finished, finished)
            },
        )
    }

    /**
     * Abandon this room when its players are gone or have given up.
     *
     * Applies [Room.abandon] under that room's mutex, storing the resulting [RoomState.ABANDONED]
     * room. The abandon operation and the write-back happen inside the one critical section so that
     * a concurrent abandon request on the same room always finds the authoritative state.
     *
     * @param code The room to abandon.
     * @return The room after the transition, or `null` for a code with no live room.
     */
    public suspend fun abandon(code: RoomCode): Room? {
        return mutate(
            code,
            absent = { null },
            block = { room ->
                val abandoned = room.abandon(clock.nowMillis())
                Pair(abandoned, abandoned)
            },
        )
    }

    /**
     * Offer a rematch on this finished room.
     *
     * Applies [Room.offerRematch] under that room's mutex. The offer, decision and any write-back
     * happen inside the one critical section so that two concurrent offers on the same room are
     * serialized and both see a consistent view of what has been offered.
     *
     * @param code The room to offer a rematch on.
     * @param player The player offering the rematch.
     * @return The result of [Room.offerRematch]; a [RematchResult.Offered] or [RematchResult.Agreed]
     *   room is written back, but a [RematchResult.Refused] room is left exactly as it was.
     */
    public suspend fun offerRematch(code: RoomCode, player: PlayerId): RematchResult {
        return mutate(
            code,
            absent = { RematchResult.Refused(RematchRefusal.UNKNOWN_ROOM) },
            block = { room ->
                when (val result = room.offerRematch(player, clock.nowMillis())) {
                    is RematchResult.Offered -> Pair(result.room, result)
                    is RematchResult.Agreed -> Pair(result.room, result)
                    is RematchResult.Refused -> Pair(null, result)
                }
            },
        )
    }

    /**
     * Look up, lock, mutate and store a room under a single critical section.
     *
     * This enforces the single-writer rule: a read-modify-write against a room always happens
     * entirely inside the room's mutex, never taking a stale copy outside. The holder is
     * re-checked by reference after taking the lock, so a room reaped between the lookup and
     * the lock returns the absent result instead of mutating a room nobody can find.
     *
     * @param code The room code to look up.
     * @param absent A block to call and return if no live room holds this code, or the holder
     *   identity changed while waiting for the lock.
     * @param block A block to call with the current room; it returns a [Pair] of the new room
     *   (or null to leave it untouched) and a value to return.
     * @return The value from [block], or the result of [absent] if the room was not found.
     */
    private suspend fun <T> mutate(
        code: RoomCode,
        absent: () -> T,
        block: (Room) -> Pair<Room?, T>,
    ): T {
        val holder = rooms[code] ?: return absent()
        return holder.mutex.withLock {
            if (rooms[code] !== holder) {
                return@withLock absent()
            }
            val (updatedRoom, result) = block(holder.room)
            if (updatedRoom != null) {
                holder.room = updatedRoom
            }
            result
        }
    }

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
