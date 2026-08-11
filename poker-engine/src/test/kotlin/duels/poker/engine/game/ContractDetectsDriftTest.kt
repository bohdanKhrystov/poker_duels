package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Without this file, a checker that silently stopped asserting would leave every engine test
 * green and this project's central invariant — that a result's events and its `newState` always
 * describe the same transition — unguarded. These engines misbehave on purpose, and this test
 * proves the contract's fold check actually catches them.
 */
internal class ContractDetectsDriftTest {

    /** Commits chips in [newState] but reports the move with no events at all. */
    private class SilentEngine : PokerEngine {
        override fun handle(state: GameState, action: PlayerAction): EngineResult {
            val newState = state.withSeat(0) { it.commit(300) }
            return EngineResult.accepted(newState, emptyList())
        }
    }

    /** Emits a bet event but returns [newState] unchanged, as if nothing happened. */
    private class LyingEngine : PokerEngine {
        override fun handle(state: GameState, action: PlayerAction): EngineResult {
            val event = PlayerBet(state.eventCount, 0, 300)
            return EngineResult.accepted(state, listOf(event))
        }
    }

    @Test
    fun aStateChangeWithNoEventsFails() {
        val state = handState()
        val result = SilentEngine().handle(state, PlayerAction.Bet(0, to = 300))

        assertThrows(AssertionError::class.java) {
            assertEventsDescribeTheTransition(state, result)
        }
    }

    @Test
    fun anEventWithNoStateChangeFails() {
        val state = handState()
        val result = LyingEngine().handle(state, PlayerAction.Bet(0, to = 300))

        assertThrows(AssertionError::class.java) {
            assertEventsDescribeTheTransition(state, result)
        }
    }

    @Test
    fun anAgreeingResultPasses() {
        val state = handState()
        val event = PlayerBet(state.eventCount, 0, 300)
        val newState = StateProjection.apply(state, event)
        val result = EngineResult.accepted(newState, listOf(event))

        assertDoesNotThrow {
            assertEventsDescribeTheTransition(state, result)
        }
    }
}
