package duels.poker.engine.game

/**
 * A seat and the amount of chips coming back from an uncalled bet.
 *
 * @property seat The seat index (0 or 1) that put in more than its opponent could cover.
 * @property amount The difference in hand commitments, always positive. An uncalled portion
 *   of zero is not a value — it is `null`.
 */
public data class UncalledPortion(val seat: Int, val amount: Int) {
    init {
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
        require(amount > 0) { "amount must be positive, was $amount" }
    }
}

/**
 * Computes the uncalled portion of a bet, if any.
 *
 * Reads `committedThisHand` (the gross total for the entire hand), never
 * `committedThisStreet` — the hand total is the only source of truth that survives
 * the per-street sweep. Once `BettingRoundEnded` clears a street's commitments,
 * `committedThisStreet` becomes zero and cannot recover the uncalled amount.
 *
 * Returns the seat with the strictly larger `committedThisHand` and the difference,
 * or `null` when both seats match.
 *
 * The seat that is owed can never be the seat that folded: folding is only legal
 * facing a bet, so a folder's hand total must always be strictly smaller than the
 * bettor's. Therefore, the seat with the larger commitment cannot have folded.
 *
 * @param state The current game state.
 * @return An `UncalledPortion` if one seat committed more than the other, or `null`
 *   if both committed equally.
 */
public fun uncalledPortion(state: GameState): UncalledPortion? {
    val seat0 = state.seat(0)
    val seat1 = state.seat(1)

    val diff0 = seat0.committedThisHand - seat1.committedThisHand

    return when {
        diff0 > 0 -> UncalledPortion(0, diff0)
        diff0 < 0 -> UncalledPortion(1, -diff0)
        else -> null
    }
}

/**
 * Closes a swept hand: the uncalled bet comes back, the pot goes to its winner, and the hand
 * is over.
 *
 * [state] must already be swept — `committedThisStreet` is zero on every seat — because [pot]
 * is only the whole of what is at stake once `BettingRoundEnded` has run.
 *
 * This ticket handles one winner only; a split pot is `TASK-010606`.
 *
 * @param state The swept state to settle.
 * @param winners The seat that won the hand. Must have exactly one entry.
 * @return The accepted result: `UncalledBetReturned` (if any), `PotAwarded` (if the remaining
 *   pot is positive), then `HandFinished`.
 */
public fun settleHand(state: GameState, winners: List<Int>): EngineResult {
    require(state.seats.all { it.committedThisStreet == 0 }) {
        "state must be swept: every seat's committedThisStreet must be zero, was ${state.seats}"
    }
    require(winners.size == 1) { "settleHand handles exactly one winner, got ${winners.size}" }

    val events = mutableListOf<GameEvent>()
    var current = state

    val uncalled = uncalledPortion(state)
    if (uncalled != null) {
        require(uncalled.amount <= current.pot) {
            "uncalled amount ${uncalled.amount} exceeds pot ${current.pot}"
        }
        val returned = UncalledBetReturned(current.eventCount, uncalled.seat, uncalled.amount)
        events += returned
        current = StateProjection.apply(current, returned)
    }

    if (current.pot > 0) {
        val awarded = PotAwarded(current.eventCount, winners.single(), current.pot)
        events += awarded
        current = StateProjection.apply(current, awarded)
    }

    val finished = HandFinished(current.eventCount)
    events += finished
    current = StateProjection.apply(current, finished)

    return EngineResult.accepted(current, events)
}
