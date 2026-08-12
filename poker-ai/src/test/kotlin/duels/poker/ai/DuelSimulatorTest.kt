package duels.poker.ai

import duels.poker.engine.game.GameState
import duels.poker.engine.game.LegalActions
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.random.Rng
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** A bot that ignores every legal action and always tries to bet 1, which is never legal. */
private object AlwaysBetsOne : Bot {
    override fun choose(state: GameState, legal: LegalActions, rng: Rng): Bot.Choice =
        Bot.Choice(PlayerAction.Bet(legal.seat, 1), rng)
}

class DuelSimulatorTest {
    private val randomBots = listOf(RandomBot, RandomBot)

    @Test
    fun aDuelBetweenTwoRandomBotsProducesAnOutcome() {
        for (seed in 1L..20L) {
            val duel = simulateDuel(seed, randomBots)

            assertNotNull(duel.outcome.winner, "seed $seed produced no winner")
            assertTrue(duel.hands.isNotEmpty(), "seed $seed produced no hands")
        }
    }

    @Test
    fun theSameSeedProducesTheSameDuel() {
        val first = simulateDuel(7, randomBots)
        val second = simulateDuel(7, randomBots)

        assertEquals(first, second)
    }

    @Test
    fun everyHandRecordsItsSeedAndItsActions() {
        val duel = simulateDuel(11, randomBots)

        duel.hands.forEachIndexed { index, hand ->
            assertTrue(hand.actions.isNotEmpty(), "hand ${hand.handNumber} recorded no actions")
            assertEquals(index + 1, hand.handNumber, "hand numbers must run 1..n without a gap")
        }
    }

    @Test
    fun anIllegalBotActionIsReportedWithTheSeedAndActions() {
        val seed = 3L
        val bots = listOf(AlwaysBetsOne, AlwaysBetsOne)

        val failure = assertThrows<SimulationFailure> { simulateDuel(seed, bots) }

        assertEquals(seed, failure.seed)
        assertEquals(SplitMix64Rng(seed).nextLong().value, failure.handSeed)
        assertFalse(failure.actions.contains(PlayerAction.Bet(0, 1)))
        assertFalse(failure.actions.contains(PlayerAction.Bet(1, 1)))
    }

    @Test
    fun theHandCeilingIsAnAssertionNotAnAssumption() {
        val seed = 42L

        val failure = assertThrows<SimulationFailure> { simulateDuel(seed, randomBots, maxHands = 0) }

        assertEquals(seed, failure.seed)
    }
}
