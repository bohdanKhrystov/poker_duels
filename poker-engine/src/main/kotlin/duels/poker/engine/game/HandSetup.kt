package duels.poker.engine.game

import duels.poker.engine.card.Deck
import duels.poker.engine.random.Rng

/**
 * Opens a hand: the button posts the small blind, the other seat posts the big blind, and a
 * fresh deck is shuffled for the hand — this is the deck's only shuffle point. A seat too short
 * for its blind posts all-in for its whole stack.
 *
 * Hole cards and the first [ActionOn] are not part of this function; see `TASK-010507`.
 *
 * @param handNumber the 1-based index of this hand within the match
 * @param buttonSeat the seat holding the button this hand: 0 or 1
 * @param stacks both seats' starting stacks, indexed by seat; every seat needs at least one chip
 * @param smallBlind the small blind amount for this hand
 * @param bigBlind the big blind amount for this hand
 * @param rng the source of randomness this hand draws its shuffle from
 * @return the opened hand's state and the events that produced it
 */
public fun startHand(
    handNumber: Int,
    buttonSeat: Int,
    stacks: List<Int>,
    smallBlind: Int,
    bigBlind: Int,
    rng: Rng,
): EngineResult {
    require(stacks.size == 2) { "stacks must have exactly 2 entries, had ${stacks.size}" }
    require(stacks.all { it >= 1 }) { "every seat needs at least one chip to start a hand, had $stacks" }

    val shuffle = Deck.full().shuffled(rng)

    val smallBlindIndex = smallBlindSeat(buttonSeat)
    val bigBlindIndex = bigBlindSeat(buttonSeat)

    val events = listOf(
        HandStarted(
            sequence = 0,
            handNumber = handNumber,
            buttonSeat = buttonSeat,
            smallBlind = smallBlind,
            bigBlind = bigBlind,
            stacks = stacks,
        ),
        BlindPosted(
            sequence = 1,
            seat = smallBlindIndex,
            to = minOf(smallBlind, stacks[smallBlindIndex]),
            isBigBlind = false,
        ),
        BlindPosted(
            sequence = 2,
            seat = bigBlindIndex,
            to = minOf(bigBlind, stacks[bigBlindIndex]),
            isBigBlind = true,
        ),
    )

    val opening = GameState(
        handNumber = handNumber,
        buttonSeat = buttonSeat,
        street = Street.PREFLOP,
        seats = stacks.mapIndexed { index, stack -> Seat(index = index, stack = stack) },
        board = Board.EMPTY,
        pot = 0,
        betToMatch = 0,
        minRaiseTo = bigBlind,
        seatToAct = null,
        smallBlind = smallBlind,
        bigBlind = bigBlind,
        eventCount = 0,
        deck = shuffle.deck,
        rng = shuffle.rng,
    )

    val folded = StateProjection.fold(opening, events)
    val newState = folded.copy(deck = shuffle.deck, rng = shuffle.rng)

    return EngineResult.accepted(newState, events)
}
