package duels.poker.server.room

/**
 * The outcome of [Room.join]: either the guest is seated, or the join is refused with a reason.
 */
public sealed interface JoinResult {
    /**
     * The guest was seated and the duel started.
     *
     * @property room the room after seating, [RoomState.PLAYING] with the duel's [MatchState][
     *   duels.poker.engine.duel.MatchState] running.
     */
    public data class Seated(val room: Room) : JoinResult

    /**
     * The join was refused; the caller's existing, unchanged room is still valid.
     *
     * @property reason why the join was refused.
     */
    public data class Refused(val reason: RoomRefusal) : JoinResult
}

/**
 * Why a join was refused. A **room-domain** enum: it happens to line up with the `ProtocolError`
 * codes reserved elsewhere, but this module neither imports nor decides that mapping.
 */
public enum class RoomRefusal {
    /** The room does not exist — or exists but is dead, which must look identical. */
    UNKNOWN_ROOM,

    /** Both seats are already filled. */
    ROOM_FULL,

    /** The joiner is already seated in this room. */
    ALREADY_SEATED,
}
