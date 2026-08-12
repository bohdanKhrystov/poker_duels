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
