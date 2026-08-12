package duels.poker.engine.game

import duels.poker.engine.card.cards
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A closed betting round sweeps the chips, deals the next street and puts the non-button on
 * turn — or, on the river, reaches showdown. `TASK-010517`.
 */
class StreetAdvanceTest {
    private fun openedHand(): GameState =
        startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L)).newState

    /** Preflop closes: the button calls the big blind, the big blind checks its option. */
    private fun afterPreflop(): EngineResult {
        val afterCall = DefaultPokerEngine.handle(openedHand(), PlayerAction.Call(0))
        return DefaultPokerEngine.handle(afterCall.newState, PlayerAction.Check(1))
    }

    /** Flop closes: the non-button checks first, the button checks behind. */
    private fun afterFlop(): EngineResult {
        val afterCheck1 = DefaultPokerEngine.handle(afterPreflop().newState, PlayerAction.Check(1))
        return DefaultPokerEngine.handle(afterCheck1.newState, PlayerAction.Check(0))
    }

    /**
     * A river position built directly rather than played to, per the ticket: both seats already
     * hold chips, nobody owes anything, and the non-button is on turn as [firstToActOn] requires.
     */
    private fun riverState(): GameState {
        return handState().copy(
            street = Street.RIVER,
            board = Board(cards("As Kd 7c 2h 9s")),
            pot = 600,
            betToMatch = 0,
            minRaiseTo = 100,
            seatToAct = 1,
            seats = listOf(
                Seat(index = 0, stack = 9_700, committedThisStreet = 0, committedThisHand = 300),
                Seat(index = 1, stack = 9_700, committedThisStreet = 0, committedThisHand = 300),
            ),
        )
    }

    @Test
    fun aFinishedPreflopDealsThreeFlopCards() {
        val result = afterPreflop()

        assertEquals(3, result.newState.board.size)
        assertEquals(Street.FLOP, result.newState.street)
    }

    @Test
    fun theEventOrderIsEndedThenDealtThenActionOn() {
        val result = afterPreflop()

        assertEquals(4, result.events.size)
        assertTrue(result.events[0] is PlayerChecked)

        val ended = result.events[1]
        assertTrue(ended is BettingRoundEnded)
        assertEquals(Street.PREFLOP, (ended as BettingRoundEnded).street)

        val dealt = result.events[2]
        assertTrue(dealt is StreetDealt)
        assertEquals(Street.FLOP, (dealt as StreetDealt).street)
        assertEquals(3, dealt.cards.size)

        val actionOn = result.events[3]
        assertTrue(actionOn is ActionOn)
        assertEquals(1, (actionOn as ActionOn).seat)
    }

    @Test
    fun theNonButtonActsFirstOnTheFlop() {
        val result = afterPreflop()

        assertEquals(1, result.newState.seatToAct)
    }

    @Test
    fun everyCommitmentIsSweptAndTheBarResets() {
        val result = afterPreflop()

        assertEquals(200, result.newState.pot)
        assertEquals(0, result.newState.betToMatch)
        assertEquals(100, result.newState.minRaiseTo)
        assertEquals(0, result.newState.seat(0).committedThisStreet)
        assertEquals(0, result.newState.seat(1).committedThisStreet)
    }

    @Test
    fun committedThisHandSurvivesTheAdvance() {
        val result = afterPreflop()

        assertEquals(100, result.newState.seat(0).committedThisHand)
        assertEquals(100, result.newState.seat(1).committedThisHand)
    }

    @Test
    fun theDeckShrinksByExactlyTheCardsDealt() {
        assertEquals(48, openedHand().deck.remaining)
        assertEquals(45, afterPreflop().newState.deck.remaining)
        assertEquals(44, afterFlop().newState.deck.remaining)
    }

    @Test
    fun noCardAppearsTwice() {
        val state = afterFlop().newState

        val allCards = state.seat(0).holeCards + state.seat(1).holeCards + state.board.cards

        assertEquals(8, allCards.size)
        assertEquals(8, allCards.toSet().size)
    }

    @Test
    fun theRiverEndsInShowdownReached() {
        val afterCheck1 = DefaultPokerEngine.handle(riverState(), PlayerAction.Check(1))
        val result = DefaultPokerEngine.handle(afterCheck1.newState, PlayerAction.Check(0))

        assertEquals(3, result.events.size)
        assertTrue(result.events[0] is PlayerChecked)
        assertTrue(result.events[1] is BettingRoundEnded)
        assertTrue(result.events[2] is ShowdownReached)
        assertTrue(result.events.none { it is StreetDealt })

        assertEquals(Street.SHOWDOWN, result.newState.street)
        assertNull(result.newState.seatToAct)
    }

    @Test
    fun chipsAreConservedAcrossTheAdvance() {
        val opened = openedHand()
        assertEquals(20_000, opened.chipsInPlay)

        val afterCall = DefaultPokerEngine.handle(opened, PlayerAction.Call(0))
        assertEquals(20_000, afterCall.newState.chipsInPlay)

        val afterPreflopCheck = DefaultPokerEngine.handle(afterCall.newState, PlayerAction.Check(1))
        assertEquals(20_000, afterPreflopCheck.newState.chipsInPlay)

        val afterFlopCheck1 = DefaultPokerEngine.handle(afterPreflopCheck.newState, PlayerAction.Check(1))
        assertEquals(20_000, afterFlopCheck1.newState.chipsInPlay)

        val afterFlopCheck0 = DefaultPokerEngine.handle(afterFlopCheck1.newState, PlayerAction.Check(0))
        assertEquals(20_000, afterFlopCheck0.newState.chipsInPlay)

        val river = riverState()
        assertEquals(20_000, river.chipsInPlay)

        val afterRiverCheck1 = DefaultPokerEngine.handle(river, PlayerAction.Check(1))
        assertEquals(20_000, afterRiverCheck1.newState.chipsInPlay)

        val afterRiverCheck0 = DefaultPokerEngine.handle(afterRiverCheck1.newState, PlayerAction.Check(0))
        assertEquals(20_000, afterRiverCheck0.newState.chipsInPlay)
    }

    @Test
    fun theEventsDescribeTheTransition() {
        val afterCall = DefaultPokerEngine.handle(openedHand(), PlayerAction.Call(0))
        val preflopEnd = DefaultPokerEngine.handle(afterCall.newState, PlayerAction.Check(1))
        assertEventsDescribeTheTransition(afterCall.newState, preflopEnd)

        val flopCheck1 = DefaultPokerEngine.handle(preflopEnd.newState, PlayerAction.Check(1))
        val flopEnd = DefaultPokerEngine.handle(flopCheck1.newState, PlayerAction.Check(0))
        assertEventsDescribeTheTransition(flopCheck1.newState, flopEnd)

        val river = riverState()
        val riverCheck1 = DefaultPokerEngine.handle(river, PlayerAction.Check(1))
        val riverEnd = DefaultPokerEngine.handle(riverCheck1.newState, PlayerAction.Check(0))
        assertEventsDescribeTheTransition(riverCheck1.newState, riverEnd)
    }
}
