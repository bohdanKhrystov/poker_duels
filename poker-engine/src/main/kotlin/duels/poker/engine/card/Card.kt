package duels.poker.engine.card

import kotlinx.serialization.Serializable

private const val SUIT_COUNT = 4

/**
 * A playing card, identified by its rank and suit. Exactly the 52 real cards are constructible,
 * each one allocation-free via the `@JvmInline` value class. The integer encoding used
 * internally is an implementation detail; use [of] to construct cards and [rank], [suit] to
 * inspect them. The order of [all] is contractual: recorded shuffles and the integer encoding
 * both depend on it.
 */
@Serializable(with = CardSerializer::class)
@JvmInline
public value class Card private constructor(private val code: Int) {
    public val rank: Rank get() = Rank.entries[code / SUIT_COUNT]
    public val suit: Suit get() = Suit.entries[code % SUIT_COUNT]

    override fun toString(): String = "${rank.symbol}${suit.symbol}"

    public companion object {
        public fun of(rank: Rank, suit: Suit): Card =
            Card(rank.ordinal * SUIT_COUNT + suit.ordinal)

        /**
         * All 52 real cards, ordered rank-major: for each rank in ordinal order, each suit in
         * ordinal order. So `all[0]` is the two of clubs and `all[51]` is the ace of spades.
         * This order is contractual: recorded shuffles begin from it.
         */
        public val all: List<Card> = (0..51).map { code -> Card(code) }

        /**
         * Parse a card from standard poker notation: rank symbol (2-9, T, J, Q, K, A) followed
         * by suit symbol (c, d, h, s). The rank character is case-insensitive; the suit must be
         * lowercase. Examples: "As", "Th", "2c", "Kd". Returns null if the input is malformed
         * (wrong length, invalid rank or suit symbol).
         */
        public fun parseOrNull(text: String): Card? {
            if (text.length != 2) return null

            val rankChar = text[0].uppercaseChar()
            val suitChar = text[1]

            val rank = Rank.entries.find { it.symbol == rankChar } ?: return null
            val suit = Suit.entries.find { it.symbol == suitChar } ?: return null

            return of(rank, suit)
        }

        /**
         * Parse a card from standard poker notation: rank symbol (2-9, T, J, Q, K, A) followed
         * by suit symbol (c, d, h, s). The rank character is case-insensitive; the suit must be
         * lowercase. Examples: "As", "Th", "2c", "Kd".
         *
         * @throws IllegalArgumentException if the input is malformed (wrong length, invalid rank
         * or suit symbol). The message contains the offending input.
         */
        public fun parse(text: String): Card =
            parseOrNull(text) ?: throw IllegalArgumentException("Invalid card notation: $text")
    }
}
