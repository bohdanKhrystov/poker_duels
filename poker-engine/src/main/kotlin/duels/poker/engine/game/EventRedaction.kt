package duels.poker.engine.game

/**
 * Answers *may [seat] see [event]?* — the projection layer's security boundary.
 *
 * The `when` below names all seventeen concrete [GameEvent] subtypes individually, with no
 * `else` and no branch on the [BettingEvent] or [DealerEvent] sub-interfaces. That is deliberate:
 * a new event type must fail to compile here until someone decides who may see it, rather than
 * silently falling through an `else` or a sub-interface shortcut that would let a future
 * card-carrying event reach every seat by default.
 *
 * Exactly one branch filters — a seat's own [HoleCardsDealt] is withheld from the other seat.
 * [HandRevealed] reaches both seats unfiltered: the engine emits it only for a hand actually
 * shown (`ADR-0008`), so by the time it exists there is nothing left to hide.
 *
 * A filtered-out event is dropped entirely, never returned with its cards blanked or masked.
 * Returning `null` instead of a redacted copy guarantees no half-redacted event object is ever
 * constructed, so there is nothing for a later refactor to leak.
 *
 * @return [event] unchanged if [seat] may see it, `null` otherwise.
 */
public fun visibleTo(event: GameEvent, seat: Int): GameEvent? {
    require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
    return when (event) {
        is HoleCardsDealt -> if (event.seat == seat) event else null
        is HandStarted -> event
        is BlindPosted -> event
        is ActionOn -> event
        is PlayerFolded -> event
        is PlayerChecked -> event
        is PlayerCalled -> event
        is PlayerBet -> event
        is PlayerRaised -> event
        is PlayerAllIn -> event
        is BettingRoundEnded -> event
        is StreetDealt -> event
        is ShowdownReached -> event
        is HandRevealed -> event
        is UncalledBetReturned -> event
        is PotAwarded -> event
        is HandFinished -> event
    }
}

/**
 * [visibleTo] applied to a whole event stream: the events [seat] may see, in their original
 * order, with the ones it may not simply absent.
 */
public fun visibleTo(events: List<GameEvent>, seat: Int): List<GameEvent> =
    events.mapNotNull { visibleTo(it, seat) }
