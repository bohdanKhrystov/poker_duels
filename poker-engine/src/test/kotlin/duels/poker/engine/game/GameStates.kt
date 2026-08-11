package duels.poker.engine.game

import duels.poker.engine.card.Deck
import duels.poker.engine.random.SplitMix64Rng

/**
 * Test-only helpers for building GameState instances with sensible defaults.
 */

internal const val SMALL_BLIND: Int = 50
internal const val BIG_BLIND: Int = 100
internal const val START_STACK: Int = 10_000
internal const val TEST_SEED: Long = 1L

/**
 * Two untouched seats with the given stacks.
 */
internal fun seats(stack0: Int = START_STACK, stack1: Int = START_STACK): List<Seat> {
    return listOf(
        Seat(index = 0, stack = stack0),
        Seat(index = 1, stack = stack1),
    )
}

/**
 * A fresh preflop state: hand 1, button on seat 0, empty board, empty pot, no bet to match,
 * `minRaiseTo = BIG_BLIND`, `seatToAct = 0`, `eventCount = 0`, a full deck and
 * `SplitMix64Rng(TEST_SEED)`.
 * Vary anything else with `copy` — it is a data class.
 */
internal fun handState(seats: List<Seat> = seats()): GameState {
    return GameState(
        handNumber = 1,
        buttonSeat = 0,
        street = Street.PREFLOP,
        seats = seats,
        board = Board.EMPTY,
        pot = 0,
        betToMatch = 0,
        minRaiseTo = BIG_BLIND,
        seatToAct = 0,
        smallBlind = SMALL_BLIND,
        bigBlind = BIG_BLIND,
        eventCount = 0,
        deck = Deck.full(),
        rng = SplitMix64Rng(TEST_SEED),
    )
}
