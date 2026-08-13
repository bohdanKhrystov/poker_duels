package duels.poker.server.duel

import duels.poker.engine.game.DefaultPokerEngine
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.game.startHand
import duels.poker.engine.random.SplitMix64Rng
import duels.poker.server.protocol.Act
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class DuelGuardTest {

    private val opening = startHand(1, 0, listOf(1_000, 1_000), 50, 100, SplitMix64Rng(7))
    private val current = Act(
        handNumber = 1,
        actionSequence = decisionPointOf(opening.events)!!.sequence,
        action = PlayerAction.Call(0),
    )

    @Test
    fun theSeatOnTurnAnsweringTheCurrentDecisionPointPasses() {
        assertNull(guard(opening.newState, opening.events, 0, current))
    }

    @Test
    fun aFrameFromTheSeatNotOnTurnIsNotYourTurn() {
        assertEquals(ActRefusal.NOT_YOUR_TURN, guard(opening.newState, opening.events, 1, current))
    }

    @Test
    fun aFrameActingForTheOpponentIsNotYourTurn() {
        val actingForOpponent = current.copy(action = PlayerAction.Fold(1))
        assertEquals(
            ActRefusal.NOT_YOUR_TURN,
            guard(opening.newState, opening.events, 0, actingForOpponent),
        )
    }

    @Test
    fun anotherHandsNumberIsAStaleFrame() {
        val otherHand = current.copy(handNumber = 2)
        assertEquals(ActRefusal.STALE_FRAME, guard(opening.newState, opening.events, 0, otherHand))
    }

    @Test
    fun anEarlierActionSequenceIsAStaleFrame() {
        val earlier = current.copy(actionSequence = current.actionSequence - 1)
        assertEquals(ActRefusal.STALE_FRAME, guard(opening.newState, opening.events, 0, earlier))
    }

    @Test
    fun alaterActionSequenceIsAStaleFrame() {
        val later = current.copy(actionSequence = current.actionSequence + 1)
        assertEquals(ActRefusal.STALE_FRAME, guard(opening.newState, opening.events, 0, later))
    }

    @Test
    fun areplayedFrameIsStaleRatherThanOutOfTurn() {
        val applied = DefaultPokerEngine.handle(opening.newState, current.action)
        val handEvents = opening.events + applied.events

        assertEquals(
            ActRefusal.STALE_FRAME,
            guard(applied.newState, handEvents, 0, current),
        )
    }

    @Test
    fun aseatOutsideTheTableIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            guard(opening.newState, opening.events, 2, current)
        }
    }
}
