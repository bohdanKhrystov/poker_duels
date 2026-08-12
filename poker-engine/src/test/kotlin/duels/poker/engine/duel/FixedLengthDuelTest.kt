package duels.poker.engine.duel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * The alternative format `docs/duel-rules.md` Part 2 records under `DEC-001` — a set number of
 * hands, winner decided on chips rather than on a knockout — plays end to end through the same
 * `playRandomDuel` harness the freezeout does, with no production or harness change at all.
 */
class FixedLengthDuelTest {

    private val format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(3))

    private fun duels(): List<PlayedDuel> = (1L..40L).map { seed -> playRandomDuel(seed, format = format) }

    @Test
    @Timeout(60)
    fun aFixedLengthDuelNeverPlaysPastItsLimit() {
        for (played in duels()) {
            val seed = played.seed
            assertTrue(
                played.outcome.handsPlayed <= 3,
                "seed $seed: expected handsPlayed (${played.outcome.handsPlayed}) <= 3",
            )
            assertEquals(
                played.outcome.handsPlayed,
                played.hands.size,
                "seed $seed: expected hands.size (${played.hands.size}) to equal " +
                    "outcome.handsPlayed (${played.outcome.handsPlayed})",
            )
        }
    }

    @Test
    @Timeout(60)
    fun aFixedLengthDuelIsDecidedOnTheFinalStacks() {
        for (played in duels().filter { it.outcome.handsPlayed == 3 }) {
            val seed = played.seed
            val (first, second) = played.outcome.finalStacks

            when {
                first > second -> assertEquals(
                    0,
                    played.outcome.winner,
                    "seed $seed: seat 0 has the larger stack ($first > $second) but did not win",
                )
                second > first -> assertEquals(
                    1,
                    played.outcome.winner,
                    "seed $seed: seat 1 has the larger stack ($second > $first) but did not win",
                )
                else -> assertNull(
                    played.outcome.winner,
                    "seed $seed: equal stacks ($first == $second) but the duel named a winner",
                )
            }

            assertEquals(
                first == second,
                played.outcome.isDraw,
                "seed $seed: isDraw should be true exactly when the stacks are equal " +
                    "($first vs $second)",
            )
        }
    }

    @Test
    @Timeout(60)
    fun aFixedLengthDuelEndsEarlyWhenASeatIsBroke() {
        for (played in duels().filter { it.outcome.handsPlayed < 3 }) {
            val seed = played.seed
            val stacks = played.outcome.finalStacks
            val brokeSeat = stacks.indexOfFirst { it == 0 }

            assertTrue(
                brokeSeat != -1,
                "seed $seed: duel ended early at hand ${played.outcome.handsPlayed} but neither " +
                    "stack is 0 ($stacks)",
            )
            assertEquals(
                1 - brokeSeat,
                played.outcome.winner,
                "seed $seed: seat $brokeSeat is broke ($stacks) but seat ${1 - brokeSeat} did not win",
            )
        }
    }

    @Test
    @Timeout(60)
    fun aFixedLengthDuelThatEndsLevelIsADraw() {
        // Seed 73 is not arbitrary: confirmed (outside the 1..40 sample above) to play three
        // hands and finish with both stacks equal, which is the one outcome the 1..40 sample
        // never exercises. Without this test, an implementation that named an arbitrary winner
        // on a tie instead of returning winner = null would still pass the suite.
        val seed = 73L
        val played = playRandomDuel(seed, format = format)
        val (first, second) = played.outcome.finalStacks

        assertEquals(
            first,
            second,
            "seed $seed: expected a level finish (equal stacks), got $first vs $second",
        )
        assertTrue(
            played.outcome.isDraw,
            "seed $seed: expected isDraw to be true for a level finish, stacks were " +
                "${played.outcome.finalStacks}",
        )
        assertNull(
            played.outcome.winner,
            "seed $seed: expected winner to be null for a level finish, was " +
                "${played.outcome.winner}",
        )
    }

    @Test
    @Timeout(60)
    fun theSampleContainsBothEndings() {
        val played = duels()

        assertTrue(
            played.any { it.outcome.handsPlayed == 3 },
            "expected at least one duel across seeds 1..40 to reach hand 3",
        )
        assertTrue(
            played.any { it.outcome.handsPlayed < 3 },
            "expected at least one duel across seeds 1..40 to end before hand 3",
        )
    }
}
