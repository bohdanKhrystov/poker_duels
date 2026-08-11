package duels.poker.engine.card

/**
 * An immutable deck of playing cards. Dealing returns a new deck rather than mutating the
 * receiver, so no caller can deal the same card twice by holding a stale reference.
 * The initial order is [Card.all].
 */
@ConsistentCopyVisibility
public data class Deck private constructor(private val cards: List<Card>) {
    public val remaining: Int get() = cards.size

    public fun deal(count: Int): Deal {
        require(count in 0..remaining) {
            "Cannot deal $count cards from a deck with $remaining remaining"
        }
        val dealt = cards.take(count)
        val newDeck = Deck(cards.drop(count))
        return Deal(dealt, newDeck)
    }

    /**
     * The cards taken, in order, and the deck that remains.
     */
    public data class Deal(val cards: List<Card>, val deck: Deck)

    override fun toString(): String = "Deck(remaining=$remaining)"

    public companion object {
        /**
         * A full deck with all 52 cards in their initial order.
         */
        public fun full(): Deck = Deck(Card.all)
    }
}
