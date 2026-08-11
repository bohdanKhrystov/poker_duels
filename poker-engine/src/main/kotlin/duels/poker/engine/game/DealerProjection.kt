package duels.poker.engine.game

/**
 * Folds one [DealerEvent] into the [GameState] it happened in, producing the state after it.
 *
 * `UncalledBetReturned` and `PotAwarded` carry a **delta** — chips moving out of the pot — unlike
 * a `BettingEvent`'s `to`, which is a street total (see the KDoc on those events). Both move
 * chips *between* the pot and a stack and nothing else, which is what keeps `chipsInPlay`
 * constant; taking more than the pot holds drives `pot` negative and `GameState`'s own `require`
 * throws.
 *
 * Callers should not reach for this directly; it exists so `StateProjection.apply`
 * (`TASK-010425`) can dispatch to it for the dealer branch of the full event hierarchy.
 */
public fun applyDealer(state: GameState, event: DealerEvent): GameState {
    val folded = when (event) {
        is BettingRoundEnded -> endRound(state)
        is StreetDealt -> state.copy(street = event.street, board = state.board.dealt(event.cards))
        is ShowdownReached -> state.copy(street = Street.SHOWDOWN, seatToAct = null)
        is HandRevealed -> state.withSeat(event.seat) { it.copy(holeCards = event.cards) }
        is UncalledBetReturned -> award(state, event.seat, event.amount)
        is PotAwarded -> award(state, event.seat, event.amount)
        is HandFinished -> state.copy(street = Street.COMPLETE, seatToAct = null)
    }
    return folded.copy(eventCount = event.sequence + 1)
}

/** Closes a betting round: every commitment sweeps into the pot and the bar resets. */
private fun endRound(state: GameState): GameState {
    val swept = state.copy(
        pot = state.potTotal,
        seats = state.seats.map { it.collected() },
    )
    return swept.copy(betToMatch = 0, minRaiseTo = state.bigBlind, seatToAct = null)
}

/** Moves [amount] out of the pot and onto [seat]'s stack. */
private fun award(state: GameState, seat: Int, amount: Int): GameState {
    return state.withSeat(seat) { it.award(amount) }.copy(pot = state.pot - amount)
}
