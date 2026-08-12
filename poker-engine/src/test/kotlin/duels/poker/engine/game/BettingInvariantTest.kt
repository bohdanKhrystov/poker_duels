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
    fun everyRandomHandEndsAtShowdownOrWithAFold() {
        for (seed in 1L..1000L) {
            val played = playRandomHand(seed)
            val ended = played.finalState.street == Street.SHOWDOWN || played.finalState.seats.any { it.hasFolded }
            assertTrue(ended, "seed $seed: hand ended at ${played.finalState.street} with no fold")
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
