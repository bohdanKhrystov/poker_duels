package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A tied showdown, played through [DefaultPokerEngine], reveals both hands in the order the
 * betting decided, per [ADR-0008](../../../../../../../docs/adr/ADR-0008-loser-mucks-at-showdown.md).
 *
 * This pins the whole path rather than a hand-built state: a real bet sets [GameState.lastAggressor],
 * and a real showdown reads it back through [revealOrder]. `ShowdownRevealTest` pins the rule in
 * isolation; this pins where it is actually observable.
 */
class ShowdownRevealOrderTest {
    /**
     * A river both seats tie on: the board alone makes the broadway straight, and neither hole
     * card improves it or makes a flush. Built directly, per the ticket, in [StreetAdvanceTest]'s
     * `riverState()` style.
     */
    private fun riverTieState(): GameState {
        return handState().copy(
            street = Street.RIVER,
            board = Board(cards("As Kd Qc Jh Ts")),
            pot = 600,
            betToMatch = 0,
            minRaiseTo = 100,
            seatToAct = 1,
            seats = listOf(
                Seat(index = 0, stack = 9_700, holeCards = cards("2c 3d"), committedThisStreet = 0, committedThisHand = 300),
                Seat(index = 1, stack = 9_700, holeCards = cards("4h 5c"), committedThisStreet = 0, committedThisHand = 300),
            ),
        )
    }

    /** Seat 0, the button, bets the river and seat 1 calls: seat 0 becomes the last aggressor. */
    private fun betAndCall(): EngineResult {
        val afterCheck1 = DefaultPokerEngine.handle(riverTieState(), PlayerAction.Check(1))
        val afterBet0 = DefaultPokerEngine.handle(afterCheck1.newState, PlayerAction.Bet(0, 200))
        return DefaultPokerEngine.handle(afterBet0.newState, PlayerAction.Call(1))
    }

    /** The river is checked through: no aggressor survives to showdown. */
    private fun checkedThrough(): EngineResult {
        val afterCheck1 = DefaultPokerEngine.handle(riverTieState(), PlayerAction.Check(1))
        return DefaultPokerEngine.handle(afterCheck1.newState, PlayerAction.Check(0))
    }

    @Test
    fun aTieRevealsBothHands() {
        val result = checkedThrough()

        val revealed = result.events.filterIsInstance<HandRevealed>()
        assertEquals(2, revealed.size)
        assertEquals(setOf(0, 1), revealed.map { it.seat }.toSet())
        assertEquals(cards("2c 3d"), revealed.first { it.seat == 0 }.cards)
        assertEquals(cards("4h 5c"), revealed.first { it.seat == 1 }.cards)

        // A genuine tie, not a trusted fixture: the split is even, not a single winner's full pot.
        val awarded = result.events.filterIsInstance<PotAwarded>()
        assertEquals(listOf(300, 300), awarded.map { it.amount }.sorted())
    }

    @Test
    fun theRiverAggressorShowsFirst() {
        val result = betAndCall()

        val revealed = result.events.filterIsInstance<HandRevealed>()
        assertEquals(listOf(0, 1), revealed.map { it.seat })
    }

    @Test
    fun aCheckedRiverShowsTheSeatOutOfPositionFirst() {
        val result = checkedThrough()

        val revealed = result.events.filterIsInstance<HandRevealed>()
        assertEquals(listOf(1, 0), revealed.map { it.seat })
    }

    @Test
    fun bothHandsShowBeforeThePotIsSplit() {
        val result = betAndCall()

        val lastRevealedIndex = result.events.indexOfLast { it is HandRevealed }
        val firstAwardedIndex = result.events.indexOfFirst { it is PotAwarded }
        assertTrue(lastRevealedIndex >= 0)
        assertTrue(firstAwardedIndex >= 0)
        assertTrue(lastRevealedIndex < firstAwardedIndex)

        val awarded = result.events.filterIsInstance<PotAwarded>()
        assertEquals(listOf(500, 500), awarded.map { it.amount }.sorted())
    }
}
