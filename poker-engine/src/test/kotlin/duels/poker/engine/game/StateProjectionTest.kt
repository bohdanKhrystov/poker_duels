package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class StateProjectionTest {
    @Test
    fun foldingNoEventsChangesNothing() {
        val state = handState()
        assertEquals(state, StateProjection.fold(state, emptyList()))
    }

    @Test
    fun handStartedResetsToAFreshPreflopPosition() {
        val midHand = handState().copy(
            street = Street.FLOP,
            board = Board(cards("As Kd 2h")),
            pot = 500,
            betToMatch = 200,
            minRaiseTo = 400,
            seatToAct = 1,
            seats = listOf(
                Seat(index = 0, stack = 8_000, committedThisStreet = 100, committedThisHand = 300),
                Seat(index = 1, stack = 7_000, committedThisStreet = 200, committedThisHand = 400),
            ),
        )
        val event = HandStarted(0, 7, 1, 50, 100, listOf(9_000, 11_000))

        val result = StateProjection.apply(midHand, event)

        assertEquals(7, result.handNumber)
        assertEquals(1, result.buttonSeat)
        assertEquals(Street.PREFLOP, result.street)
        assertEquals(Board.EMPTY, result.board)
        assertEquals(0, result.pot)
        assertEquals(0, result.betToMatch)
        assertEquals(100, result.minRaiseTo)
        assertEquals(50, result.smallBlind)
        assertEquals(100, result.bigBlind)
        assertEquals(9_000, result.seat(0).stack)
        assertEquals(11_000, result.seat(1).stack)
        assertEquals(0, result.seat(0).committedThisStreet)
        assertEquals(0, result.seat(0).committedThisHand)
        assertEquals(0, result.seat(1).committedThisStreet)
        assertEquals(0, result.seat(1).committedThisHand)
    }

    @Test
    fun handStartedKeepsTheDeckAndTheGenerator() {
        val state = handState()
        val event = HandStarted(0, 7, 1, 50, 100, listOf(9_000, 11_000))

        val result = StateProjection.apply(state, event)

        assertSame(state.deck, result.deck)
        assertSame(state.rng, result.rng)
    }

    @Test
    fun blindsLeaveTheBarAtTheBigBlind() {
        val state = handState()
        val chipsBefore = state.chipsInPlay

        val afterSmall = StateProjection.apply(state, BlindPosted(1, 0, 50, false))
        val afterBig = StateProjection.apply(afterSmall, BlindPosted(2, 1, 100, true))

        assertEquals(100, afterBig.betToMatch)
        assertEquals(200, afterBig.minRaiseTo)
        assertEquals(50, afterBig.seat(0).committedThisStreet)
        assertEquals(100, afterBig.seat(1).committedThisStreet)
        assertEquals(0, afterBig.pot)
        assertEquals(chipsBefore, afterBig.chipsInPlay)
    }

    @Test
    fun holeCardsGoToTheirSeat() {
        val state = handState()
        val dealt = cards("As Kd")

        val result = StateProjection.apply(state, HoleCardsDealt(3, 1, dealt))

        assertEquals(dealt, result.seat(1).holeCards)
        assertEquals(emptyList<Any>(), result.seat(0).holeCards)
    }

    @Test
    fun actionOnNamesTheSeatToAct() {
        val state = handState()

        val result = StateProjection.apply(state, ActionOn(5, 1))

        assertEquals(1, result.seatToAct)
    }

    @Test
    fun everyAppliedEventAdvancesTheEventCount() {
        val state = handState()

        assertEquals(6, StateProjection.apply(state, ActionOn(5, 1)).eventCount)

        val events = (0..5).map { ActionOn(it, it % 2) }
        assertEquals(6, StateProjection.fold(state, events).eventCount)
    }

    @Test
    fun bettingAndDealerEventsAreDispatched() {
        val bettingState = handState()
        val bettingEvent = PlayerBet(1, 0, 300)
        assertEquals(
            applyBetting(bettingState, bettingEvent).copy(eventCount = 2),
            StateProjection.apply(bettingState, bettingEvent),
        )

        val dealerState = handState()
        val dealerEvent = StreetDealt(2, Street.FLOP, cards("2c 3d 4h"))
        assertEquals(
            applyDealer(dealerState, dealerEvent).copy(eventCount = 3),
            StateProjection.apply(dealerState, dealerEvent),
        )
    }

    @Test
    fun foldReplaysAnOpeningInOrder() {
        val heroCards = cards("As Kd")
        val villainCards = cards("2c 3d")
        val events = listOf(
            HandStarted(0, 1, 0, 50, 100, listOf(10_000, 10_000)),
            BlindPosted(1, 0, 50, false),
            BlindPosted(2, 1, 100, true),
            HoleCardsDealt(3, 0, heroCards),
            HoleCardsDealt(4, 1, villainCards),
            ActionOn(6, 0),
        )

        val result = StateProjection.fold(handState(), events)

        assertEquals(heroCards, result.seat(0).holeCards)
        assertEquals(villainCards, result.seat(1).holeCards)
        assertEquals(100, result.betToMatch)
        assertEquals(0, result.seatToAct)
    }
}
