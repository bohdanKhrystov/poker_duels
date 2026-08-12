package duels.poker.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Tag("soak")
class SimulationSoakTest {
    @Test
    @Timeout(value = 90, unit = TimeUnit.MINUTES)
    fun oneHundredThousandDuelsPlayWithNoViolation() {
        val duels = System.getProperty("soakDuels")?.toInt() ?: 100_000
        val report = runSimulation(duels, listOf(RandomBot, RandomBot))

        assertEquals(duels, report.duels, "duels count mismatch")
        assertEquals(duels, report.winsBySeat.sum() + report.draws, "all duels must be accounted for")
        assertTrue(report.hands >= report.duels, "hands must be at least duels")
    }
}
