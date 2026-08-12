package duels.poker.engine.duel

import kotlinx.serialization.Serializable

/**
 * The result of a finished duel.
 *
 * @param winner the seat of the winner (0 or 1), or null if the duel was a draw. The engine names
 * a seat, never a player or account — player identity lives in EPIC-05.
 * @param handsPlayed the number of hands played
 * @param finalStacks the chip stack of each player at the end of the duel
 *
 * A fixed-length duel can finish level. In that case, [winner] is null and [isDraw] is true.
 * Rather than arbitrarily naming a seat as the winner, the engine reports a draw to preserve
 * the integrity of ranked play.
 */
@Serializable
public data class DuelOutcome(
    val winner: Int?,
    val handsPlayed: Int,
    val finalStacks: List<Int>,
) {
    init {
        require(winner == null || winner in 0..1) {
            "winner must be null or 0..1, got $winner"
        }
        require(finalStacks.size == 2) {
            "finalStacks must have exactly 2 elements, got ${finalStacks.size}"
        }
        require(finalStacks.all { it >= 0 }) {
            "finalStacks must not contain negative values, got $finalStacks"
        }
        require(handsPlayed >= 0) {
            "handsPlayed must be >= 0, got $handsPlayed"
        }
    }

    /**
     * True if the duel ended in a draw (no winner), false otherwise.
     */
    public val isDraw: Boolean
        get() = winner == null
}

/**
 * Decides whether [match] has ended and, if so, who won.
 *
 * The broke-seat check runs *before* the [EndCondition] check, for every condition: a seat with
 * zero chips cannot be dealt another hand — `startNextHand` already refuses it — so a match in
 * that state is over regardless of what its end condition says. Checking the end condition first
 * would let a fixed-length duel with a broke seat report `null` (still running) right up to its
 * final hand, promising a hand that can never be dealt.
 *
 * @param match the match to evaluate.
 * @return `null` if the duel is still running, otherwise the [DuelOutcome].
 */
public fun outcomeOf(match: MatchState): DuelOutcome? {
    val brokeSeat = match.stacks.indexOfFirst { it == 0 }
    if (brokeSeat != -1) {
        val winner = 1 - brokeSeat
        return DuelOutcome(winner, match.handsPlayed, match.stacks)
    }

    val endCondition = match.format.endCondition
    return when (endCondition) {
        is EndCondition.Freezeout -> null
        is EndCondition.FixedHands -> {
            if (match.handsPlayed < endCondition.hands) {
                null
            } else {
                val (first, second) = match.stacks
                val winner = when {
                    first > second -> 0
                    second > first -> 1
                    else -> null
                }
                DuelOutcome(winner, match.handsPlayed, match.stacks)
            }
        }
    }
}
