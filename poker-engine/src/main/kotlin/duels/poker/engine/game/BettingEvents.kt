package duels.poker.engine.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Something a seat did with its chips. The projection dispatches on this sub-interface.
 *
 * For events carrying an amount: [to] is the seat's **street total after the action**, not an
 * increment. The chips that moved are `to - committedThisStreet`, which only the state knows, so
 * a reader of the log folds rather than sums.
 */
@Serializable
public sealed interface BettingEvent : GameEvent {
    public val seat: Int
}

/** A player folded their hand. */
@Serializable
@SerialName("PlayerFolded")
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
@Serializable
@SerialName("PlayerChecked")
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
@Serializable
@SerialName("PlayerCalled")
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
@Serializable
@SerialName("PlayerBet")
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
@Serializable
@SerialName("PlayerRaised")
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
@Serializable
@SerialName("PlayerAllIn")
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
