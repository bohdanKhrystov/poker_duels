package duels.poker.engine.game

import duels.poker.engine.card.Card

private val SEAT_INDICES = 0..1
private val HOLE_CARD_SIZES = setOf(0, 2)

/**
 * One player's position in a hand — chips, cards and status — as an immutable value.
 *
 * @property index The seat's identity: 0 or 1. Never "hero" or "villain" (viewer-relative,
 *   belong to the projection layer).
 * @property stack The player's chip stack for this hand.
 * @property holeCards The player's hole cards (0 or 2).
 * @property committedThisStreet The amount put in on the current street.
 * @property committedThisHand The gross total put in for the entire hand. Never decreases
 *   while the hand runs.
 * @property hasFolded Whether the player has folded in this hand.
 * @property isAllIn Whether the player is all-in.
 */
public data class Seat(
    val index: Int,
    val stack: Int,
    val holeCards: List<Card> = emptyList(),
    val committedThisStreet: Int = 0,
    val committedThisHand: Int = 0,
    val hasFolded: Boolean = false,
    val isAllIn: Boolean = false,
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
