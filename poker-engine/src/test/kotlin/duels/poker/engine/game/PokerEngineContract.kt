package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Fails unless [result]'s events, folded over [before], reproduce its own `newState`.
 *
 * `deck` and `rng` are copied across before comparing: no event carries an undealt card or a
 * seed, by design (see StateProjection), so those two fields cannot be reproduced from a log
 * and are the one deliberate gap in this check. Every other field must match exactly.
 */
internal fun assertEventsDescribeTheTransition(before: GameState, result: EngineResult) {
    val folded = StateProjection.fold(before, result.events)
    val comparable = folded.copy(deck = result.newState.deck, rng = result.newState.rng)
    assertEquals(result.newState, comparable)
}

/** The suite every [PokerEngine] implementation must pass. Subclass it, supply the engine. */
internal abstract class PokerEngineContract {
    protected abstract fun engine(): PokerEngine

    /** Positions and actions to exercise. Override to add implementation-specific ones. */
    protected open fun cases(): List<Pair<GameState, PlayerAction>> {
        val positions = listOf(
            handState(),
            handState().copy(seatToAct = 1),
            handState().copy(street = Street.COMPLETE, seatToAct = null),
        )
        return positions.flatMap { state ->
            val seat = state.seatToAct ?: 0
            listOf(
                state to PlayerAction.Fold(seat),
                state to PlayerAction.Check(seat),
                state to PlayerAction.Call(seat),
                state to PlayerAction.Bet(seat, to = state.bigBlind * 2),
                state to PlayerAction.Raise(seat, to = state.bigBlind * 3),
                state to PlayerAction.AllIn(seat),
            )
        }
    }

    /** Every call in the suite goes through here, so the fold check is unavoidable. */
    protected fun handle(state: GameState, action: PlayerAction): EngineResult {
        val result = engine().handle(state, action)
        assertEventsDescribeTheTransition(state, result)
        return result
    }

    @Test
    fun eventsAlwaysDescribeTheStateTransition() {
        cases().forEach { (state, action) -> handle(state, action) }
    }

    @Test
    fun aRejectedActionChangesNothing() {
        cases().forEach { (state, action) ->
            val result = handle(state, action)
            if (result.isRejected) {
                assertEquals(state, result.newState)
                assertTrue(result.events.isEmpty())
            }
        }
    }

    @Test
    fun handleIsPure() {
        cases().forEach { (state, action) ->
            val first = handle(state, action)
            val second = handle(state, action)
            assertEquals(first, second)
        }
    }

    @Test
    fun eventsContinueTheHandSequence() {
        cases().forEach { (state, action) ->
            val result = handle(state, action)
            if (result.isRejected) {
                assertTrue(result.events.isEmpty())
            } else {
                val expectedSequences = (0 until result.events.size).map { state.eventCount + it }
                assertEquals(expectedSequences, result.events.map { it.sequence })
                assertEquals(state.eventCount + result.events.size, result.newState.eventCount)
            }
        }
    }
}
