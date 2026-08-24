package duels.poker.server.auth

import duels.poker.server.time.ServerClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The shape of a rolling-window rate limiter: at most [maxAttempts] admitted attempts in any
 * [windowMillis]-long window.
 */
public data class AttemptLimits(val maxAttempts: Int, val windowMillis: Long)

/**
 * The first rate limiter in this repository: a rolling window over [clock], keyed by an
 * arbitrary caller-chosen string, that records an attempt against a key and answers whether it
 * was within [limits].
 *
 * State is an in-memory map of key to the timestamps of its recorded attempts. A restart forgets
 * it, and that is accepted for the same reason rooms are not durable (`ADR-0055` §2). A single
 * [Mutex] is held across the whole read-prune-record in both [admit] and [refund]: a
 * check-then-record with any gap between the two admits *N* concurrent callers against a budget
 * of one, and concurrency *is* the attack, so the critical section is the feature and not an
 * optimisation.
 */
public class AttemptBudget(private val limits: AttemptLimits, private val clock: ServerClock) {

    private val mutex = Mutex()
    private val attemptsByKey = mutableMapOf<String, MutableList<Long>>()

    /**
     * Records an attempt against [key] and answers whether it is within budget.
     *
     * The attempt is recorded whether or not it is admitted — **an over-budget attempt still
     * counts** against [key]'s window, so hammering a key extends the window rather than
     * resetting it (`ADR-0055` §1). This is deliberate: do not "simplify" it away by returning
     * early without recording once the budget is exhausted.
     *
     * Pruning is scoped to the key this call touches; there is no background sweep here.
     */
    public suspend fun admit(key: String): Boolean = mutex.withLock {
        val now = clock.nowMillis()
        val timestamps = liveTimestamps(key, now)
        val withinBudget = timestamps.size < limits.maxAttempts
        timestamps.add(now)
        withinBudget
    }

    /**
     * Returns one recorded attempt for [key] to the budget, if it holds any.
     *
     * This is a refund of one reservation, **not a reset of [key]**: it removes only the most
     * recently recorded attempt, under the same lock [admit] holds, and does nothing when [key]
     * holds none. `ADR-0074` §2 is why it exists: sign-in reserves before it hashes and refunds
     * when the password turns out to be right, so that only wrong guesses accumulate over the
     * window while the reservation still bounds how many verifications one address can have in
     * flight. Sign-up never calls it.
     */
    public suspend fun refund(key: String) {
        mutex.withLock {
            val timestamps = attemptsByKey[key]
            if (timestamps != null && timestamps.isNotEmpty()) {
                timestamps.removeAt(timestamps.lastIndex)
            }
        }
    }

    /**
     * The still-live timestamps recorded for [key] as of [now], with everything older than
     * [AttemptLimits.windowMillis] removed. Must be called with [mutex] held.
     */
    private fun liveTimestamps(key: String, now: Long): MutableList<Long> {
        val timestamps = attemptsByKey.getOrPut(key) { mutableListOf() }
        timestamps.removeAll { recordedAt -> now - recordedAt >= limits.windowMillis }
        return timestamps
    }
}
