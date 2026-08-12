package duels.poker.ai

import duels.poker.engine.card.Card
import duels.poker.engine.game.GameState

/**
 * The first invariant [state] breaks, described, or null if it breaks none.
 *
 * Checked in a fixed order — chip conservation first, then stack sanity, then card uniqueness,
 * then the seat-to-act rule — so that when several invariants are broken at once, the message
 * always names the same one. The harness calls this after every action of every simulated duel;
 * a single, cheap, pure function is what makes that affordable.
 */
public fun firstViolation(state: GameState, chipsAtOpen: Int): String? {
    if (state.chipsInPlay != chipsAtOpen) {
        return "chips: chipsInPlay was ${state.chipsInPlay}, expected $chipsAtOpen (opened with $chipsAtOpen)"
    }

    for (seat in state.seats) {
        if (seat.stack < 0) {
            return "negative stack: seat ${seat.index} has stack ${seat.stack}"
        }
        if (seat.committedThisStreet > seat.committedThisHand) {
            return "committedThisStreet exceeds committedThisHand: seat ${seat.index} has " +
                "committedThisStreet=${seat.committedThisStreet}, committedThisHand=${seat.committedThisHand}"
        }
    }

    val cardsInPlay = state.seats.flatMap { it.holeCards } + state.board.cards
    val seen = mutableSetOf<Card>()
    for (card in cardsInPlay) {
        if (!seen.add(card)) {
            return "duplicate card in play: $card appears more than once among $cardsInPlay"
        }
    }

    if (!state.isHandOver) {
        val seatToAct = state.seatToAct
            ?: return "no seat to act: hand is not over (street=${state.street}) but seatToAct is null"
        val seat = state.seat(seatToAct)
        if (seat.hasFolded) {
            return "seat to act has folded: seat $seatToAct is seatToAct but hasFolded=true"
        }
        if (seat.isAllIn) {
            return "seat to act is all-in: seat $seatToAct is seatToAct but isAllIn=true"
        }
        if (seat.stack <= 0) {
            return "seat to act has no chips: seat $seatToAct is seatToAct but stack=${seat.stack}"
        }
    }

    return null
}
