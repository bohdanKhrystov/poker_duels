package duels.poker.engine.log

import duels.poker.engine.duel.MatchFinished
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `LoggedDuelPlayer` records a per-hand-seeded [MatchLog] instead of `RandomDuelPlayer`'s single
 * duel-wide seed. These tests prove the log it hands back is exactly what `MatchLog` and
 * `HandLog` promise: every hand replays on its own, and the match closes with the same outcome
 * replaying its final hand would recover.
 */
class LoggedDuelPlayerTest {
    @Test
    fun everyHandInALoggedDuelReplaysExactly() {
        for (seed in 1L..10L) {
            val log = playLoggedDuel(seed)

            for (hand in log.hands) {
                replayHand(hand).events shouldBe hand.events
            }
        }
    }

    @Test
    fun theLogEndsWithMatchFinished() {
        for (seed in 1L..10L) {
            val log = playLoggedDuel(seed)

            val finished = log.events.last() as MatchFinished
            finished.outcome.handsPlayed shouldBe log.hands.size
        }
    }

    @Test
    fun theSameSeedProducesTheSameLog() {
        playLoggedDuel(7) shouldBe playLoggedDuel(7)
    }

    @Test
    fun theLastHandsStacksAreTheOutcomesStacks() {
        for (seed in 1L..10L) {
            val log = playLoggedDuel(seed)

            val finished = log.events.last() as MatchFinished
            val replay = replayHand(log.hands.last())

            finished.outcome.finalStacks shouldBe replay.finalState.seats.map { it.stack }
        }
    }

    @Test
    fun theCeilingIsAnAssertionNotAnAssumption() {
        val error = assertThrows(AssertionError::class.java) {
            playLoggedDuel(seed = 1L, maxHands = 0)
        }

        assertTrue(
            error.message?.contains("seed 1") == true,
            "expected the failure message to name the seed, was: ${error.message}",
        )
    }
}
