package duels.poker.engine.game

/**
 * Something a seat did with its chips. The projection dispatches on this sub-interface.
 *
 * For events carrying an amount: [to] is the seat's **street total after the action**, not an
 * increment. The chips that moved are `to - committedThisStreet`, which only the state knows, so
 * a reader of the log folds rather than sums.
 */
public sealed interface BettingEvent : GameEvent {
    public val seat: Int
}

/** A player folded their hand. */
public data class PlayerFolded(
    override val sequence: Int,
    override val seat: Int,
) : BettingEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
    }
}

/** A player checked. */
public data class PlayerChecked(
    override val sequence: Int,
    override val seat: Int,
) : BettingEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
    }
}

/** A player called. [to] is the seat's street total after calling. */
public data class PlayerCalled(
    override val sequence: Int,
    override val seat: Int,
    val to: Int,
) : BettingEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
        require(to > 0) { "to must be positive, was $to" }
    }
}

/** A player bet. [to] is the seat's street total after betting. */
public data class PlayerBet(
    override val sequence: Int,
    override val seat: Int,
    val to: Int,
) : BettingEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
        require(to > 0) { "to must be positive, was $to" }
    }
}

/** A player raised. [to] is the seat's street total after raising. */
public data class PlayerRaised(
    override val sequence: Int,
    override val seat: Int,
    val to: Int,
) : BettingEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
        require(to > 0) { "to must be positive, was $to" }
    }
}

/** A player went all-in. [to] is the seat's street total after going all-in. */
public data class PlayerAllIn(
    override val sequence: Int,
    override val seat: Int,
    val to: Int,
) : BettingEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
        require(to > 0) { "to must be positive, was $to" }
    }
}
