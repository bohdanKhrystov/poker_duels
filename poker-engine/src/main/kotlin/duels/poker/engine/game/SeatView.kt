package duels.poker.engine.game

import duels.poker.engine.card.Card
import kotlinx.serialization.Serializable

private val SEAT_INDICES = 0..1
private val HOLE_CARD_SIZES = setOf(0, 2)

/**
 * Everything about a seat a recipient may be told: chips, position, status, and optionally
 * hole cards.
 *
 * A separate type from [Seat]: a future field on `Seat` will not reach a client unless
 * someone adds it here deliberately. This separation is the whole point of per-recipient
 * projection.
 *
 * **Hole cards are hidden by default.** A `SeatView` built without [holeCards] has
 * `holeCards == emptyList()`. Showing them is a deliberate act by the caller — only
 * [`PlayerView`](PlayerView) with an entitlement rule should do so.
 *
 * @property index The seat's identity: 0 or 1.
 * @property stack The player's chip stack for this hand.
 * @property committedThisStreet The amount put in on the current street.
 * @property committedThisHand The gross total put in for the entire hand.
 * @property hasFolded Whether the player has folded in this hand.
 * @property isAllIn Whether the player is all-in.
 * @property holeCards The seat's hole cards (0 or 2), **absent by default**. Clients see
 *   these only after the sender has verified entitlement.
 */
@Serializable
public data class SeatView(
    val index: Int,
    val stack: Int,
    val committedThisStreet: Int,
    val committedThisHand: Int,
    val hasFolded: Boolean,
    val isAllIn: Boolean,
    val holeCards: List<Card> = emptyList(),
) {
    init {
        require(index in SEAT_INDICES) { "Seat index must be 0 or 1, was $index" }
        require(stack >= 0) { "Stack cannot be negative, was $stack" }
        require(holeCards.size in HOLE_CARD_SIZES) { "A seat holds 0 or 2 hole cards, not ${holeCards.size}" }
        require(holeCards.toSet().size == holeCards.size) { "Duplicate hole card in $holeCards" }
        require(committedThisStreet >= 0) { "Committed this street cannot be negative, was $committedThisStreet" }
        require(committedThisHand >= committedThisStreet) { "Hand commitment cannot be less than street commitment: hand=$committedThisHand, street=$committedThisStreet" }
    }
}
