package duels.poker.engine.duel

/**
 * The two end conditions for a duel: [Freezeout] and [FixedHands].
 *
 * Both are candidates the `docs/duel-rules.md` Part 2 records under [DEC-001][https://github.com/bohdanKhrystov/poker-duels/blob/develop/docs/duel-rules.md#-open-decision---dec-001]:
 * - [Freezeout]: play until one seat holds every chip (the default).
 * - [FixedHands]: a set number of hands, winner is whoever holds more chips at the end.
 *
 * Neither is chosen here because `DEC-001` is open. This sealed interface is what makes the
 * answer cheap: a later decision becomes a configuration change, not an engine change.
 */
public sealed interface EndCondition {
    /** Play until one seat holds every chip. */
    public data object Freezeout : EndCondition

    /**
     * A set number of hands. Winner is whoever holds more chips at the end.
     *
     * @property hands the number of hands to play before comparing stacks; must be at least 1.
     */
    public data class FixedHands(val hands: Int) : EndCondition {
        init {
            require(hands >= 1) { "hands must be at least 1, was $hands" }
        }
    }
}
