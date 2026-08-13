package duels.poker.server.protocol.http

/**
 * Reads the outcome of a duel from its stored coin delta.
 *
 * The function considers only the sign of the delta, not its magnitude. Per `ADR-0014`, a
 * winner gains and a loser loses under any award rule (constant, floating-point, or
 * opponent-weighted), so a function that reads the sign will survive a future change to the
 * award and one that compares against a literal will not.
 *
 * A zero delta is a draw, never a missing value or a default. Per `ADR-0015`, every
 * participant of every completed duel has exactly one `duel_result` row, and a drawn duel
 * writes two rows of `coin_delta = 0`. So a zero value is a real, expected input.
 *
 * @param coinDelta The change in coins from a duel. Positive for a win, negative for a loss,
 *   zero for a draw.
 * @return The outcome from the player's perspective.
 */
public fun outcomeOf(coinDelta: Int): DuelOutcomeLabel =
    when {
        coinDelta > 0 -> DuelOutcomeLabel.WON
        coinDelta < 0 -> DuelOutcomeLabel.LOST
        else -> DuelOutcomeLabel.DREW
    }
