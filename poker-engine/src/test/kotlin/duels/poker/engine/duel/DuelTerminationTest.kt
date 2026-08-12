package duels.poker.engine.duel

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

/**
 * A hundred duels nobody designed all end, none of them near the ceiling — proving the
 * escalating blind ladder (`docs/duel-rules.md` Part 2) does what it claims: a heads-up freezeout
 * cannot stall forever. The ceiling asserted here is not a comment; the second test proves it
 * would actually fail a duel that ran away.
 */
class DuelTerminationTest {

    @Test
    @Timeout(120)
    fun everyDefaultDuelEndsInsideTheCeiling() {
        for (seed in 1L..100L) {
            val played = playRandomDuel(seed, maxHands = 200)

            assertNotNull(played.outcome.winner, "seed $seed: expected a winner")
            assertTrue(
                played.outcome.handsPlayed < 200,
                "seed $seed: expected handsPlayed (${played.outcome.handsPlayed}) < 200",
            )
        }
    }

    @Test
    @Timeout(120)
    fun theCeilingIsAnAssertionNotAnAssumption() {
        val error = assertThrows(AssertionError::class.java) {
            playRandomDuel(seed = 1L, maxHands = 0)
        }

        assertTrue(
            error.message?.contains("1") == true,
            "expected the failure message to name the seed, was: ${error.message}",
        )
    }

    @Test
    @Timeout(120)
    fun theSampleIsNotAllShortDuels() {
        val handsPlayed = (1L..100L).map { seed -> playRandomDuel(seed, maxHands = 200).outcome.handsPlayed }

        val longerThanThreeHands = handsPlayed.count { it > 3 }
        assertTrue(
            longerThanThreeHands >= 10,
            "expected at least 10 duels longer than 3 hands, got $longerThanThreeHands",
        )

        val shortest = handsPlayed.min()
        val longest = handsPlayed.max()
        assertTrue(
            longest > shortest,
            "expected the longest duel ($longest) to be strictly longer than the shortest ($shortest)",
        )
    }
}
