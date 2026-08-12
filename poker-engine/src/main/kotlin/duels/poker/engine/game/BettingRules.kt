package duels.poker.engine.game

/**
 * Computes what the seat on turn may legally do at an ordinary decision point.
 *
 * "Ordinary" excludes the all-in restrictions of an opponent who cannot cover a bet, or a stack
 * too short to cover one — see `TASK-010509`. Here, both seats always hold chips.
 *
 * Folding is legal only when facing a bet: with no bet outstanding, the whole set is `check,
 * bet` — there is nothing to give up by folding for free.
 *
 * Returns [LegalActions.none] when the hand is over, no seat is to act, the seat to act has
 * folded, or that seat has no chips left — nothing else yields an empty set.
 */
public fun legalActions(state: GameState): LegalActions {
    val seatToAct = state.seatToAct
        ?: return LegalActions.none(0)

    if (state.isHandOver) {
        return LegalActions.none(seatToAct)
    }

    val seat = state.seat(seatToAct)
    if (seat.hasFolded || seat.stack == 0) {
        return LegalActions.none(seatToAct)
    }

    val committed = seat.committedThisStreet
    val allInTo = committed + seat.stack
    val callTo = committed + state.toCall(seatToAct)
    val minBetTo = minOf(state.bigBlind, allInTo)
    val minRaiseTo = minOf(state.minRaiseTo, allInTo)

    val allowed = buildSet {
        if (callTo == committed) {
            add(ActionType.CHECK)
        } else {
            add(ActionType.FOLD)
            add(ActionType.CALL)
        }

        if (state.betToMatch == 0) {
            add(ActionType.BET)
        } else {
            add(ActionType.RAISE)
        }

        add(ActionType.ALL_IN)
    }

    return LegalActions(
        seat = seatToAct,
        allowed = allowed,
        callTo = callTo,
        minBetTo = minBetTo,
        minRaiseTo = minRaiseTo,
        allInTo = allInTo,
    )
}
