package duels.poker.engine.duel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class EndConditionTest {
    @Test
    fun fixedHandsCarriesItsHandCount() {
        val handCount = 25
        val endCondition = EndCondition.FixedHands(handCount)
        assertEquals(handCount, endCondition.hands)
    }

    @Test
    fun fixedHandsRejectsANonPositiveCount() {
        assertThrows(IllegalArgumentException::class.java) {
            EndCondition.FixedHands(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EndCondition.FixedHands(-1)
        }
    }

    @Test
    fun bothConditionsShareTheSealedType() {
        val freezeout: EndCondition = EndCondition.Freezeout
        val fixedHands: EndCondition = EndCondition.FixedHands(50)

        // Test Freezeout branch
        val freezeoutResult = when (freezeout) {
            EndCondition.Freezeout -> "freezeout"
            is EndCondition.FixedHands -> "fixed_hands"
        }
        assertEquals("freezeout", freezeoutResult)

        // Test FixedHands branch
        val fixedHandsResult = when (fixedHands) {
            EndCondition.Freezeout -> "freezeout"
            is EndCondition.FixedHands -> "fixed_hands"
        }
        assertEquals("fixed_hands", fixedHandsResult)
    }
}
