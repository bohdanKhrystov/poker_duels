package duels.poker.engine.game

import duels.poker.engine.card.Card
import kotlinx.serialization.Serializable

private val LEGAL_SIZES = setOf(0, 3, 4, 5)

/**
 * The community cards on the board. A board always has 0, 3, 4, or 5 cards and never contains
 * duplicates. Hole cards live on [Seat]; this type represents only the shared cards dealt to
 * the center of the table.
 */
@Serializable
public data class Board(val cards: List<Card>) {
    init {
        require(cards.size in LEGAL_SIZES) { "A board holds 0, 3, 4 or 5 cards, not ${cards.size}" }
        require(cards.toSet().size == cards.size) { "Duplicate card on board $cards" }
    }

    public val size: Int get() = cards.size

    /**
     * This board plus [more], validated by the same rules.
     */
    public fun dealt(more: List<Card>): Board = Board(cards + more)

    public companion object {
        public val EMPTY: Board = Board(emptyList())
    }
}
