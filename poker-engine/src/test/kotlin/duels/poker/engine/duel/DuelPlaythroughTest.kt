package duels.poker.engine.duel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * A whole duel played end to end from a seed alone — deal, bet, settle, next hand, repeat — over
 * `1L..20L` under [DuelFormat.DEFAULT]: proof that the harness introduced by `TASK-010713`
 * actually terminates with a winner, not just that a single hand does.
 */
class DuelPlaythroughTest {

    @Test
    @Timeout(60)
    fun aDefaultFreezeoutEndsWithExactlyOneWinner() {
        for (seed in 1L..20L) {
            val played = playRandomDuel(seed, DuelFormat.DEFAULT)
            val outcome = played.outcome

            assertNotNull(outcome.winner, "seed $seed: expected a winner, got a draw")
            val winner = outcome.winner!!
            val loser = 1 - winner

            assertEquals(
                2 * DuelFormat.DEFAULT.startingStack,
                outcome.finalStacks[winner],
                "seed $seed: winner's final stack should hold every chip",
            )
            assertEquals(
                0,
                outcome.finalStacks[loser],
                "seed $seed: loser's final stack should be zero",
            )
        }
    }

    @Test
    @Timeout(60)
    fun theOutcomeAgreesWithTheHandsPlayed() {
        for (seed in 1L..20L) {
            val played = playRandomDuel(seed, DuelFormat.DEFAULT)
            val outcome = played.outcome
            val hands = played.hands

            assertEquals(
                outcome.handsPlayed,
                hands.size,
                "seed $seed: outcome.handsPlayed should match the number of hands played",
            )
            assertEquals(
                outcome.finalStacks,
                hands.last().stacksAfter,
                "seed $seed: outcome.finalStacks should match the last hand's stacksAfter",
            )

            val handNumbers = hands.map { it.handNumber }
            assertEquals(
                (1..hands.size).toList(),
                handNumbers,
                "seed $seed: hand numbers should run 1..${hands.size} with no gap, got $handNumbers",
            )
        }
    }
}
