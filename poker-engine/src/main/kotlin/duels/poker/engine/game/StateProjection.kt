package duels.poker.engine.game

/**
 * Folds [GameEvent]s into a [GameState], one event at a time. This is the single place replay,
 * recovery and the contract suite (`TASK-010426`) all go through — no other function in this
 * module is allowed to apply an event to a state.
 *
 * The one deliberate gap in the fold: it never changes [GameState.deck] or [GameState.rng],
 * because no event carries an undealt card or a seed — that is a project non-negotiable (see the
 * KDoc on [GameEvent]). A folded state therefore matches the engine's state in every field except
 * those two, and the contract suite compares accordingly. Recovering a hand from a log alone
 * needs the seed stored beside it: `TASK-010801`.
 */
public object StateProjection {
    /** Replays [events] over [state], in order. */
    public fun fold(state: GameState, events: List<GameEvent>): GameState =
        events.fold(state, ::apply)

    /** The state after [event]. */
    public fun apply(state: GameState, event: GameEvent): GameState =
        dispatch(state, event).copy(eventCount = event.sequence + 1)

    /** Exhaustive over [GameEvent] — no `else`, ever. */
    private fun dispatch(state: GameState, event: GameEvent): GameState = when (event) {
        is BettingEvent -> applyBetting(state, event)
        is DealerEvent -> applyDealer(state, event)
        is HandStarted -> state.copy(
            handNumber = event.handNumber,
            buttonSeat = event.buttonSeat,
            street = Street.PREFLOP,
            seats = event.stacks.mapIndexed { index, stack -> Seat(index = index, stack = stack) },
            board = Board.EMPTY,
            pot = 0,
            betToMatch = 0,
            minRaiseTo = event.bigBlind,
            seatToAct = null,
            smallBlind = event.smallBlind,
            bigBlind = event.bigBlind,
        )
        is BlindPosted -> {
            val committed = state.withSeat(event.seat) { it.commit(event.to - it.committedThisStreet) }
            val betToMatch = maxOf(committed.betToMatch, event.to)
            committed.copy(betToMatch = betToMatch, minRaiseTo = betToMatch + committed.bigBlind)
        }
        is HoleCardsDealt -> state.withSeat(event.seat) { it.copy(holeCards = event.cards) }
        is ActionOn -> state.copy(seatToAct = event.seat)
    }
}
