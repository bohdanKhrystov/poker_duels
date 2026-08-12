package duels.poker.ai

/**
 * The aggregate result of a batch of duels run by [runSimulation].
 *
 * @property duels the number of duels played.
 * @property hands the total number of hands played across every duel.
 * @property shortestDuel the fewest hands any one duel took to finish.
 * @property longestDuel the most hands any one duel took to finish.
 * @property winsBySeat the number of duels won by each seat, indexed 0 and 1.
 * @property draws the number of duels that ended level, with no winner.
 */
public data class SimulationReport(
    val duels: Int,
    val hands: Int,
    val shortestDuel: Int,
    val longestDuel: Int,
    val winsBySeat: List<Int>,
    val draws: Int,
) {
    /** The mean number of hands per duel. */
    public val averageHandsPerDuel: Double get() = hands.toDouble() / duels
}
