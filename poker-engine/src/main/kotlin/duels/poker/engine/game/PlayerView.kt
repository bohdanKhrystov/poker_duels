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

    public companion object {
        /**
         * Projects [state] into what [seat] is entitled to see: its own hole cards, the
         * opponent's hidden unless [revealed], and every other fact of the hand unchanged.
         *
         * [revealed] names the seats a `HandRevealed` event has already been emitted for **in
         * this hand**, as `revealedSeats(events)` (`TASK-020407`) computes it. [GameState] keeps
         * both seats' hole cards from the deal to the last event, so the state alone cannot say
         * what has been shown — the event log is the only record of that, which is why this is a
         * parameter rather than something derived from [state]. It defaults to `emptySet()`:
         * under-revealing is a visible bug, over-revealing is a silent leak that only an explicit
         * argument can cause.
         *
         * Never reads [GameState.deck] or [GameState.rng] — neither belongs in a client-facing
         * view.
         */
        public fun of(state: GameState, seat: Int, revealed: Set<Int> = emptySet()): PlayerView {
            require(seat in SEAT_INDICES) { "seat must be 0 or 1, was $seat" }
            require(revealed.all { it in SEAT_INDICES }) { "revealed must contain only 0 or 1, was $revealed" }
            return PlayerView(
                viewerSeat = seat,
                handNumber = state.handNumber,
                buttonSeat = state.buttonSeat,
                street = state.street,
                board = state.board,
                pot = state.pot,
                betToMatch = state.betToMatch,
                minRaiseTo = state.minRaiseTo,
                seatToAct = state.seatToAct,
                smallBlind = state.smallBlind,
                bigBlind = state.bigBlind,
                seats = state.seats.map { seatView(it, showCards = it.index == seat || it.index in revealed) },
            )
        }

        /** Builds one [SeatView] from [seat], revealing its hole cards only when [showCards]. */
        private fun seatView(seat: Seat, showCards: Boolean): SeatView {
            return SeatView(
                index = seat.index,
                stack = seat.stack,
                committedThisStreet = seat.committedThisStreet,
                committedThisHand = seat.committedThisHand,
                hasFolded = seat.hasFolded,
                isAllIn = seat.isAllIn,
                holeCards = if (showCards) seat.holeCards else emptyList(),
            )
        }
    }
}
