package duels.poker.engine.game

/**
 * The events that follow [lastAction]'s own event. [state] is the state after that event has
 * been applied, so `seatToAct` is already null.
 *
 * Closes on a fold with [BettingRoundEnded] (`TASK-010516`); advancing the street is
 * `TASK-010517`, and running the board out when nobody can bet again is `TASK-010518`.
 */
public fun continueHand(state: GameState, lastAction: PlayerAction): EngineResult {
    // A fold ends the hand immediately: sweep commitments to pot, then settle to the seat that
    // did not fold. The uncalled part of the last bet stays recoverable because
    // Seat.committedThisHand is gross and never decreases — the difference between seats'
    // committedThisHand is the amount to return.
    val foldedSeat = when {
        state.seat(0).hasFolded -> 0
        state.seat(1).hasFolded -> 1
        else -> null
    }
    if (foldedSeat != null) {
        val endedEvent = BettingRoundEnded(state.eventCount, state.street)
        val afterEnd = StateProjection.apply(state, endedEvent)
        val settled = settleHand(afterEnd, listOf(otherSeat(foldedSeat)))
        return EngineResult.accepted(settled.newState, listOf(endedEvent) + settled.events)
    }

    if (!roundContinues(state, lastAction)) {
        return endBettingRound(state)
    }
    val event = ActionOn(state.eventCount, otherSeat(lastAction.seat))
    return EngineResult.accepted(StateProjection.apply(state, event), listOf(event))
}

/**
 * Closes the current betting round and takes the hand as far as it can go without a player:
 * sweep the commitments, then either deal the next street and put its first-to-act seat on turn,
 * run the whole board out because nobody can bet again, or — closing the river — reach showdown.
 *
 * Dealing draws from [GameState.deck] directly, because [StateProjection.apply] deliberately
 * never advances it (see its KDoc): the deck dealt from here has to be carried forward by hand
 * onto the state that keeps accumulating events, or the next hand redeals a card already out.
 *
 * A second caller besides [continueHand] is [startHand]: a seat whose blind already puts it
 * all-in never gets an [ActionOn] to answer, so `startHand` closes the (blind-only) round itself
 * instead of leaving the hand stuck on a turn nobody can take.
 */
public fun endBettingRound(state: GameState): EngineResult {
    val endedEvent = BettingRoundEnded(state.eventCount, state.street)
    val afterEnd = StateProjection.apply(state, endedEvent)

    if (seatsAbleToAct(afterEnd) < 2) {
        return runOutBoard(afterEnd, endedEvent)
    }

    if (state.street == Street.RIVER) {
        val showdown = reachShowdownAndSettle(afterEnd)
        return EngineResult.accepted(showdown.newState, listOf(endedEvent) + showdown.events)
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
 * Whether [seat] could still make a betting decision if the hand kept going: not folded, not
 * all-in, and holding chips. A seat with a zero stack that was never flagged all-in (an
 * unreachable shape today, but not one this function should assume away) is just as unable to
 * act as one that is.
 */
private fun canStillAct(seat: Seat): Boolean = !seat.hasFolded && !seat.isAllIn && seat.stack > 0

/**
 * How many seats on [state] could still make a betting decision. Betting needs two live seats
 * to mean anything: with one seat all-in and the other free, or both all-in, there is nobody
 * left to bet into, and it makes no difference which of those two shapes it is — both send the
 * hand straight to showdown.
 */
private fun seatsAbleToAct(state: GameState): Int = state.seats.count(::canStillAct)

/**
 * Deals every street still missing from the board, in order, from [state.street.next] through
 * [Street.RIVER], then reaches showdown — no [ActionOn] anywhere, because nobody able to act is
 * why this runs. Already on the river, the loop deals nothing and only [ShowdownReached] follows.
 *
 * Each [StreetDealt] draws `street.boardCards - board.size` cards from the running deck — three
 * for the flop, one apiece for the turn and the river — carrying [GameState.deck] forward by hand
 * exactly as [closeRound]'s single-street path does, so the log reads like the deal it was
 * instead of one card dump.
 */
private fun runOutBoard(afterEnd: GameState, endedEvent: BettingRoundEnded): EngineResult {
    val events = mutableListOf<GameEvent>(endedEvent)
    var current = afterEnd

    var next = current.street.next
    while (next != null && next.isBetting) {
        val deal = current.deck.deal(next.boardCards - current.board.size)
        val dealtEvent = StreetDealt(current.eventCount, next, deal.cards)
        current = StateProjection.apply(current, dealtEvent).copy(deck = deal.deck)
        events += dealtEvent
        next = current.street.next
    }

    val showdown = reachShowdownAndSettle(current)
    events += showdown.events

    return EngineResult.accepted(showdown.newState, events)
}

/**
 * Reaches showdown from [state] and settles the hand in the same step: [ShowdownReached] first,
 * then one [HandRevealed] per seat named by [revealOrder] — a mucked hand appears in no event,
 * per [ADR-0008](../../docs/adr/ADR-0008-loser-mucks-at-showdown.md) — then whatever
 * [settleHand] returns for [showdownWinners] of the post-showdown state. Shared by
 * [endBettingRound]'s river branch and [runOutBoard], the two places a betting round can end with
 * both seats still in the hand.
 */
private fun reachShowdownAndSettle(state: GameState): EngineResult {
    val showdownEvent = ShowdownReached(state.eventCount)
    var current = StateProjection.apply(state, showdownEvent)
    val events = mutableListOf<GameEvent>(showdownEvent)

    val winners = showdownWinners(current)
    for (seat in revealOrder(current, winners)) {
        val revealEvent = HandRevealed(current.eventCount, seat, current.seat(seat).holeCards)
        current = StateProjection.apply(current, revealEvent)
        events += revealEvent
    }

    val settled = settleHand(current, winners)
    events += settled.events
    return EngineResult.accepted(settled.newState, events)
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
