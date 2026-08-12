package duels.poker.engine.game

import duels.poker.engine.card.cards
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Once fewer than two seats can still act — both all-in, or one all-in and the other with nobody
 * to bet into — `continueHand` deals the rest of the board in one step and reaches showdown
 * without ever asking anybody anything. `TASK-010518`.
 */
class AllInRunOutTest {
    private fun openedHand(): GameState =
        startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L)).newState

    /**
     * Both seats shove preflop: seat 0 goes all-in, seat 1 calls for the whole of its own stack.
     * Returns the state right before the call — the action that triggers the run-out — paired
     * with the result of that call.
     */
    private fun bothAllInPreflop(): Pair<GameState, EngineResult> {
        val afterAllIn = DefaultPokerEngine.handle(openedHand(), PlayerAction.AllIn(0))
        val result = DefaultPokerEngine.handle(afterAllIn.newState, PlayerAction.Call(1))
        return afterAllIn.newState to result
    }

    /**
     * A turn position built directly, the way `StreetAdvanceTest`'s river fixture is: seat 1 is
     * already all-in and matched, seat 0 still holds chips and is the only seat with a decision.
     */
    private fun turnWithSeatOneAllIn(): GameState {
        return handState().copy(
            street = Street.TURN,
            board = Board(cards("As Kd 7c 2h")),
            pot = 11_000,
            betToMatch = 0,
            minRaiseTo = 100,
            seatToAct = 0,
            seats = listOf(
                Seat(index = 0, stack = 9_000, holeCards = cards("Qs Jh"), committedThisStreet = 0, committedThisHand = 1_000),
                Seat(
                    index = 1,
                    stack = 0,
                    holeCards = cards("9d 9c"),
                    committedThisStreet = 0,
                    committedThisHand = 10_000,
                    isAllIn = true,
                ),
            ),
        )
    }

    @Test
    fun bothAllInPreflopRunsOutFiveCards() {
        val (_, result) = bothAllInPreflop()

        assertEquals(5, result.newState.board.size)
        assertEquals(Street.COMPLETE, result.newState.street)
        assertTrue(result.events.any { it is ShowdownReached })
        assertEquals(0, result.newState.pot)
        assertEquals(20_000, result.newState.seat(0).stack + result.newState.seat(1).stack)
    }

    @Test
    fun theRunOutDealsEachStreetInOrder() {
        val (_, result) = bothAllInPreflop()

        val dealerEvents = result.events.filterIsInstance<DealerEvent>()
        assertEquals(8, dealerEvents.size)

        assertTrue(dealerEvents[0] is BettingRoundEnded)

        val flop = dealerEvents[1]
        assertTrue(flop is StreetDealt)
        assertEquals(Street.FLOP, (flop as StreetDealt).street)
        assertEquals(3, flop.cards.size)

        val turn = dealerEvents[2]
        assertTrue(turn is StreetDealt)
        assertEquals(Street.TURN, (turn as StreetDealt).street)
        assertEquals(1, turn.cards.size)

        val river = dealerEvents[3]
        assertTrue(river is StreetDealt)
        assertEquals(Street.RIVER, (river as StreetDealt).street)
        assertEquals(1, river.cards.size)

        assertTrue(dealerEvents[4] is ShowdownReached)

        val revealed = dealerEvents[5]
        assertTrue(revealed is HandRevealed)
        val awarded = dealerEvents.subList(6, dealerEvents.size - 1)
        assertTrue(awarded.all { it is PotAwarded })
        assertEquals((revealed as HandRevealed).seat, (awarded.single() as PotAwarded).seat)
        assertTrue(dealerEvents.last() is HandFinished)
    }

    @Test
    fun theRunOutEmitsNoActionOn() {
        val (_, result) = bothAllInPreflop()

        assertTrue(result.events.none { it is ActionOn })
        assertNull(result.newState.seatToAct)
    }

    @Test
    fun aShorterStackAllInStillRunsOut() {
        val opened = startHand(1, 0, listOf(4_000, 10_000), 50, 100, SplitMix64Rng(1L)).newState
        val afterAllIn = DefaultPokerEngine.handle(opened, PlayerAction.AllIn(0))
        val result = DefaultPokerEngine.handle(afterAllIn.newState, PlayerAction.Call(1))

        assertTrue(result.newState.seat(1).stack > 0)
        assertEquals(5, result.newState.board.size)
        assertEquals(Street.COMPLETE, result.newState.street)
        assertEquals(0, result.newState.pot)
        assertEquals(14_000, result.newState.seat(0).stack + result.newState.seat(1).stack)
    }

    @Test
    fun aRunOutFromTheTurnDealsOnlyTheRiver() {
        val result = DefaultPokerEngine.handle(turnWithSeatOneAllIn(), PlayerAction.Check(0))

        val streetsDealt = result.events.filterIsInstance<StreetDealt>()
        assertEquals(1, streetsDealt.size)
        assertEquals(Street.RIVER, streetsDealt[0].street)
        assertEquals(1, streetsDealt[0].cards.size)

        assertTrue(result.events.any { it is ShowdownReached })
        assertTrue(result.events.last() is HandFinished)

        val returned = result.events.filterIsInstance<UncalledBetReturned>().single()
        assertEquals(1, returned.seat)
        assertEquals(9_000, returned.amount)

        val awarded = result.events.filterIsInstance<PotAwarded>().single()
        assertEquals(1, awarded.seat)
        assertEquals(2_000, awarded.amount)

        assertEquals(9_000, result.newState.seat(0).stack)
        assertEquals(11_000, result.newState.seat(1).stack)
        assertEquals(5, result.newState.board.size)
        assertEquals(Street.COMPLETE, result.newState.street)
    }

    @Test
    fun noCardIsDealtTwiceInARunOut() {
        val (_, result) = bothAllInPreflop()

        val holeCards = result.newState.seat(0).holeCards + result.newState.seat(1).holeCards
        val allCards = holeCards + result.newState.board.cards

        assertEquals(9, allCards.size)
        assertEquals(9, allCards.toSet().size)
        assertEquals(43, result.newState.deck.remaining)
    }

    @Test
    fun chipsAreConservedByTheRunOut() {
        val (before, result) = bothAllInPreflop()

        assertEquals(20_000, before.chipsInPlay)
        assertEquals(20_000, result.newState.chipsInPlay)
        assertEquals(result.newState.potTotal, result.newState.pot)
    }

    @Test
    fun theEventsDescribeTheTransition() {
        val (before, result) = bothAllInPreflop()

        assertEventsDescribeTheTransition(before, result)
    }
}
