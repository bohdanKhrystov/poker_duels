package duels.poker.engine.game

/**
 * Turns an already-legal [PlayerAction] into the [BettingEvent] that records it.
 *
 * The event's `sequence` is always `state.eventCount` — the next event of the hand. Amounts come
 * from [legalActions], never from the caller, with one exception: a [PlayerAction.Bet] or
 * [PlayerAction.Raise] carries the `to` the client chose, which `TASK-010510`'s validation has
 * already bounded before this function is called.
 *
 * A whole-stack [PlayerAction.Bet] or [PlayerAction.Raise] — one whose `to` equals
 * `legal.allInTo` — is logged as [PlayerAllIn], not [PlayerBet] or [PlayerRaised]. This matters
 * because `applyBetting` raises `minRaiseTo` unconditionally for a bet or a raise, but for an
 * all-in only when the shove reaches a full raise (`TASK-010422`). Logging a short shove as a
 * raise would lower the bar for the next raise, when the rules say a short all-in must not
 * reopen the betting.
 *
 * A [PlayerAction.Call] always becomes [PlayerCalled], even when it commits the seat's last
 * chip: `Seat.commit` sets `isAllIn` on its own, and the minimum-raise bar must not move for a
 * call.
 *
 * @throws IllegalArgumentException if [action] is not legal for [state] — an unvalidated action
 *   must never produce an event.
 */
public fun eventFor(state: GameState, action: PlayerAction): BettingEvent {
    val legal = legalActions(state)
    require(legal.allows(action.type)) {
        "${action.type} is not legal for seat ${action.seat}: allowed = ${legal.allowed}"
    }

    val sequence = state.eventCount
    val seat = action.seat

    return when (action) {
        is PlayerAction.Fold -> PlayerFolded(sequence, seat)
        is PlayerAction.Check -> PlayerChecked(sequence, seat)
        is PlayerAction.Call -> PlayerCalled(sequence, seat, legal.callTo)
        is PlayerAction.AllIn -> PlayerAllIn(sequence, seat, legal.allInTo)
        is PlayerAction.Bet ->
            if (action.to == legal.allInTo) {
                PlayerAllIn(sequence, seat, action.to)
            } else {
                PlayerBet(sequence, seat, action.to)
            }

        is PlayerAction.Raise ->
            if (action.to == legal.allInTo) {
                PlayerAllIn(sequence, seat, action.to)
            } else {
                PlayerRaised(sequence, seat, action.to)
            }
    }
}
