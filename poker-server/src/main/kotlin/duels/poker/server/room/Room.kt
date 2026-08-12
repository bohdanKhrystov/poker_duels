package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.MatchState
import duels.poker.server.session.PlayerId

/**
 * The lifecycle a room moves through, host to finish, and the invariants each state carries.
 */
public enum class RoomState {
    /** Open, waiting for a second player: no guest, no match. */
    WAITING,

    /** A duel is running: both seats are filled and a match is in progress. */
    PLAYING,

    /** The duel has concluded: both seats are still filled, the match's final state is kept. */
    FINISHED,

    /** The room was abandoned; it carries no further invariant beyond seating. */
    ABANDONED,
}

/**
 * A heads-up room: exactly two seats, host in seat 0 and an optional guest in seat 1.
 *
 * The combinations that make no sense — a [RoomState.WAITING] room holding a match, a
 * [RoomState.PLAYING] room with no guest, a host sitting opposite themselves — cannot be
 * constructed: [init] rejects them.
 *
 * @property code the room's join code.
 * @property host the player who opened the room; always seated in seat 0.
 * @property guest the player who joined, if any; always seated in seat 1 when present.
 * @property state the room's current lifecycle state.
 * @property format the duel's configuration.
 * @property match the running or concluded duel's ledger, present only while [state] is
 *   [RoomState.PLAYING] or [RoomState.FINISHED].
 * @property openingButtonSeat the seat that held the button for the room's first duel.
 * @property rematchOffers the players who have offered a rematch; only meaningful once
 *   [state] is [RoomState.FINISHED].
 * @property lastActivityAt the timestamp, in milliseconds, of the room's most recent activity.
 */
