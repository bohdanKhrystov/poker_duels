package duels.poker.engine.duel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DuelOutcomeTest {
    @Test
    fun carriesTheWinnerHandCountAndFinalStacks() {
        val outcome = DuelOutcome(1, 34, listOf(0, 20_000))
        assertEquals(1, outcome.winner)
        assertEquals(34, outcome.handsPlayed)
        assertEquals(listOf(0, 20_000), outcome.finalStacks)
        assertFalse(outcome.isDraw)
    }

    @Test
    fun aDrawHasNoWinner() {
        val outcome = DuelOutcome(null, 25, listOf(10_000, 10_000))
        assertTrue(outcome.isDraw)
    }

    @Test
    fun rejectsAMalformedOutcome() {
        // winner = 2
        assertThrows(IllegalArgumentException::class.java) {
            DuelOutcome(2, 10, listOf(5_000, 15_000))
        }

        // single-element finalStacks
        assertThrows(IllegalArgumentException::class.java) {
            DuelOutcome(0, 10, listOf(20_000))
        }

        // negative stack
        assertThrows(IllegalArgumentException::class.java) {
            DuelOutcome(0, 10, listOf(-100, 20_100))
        }

        // handsPlayed = -1
        assertThrows(IllegalArgumentException::class.java) {
            DuelOutcome(0, -1, listOf(0, 20_000))
        }
    }
}
