package duels.poker.engine.game

/**
 * The events that follow [lastAction]'s own event. [state] is the state after that event has
 * been applied, so `seatToAct` is already null.
 *
 * Closes on a fold with [BettingRoundEnded] (`TASK-010516`); advancing the street is
 * `TASK-010517`, and running out the board is `TASK-010518`.
 */
public fun continueHand(state: GameState, lastAction: PlayerAction): EngineResult {
    // A fold ends the hand immediately: sweep commitments to pot, mark seatToAct null.
    // Awarding the pot is STORY-0106. The uncalled part of the last bet stays recoverable because
    // Seat.committedThisHand is gross and never decreases — the difference between seats'
    // committedThisHand is the amount to return.
    if (state.seat(0).hasFolded || state.seat(1).hasFolded) {
        val event = BettingRoundEnded(state.eventCount, state.street)
        return EngineResult.accepted(StateProjection.apply(state, event), listOf(event))
    }

    if (!roundContinues(state, lastAction)) {
        return closeRound(state)
    }
    val event = ActionOn(state.eventCount, otherSeat(lastAction.seat))
    return EngineResult.accepted(StateProjection.apply(state, event), listOf(event))
}

/**
 * The round on [state.street] is complete and nobody folded: sweep the commitments, then either
 * deal the next street and put its first-to-act seat on turn, or — closing the river — reach
 * showdown.
 *
 * Dealing draws from [GameState.deck] directly, because [StateProjection.apply] deliberately
 * never advances it (see its KDoc): the deck dealt from here has to be carried forward by hand
 * onto the state that keeps accumulating events, or the next hand redeals a card already out.
 */
private fun closeRound(state: GameState): EngineResult {
    val endedEvent = BettingRoundEnded(state.eventCount, state.street)
    val afterEnd = StateProjection.apply(state, endedEvent)

    if (state.street == Street.RIVER) {
        val showdownEvent = ShowdownReached(afterEnd.eventCount)
        val afterShowdown = StateProjection.apply(afterEnd, showdownEvent)
        return EngineResult.accepted(afterShowdown, listOf(endedEvent, showdownEvent))
    }

    val next = requireNotNull(state.street.next) { "No street follows ${state.street}" }
    val deal = afterEnd.deck.deal(next.boardCards - afterEnd.board.size)
    val dealtEvent = StreetDealt(afterEnd.eventCount, next, deal.cards)
    val afterDeal = StateProjection.apply(afterEnd, dealtEvent).copy(deck = deal.deck)

    val actionEvent = ActionOn(afterDeal.eventCount, firstToActOn(next, state.buttonSeat))
    val afterAction = StateProjection.apply(afterDeal, actionEvent)

    return EngineResult.accepted(afterAction, listOf(endedEvent, dealtEvent, actionEvent))
}

/**
 * Whether the opponent of [lastAction]'s seat still has a decision on this street.
 *
 * [state] is the state *after* [lastAction]'s event has been applied.
 *
 * ### Why this needs no "has acted this street" field
 *
 * `GameState` carries no flag recording who has already acted this street, and this function
 * does not need one. Heads-up alternates strictly — after any action the turn passes to the
 * other seat — so a seat that owes nothing (`committedThisStreet == betToMatch`) can only still
 * have a decision in one of two positions, and both are derivable from `(state, lastAction)`
 * alone:
 *
 * 1. **The street's first check.** The seat that acts first on the street (see
 *    [firstToActOn]) checks with nothing owed; the *other* seat has not acted yet this street
 *    at all, so it still gets a turn even though it owes zero. This is knowable without a flag
 *    because [firstToActOn] is a pure function of [GameState.street] and [GameState.buttonSeat]
 *    — whoever just checked is either the street's first-to-act (round continues) or its
 *    second-to-act (round ends), and there is no third case in a two-seat street.
 * 2. **The big blind's option after a limp.** Preflop, if the small blind only calls the big
 *    blind (`betToMatch == bigBlind`), the big blind owes nothing but has never had a chance to
 *    raise. This is knowable without a flag because it is preflop's *only* call that can leave
 *    `betToMatch` sitting at exactly one big blind with the big blind seat as the one still
 *    silent — any later call is either a call of a raise (`betToMatch > bigBlind`, caught by the
 *    "owes chips" case below before the caller ever gets here) or the big blind itself acting
 *    (in which case its option is spent).
 *
 * Every other seat-owes-nothing situation is the *second* action in its street or betting
 * exchange, and heads-up has no more than two seats to alternate between, so the round is over.
 *
 * @param state the state after [lastAction]'s event has been applied
 * @param lastAction the action just taken
 * @return `true` if the other seat still has a decision to make on this street
 */
public fun roundContinues(state: GameState, lastAction: PlayerAction): Boolean {
    val actor = state.seat(lastAction.seat)
    val other = state.seat(otherSeat(lastAction.seat))
    return when {
        actor.hasFolded || other.hasFolded -> false
        other.isAllIn || other.stack == 0 -> false
        other.committedThisStreet < state.betToMatch -> true
        actor.isAllIn -> false
        lastAction.type == ActionType.CHECK ->
            lastAction.seat == firstToActOn(state.street, state.buttonSeat)
        lastAction.type == ActionType.CALL ->
            state.street == Street.PREFLOP &&
                state.betToMatch == state.bigBlind &&
                otherSeat(lastAction.seat) == bigBlindSeat(state.buttonSeat)
        else -> false
    }
}
