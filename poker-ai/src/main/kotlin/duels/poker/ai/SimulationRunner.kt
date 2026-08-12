package duels.poker.ai

import duels.poker.engine.duel.DuelFormat

/**
 * Plays [duels] duels between [bots], seat-indexed, and returns their aggregate [SimulationReport].
 *
 * Duel `n` (0-based) is played with seed `seed + n`, so the report names the exact seeds it
 * covers: `[seed, seed + duels)`. Runs single-threaded, one duel at a time, in seed order:
 * correctness first, and this batch does not need concurrency.
 *
 * A [SimulationFailure] from any duel propagates unchanged, stopping the run at the first
 * violation rather than counting how many there were — the exception already carries the seed
 * and actions that reproduce it.
 *
 * @param duels the number of duels to play; must be at least 1.
 * @param bots the two bots, seat-indexed; must hold exactly two entries.
 * @param format the duel's configuration; defaults to [DuelFormat.DEFAULT].
 * @param seed the seed of duel 0; every later duel's seed is `seed + n`.
 * @return the aggregate [SimulationReport] over every duel played.
 * @throws SimulationFailure if any duel breaks an invariant or fails to terminate.
 */
public fun runSimulation(
    duels: Int,
    bots: List<Bot>,
    format: DuelFormat = DuelFormat.DEFAULT,
    seed: Long = 1L,
): SimulationReport {
    require(duels >= 1) { "duels must be at least 1, got $duels" }
    require(bots.size == 2) { "bots must hold exactly two entries, seat-indexed, had ${bots.size}" }

    var hands = 0
    var shortestDuel = Int.MAX_VALUE
    var longestDuel = 0
    val winsBySeat = mutableListOf(0, 0)
    var draws = 0

    for (n in 0 until duels) {
        val outcome = simulateDuel(seed + n, bots, format).outcome

        hands += outcome.handsPlayed
        shortestDuel = minOf(shortestDuel, outcome.handsPlayed)
        longestDuel = maxOf(longestDuel, outcome.handsPlayed)
        when (val winner = outcome.winner) {
            null -> draws++
            else -> winsBySeat[winner] = winsBySeat[winner] + 1
        }
    }

    return SimulationReport(
        duels = duels,
        hands = hands,
        shortestDuel = shortestDuel,
        longestDuel = longestDuel,
        winsBySeat = winsBySeat.toList(),
        draws = draws,
    )
}
