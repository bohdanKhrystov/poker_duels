package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NoOpEngineTest {
    private val state = handState()

    private val actions = listOf(
        PlayerAction.Fold(seat = 0),
        PlayerAction.Check(seat = 0),
        PlayerAction.Call(seat = 0),
        PlayerAction.Bet(seat = 0, to = 100),
        PlayerAction.Raise(seat = 0, to = 200),
        PlayerAction.AllIn(seat = 0),
    )

    @Test
    fun refusesEveryActionType() {
        for (action in actions) {
            val result = NoOpEngine.handle(state, action)
            assertTrue(result.isRejected, "Action $action should be rejected")
        }
    }

    @Test
    fun returnsTheInputStateUntouched() {
        for (action in actions) {
            val result = NoOpEngine.handle(state, action)
            assertEquals(state, result.newState, "State should be unchanged for action $action")
        }
    }

    @Test
    fun emitsNothing() {
        for (action in actions) {
            val result = NoOpEngine.handle(state, action)
            assertTrue(result.events.isEmpty(), "No events should be emitted for action $action")
        }
    }

    @Test
    fun isPure() {
        for (action in actions) {
            val result1 = NoOpEngine.handle(state, action)
            val result2 = NoOpEngine.handle(state, action)
            assertEquals(result1, result2, "Calling handle twice with same state and action should give equal results")
        }
    }
}
