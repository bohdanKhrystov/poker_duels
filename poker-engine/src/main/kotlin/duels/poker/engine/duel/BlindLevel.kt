package duels.poker.engine.duel

/**
 * A validated pair of small and big blinds, guaranteeing `0 < smallBlind < bigBlind`.
 *
 * Any `BlindLevel` satisfies the same bar `GameState` sets, so it can open a hand.
 *
 * @property smallBlind The small blind amount; must be positive and less than the big blind.
 * @property bigBlind The big blind amount; must be strictly greater than the small blind.
 */
public data class BlindLevel(val smallBlind: Int, val bigBlind: Int) {
    init {
        require(smallBlind > 0) { "smallBlind must be positive, was $smallBlind" }
        require(bigBlind > smallBlind) { "bigBlind must be larger than smallBlind, was bigBlind=$bigBlind, smallBlind=$smallBlind" }
    }

    /**
     * Returns a new `BlindLevel` with both blinds doubled.
     *
     * Doubling preserves the ordering invariant, so the result always satisfies the same validation.
     *
     * @return A new `BlindLevel` with both `smallBlind` and `bigBlind` multiplied by two.
     */
    public fun doubled(): BlindLevel = BlindLevel(smallBlind * 2, bigBlind * 2)
}
