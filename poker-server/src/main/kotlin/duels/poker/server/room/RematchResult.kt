package duels.poker.server.room

import duels.poker.server.duel.Addressed

/**
 * The outcome of a player offering a rematch on a [Room].
 */
public sealed interface RematchResult {
    /** The offer was recorded; the room is still [RoomState.FINISHED], waiting on the other seat. */
    public data class Offered(val room: Room) : RematchResult

    /**
     * Both seats have offered; the room is now [RoomState.PLAYING] with a fresh match.
     *
     * @property room the room after the rematch starts, [RoomState.PLAYING] with the rematch's
     *   [MatchState][duels.poker.engine.duel.MatchState] running.
     * @property outbound the frames the rematch's opening hand produced, empty when this result
     *   came from the pure [Room.offerRematch], which deals no hand.
     */
    public data class Agreed(val room: Room, val outbound: List<Addressed> = emptyList()) : RematchResult

    /** The offer could not be accepted. */
    public data class Refused(val reason: RematchRefusal) : RematchResult
}

/**
 * The reasons a rematch offer can be refused.
 */
public enum class RematchRefusal {
    /** No room exists for the given code. Never returned by [Room] itself — only a registry
     * that fails to find a room can produce this. */
    UNKNOWN_ROOM,

    /** The room is not [RoomState.FINISHED]; a duel is still running or has not started. */
    NOT_FINISHED,

    /** The offering player is not seated in this room. */
    NOT_A_PLAYER,

    /** This player has already offered a rematch for this finished duel. */
    ALREADY_OFFERED,
}
