package duels.poker.engine.duel

import kotlinx.serialization.Serializable

/**
 * The complete configuration of a duel: starting stack, blind schedule, and end condition.
 *
 * The default format ([DEFAULT]) is what the first playable build ships with. The exact choice
 * of format is an open decision ([DEC-001](https://github.com/bohdanKhrystov/poker-duels/blob/develop/docs/duel-rules.md#-open-decision---dec-001)).
 * Changing the default values requires a change to this class only, not to any engine logic.
 *
 * @property startingStack the number of chips each player starts with; must be at least as much
 *   as the first hand's big blind, so the duel can cover its own opening.
 * @property blinds the schedule of blind levels the duel climbs.
 * @property endCondition the rule by which the duel terminates: [EndCondition.Freezeout] or
 *   [EndCondition.FixedHands].
 */
@Serializable
public data class DuelFormat(
    val startingStack: Int,
    val blinds: BlindSchedule,
    val endCondition: EndCondition,
) {
    init {
        val firstBigBlind = blinds.blindsFor(1).bigBlind
        require(startingStack >= firstBigBlind) {
            "starting stack ($startingStack) must be at least the first big blind ($firstBigBlind)"
        }
    }

    public companion object {
        /**
         * The default duel format: freezeout with a 10,000-chip starting stack and a blind
         * ladder that doubles every 10 hands after level 5.
         *
         * See `docs/duel-rules.md` Part 2 for the full specification.
         */
        public val DEFAULT: DuelFormat = DuelFormat(
            startingStack = 10_000,
            blinds = BlindSchedule(
                levels = listOf(
                    BlindLevel(50, 100),
                    BlindLevel(75, 150),
                    BlindLevel(100, 200),
                    BlindLevel(150, 300),
                    BlindLevel(200, 400),
                ),
                handsPerLevel = 10,
            ),
            endCondition = EndCondition.Freezeout,
        )
    }
}
