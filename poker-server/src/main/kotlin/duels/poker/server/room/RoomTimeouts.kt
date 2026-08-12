package duels.poker.server.room

/**
 * The two idle limits that determine when a room is reaped from the registry.
 *
 * - `waitingMillis`: duration a room with one player and an unused code survives. Long enough to
 *   send a link and have someone open it.
 * - `finishedMillis`: duration a `FINISHED` or `ABANDONED` room lingers. Long enough for the other
 *   player to offer a rematch.
 *
 * Note: A `PLAYING` room is never reaped for idleness. A disconnected player in a live duel is
 * subject to ADR-0013's grace period, not these timeouts.
 */
public data class RoomTimeouts(
    val waitingMillis: Long,
    val finishedMillis: Long,
) {
    init {
        require(waitingMillis > 0)
        require(finishedMillis > 0)
    }

    public companion object {
        /** Duration a room with one player and an unused code survives (10 minutes). */
        public const val DEFAULT_WAITING_MILLIS: Long = 10 * 60 * 1000L

        /** Duration a FINISHED or ABANDONED room lingers (5 minutes). */
        public const val DEFAULT_FINISHED_MILLIS: Long = 5 * 60 * 1000L

        /** Default timeout configuration using the declared constants. */
        public val DEFAULT: RoomTimeouts = RoomTimeouts(DEFAULT_WAITING_MILLIS, DEFAULT_FINISHED_MILLIS)
    }
}
