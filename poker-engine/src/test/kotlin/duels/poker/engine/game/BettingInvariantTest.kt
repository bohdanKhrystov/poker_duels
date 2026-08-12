package duels.poker.engine.game

import duels.poker.engine.card.Deck
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * A thousand hands nobody sat down and designed, each reproducible from its seed alone: the last
 * ticket of `STORY-0105` proves the betting invariants hold not only for the hands somebody
 * thought to write down.
 */
class BettingInvariantTest {

    @Test
    @Timeout(30)
    fun aThousandRandomHandsHoldEveryInvariant() {
        for (seed in 1L..1000L) {
            playRandomHand(seed)
        }
    }

    @Test
    @Timeout(30)
    fun everyRandomHandEndsWhereNoActionIsLeft() {
        for (seed in 1L..1000L) {
            val played = playRandomHand(seed)
            val finalState = played.finalState

            // Assert no seat is to act
            assertEquals(null, finalState.seatToAct, "seed $seed: expected seatToAct to be null")

            // Assert no legal actions remain
            val legal = legalActions(finalState)
            assertTrue(legal.allowed.isEmpty(), "seed $seed: expected no legal actions, got ${legal.allowed}")

            // Assert every action is rejected by the engine
            val allActions = listOf(
                PlayerAction.Fold(0),
                PlayerAction.Fold(1),
                PlayerAction.Check(0),
                PlayerAction.Check(1),
                PlayerAction.Call(0),
                PlayerAction.Call(1),
                PlayerAction.AllIn(0),
                PlayerAction.AllIn(1),
            )
            for (action in allActions) {
                val result = DefaultPokerEngine.handle(finalState, action)
                assertTrue(result.isRejected, "seed $seed: expected $action to be rejected")
            }
        }
    }

    @Test
    fun everyRandomHandsLogReproducesItsFinalState() {
        for (seed in 1L..50L) {
            val played = playRandomHand(seed)
            val replayed = StateProjection.fold(played.opening, played.events)

            // The fold never touches deck or rng — no event carries either — so both sides are
            // normalized to a shared, arbitrary value before comparing the rest of the state.
            val normalize: (GameState) -> GameState = { it.copy(deck = Deck.full(), rng = SplitMix64Rng(0)) }
            assertEquals(normalize(played.finalState), normalize(replayed), "seed $seed")
        }
    }

    @Test
    fun theSameSeedPlaysTheSameHandTwice() {
        val first = playRandomHand(7L)
        val second = playRandomHand(7L)

        assertEquals(first.actions, second.actions)
        assertEquals(first.finalState, second.finalState)
    }

    @Test
    fun aHandThatCannotProgressNamesItsSeed() {
        val error = assertThrows(AssertionError::class.java) {
            playRandomHand(3L, engine = NoOpEngine)
        }

        assertTrue(error.message.orEmpty().contains("3"), "expected the seed in the message, was: ${error.message}")
    }
}
