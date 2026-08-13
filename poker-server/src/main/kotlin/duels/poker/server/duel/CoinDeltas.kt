package duels.poker.server.duel

import duels.poker.engine.duel.DuelOutcome

/**
 * The signed coin deltas for each seat from a finished duel.
 *
 * @param seat0 the coin delta for seat 0
 * @param seat1 the coin delta for seat 1
 *
 * The deltas always sum to zero — coins are conserved, never minted. Per ADR-0014, the winner
 * gains one, the loser loses one, and a draw pays nothing to either seat. This class holds the
 * result of that calculation.
 */
public data class CoinDeltas(val seat0: Int, val seat1: Int) {
    /**
     * Returns the coin delta for the given seat.
     *
     * @param seat the seat (0 or 1)
     * @return the coin delta for that seat
     * @throws IllegalArgumentException if seat is not 0 or 1
     */
    public fun forSeat(seat: Int): Int =
        when (seat) {
            0 -> seat0
            1 -> seat1
            else -> throw IllegalArgumentException("seat must be 0 or 1, got $seat")
        }
}

/**
 * Maps a finished duel's [DuelOutcome] to two signed coin deltas, per ADR-0014.
 *
 * This is the single place in the server that decides what a duel is worth. The ADR anticipates
 * being superseded by a floating or opponent-weighted award; a rule expressed once is a rule
 * that can be replaced once.
 *
 * @param outcome the finished duel's outcome
 * @return the signed coin deltas for each seat
 */
public fun coinDeltas(outcome: DuelOutcome): CoinDeltas =
    when {
        outcome.isDraw -> CoinDeltas(seat0 = 0, seat1 = 0)
        outcome.winner == 0 -> CoinDeltas(seat0 = 1, seat1 = -1)
        else -> CoinDeltas(seat0 = -1, seat1 = 1)
    }
