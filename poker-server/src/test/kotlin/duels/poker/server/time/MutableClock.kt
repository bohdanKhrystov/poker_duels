package duels.poker.server.time

/**
 * A test fixture for [ServerClock] that never advances time automatically.
 *
 * Use this in tests instead of [SystemClock] to avoid flaky timing-dependent tests.
 * Advance time explicitly with [advance] or [set], so the test is deterministic
 * and does not depend on wall-clock delays.
 */
class MutableClock(private var current: Long = 0L) : ServerClock {
    override fun nowMillis(): Long = current

    /**
     * Advance time forward by [millis] milliseconds.
     *
     * @throws IllegalArgumentException if [millis] is negative
     */
    fun advance(millis: Long) {
        require(millis >= 0) { "cannot advance time backwards: $millis" }
        current += millis
    }

    /**
     * Set the time to exactly [millis] milliseconds.
     */
    fun set(millis: Long) {
        current = millis
    }
}
