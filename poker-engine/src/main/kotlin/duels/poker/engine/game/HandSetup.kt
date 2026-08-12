package duels.poker.engine.game

import duels.poker.engine.card.Deck
import duels.poker.engine.random.Rng

/**
 * Opens a hand: the button posts the small blind, the other seat posts the big blind, a fresh
 * deck is shuffled for the hand — this is the deck's only shuffle point — and both seats are
 * dealt their hole cards, dealt one at a time starting with the big blind, the seat left of the
 * button. Action then opens on the button, which acts first preflop heads-up. A seat too short
 * for its blind posts all-in for its whole stack.
 *
 * A seat that posts its blind all-in can leave the seat named by [firstToActOn] with no [ActionOn]
 * it could answer: folded, all-in, or out of chips, none of which happen this early to the acting
 * seat except by going all-in on its own blind. Only the seat on turn is checked — an opponent
 * that is itself all-in for less does not deadlock anything, because the seat on turn still has a
 * real, if restricted, decision (`TASK-010509`), and taking that decision away by running out
 * would be a worse bug than the one this guards against. When the seat on turn cannot act, no
 * [ActionOn] is emitted; instead [endBettingRound] sweeps the blinds and runs the board out to
 * [ShowdownReached], exactly as it would if the round had ended mid-hand.
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
    val deal = shuffle.deck.deal(4)
    val dealt = deal.cards

    val smallBlindIndex = smallBlindSeat(buttonSeat)
    val bigBlindIndex = bigBlindSeat(buttonSeat)

    val dealingEvents = listOf(
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
        HoleCardsDealt(
            sequence = 3,
            seat = bigBlindIndex,
            cards = listOf(dealt[0], dealt[2]),
        ),
        HoleCardsDealt(
            sequence = 4,
            seat = smallBlindIndex,
            cards = listOf(dealt[1], dealt[3]),
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

    val afterDealing = StateProjection.fold(opening, dealingEvents).copy(deck = deal.deck, rng = shuffle.rng)

    // Only the seat about to receive ActionOn matters: an opponent that is itself all-in for
    // less still leaves the seat on turn a real decision, so checking both seats here would take
    // a genuine turn away from a player with chips — see the KDoc above.
    val seatOnTurn = firstToActOn(Street.PREFLOP, buttonSeat)
    val seatOnTurnCanAct = afterDealing.seat(seatOnTurn).let { !it.hasFolded && !it.isAllIn && it.stack > 0 }

    val (events, newState) = if (seatOnTurnCanAct) {
        val actionEvent = ActionOn(sequence = afterDealing.eventCount, seat = seatOnTurn)
        (dealingEvents + actionEvent) to StateProjection.apply(afterDealing, actionEvent)
    } else {
        val runOut = endBettingRound(afterDealing)
        (dealingEvents + runOut.events) to runOut.newState
    }

    return EngineResult.accepted(newState, events)
}
