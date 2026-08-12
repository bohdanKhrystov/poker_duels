package duels.poker.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows

class SimulationRunnerTest {
    private val randomBots = listOf(RandomBot, RandomBot)

    @Test
    @Timeout(300)
    fun aThousandDuelsRunWithNoViolation() {
        val report = runSimulation(1000, randomBots)

        assertEquals(1000, report.duels)
    }

    @Test
    fun theReportAccountsForEveryDuel() {
        val report = runSimulation(50, randomBots)

        assertEquals(report.duels, report.winsBySeat.sum() + report.draws)
        assertTrue(report.hands >= report.duels, "hands ${report.hands} must be at least duels ${report.duels}")
    }

    @Test
    fun theHandCountsBracketTheAverage() {
        val report = runSimulation(50, randomBots)

        assertTrue(report.shortestDuel >= 1, "shortestDuel ${report.shortestDuel} must be at least 1")
        assertTrue(
            report.shortestDuel <= report.averageHandsPerDuel,
            "shortestDuel ${report.shortestDuel} must be <= average ${report.averageHandsPerDuel}",
        )
        assertTrue(
            report.averageHandsPerDuel <= report.longestDuel,
            "average ${report.averageHandsPerDuel} must be <= longestDuel ${report.longestDuel}",
        )
    }

    @Test
    fun theSameSeedGivesTheSameReport() {
        val first = runSimulation(50, randomBots, seed = 7L)
        val second = runSimulation(50, randomBots, seed = 7L)

        assertEquals(first, second)
    }

    @Test
    fun aDifferentSeedGivesADifferentRun() {
        val first = runSimulation(50, randomBots, seed = 1L)
        val second = runSimulation(50, randomBots, seed = 1000L)

        assertNotEquals(first, second)
    }

    @Test
    fun rejectsANonPositiveDuelCount() {
        assertThrows<IllegalArgumentException> { runSimulation(0, randomBots) }
    }
}
