package duels.poker.server.protocol

import kotlinx.serialization.Serializable

/**
 * How present the seat a frame describes is, from the server's point of view alone.
 *
 * The three values are exactly the three states the server's own room bookkeeping already
 * distinguishes: a seat with a live connection, one inside a grace period, and one whose
 * grace period has run out.
 */
@Serializable
public enum class SeatPresence {
    /** Neither [AWAY] nor [ABSENT]. */
    PRESENT,

    /** A grace period is running for this seat; the duel is paused. */
    AWAY,

    /** This seat's grace period ran out; the duel is live again and the server acts for it. */
    ABSENT,
}
