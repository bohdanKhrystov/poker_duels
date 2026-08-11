package duels.poker.engine.random

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.forAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SplitMix64RngTest {

    @Test
    fun matchesTheSplitMix64ReferenceVector() {
        val rng0 = SplitMix64Rng(0)
        val draw1 = rng0.nextLong()
        val draw2 = draw1.next.nextLong()
        val draw3 = draw2.next.nextLong()

        assertEquals(-2152535657050944081L, draw1.value)
        assertEquals(7960286522194355700L, draw2.value)
        assertEquals(487617019471545679L, draw3.value)
    }

    @Test
    fun sameSeedProducesTheSameSequence() {
        var left: Rng = SplitMix64Rng(12345)
        var right: Rng = SplitMix64Rng(12345)

        repeat(100) {
            val leftDraw = left.nextInt(52)
            val rightDraw = right.nextInt(52)
            assertEquals(leftDraw.value, rightDraw.value)
            left = leftDraw.next
            right = rightDraw.next
        }
    }

    @Test
    fun drawingDoesNotMutateTheReceiver() {
        val rng = SplitMix64Rng(42)

        val first = rng.nextInt(52)
        val second = rng.nextInt(52)

        assertEquals(first.value, second.value)
        assertEquals(first.next, second.next)
    }

    @Test
    fun nextIntStaysWithinItsBound() {
        runBlocking {
            forAll(Arb.long(), Arb.int(1..1000)) { seed, bound ->
                val draw = SplitMix64Rng(seed).nextInt(bound)
                draw.value in 0 until bound
            }
        }
    }

    @Test
    fun nextIntRejectsANonPositiveBound() {
        val rng = SplitMix64Rng(1)

        assertThrows(IllegalArgumentException::class.java) { rng.nextInt(0) }
        assertThrows(IllegalArgumentException::class.java) { rng.nextInt(-1) }
    }
}
