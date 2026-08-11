package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EngineResultTest {
    @Test
    fun acceptedCarriesItsEvents() {
        val state = handState()
        val event = ActionOn(0, 1)
        val result = EngineResult.accepted(state, listOf(event))

        assertFalse(result.isRejected)
        assertEquals(1, result.events.size)
        assertEquals(event, result.events[0])
        assertSame(state, result.newState)
    }

    @Test
    fun rejectedCarriesNoEvents() {
        val state = handState()
        val result = EngineResult.rejected(state, Rejection.HandComplete)

        assertTrue(result.isRejected)
        assertTrue(result.events.isEmpty())
        assertEquals(state, result.newState)
    }

    @Test
    fun rejectionWithEventsIsImpossible() {
        val state = handState()
        val event = ActionOn(0, 1)

        assertThrows(IllegalArgumentException::class.java) {
            EngineResult(state, listOf(event), Rejection.HandComplete)
        }
    }

    @Test
    fun defaultsToAnAcceptedEmptyResult() {
        val state = handState()
        val result = EngineResult(state)

        assertFalse(result.isRejected)
        assertTrue(result.events.isEmpty())
    }
}
