package duels.poker.engine.card

/**
 * Card suit.
 *
 * Declaration order is contractual: [Card]'s integer encoding and every recorded shuffle are
 * derived from these ordinals, so reordering them invalidates stored replays.
 */
public enum class Suit(public val symbol: Char) {
    CLUBS('c'),
    DIAMONDS('d'),
    HEARTS('h'),
    SPADES('s'),
}
