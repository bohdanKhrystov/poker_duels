package duels.poker.engine.game

import duels.poker.engine.card.Deck
import duels.poker.engine.random.Rng

private val SEAT_INDICES = 0..1

/**
 * The whole of one hand of heads-up hold'em — everything needed to resume it lives here and
 * nowhere else. Copy it, store it, hand it back to the engine, and the hand continues exactly
 * where it was.
 *
 * [betToMatch] and [minRaiseTo] are **street totals**, not increments — see `TASK-010412`.
 *
 * [pot] holds only chips already swept from finished betting rounds; chips still in front of a
 * seat are that seat's `committedThisStreet` until the round ends.
 *
 * [deck] and [rng] are carried in the state because the engine may never reach for an ambient
 * random source, and no `GameEvent` ever reveals either of them.
 *
 * @property handNumber The 1-based index of this hand within the match.
 * @property buttonSeat The seat holding the button this hand: 0 or 1.
 * @property street The current phase of the hand.
 * @property seats Both seats, ordered by index so `seats[i]` is always seat `i`.
 * @property board The community cards dealt so far.
 * @property pot Chips already swept from finished betting rounds.
 * @property betToMatch The street-total amount a seat must match to stay in the hand.
 * @property minRaiseTo The street-total amount the next raise must reach at minimum.
 * @property seatToAct The seat on turn to act, or `null` if no seat is currently to act.
 * @property smallBlind The small blind amount in force for this hand.
 * @property bigBlind The big blind amount in force for this hand.
 * @property eventCount How many events this hand has already produced, so the next event's
 *   `sequence` is exactly `eventCount`. It lives in the state because `handle` is pure: an
 *   engine that kept a counter of its own would be hidden state, which ADR-0001 forecloses.
 * @property deck The deck as it stands, undealt cards only.
 * @property rng The random generator as it stands, ready for the next draw.
 * @property lastAggressor The seat that last bet or raised on the current street, or `null`
 *   when the street has seen no aggression. Used to order reveals at showdown per
 *   [ADR-0008](../../docs/adr/ADR-0008-loser-mucks-at-showdown.md). This ticket only declares
 *   it; `TASK-010619` sets it, `TASK-010620` clears it on a new street.
 */
public data class GameState(
    val handNumber: Int,
    val buttonSeat: Int,
    val street: Street,
    val seats: List<Seat>,
    val board: Board,
    val pot: Int,
    val betToMatch: Int,
    val minRaiseTo: Int,
    val seatToAct: Int?,
    val smallBlind: Int,
    val bigBlind: Int,
    val eventCount: Int,
    val deck: Deck,
    val rng: Rng,
    val lastAggressor: Int? = null,
) {
    init {
        require(seats.size == 2) { "GameState requires exactly two seats, was ${seats.size}" }
        require(seats[0].index == 0 && seats[1].index == 1) {
            "Seats must be ordered by index: seats[0].index=${seats[0].index}, seats[1].index=${seats[1].index}"
        }
        require(buttonSeat in SEAT_INDICES) { "buttonSeat must be 0 or 1, was $buttonSeat" }
        require(seatToAct == null || seatToAct in SEAT_INDICES) {
            "seatToAct must be null, 0 or 1, was $seatToAct"
        }
        require(lastAggressor == null || lastAggressor in SEAT_INDICES) {
            "lastAggressor must be null, 0 or 1, was $lastAggressor"
        }
        require(handNumber >= 1) { "handNumber must be at least 1, was $handNumber" }
        require(pot >= 0) { "pot cannot be negative, was $pot" }
        require(betToMatch >= 0) { "betToMatch cannot be negative, was $betToMatch" }
        require(minRaiseTo >= 0) { "minRaiseTo cannot be negative, was $minRaiseTo" }
        require(eventCount >= 0) { "eventCount cannot be negative, was $eventCount" }
        require(0 < smallBlind && smallBlind < bigBlind) {
            "Blinds must be ascending and positive: smallBlind=$smallBlind, bigBlind=$bigBlind"
        }
        require(street == Street.COMPLETE || board.size == street.boardCards) {
            "Board size ${board.size} does not match $street, which expects ${street.boardCards}"
        }
    }

    /** The pot plus everything still in front of the seats on this street. */
    public val potTotal: Int get() = pot + seats.sumOf { it.committedThisStreet }

    /**
     * Every chip in the hand. Constant from the first blind to the last award — the
     * conservation invariant the rules document requires.
     */
    public val chipsInPlay: Int get() = potTotal + seats.sumOf { it.stack }

    /** True once the hand can accept no further action. */
    public val isHandOver: Boolean get() = street == Street.COMPLETE

    /** The seat at [index]; `seats` is ordered by index so this is `seats[index]`. */
    public fun seat(index: Int): Seat {
        require(index in SEAT_INDICES) { "seat index must be 0 or 1, was $index" }
        return seats[index]
    }

    /**
     * Chips [index] must put in to match [betToMatch], capped at its stack: a seat can always
     * call all-in for less than the full amount.
     */
    public fun toCall(index: Int): Int {
        val seat = seat(index)
        return (betToMatch - seat.committedThisStreet).coerceIn(0, seat.stack)
    }

    /** This state with [index] replaced by `transform(seat(index))`. */
    public fun withSeat(index: Int, transform: (Seat) -> Seat): GameState {
        val updated = transform(seat(index))
        return copy(seats = seats.mapIndexed { i, seat -> if (i == index) updated else seat })
    }
}
