package duels.poker.server.time

/**
 * An injectable abstraction for elapsed time, measured in milliseconds.
 *
 * This clock measures elapsed time from an arbitrary epoch using [System.nanoTime],
 * never wall-clock time. This matters: wall-clock time can step backwards when the host
 * corrects its time, which would stretch or collapse a timeout. An elapsed-time clock
 * is monotonically increasing and suitable for measuring durations.
 *
 * Never use this clock to stamp a database row with a date — use [System.currentTimeMillis]
 * for wall-clock time and [java.time.Instant] for UTC dates.
 */
public fun interface ServerClock {
    /**
     * The current elapsed time in milliseconds since an arbitrary epoch.
     */
    public fun nowMillis(): Long
}

/**
 * The system's elapsed-time clock, backed by [System.nanoTime].
 *
 * Because it uses [System.nanoTime], this clock is monotonically increasing
 * and unaffected by host clock corrections.
 */
public object SystemClock : ServerClock {
    override fun nowMillis(): Long = System.nanoTime() / 1_000_000
}
