package duels.poker.engine.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The tag of a [PlayerAction], for exhaustive dispatch without a type check per branch. */
public enum class ActionType { FOLD, CHECK, CALL, BET, RAISE, ALL_IN }

/**
 * A closed set of the six things a player can attempt on their turn.
 *
 * This is the input contract only: whether the seat index is even in range, and whether the
 * action is currently legal, are questions for the engine to answer elsewhere, not for this
 * type to enforce.
 */
@Serializable
public sealed interface PlayerAction {
    /** The seat attempting the action: 0 or 1. Whether it is that seat's turn is the engine's business. */
    public val seat: Int

    /** The tag identifying which of the six actions this is. */
    public val type: ActionType

    /** Give up the hand and forfeit any claim to the pot. */
    @Serializable
    @SerialName("Fold")
    public data class Fold(override val seat: Int) : PlayerAction {
        override val type: ActionType get() = ActionType.FOLD
    }

    /** Decline to bet when there is nothing to call. */
    @Serializable
    @SerialName("Check")
    public data class Check(override val seat: Int) : PlayerAction {
        override val type: ActionType get() = ActionType.CHECK
    }

    /**
     * Match the outstanding bet. Carries no amount: the amount owed is a function of the
     * game state, and letting a client name it is letting a client assert a game fact.
     */
    @Serializable
    @SerialName("Call")
    public data class Call(override val seat: Int) : PlayerAction {
        override val type: ActionType get() = ActionType.CALL
    }

    /**
     * Open the betting on a street with no bet outstanding.
     *
     * [to] is the street total the seat's contribution will reach, never an increment. Facing
     * no bet and opening for 300, this is `Bet(seat, to = 300)`.
     */
    @Serializable
    @SerialName("Bet")
    public data class Bet(override val seat: Int, val to: Int) : PlayerAction {
        init {
            require(to > 0) { "A bet must be positive, was $to" }
        }

        override val type: ActionType get() = ActionType.BET
    }

    /**
     * Increase a bet already facing the seat.
     *
     * [to] is the street total the seat's contribution will reach, never an increment. Facing a
     * bet of 10 raised to 40, the next raise is `Raise(seat, to = 70)` — it is not `Raise(70)`
     * meaning "70 more".
     */
    @Serializable
    @SerialName("Raise")
    public data class Raise(override val seat: Int, val to: Int) : PlayerAction {
        init {
            require(to > 0) { "A raise must be positive, was $to" }
        }

        override val type: ActionType get() = ActionType.RAISE
    }

    /**
     * Commit the seat's entire remaining stack. Carries no amount: the amount is a function of
     * the game state, and letting a client name it is letting a client assert a game fact.
     */
    @Serializable
    @SerialName("AllIn")
    public data class AllIn(override val seat: Int) : PlayerAction {
        override val type: ActionType get() = ActionType.ALL_IN
    }
}
