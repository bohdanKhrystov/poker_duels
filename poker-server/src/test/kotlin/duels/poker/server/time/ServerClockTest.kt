package duels.poker.server.time

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ServerClockTest {
    @Test
    fun theSystemClockNeverGoesBackwards() {
        var previous = SystemClock.nowMillis()
        repeat(1_000) {
            val current = SystemClock.nowMillis()
            assert(current >= previous) {
                "SystemClock went backwards: $previous -> $current"
            }
            previous = current
        }
    }

    @Test
    fun aMutableClockReportsExactlyWhatItWasSet() {
        val clock = MutableClock(7_000)
        assertEquals(7_000L, clock.nowMillis())

        clock.set(9_000)
        assertEquals(9_000L, clock.nowMillis())
    }

    @Test
    fun advancingTheMutableClockMovesTimeForward() {
        val clock = MutableClock()
        clock.advance(1_500)
        clock.advance(500)
        assertEquals(2_000L, clock.nowMillis())
    }

    @Test
    fun aMutableClockRefusesToGoBackwards() {
        val clock = MutableClock()
        assertThrows(IllegalArgumentException::class.java) {
            clock.advance(-1)
        }
    }
}
