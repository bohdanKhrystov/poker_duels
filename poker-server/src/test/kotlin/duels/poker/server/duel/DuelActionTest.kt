package duels.poker.server.duel

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.DefaultPokerEngine
import duels.poker.engine.game.PlayerAction
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.ServerMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val seeds = HandSeedSource { 99L }

class DuelActionTest {

    private val started = startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L)
    private val runner = started.runner
    private val seatToAct = runner.hand!!.state.seatToAct!!
    private val otherSeat = 1 - seatToAct
    private val current = Act(
        handNumber = 1,
        actionSequence = decisionPointOf(runner.hand!!.log.events)!!.sequence,
        action = PlayerAction.Call(seatToAct),
    )

    /** Folds hand 1 in a duel that ends after one hand, so the resulting runner has no live hand. */
    private fun finishedDuel(): DuelRunner {
        val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val toActSeat = step.runner.hand!!.state.seatToAct!!
        val fold = Act(
            handNumber = 1,
            actionSequence = decisionPointOf(step.runner.hand!!.log.events)!!.sequence,
            action = PlayerAction.Fold(toActSeat),
        )
        return act(step.runner, toActSeat, fold, seeds).runner
    }

    @Test
    fun anAcceptedActionAppearsInTheHandLog() {
        val before = runner.hand!!.log.events.size
        val step = act(runner, seatToAct, current, seeds)

        assertEquals(listOf(current.action), step.runner.hand!!.log.actions)
        assertTrue(step.runner.hand!!.log.events.size > before)
    }

    @Test
    fun anAcceptedActionReachesBothSeats() {
        val step = act(runner, seatToAct, current, seeds)

        assertTrue(step.outbound.any { it.seat == 0 && it.message is ServerMessage.Snapshot })
        assertTrue(step.outbound.any { it.seat == 1 && it.message is ServerMessage.Snapshot })
    }

    @Test
    fun theNextSeatIsPromptedAndTheOtherIsNot() {
        val step = act(runner, seatToAct, current, seeds)

        val yourTurns = step.outbound.filter { it.message is ServerMessage.YourTurn }
        assertEquals(1, yourTurns.size)
        assertEquals(step.runner.hand!!.state.seatToAct, yourTurns.single().seat)
    }

    @Test
    fun anActionFromTheWrongSeatIsRejectedToThatSeatAlone() {
        val step = act(runner, otherSeat, current, seeds)

        assertEquals(1, step.outbound.size)
        val addressed = step.outbound.single()
        assertEquals(otherSeat, addressed.seat)
        assertTrue(addressed.message is ServerMessage.Rejected)
        assertEquals(runner, step.runner)
    }

    @Test
    fun anActionForTheOpponentIsRejectedToTheSenderAlone() {
        val forOpponent = current.copy(action = PlayerAction.Fold(otherSeat))
        val step = act(runner, seatToAct, forOpponent, seeds)

        assertEquals(1, step.outbound.size)
        val addressed = step.outbound.single()
        assertEquals(seatToAct, addressed.seat)
        assertTrue(addressed.message is ServerMessage.Rejected)
        assertEquals(runner, step.runner)
    }

    @Test
    fun areplayedFrameChangesNothingAndSaysNothing() {
        val first = act(runner, seatToAct, current, seeds)
        val second = act(first.runner, seatToAct, current, seeds)

        assertTrue(second.outbound.isEmpty())
        assertEquals(first.runner, second.runner)
    }

    @Test
    fun anIllegalActionIsRejectedWithTheEnginesReason() {
        val illegalAction = PlayerAction.Bet(seatToAct, 1)
        val illegal = current.copy(action = illegalAction)
        val expected = DefaultPokerEngine.handle(runner.hand!!.state, illegalAction).rejection

        val step = act(runner, seatToAct, illegal, seeds)

        assertEquals(1, step.outbound.size)
        val addressed = step.outbound.single()
        assertEquals(seatToAct, addressed.seat)
        val rejected = addressed.message as ServerMessage.Rejected
        assertEquals(expected, rejected.rejection)
    }

    @Test
    fun afoldEndsTheHandAndOpensTheNext() {
        val fold = current.copy(action = PlayerAction.Fold(seatToAct))
        val step = act(runner, seatToAct, fold, seeds)

        assertEquals(2, step.runner.hand!!.log.handNumber)
        assertEquals(1, step.runner.log.hands.size)

        val snapshotHandNumbers = step.outbound
            .mapNotNull { (it.message as? ServerMessage.Snapshot)?.view?.handNumber }
            .toSet()
        assertTrue(1 in snapshotHandNumbers)
        assertTrue(2 in snapshotHandNumbers)
    }

    @Test
    fun aframeForAFinishedDuelIsDropped() {
        val finished = finishedDuel()
        assertNull(finished.hand)

        val frame = Act(handNumber = 1, actionSequence = 0, action = PlayerAction.Call(0))
        val step = act(finished, 0, frame, seeds)

        assertEquals(finished, step.runner)
        assertTrue(step.outbound.isEmpty())
    }

    @Test
    fun theReturnedRunnerAlwaysAwaitsAnAction() {
        val accepted = act(runner, seatToAct, current, seeds)
        val fold = current.copy(action = PlayerAction.Fold(seatToAct))
        val handOver = act(runner, seatToAct, fold, seeds)
        val illegal = act(runner, seatToAct, current.copy(action = PlayerAction.Bet(seatToAct, 1)), seeds)

        for (step in listOf(accepted, handOver, illegal)) {
            assertTrue(step.runner.hand == null || step.runner.hand!!.state.seatToAct != null)
        }
    }

    @Test
    fun aRejectionMovesNoDecisionPoint() {
        val illegal = current.copy(action = PlayerAction.Bet(seatToAct, 1))
        val handNumberBefore = runner.hand!!.state.handNumber
        val sequenceBefore = decisionPointOf(runner.hand!!.log.events)!!.sequence
        val seatToActBefore = runner.hand!!.state.seatToAct

        val step = act(runner, seatToAct, illegal, seeds)

        assertEquals(handNumberBefore, step.runner.hand!!.state.handNumber)
        assertEquals(sequenceBefore, decisionPointOf(step.runner.hand!!.log.events)!!.sequence)
        assertEquals(seatToActBefore, step.runner.hand!!.state.seatToAct)
    }

    @Test
    fun aFrameTheEngineRejectedStillPassesTheGuard() {
        val illegal = current.copy(action = PlayerAction.Bet(seatToAct, 1))
        val step = act(runner, seatToAct, illegal, seeds)

        val refusal = guard(step.runner.hand!!.state, step.runner.hand!!.log.events, seatToAct, current)

        assertNull(refusal)
    }

    @Test
    fun anActionRetriedAfterARejectionIsApplied() {
        val illegal = current.copy(action = PlayerAction.Bet(seatToAct, 1))
        val rejected = act(runner, seatToAct, illegal, seeds)

        val retried = act(rejected.runner, seatToAct, current, seeds)

        assertTrue(current.action in retried.runner.hand!!.log.actions)
        assertTrue(retried.outbound.any { it.seat == 0 && it.message is ServerMessage.Snapshot })
        assertTrue(retried.outbound.any { it.seat == 1 && it.message is ServerMessage.Snapshot })
    }
}
