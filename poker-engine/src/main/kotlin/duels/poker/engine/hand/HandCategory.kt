package duels.poker.engine.hand

/**
 * A poker hand category, ordered from weakest to strongest.
 *
 * The declaration order of this enum *is* the hand ranking. Reordering entries silently changes
 * who wins every showdown. Never reorder without explicit intent.
 *
 * Comparison operators and [ordinal] both work correctly without a table: stronger hands have
 * higher ordinals.
 */
public enum class HandCategory {
    HIGH_CARD,
    PAIR,
    TWO_PAIR,
    THREE_OF_A_KIND,
    STRAIGHT,
    FLUSH,
    FULL_HOUSE,
    FOUR_OF_A_KIND,
    STRAIGHT_FLUSH,
}
