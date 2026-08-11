package duels.poker.engine.game

/**
 * The six phases a hand passes through. The enum is ordered; [next] and [isBetting] are derived
 * from declaration order.
 *
 * @property boardCards How many cards are face up once the street has been dealt. A hand that
 *   ends by a fold can sit in [COMPLETE] with fewer.
 */
public enum class Street(public val boardCards: Int) {
    PREFLOP(0),
    FLOP(3),
    TURN(4),
    RIVER(5),
    SHOWDOWN(5),
    COMPLETE(5),
    ;

    /** True while chips can still go in: preflop through the river. */
    public val isBetting: Boolean get() = this <= RIVER

    /** The street that follows, or null after [COMPLETE]. */
    public val next: Street? get() = entries.getOrNull(ordinal + 1)
}
