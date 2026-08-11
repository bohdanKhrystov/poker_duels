package duels.poker.engine.card

import duels.poker.engine.random.Rng

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
     * Shuffles the cards that remain in this deck using the downward Fisher–Yates traversal,
     * drawing exactly one [Rng.nextInt] per swap. This is a **durable replay contract**: for
     * `i` from the last index down to `1`, a single draw with bound `i + 1` picks the index `j`
     * to swap with `i`, and the generator returned by that draw feeds the next step. Changing
     * this traversal direction, the draw range, or the one-draw-per-step rule reorders every
     * deck ever recorded from a seed — see `TASK-010208` for the recorded orderings that pin it
     * down.
     *
     * The receiver is left untouched; the returned [Shuffle] carries the new deck and the
     * advanced [Rng], ready for the caller's next draw. Valid on a partially dealt deck: only
     * the cards that remain are shuffled.
     */
    public fun shuffled(rng: Rng): Shuffle {
        val order = cards.toMutableList()
        var source = rng
        for (i in order.lastIndex downTo 1) {
            val draw = source.nextInt(i + 1)
            val j = draw.value
            val held = order[i]
            order[i] = order[j]
            order[j] = held
            source = draw.next
        }
        return Shuffle(Deck(order.toList()), source)
    }

    /**
     * The cards taken, in order, and the deck that remains.
     */
    public data class Deal(val cards: List<Card>, val deck: Deck)

    /** The shuffled deck and the generator left over, ready for the next draw. */
    public data class Shuffle(val deck: Deck, val rng: Rng)

    override fun toString(): String = "Deck(remaining=$remaining)"

    public companion object {
        /**
         * A full deck with all 52 cards in their initial order.
         */
        public fun full(): Deck = Deck(Card.all)
    }
}
