package duels.poker.engine.game

import duels.poker.engine.hand.FastHandEvaluator

private const val COMPLETE_BOARD = 5
private const val HOLE_CARDS = 2

/**
 * Names the seat with the better five-card hand at a real showdown, or both seats when the two
 * hands are equal.
 *
 * This decides who won, nothing more — awarding chips is [settleHand]'s job. Keeping the
 * decision separate from the chips is what lets each be tested alone: this function never
 * touches a stack or a pot.
 *
 * @throws IllegalArgumentException if the board is not complete, either seat is not holding
 *   exactly two hole cards, or either seat has folded
 */
public fun showdownWinners(state: GameState): List<Int> {
    require(state.board.size == COMPLETE_BOARD) {
        "showdownWinners requires a complete board, had ${state.board.size} cards"
    }
    for (seat in state.seats) {
        require(seat.holeCards.size == HOLE_CARDS) {
            "seat ${seat.index} must hold exactly two hole cards, had ${seat.holeCards.size}"
        }
        require(!seat.hasFolded) { "seat ${seat.index} has folded, a folded seat cannot reach showdown" }
    }

    val ranks = state.seats.map { seat ->
        FastHandEvaluator.bestOfSeven(seat.holeCards + state.board.cards).rank
    }

    return when {
        ranks[0] > ranks[1] -> listOf(0)
        ranks[1] > ranks[0] -> listOf(1)
        else -> listOf(0, 1)
    }
}
