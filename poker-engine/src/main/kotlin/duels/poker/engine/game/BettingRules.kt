package duels.poker.engine.game

/**
 * Computes what the seat on turn may legally do, including the all-in restrictions of
 * `TASK-010509`.
 *
 * Folding is legal only when facing a bet: with no bet outstanding, the whole set is `check,
 * bet` — there is nothing to give up by folding for free.
 *
 * Two restrictions narrow the ordinary set from `TASK-010508`:
 *
 * - **The opponent must be contestable.** An opponent who has folded, is already all-in, or has
 *   no chips left cannot face a bet, a raise, or a further all-in — there is nobody left to call
 *   it. `BET`, `RAISE` and `ALL_IN` all disappear, leaving `FOLD` and `CALL` (or `CHECK` when
 *   there is nothing to call). This is the whole of the heads-up "**a short all-in does not
 *   reopen the betting**" rule from `docs/duel-rules.md`: with only two seats, the player facing
 *   a short all-in always has a non-contestable opponent, so raising is never on the table —
 *   only folding or calling for the amount actually at stake (`DEC-003`).
 * - **A seat that cannot cover the bet cannot raise.** When this seat's own `allInTo` does not
 *   exceed `betToMatch`, no amount it could add would out-raise what is already there, so `RAISE`
 *   is removed. `CALL` remains, capped at the stack by `GameState.toCall`.
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

    val other = state.seat(1 - seatToAct)
    val contestable = !other.hasFolded && !other.isAllIn && other.stack > 0

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

        if (contestable) {
            if (state.betToMatch == 0) {
                add(ActionType.BET)
            } else if (allInTo > state.betToMatch) {
                add(ActionType.RAISE)
            }

            add(ActionType.ALL_IN)
        }
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
