package duels.poker.server.room

/**
 * The idle limits that determine when a room is reaped from the registry, and the grace window
 * for a disconnected player.
 *
 * - `waitingMillis`: duration a room with one player and an unused code survives. Long enough to
 *   send a link and have someone open it.
 * - `finishedMillis`: duration a `FINISHED` or `ABANDONED` room lingers. Long enough for the other
 *   player to offer a rematch.
 * - `disconnectGraceMillis`: duration a dropped connection holds its seat in a `PLAYING` room.
 *   Long enough for an app to come back from a tunnel, short enough that the opponent is not held
 *   hostage. This is ADR-0013's grace period, measured per seat rather than per room.
 *
 * Note: A `PLAYING` room is never reaped for idleness.
 */
public data class RoomTimeouts(
    val waitingMillis: Long,
    val finishedMillis: Long,
    // The field has a default so that RoomReapTest can build RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000)
    // and have no opinion on the grace window. A reaping test should not have to state one.
    val disconnectGraceMillis: Long = DEFAULT_DISCONNECT_GRACE_MILLIS,
) {
    init {
        require(waitingMillis > 0)
        require(finishedMillis > 0)
        require(disconnectGraceMillis > 0)
    }

    public companion object {
        /** Duration a room with one player and an unused code survives (10 minutes). */
        public const val DEFAULT_WAITING_MILLIS: Long = 10 * 60 * 1000L

        /** Duration a FINISHED or ABANDONED room lingers (5 minutes). */
        public const val DEFAULT_FINISHED_MILLIS: Long = 5 * 60 * 1000L

        /** Duration a dropped connection holds its seat in a PLAYING room (60 seconds). */
        public const val DEFAULT_DISCONNECT_GRACE_MILLIS: Long = 60 * 1000L

        /** Default timeout configuration using the declared constants. */
        public val DEFAULT: RoomTimeouts = RoomTimeouts(DEFAULT_WAITING_MILLIS, DEFAULT_FINISHED_MILLIS, DEFAULT_DISCONNECT_GRACE_MILLIS)
    }
}
