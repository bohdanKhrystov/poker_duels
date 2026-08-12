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

/**
 * Names the seats that show their hole cards at a showdown, in the order they show, per
 * [ADR-0008](../../docs/adr/ADR-0008-loser-mucks-at-showdown.md).
 *
 * The seats that show are exactly [winners]: a losing hand is never revealed — not even a losing
 * aggressor's — so a single winner shows alone and a mucked hand appears in **no event**, exactly
 * as a folded hand does. This function only orders a reveal; emitting one is someone else's job.
 *
 * Order is decided by [GameState.lastAggressor] — the last seat to bet or raise on the final
 * street — and matters only when there are two winners, since the loser is never in the returned
 * list to be ordered. When the street was checked through, `lastAggressor` is `null` and the seat
 * out of position, first to act on the river, shows first instead. `lastAggressor` is tracked for
 * this reason even though a lone winner shows nothing: a future voluntary show (ADR-0008's
 * rejected-for-now alternative) would need the order for every showdown, not only ties.
 *
 * @param winners the winning seats, as returned by [showdownWinners]: one or two distinct seat
 *   indices
 * @return [winners], reordered so the seat that shows first comes first
 * @throws IllegalArgumentException if [winners] does not hold one or two distinct seat indices
 */
public fun revealOrder(state: GameState, winners: List<Int>): List<Int> {
    require(winners.size in 1..2) { "revealOrder handles one or two winners, got ${winners.size}" }
    require(winners.distinct().size == winners.size) {
        "the same seat cannot win twice, got $winners"
    }
    for (seat in winners) {
        state.seat(seat) // throws IllegalArgumentException unless seat is a valid seat index
    }

    if (winners.size == 1) return winners

    // state.street is Street.SHOWDOWN here, and firstToActOn throws on it — RIVER is the street
    // whose action order decides who shows first.
    val firstToShow = state.lastAggressor ?: firstToActOn(Street.RIVER, state.buttonSeat)
    return listOf(firstToShow, otherSeat(firstToShow))
}
