package duels.poker.engine.game

/**
 * Answers "may this seat do this, right now, for this much?" without executing anything.
 *
 * The checks run in a fixed order, because a complete hand has no seat to act and would
 * otherwise be reported as [Rejection.NotYourTurn] instead of [Rejection.HandComplete]:
 *
 * 1. The hand is over.
 * 2. It is not the sender's turn.
 * 3. The action's type is not currently legal.
 * 4. A bet or raise falls below the legal minimum.
 * 5. A bet or raise exceeds the seat's stack.
 *
 * Returns `null` when none of these apply — the action is legal as sent.
 */
public fun rejectionFor(state: GameState, action: PlayerAction): Rejection? {
    if (state.isHandOver) {
        return Rejection.HandComplete
    }

    if (action.seat != state.seatToAct) {
        return Rejection.NotYourTurn(state.seatToAct)
    }

    val legal = legalActions(state)
    if (!legal.allows(action.type)) {
        return Rejection.ActionNotAllowed(action.type, legal.allowed)
    }

    val to = when (action) {
        is PlayerAction.Bet -> action.to
        is PlayerAction.Raise -> action.to
        else -> null
    }

    if (to != null) {
        val minimum = if (action is PlayerAction.Bet) legal.minBetTo else legal.minRaiseTo
        if (to < minimum) {
            return Rejection.AmountTooSmall(to, minimum)
        }

        if (to > legal.allInTo) {
            return Rejection.AmountTooLarge(to, legal.allInTo)
        }
    }

    return null
}
