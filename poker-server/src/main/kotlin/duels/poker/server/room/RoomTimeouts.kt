package duels.poker.server.room

/**
 * The idle limits that determine when a room is reaped from the registry, and the turn clock
 * durations.
 *
 * - `waitingMillis`: duration a room with one player and an unused code survives. Long enough to
 *   send a link and have someone open it.
 * - `finishedMillis`: duration a `FINISHED` or `ABANDONED` room lingers. Long enough for the other
 *   player to offer a rematch.
 * - `turnMillis`: the turn allowance — the time each player has to act before the default action
 *   is taken. See ADR-0108 §1 and ADR-0113 §3.
 * - `timebankMillis`: the total bank of additional turn time a player may accrue and spend, drawn
 *   down during play, restored between hands. See ADR-0108 §1 and ADR-0113 §3.
 *
 * Note: A `PLAYING` room is never reaped for idleness.
 */
public data class RoomTimeouts(
    val waitingMillis: Long,
    val finishedMillis: Long,
    // The fields have defaults so that a reaping test can build RoomTimeouts(waitingMillis = ..., finishedMillis = ...)
    // and have no opinion on the turn clock. A reaping test should not have to state either.
    val turnMillis: Long = DEFAULT_TURN_MILLIS,
    val timebankMillis: Long = DEFAULT_TIMEBANK_MILLIS,
) {
    init {
        require(waitingMillis > 0)
        require(finishedMillis > 0)
        require(turnMillis > 0)
        require(timebankMillis > 0)
    }

    public companion object {
        /** Duration a room with one player and an unused code survives (10 minutes). */
        public const val DEFAULT_WAITING_MILLIS: Long = 10 * 60 * 1000L

        /** Duration a FINISHED or ABANDONED room lingers (5 minutes). */
        public const val DEFAULT_FINISHED_MILLIS: Long = 5 * 60 * 1000L

        /** The turn allowance — the time each player has to act (30 seconds). */
        public const val DEFAULT_TURN_MILLIS: Long = 30 * 1000L

        /** The total bank of additional turn time a player may accrue and spend (3 minutes). */
        public const val DEFAULT_TIMEBANK_MILLIS: Long = 3 * 60 * 1000L

        /** Default timeout configuration using the declared constants. */
        public val DEFAULT: RoomTimeouts = RoomTimeouts(DEFAULT_WAITING_MILLIS, DEFAULT_FINISHED_MILLIS, DEFAULT_TURN_MILLIS, DEFAULT_TIMEBANK_MILLIS)
    }
}