public data class Room(
    val code: RoomCode,
    val host: PlayerId,
    val guest: PlayerId?,
    val state: RoomState,
    val format: DuelFormat,
    val match: MatchState?,
    val openingButtonSeat: Int,
    val rematchOffers: Set<PlayerId>,
    val lastActivityAt: Long,
) {
    init {
        require(guest == null || guest != host) { "the guest must not be the host" }
        require(state != RoomState.WAITING || (guest == null && match == null)) {
            "a waiting room must have no guest and no match"
        }
        require(
            (state != RoomState.PLAYING && state != RoomState.FINISHED) ||
                (guest != null && match != null),
        ) { "a $state room must have both a guest and a match" }
        require(state == RoomState.FINISHED || rematchOffers.isEmpty()) {
            "rematch offers may only be made in a finished room"
        }
        require(rematchOffers.all { it == host || it == guest }) {
            "rematch offers must come from a seated player"
        }
        require(openingButtonSeat in 0..1) { "openingButtonSeat must be 0 or 1, was $openingButtonSeat" }
        require(lastActivityAt >= 0) { "lastActivityAt must not be negative, was $lastActivityAt" }
    }

    /** The host, plus the guest if seated. */
    public val players: Set<PlayerId>
        get() = if (guest != null) setOf(host, guest) else setOf(host)

    /**
     * The heads-up seat number of [player]: `0` for the host, `1` for the guest, `null` for
     * anyone else. Matches [MatchState.buttonSeat]'s seat numbering.
     *
     * @param player the player to find.
     * @return the player's seat, or `null` if they are not seated in this room.
     */
    public fun seatOf(player: PlayerId): Int? = when (player) {
        host -> 0
        guest -> 1
        else -> null
    }

    /**
     * Seat [guest] into this room, starting the duel if there is a seat for them.
     *
     * Checked strictly in this order: a dead room refuses as [RoomRefusal.UNKNOWN_ROOM] before
     * anything else, so it is indistinguishable from a room that never existed; a player already
     * seated is refused as [RoomRefusal.ALREADY_SEATED] before fullness is even considered, so a
     * returning player never mistakes their own room for a full one; only then does a genuinely
     * full room refuse as [RoomRefusal.ROOM_FULL].
     *
     * Pure and total: never throws, never mutates. A [JoinResult.Refused] carries no room —
     * the caller already holds the unchanged one.
     *
     * @param guest the player attempting to join.
     * @param now the current time in milliseconds; `Room` reads no clock of its own.
     * @return [JoinResult.Seated] with the duel started, or [JoinResult.Refused] with the reason.
     */
    public fun join(guest: PlayerId, now: Long): JoinResult = when (state) {
        RoomState.FINISHED, RoomState.ABANDONED -> JoinResult.Refused(RoomRefusal.UNKNOWN_ROOM)
        RoomState.PLAYING -> if (seatOf(guest) != null) {
            JoinResult.Refused(RoomRefusal.ALREADY_SEATED)
        } else {
            JoinResult.Refused(RoomRefusal.ROOM_FULL)
        }
        RoomState.WAITING -> if (seatOf(guest) != null) {
            JoinResult.Refused(RoomRefusal.ALREADY_SEATED)
        } else {
            JoinResult.Seated(
                copy(
                    guest = guest,
                    state = RoomState.PLAYING,
                    match = MatchState.start(format, openingButtonSeat),
                    lastActivityAt = now,
                ),
            )
        }
    }

    /**
     * Finish this room when the duel concludes.
     *
     * Transitions a [RoomState.PLAYING] room to [RoomState.FINISHED], keeping the guest,
     * format, match, and opening button seat so that a rematch can be scheduled. Calling this
     * on any room that is not [RoomState.PLAYING] is a server bug and throws
     * [IllegalStateException].
     *
     * @param now the current time in milliseconds; updates [lastActivityAt].
     * @return this room in state [RoomState.FINISHED].
     * @throws IllegalStateException if [state] is not [RoomState.PLAYING].
     */
    public fun finish(now: Long): Room {
        check(state == RoomState.PLAYING) { "can only finish a PLAYING room, not $state" }
        return copy(
            state = RoomState.FINISHED,
            lastActivityAt = now,
        )
    }

    /**
     * Abandon this room when its players are gone or have given up.
     *
     * Transitions a room from [RoomState.WAITING], [RoomState.PLAYING], or [RoomState.FINISHED]
     * to [RoomState.ABANDONED], clearing any rematch offers. Calling [abandon] on an already
     * [RoomState.ABANDONED] room returns this room unchanged, including its [lastActivityAt] —
     * both players leaving must not reset the reaping clock on a dead room.
     *
     * @param now the current time in milliseconds; updates [lastActivityAt] unless already abandoned.
     * @return this room in state [RoomState.ABANDONED].
     */
    public fun abandon(now: Long): Room {
        return when (state) {
            RoomState.ABANDONED -> this
            RoomState.WAITING, RoomState.PLAYING, RoomState.FINISHED -> copy(
                state = RoomState.ABANDONED,
                rematchOffers = emptySet(),
                lastActivityAt = now,
            )
        }
    }

    /**
     * Touch this room to advance its idle clock.
     *
     * Updates [lastActivityAt] to the given timestamp without changing any other state.
     * This is called on every action inside a live room, making it possible to reap
     * abandoned rooms without reaping a live duel.
     *
     * @param now the current time in milliseconds; updates [lastActivityAt].
     * @return this room with [lastActivityAt] set to [now].
     */
    public fun touch(now: Long): Room = copy(lastActivityAt = now)

    public companion object {
        /**
         * Open a fresh room: a host waiting for a guest, no match yet, the host holding the
         * opening button.
         *
         * @param code the room's join code.
         * @param host the player opening the room.
         * @param format the duel's configuration.
         * @param now the current time in milliseconds; `Room` reads no clock of its own.
         * @return a new [RoomState.WAITING] room.
         */
        public fun open(code: RoomCode, host: PlayerId, format: DuelFormat, now: Long): Room =
            Room(
                code = code,
                host = host,
                guest = null,
                state = RoomState.WAITING,
                format = format,
                match = null,
                openingButtonSeat = 0,
                rematchOffers = emptySet(),
                lastActivityAt = now,
            )
    }
}
