package duels.poker.engine.card

private const val SUIT_COUNT = 4

/**
 * A playing card, identified by its rank and suit. Exactly the 52 real cards are constructible,
 * each one allocation-free via the `@JvmInline` value class. The integer encoding used
 * internally is an implementation detail; use [of] to construct cards and [rank], [suit] to
 * inspect them. The order of [all] is contractual: recorded shuffles and the integer encoding
 * both depend on it.
 */
@JvmInline
public value class Card private constructor(private val code: Int) {
    public val rank: Rank get() = Rank.entries[code / SUIT_COUNT]
    public val suit: Suit get() = Suit.entries[code % SUIT_COUNT]

    public companion object {
        public fun of(rank: Rank, suit: Suit): Card =
            Card(rank.ordinal * SUIT_COUNT + suit.ordinal)

        /**
         * All 52 real cards, ordered rank-major: for each rank in ordinal order, each suit in
         * ordinal order. So `all[0]` is the two of clubs and `all[51]` is the ace of spades.
         * This order is contractual: recorded shuffles begin from it.
         */
        public val all: List<Card> = (0..51).map { code -> Card(code) }
    }
}
