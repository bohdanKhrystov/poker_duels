package duels.poker.engine.duel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class BlindLevelTest {

    @Test
    fun holdsItsSmallAndBigBlind() {
        val level = BlindLevel(50, 100)

        assertEquals(50, level.smallBlind)
        assertEquals(100, level.bigBlind)
    }

    @Test
    fun doubledDoublesBothBlinds() {
        val level = BlindLevel(200, 400)

        val doubled = level.doubled()

        assertEquals(BlindLevel(400, 800), doubled)
    }

    @Test
    fun rejectsANonPositiveSmallBlind() {
        assertThrows(IllegalArgumentException::class.java) {
            BlindLevel(0, 100)
        }

        assertThrows(IllegalArgumentException::class.java) {
            BlindLevel(-50, 100)
        }
    }

    @Test
    fun rejectsABigBlindThatIsNotLarger() {
        assertThrows(IllegalArgumentException::class.java) {
            BlindLevel(100, 100)
        }

        assertThrows(IllegalArgumentException::class.java) {
            BlindLevel(100, 50)
        }
    }
}
