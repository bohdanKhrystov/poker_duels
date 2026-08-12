package duels.poker.engine.duel

/**
 * What survives a duel between two hands: the format, how many hands have been played, both
 * stacks and the button.
 *
 * `MatchState` deliberately holds no live `GameState`. One hand in progress is a `GameState`;
 * `MatchState` is only the ledger a duel carries from one hand's end to the next hand's start,
 * per `STORY-0107`'s design notes.
 *
 * @property format the duel's configuration: starting stack, blind schedule and end condition.
 * @property handsPlayed the number of hands completed so far; must be at least 0.
 * @property stacks the two players' chip stacks, indexed by seat; must contain exactly two
 *   non-negative values.
 * @property buttonSeat the seat holding the button for the next hand; must be 0 or 1.
 */
public data class MatchState(
    val format: DuelFormat,
    val handsPlayed: Int,
    val stacks: List<Int>,
    val buttonSeat: Int,
) {
    init {
        require(stacks.size == 2) { "stacks must contain exactly two seats, was $stacks" }
        for (stack in stacks) {
            require(stack >= 0) { "stacks must not be negative, was $stack" }
        }
        require(buttonSeat in 0..1) { "buttonSeat must be 0 or 1, was $buttonSeat" }
        require(handsPlayed >= 0) { "handsPlayed must be at least 0, was $handsPlayed" }
    }

    /**
     * The 1-based number of the hand about to be dealt.
     */
    public val nextHandNumber: Int
        get() = handsPlayed + 1

    /**
     * The blinds in play for [nextHandNumber].
     *
     * This is derived, never stored: a level recomputed from [nextHandNumber] cannot change in
     * the middle of a hand, which makes that rule true by construction instead of by discipline.
     */
    public val blinds: BlindLevel
        get() = format.blinds.blindsFor(nextHandNumber)

    /**
     * Both stacks together.
     */
    public val chips: Int
        get() = stacks.sum()

    public companion object {
        /**
         * The state of a fresh duel: zero hands played and both stacks at [DuelFormat.startingStack].
         *
         * @param format the duel's configuration.
         * @param buttonSeat the seat holding the button for the first hand; defaults to 0.
         */
        public fun start(format: DuelFormat, buttonSeat: Int = 0): MatchState =
            MatchState(
                format = format,
                handsPlayed = 0,
                stacks = listOf(format.startingStack, format.startingStack),
                buttonSeat = buttonSeat,
            )
    }
}
