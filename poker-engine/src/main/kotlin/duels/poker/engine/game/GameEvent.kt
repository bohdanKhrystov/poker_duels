package duels.poker.engine.game

import duels.poker.engine.card.Card
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Payload version of every event in this module. Bumping it is a log migration. */
public const val EVENT_SCHEMA_VERSION: Int = 1

/**
 * The durable, versioned record of everything the engine has ever produced. Three rules hold
 * for every event this module ever adds:
 *
 * 1. Events are **facts, not instructions** — `PlayerBet(seat, to)`, never `ShowBetAnimation`.
 * 2. No event carries an undealt or unrevealed card, ever. This is a project non-negotiable,
 *    and it is why [HandStarted] carries neither the shuffled deck nor the seed.
 * 3. [version] is a property with a default, not a constructor parameter, so it never takes
 *    part in equality but is always available to a serializer (`TASK-010801`).
 */
@Serializable
public sealed interface GameEvent {
    /**
     * Position within the hand, starting at 0, dense and gap-free: the next event a hand
     * produces always has `sequence == state.eventCount`.
     */
    public val sequence: Int

    public val version: Int get() = EVENT_SCHEMA_VERSION
}

/** Opens a hand: the button, the blind sizes, and every seat's starting stack. */
@Serializable
@SerialName("HandStarted")
public data class HandStarted(
    override val sequence: Int,
    val handNumber: Int,
    val buttonSeat: Int,
    val smallBlind: Int,
    val bigBlind: Int,
    val stacks: List<Int>,
) : GameEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(stacks.size == 2) { "stacks must have exactly 2 entries, had ${stacks.size}" }
        require(buttonSeat in 0..1) { "buttonSeat must be 0 or 1, was $buttonSeat" }
    }
}

/** [to] is the seat's street total after posting; a short stack posts all-in for less. */
@Serializable
@SerialName("BlindPosted")
public data class BlindPosted(
    override val sequence: Int,
    val seat: Int,
    val to: Int,
    val isBigBlind: Boolean,
) : GameEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
        require(to > 0) { "to must be positive, was $to" }
    }
}

/** Addressed to one seat, so a broadcast filters by recipient rather than by field. */
@Serializable
@SerialName("HoleCardsDealt")
public data class HoleCardsDealt(
    override val sequence: Int,
    val seat: Int,
    val cards: List<Card>,
) : GameEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
        require(cards.size == 2) { "cards must have exactly 2 entries, had ${cards.size}" }
        require(cards.toSet().size == cards.size) { "cards must be distinct, were $cards" }
    }
}

@Serializable
@SerialName("ActionOn")
public data class ActionOn(override val sequence: Int, val seat: Int) : GameEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
    }
}
