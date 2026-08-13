package duels.poker.server.duel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Random

internal class HandSeedSourceTest {
    @Test
    fun theDefaultSourceDrawsFromSecureRandom() {
        val source = SecureHandSeedSource()
        assertTrue(source.isSecure)
    }

    @Test
    fun anInjectedRandomIsNotReportedAsSecure() {
        val source = SecureHandSeedSource(Random(1))
        assertFalse(source.isSecure)
    }

    @Test
    fun thesameInjectedSeedGivesTheSameHandSeeds() {
        val source1 = SecureHandSeedSource(Random(1))
        val source2 = SecureHandSeedSource(Random(1))

        val seeds1 = listOf(source1.newHandSeed(), source1.newHandSeed(), source1.newHandSeed())
        val seeds2 = listOf(source2.newHandSeed(), source2.newHandSeed(), source2.newHandSeed())

        assertEquals(seeds1, seeds2)
    }

    @Test
    fun consecutiveDrawsFromTheDefaultSourceDiffer() {
        val source = SecureHandSeedSource()
        val seeds = (1..100).map { source.newHandSeed() }

        assertEquals(100, seeds.distinct().size)
    }

    @Test
    fun aLambdaSatisfiesThePort() {
        val source: HandSeedSource = HandSeedSource { 42L }
        assertEquals(42L, source.newHandSeed())
    }
}
