package duels.poker.engine.card

/**
 * Card rank: from two to ace.
 *
 * Declaration order is contractual: [Card]'s integer encoding and every recorded shuffle are
 * derived from these ordinals, so reordering them invalidates stored replays.
 */
public enum class Rank(public val value: Int, public val symbol: Char) {
    TWO(2, '2'),
    THREE(3, '3'),
    FOUR(4, '4'),
    FIVE(5, '5'),
    SIX(6, '6'),
    SEVEN(7, '7'),
    EIGHT(8, '8'),
    NINE(9, '9'),
    TEN(10, 'T'),
    JACK(11, 'J'),
    QUEEN(12, 'Q'),
    KING(13, 'K'),
    ACE(14, 'A'),
}
