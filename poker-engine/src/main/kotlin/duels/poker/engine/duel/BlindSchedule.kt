package duels.poker.engine.duel

import kotlinx.serialization.Serializable

/**
 * The ladder of [BlindLevel]s a duel climbs, and the mechanism `docs/duel-rules.md` credits with
 * guaranteeing a duel terminates: blinds only ever escalate, so a heads-up match cannot run
 * indefinitely.
 *
 * @property levels The defined levels, strictly ascending by `bigBlind`; must not be empty.
 * @property handsPerLevel The number of hands each level holds before the schedule advances;
 *   must be at least 1.
 */
@Serializable
public data class BlindSchedule(val levels: List<BlindLevel>, val handsPerLevel: Int) {
    init {
        require(levels.isNotEmpty()) { "levels must not be empty, was $levels" }
        require(handsPerLevel >= 1) { "handsPerLevel must be at least 1, was $handsPerLevel" }
        for (i in 1 until levels.size) {
            require(levels[i].bigBlind > levels[i - 1].bigBlind) {
                "levels must be a strictly ascending ladder, but level $i (${levels[i]}) does not " +
                    "have a bigBlind greater than level ${i - 1} (${levels[i - 1]})"
            }
        }
    }

    /**
     * Returns the 0-based block of hands `handNumber` falls in.
     *
     * Uncapped: this is the raw block number, not an index into [levels] — a hand far past the
     * last defined level still returns a block, it just no longer names a level directly.
     *
     * @param handNumber The 1-based hand number; must be at least 1.
     */
    public fun levelIndexFor(handNumber: Int): Int {
        require(handNumber >= 1) { "handNumber must be at least 1, was $handNumber" }
        return (handNumber - 1) / handsPerLevel
    }

    /**
     * Returns the blinds in play for `handNumber`.
     *
     * While the block is inside [levels], returns that level directly. Past the last defined
     * level, the last level doubles once per further block, so block `levels.size - 1 + k` plays
     * the last level [BlindLevel.doubled] `k` times.
     *
     * @param handNumber The 1-based hand number; must be at least 1.
     */
    public fun blindsFor(handNumber: Int): BlindLevel {
        val index = levelIndexFor(handNumber)
        if (index < levels.size) return levels[index]

        // Twenty doublings of any real level is orders of magnitude past any stack a duel could
        // hold, so a duel that reaches it is a bug in the caller, not an arithmetic problem for
        // this function to solve. Saturating here keeps blindsFor from ever overflowing Int.
        val doublings = minOf(index - (levels.size - 1), MAX_DOUBLINGS)
        var level = levels.last()
        repeat(doublings) { level = level.doubled() }
        return level
    }

    private companion object {
        const val MAX_DOUBLINGS = 20
    }
}
