package duels.poker.engine.duel

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
