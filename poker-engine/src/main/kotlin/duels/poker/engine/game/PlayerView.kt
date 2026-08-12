package duels.poker.engine.game

import kotlinx.serialization.Serializable

private val SEAT_INDICES = 0..1

/**
 * Exactly the information one recipient is entitled to see during a live hand: the state of the
 * board, all seats (with their own hole cards visible, others' hidden), and the current action.
 *
 * Notably absent: [GameState.deck], [GameState.rng], and the seed. Per
 * [ADR-0002](../../docs/adr/ADR-0002-server-authoritative.md), none of these leave the server while a
 * hand is live. A field added to `PlayerView` is a deliberate decision to publish it.
 *
 * @property viewerSeat The recipient's seat: 0 or 1.
 * @property handNumber The 1-based index of this hand within the match.
 * @property buttonSeat The seat holding the button this hand: 0 or 1.
 * @property street The current phase of the hand.
 * @property board The community cards dealt so far.
 * @property pot Chips already swept from finished betting rounds.
 * @property betToMatch The street-total amount a seat must match to stay in the hand.
 * @property minRaiseTo The street-total amount the next raise must reach at minimum.
 * @property seatToAct The seat on turn to act, or `null` if no seat is currently to act.
 * @property smallBlind The small blind amount in force for this hand.
 * @property bigBlind The big blind amount in force for this hand.
 * @property seats Both seats, ordered by index so `seats[i]` is always seat `i`.
 */
@Serializable
public data class PlayerView(
    val viewerSeat: Int,
    val handNumber: Int,
    val buttonSeat: Int,
    val street: Street,
    val board: Board,
    val pot: Int,
    val betToMatch: Int,
    val minRaiseTo: Int,
    val seatToAct: Int?,
    val smallBlind: Int,
    val bigBlind: Int,
    val seats: List<SeatView>,
) {
    init {
        require(viewerSeat in SEAT_INDICES) { "viewerSeat must be 0 or 1, was $viewerSeat" }
        require(buttonSeat in SEAT_INDICES) { "buttonSeat must be 0 or 1, was $buttonSeat" }
        require(seats.size == 2) { "PlayerView requires exactly two seats, was ${seats.size}" }
        require(seats[0].index == 0 && seats[1].index == 1) {
            "Seats must be ordered by index: seats[0].index=${seats[0].index}, seats[1].index=${seats[1].index}"
        }
        require(handNumber >= 1) { "handNumber must be at least 1, was $handNumber" }
        require(pot >= 0) { "pot cannot be negative, was $pot" }
        require(betToMatch >= 0) { "betToMatch cannot be negative, was $betToMatch" }
        require(minRaiseTo >= 0) { "minRaiseTo cannot be negative, was $minRaiseTo" }
        require(seatToAct == null || seatToAct in SEAT_INDICES) {
            "seatToAct must be null, 0 or 1, was $seatToAct"
        }
        require(0 < smallBlind && smallBlind < bigBlind) {
            "Blinds must be ascending and positive: smallBlind=$smallBlind, bigBlind=$bigBlind"
        }
    }

    /** The recipient's own seat: `seats[viewerSeat]`. */
    public val viewer: SeatView get() = seats[viewerSeat]

    /** The opponent's seat: `seats[1 - viewerSeat]`. */
    public val opponent: SeatView get() = seats[1 - viewerSeat]
}
