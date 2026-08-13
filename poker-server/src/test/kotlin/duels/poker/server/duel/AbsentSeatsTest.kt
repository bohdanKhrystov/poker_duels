package duels.poker.server.duel

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.PlayerAction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AbsentSeatsTest {

    private val seeds = HandSeedSource { 7L }
    private val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
    private val threeHands = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(3))

    @Test
    fun aSeatSomebodyIsSittingInIsLeftAlone() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val seatToAct = step.runner.hand!!.state.seatToAct!!
        assertNotNull(step.runner.hand)

        val result = foldAbsent(step, setOf(1 - seatToAct), seeds)

        assertEquals(step.runner, result.runner)
        assertEquals(step.outbound, result.outbound)
    }

    @Test
    fun noAbsentSeatAtAllIsLeftAlone() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        assertNotNull(step.runner.hand)

        val result = foldAbsent(step, emptySet(), seeds)

        assertEquals(step.runner, result.runner)
        assertEquals(step.outbound, result.outbound)
    }

    @Test
    fun anAbsentSeatOnTurnFolds() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val seatToAct = step.runner.hand!!.state.seatToAct!!
        assertNotNull(step.runner.hand)
        assertNull(step.runner.outcome)

        val result = foldAbsent(step, setOf(seatToAct), seeds)

        assertNull(result.runner.hand)
        assertNotNull(result.runner.outcome)
    }

    @Test
    fun theFoldReachesTheEngineAsAnOrdinaryFold() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val seatToAct = step.runner.hand!!.state.seatToAct!!
        assertTrue(step.runner.hand!!.log.actions.isEmpty())
        assertTrue(step.runner.log.hands.isEmpty())

        val result = foldAbsent(step, setOf(seatToAct), seeds)

        assertEquals(PlayerAction.Fold(seatToAct), result.runner.log.hands.single().actions.last())
    }

    @Test
    fun theOpponentIsToldWhatHappened() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val seatToAct = step.runner.hand!!.state.seatToAct!!
        val opponent = 1 - seatToAct

        val result = foldAbsent(step, setOf(seatToAct), seeds)
        val newFrames = result.outbound.drop(step.outbound.size)

        assertTrue(newFrames.isNotEmpty())
        assertTrue(newFrames.any { it.seat == opponent })
    }

    @Test
    fun theDuelContinuesWhileTheFoldedSeatHasChips() {
        val step = startDuel(threeHands, buttonSeat = 0, seed = 7L)
        val seatToAct = step.runner.hand!!.state.seatToAct!!
        assertEquals(1, step.runner.hand!!.state.handNumber)
        assertNull(step.runner.outcome)

        val result = foldAbsent(step, setOf(seatToAct), seeds)

        assertNull(result.runner.outcome)
        assertEquals(2, result.runner.hand!!.state.handNumber)
        assertEquals(1 - seatToAct, result.runner.hand!!.state.seatToAct)
    }

    @Test
    @Timeout(10)
    fun bothSeatsAbsentRunTheDuelToItsEnd() {
        val step = startDuel(threeHands, buttonSeat = 0, seed = 7L)
        assertNull(step.runner.outcome)
        assertTrue(step.runner.log.hands.isEmpty())

        val result = foldAbsent(step, setOf(0, 1), seeds)

        assertNotNull(result.runner.outcome)
        assertEquals(3, result.runner.log.hands.size)
        result.runner.log.hands.forEach { hand ->
            assertTrue(hand.actions.last() is PlayerAction.Fold)
        }
    }

    @Test
    fun aFinishedDuelIsLeftAlone() {
        val started = startDuel(threeHands, buttonSeat = 0, seed = 7L)
        val finished = foldAbsent(started, setOf(0, 1), seeds)
        assertNotNull(finished.runner.outcome)

        val result = foldAbsent(finished, setOf(0, 1), seeds)

        assertEquals(finished.runner, result.runner)
        assertEquals(finished.outbound, result.outbound)
    }
}
