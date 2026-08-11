package duels.poker.engine.game

import duels.poker.engine.card.Card

/** Something the house did: no seat chose it. The projection dispatches on this sub-interface. */
public sealed interface DealerEvent : GameEvent

/** The betting on [street] is closed; every commitment goes to the pot. */
public data class BettingRoundEnded(override val sequence: Int, val street: Street) : DealerEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(street.isBetting) { "street must be a betting street, was $street" }
    }
}

/** [cards] join the board and the hand moves to [street]: three for the flop, one otherwise. */
public data class StreetDealt(
    override val sequence: Int,
    val street: Street,
    val cards: List<Card>,
) : DealerEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        val expectedSize = when (street) {
            Street.FLOP -> 3
            Street.TURN, Street.RIVER -> 1
            else -> throw IllegalArgumentException(
                "street must be FLOP, TURN or RIVER, was $street",
            )
        }
        require(cards.size == expectedSize) {
            "cards must have exactly $expectedSize entries for $street, had ${cards.size}"
        }
        require(cards.toSet().size == cards.size) { "cards must be distinct, were $cards" }
    }
}

public data class ShowdownReached(override val sequence: Int) : DealerEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
    }
}

/**
 * A seat that showed its hand. Never emitted for a fold or a muck: a folded or mucked hand
 * appears in no event, anywhere, so the engine emits this only for a hand actually shown.
 */
public data class HandRevealed(
    override val sequence: Int,
    val seat: Int,
    val cards: List<Card>,
) : DealerEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
        require(cards.size == 2) { "cards must have exactly 2 entries, had ${cards.size}" }
        require(cards.toSet().size == cards.size) { "cards must be distinct, were $cards" }
    }
}

/**
 * Chips nobody could cover, going back to the seat that bet them. Always before [PotAwarded].
 * The [amount] is a delta of chips moving out of the pot, not a street total.
 */
public data class UncalledBetReturned(
    override val sequence: Int,
    val seat: Int,
    val amount: Int,
) : DealerEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
        require(amount > 0) { "amount must be positive, was $amount" }
    }
}

/**
 * Part or all of the pot going to a seat. A split pot emits one of these per seat.
 * The [amount] is a delta of chips moving out of the pot, not a street total.
 */
public data class PotAwarded(
    override val sequence: Int,
    val seat: Int,
    val amount: Int,
) : DealerEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
        require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
        require(amount > 0) { "amount must be positive, was $amount" }
    }
}

/** The hand is over. Always the last event of a hand. */
public data class HandFinished(override val sequence: Int) : DealerEvent {
    init {
        require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
    }
}
