package duels.poker.engine.game

/**
 * Why the engine will not execute a [PlayerAction].
 *
 * A rejection never throws and never changes state — it is returned inside `EngineResult`
 * with an empty event list. It carries the numeric values a client needs to explain itself
 * to a player without knowing a single rule.
 */
public sealed interface Rejection {
    /** It is [seatToAct]'s turn, not the sender's. Null means nobody is to act. */
    public data class NotYourTurn(val seatToAct: Int?) : Rejection

    /** The action type is not among [allowed] in this position. */
    public data class ActionNotAllowed(val attempted: ActionType, val allowed: Set<ActionType>) : Rejection

    /**
     * A bet or raise below the smallest legal total.
     *
     * Both [attempted] and [minimum] are street totals, the same convention as [PlayerAction].
     */
    public data class AmountTooSmall(val attempted: Int, val minimum: Int) : Rejection

    /**
     * A bet or raise above the seat's stack.
     *
     * Both [attempted] and [maximum] are street totals, the same convention as [PlayerAction].
     */
    public data class AmountTooLarge(val attempted: Int, val maximum: Int) : Rejection

    /** The hand is over; it accepts nothing further. */
    public data object HandComplete : Rejection
}
