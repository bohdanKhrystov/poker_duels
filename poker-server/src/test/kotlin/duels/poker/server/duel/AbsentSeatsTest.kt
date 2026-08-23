package duels.poker.server.duel

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.ActionType
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.game.legalActions
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.ServerMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AbsentSeatsTest {

    private val seeds = HandSeedSource { 7L }
    private val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
    private val threeHands = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(3))

    private fun actOn(step: DuelStep, seat: Int, action: PlayerAction): DuelStep {
        val hand = step.runner.hand!!
        val frame = Act(
            handNumber = hand.state.handNumber,
            actionSequence = decisionPointOf(hand.log.events)!!.sequence,
            action = action,
        )
        val next = act(step.runner, seat, frame, seeds)
        return DuelStep(next.runner, step.outbound + next.outbound)
    }

    @Test
    fun anAbsentSeatAtTheBigBlindsOptionProgressesTheHand() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val button = step.runner.hand!!.state.seatToAct!!
        val bigBlind = 1 - button

        val afterCall = actOn(step, button, PlayerAction.Call(button))
        assertEquals(bigBlind, afterCall.runner.hand!!.state.seatToAct)
        assertFalse(legalActions(afterCall.runner.hand!!.state).allows(ActionType.FOLD))
        val loggedBefore = afterCall.runner.hand!!.log.actions

        val result = foldAbsent(afterCall, setOf(bigBlind), seeds)

        val loggedAfter = result.runner.hand!!.log.actions
        assertTrue(loggedAfter.size > loggedBefore.size)
        assertEquals(PlayerAction.Check(bigBlind), loggedAfter[loggedBefore.size])
        assertNotEquals(bigBlind, result.runner.hand!!.state.seatToAct)
    }

    @Test
    fun anAbsentSeatFirstToActOnACheckedStreetProgressesTheHand() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val button = step.runner.hand!!.state.seatToAct!!
        val bigBlind = 1 - button

        val preflopChecked = actOn(
            actOn(step, button, PlayerAction.Call(button)),
            bigBlind,
            PlayerAction.Check(bigBlind),
        )
        val flopFirst = preflopChecked.runner.hand!!.state.seatToAct!!
        val flopFirstChecked = actOn(preflopChecked, flopFirst, PlayerAction.Check(flopFirst))
        val flopSecond = flopFirstChecked.runner.hand!!.state.seatToAct!!
        val flopChecked = actOn(flopFirstChecked, flopSecond, PlayerAction.Check(flopSecond))

        val turnFirst = flopChecked.runner.hand!!.state.seatToAct!!
        assertFalse(legalActions(flopChecked.runner.hand!!.state).allows(ActionType.FOLD))
        val loggedBefore = flopChecked.runner.hand!!.log.actions

        val result = foldAbsent(flopChecked, setOf(turnFirst), seeds)

        val loggedAfter = result.runner.hand!!.log.actions
        assertTrue(loggedAfter.size > loggedBefore.size)
        assertEquals(PlayerAction.Check(turnFirst), loggedAfter[loggedBefore.size])
        assertNotEquals(turnFirst, result.runner.hand!!.state.seatToAct)
    }

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

    @Test
    fun anAbsentFoldIsMarkedAsTheServersOwn() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val seatToAct = step.runner.hand!!.state.seatToAct!!

        val result = foldAbsent(step, setOf(seatToAct), seeds)
        val newFrames = result.outbound.drop(step.outbound.size)
        // Sent to both seats identically (proven separately by theMarkGoesToBothSeats), so the
        // first copy speaks for both.
        val mark = newFrames.map { it.message }
            .filterIsInstance<ServerMessage.ActedForAbsent>()
            .first()

        assertEquals(seatToAct, mark.seat)
        assertEquals(ActionType.FOLD, mark.action)
        assertEquals(1, mark.handNumber)
    }

    @Test
    fun theMarkGoesToBothSeats() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val seatToAct = step.runner.hand!!.state.seatToAct!!

        val result = foldAbsent(step, setOf(seatToAct), seeds)
        val marks = result.outbound.drop(step.outbound.size)
            .filter { it.message is ServerMessage.ActedForAbsent }

        val toSeat0 = marks.single { it.seat == 0 }
        val toSeat1 = marks.single { it.seat == 1 }
        assertEquals(toSeat0.message, toSeat1.message)
    }

    @Test
    fun theMarkPrecedesTheFramesTheActionProduced() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val seatToAct = step.runner.hand!!.state.seatToAct!!

        val result = foldAbsent(step, setOf(seatToAct), seeds)
        val newFrames = result.outbound.drop(step.outbound.size)

        val firstMark = newFrames.indexOfFirst { it.message is ServerMessage.ActedForAbsent }
        val firstStateFrame = newFrames.indexOfFirst {
            it.message is ServerMessage.Events || it.message is ServerMessage.Snapshot
        }

        assertTrue(firstMark >= 0)
        assertTrue(firstStateFrame >= 0)
        assertTrue(firstMark < firstStateFrame)
    }

    @Test
    fun theMarkNamesTheDecisionPointTheActionWasSentFor() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val button = step.runner.hand!!.state.seatToAct!!
        val bigBlind = 1 - button
        val afterCall = actOn(step, button, PlayerAction.Call(button))
        // Not the hand's very first decision point, so this is not zero — a mark that always
        // carried 0 would pass a test that never checked anything else.
        val decisionPointSequence = decisionPointOf(afterCall.runner.hand!!.log.events)!!.sequence
        assertTrue(decisionPointSequence > 0)

        val result = foldAbsent(afterCall, setOf(bigBlind), seeds)
        val newFrames = result.outbound.drop(afterCall.outbound.size)
        val mark = newFrames.map { it.message }
            .filterIsInstance<ServerMessage.ActedForAbsent>()
            .first()

        assertEquals(decisionPointSequence, mark.actionSequence)
        // Literal, not `bigBlind`: this test constructs the interesting seat (1) on purpose, and
        // must actually check the mark's seat field to close the gap a hardcoded 0 would leave.
        assertEquals(1, mark.seat)
    }

    @Test
    fun aStepThatFoldsNothingCarriesNoMark() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val seatToAct = step.runner.hand!!.state.seatToAct!!
        // Finished by a fold a seat sent for itself, through act(), never through foldAbsent — so
        // this fixture's own history carries no mark for the third call to be left alone against.
        val finished = actOn(step, seatToAct, PlayerAction.Fold(seatToAct))
        assertNotNull(finished.runner.outcome)

        val leftAlone = listOf(
            foldAbsent(step, setOf(1 - seatToAct), seeds),
            foldAbsent(step, emptySet(), seeds),
            foldAbsent(finished, setOf(0, 1), seeds),
        )

        leftAlone.forEach { result ->
            assertTrue(result.outbound.none { it.message is ServerMessage.ActedForAbsent })
        }
    }

    @Test
    fun everyActionInARunAwayDuelIsMarked() {
        val step = startDuel(threeHands, buttonSeat = 0, seed = 7L)

        val result = foldAbsent(step, setOf(0, 1), seeds)
        val newFrames = result.outbound.drop(step.outbound.size)
        val marks = newFrames.filter { it.message is ServerMessage.ActedForAbsent }
        val totalActions = result.runner.log.hands.flatMap { it.actions }.size

        assertEquals(totalActions, marks.count { it.seat == 0 })
        // Both seats are absent through the whole run, and the button rotates hand to hand, so
        // the mark's own seat field — not just its two delivery addresses — must take both values.
        val markedSeats = marks.mapNotNull { (it.message as? ServerMessage.ActedForAbsent)?.seat }.toSet()
        assertEquals(setOf(0, 1), markedSeats)
    }

    @Test
    fun anAbsentBigBlindsOptionIsMarkedAsACheck() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val button = step.runner.hand!!.state.seatToAct!!
        val bigBlind = 1 - button

        val afterCall = actOn(step, button, PlayerAction.Call(button))
        assertEquals(bigBlind, afterCall.runner.hand!!.state.seatToAct)
        assertFalse(legalActions(afterCall.runner.hand!!.state).allows(ActionType.FOLD))

        val result = foldAbsent(afterCall, setOf(bigBlind), seeds)
        val mark = result.outbound.drop(afterCall.outbound.size)
            .map { it.message }
            .filterIsInstance<ServerMessage.ActedForAbsent>()
            .first()

        assertEquals(ActionType.CHECK, mark.action)
        assertEquals(bigBlind, mark.seat)
    }

    @Test
    fun anAbsentSeatOnACheckedStreetIsMarkedAsACheck() {
        val step = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val button = step.runner.hand!!.state.seatToAct!!
        val bigBlind = 1 - button

        val preflopChecked = actOn(
            actOn(step, button, PlayerAction.Call(button)),
            bigBlind,
            PlayerAction.Check(bigBlind),
        )
        val flopFirst = preflopChecked.runner.hand!!.state.seatToAct!!
        val flopFirstChecked = actOn(preflopChecked, flopFirst, PlayerAction.Check(flopFirst))
        val flopSecond = flopFirstChecked.runner.hand!!.state.seatToAct!!
        val flopChecked = actOn(flopFirstChecked, flopSecond, PlayerAction.Check(flopSecond))

        val turnFirst = flopChecked.runner.hand!!.state.seatToAct!!
        assertFalse(legalActions(flopChecked.runner.hand!!.state).allows(ActionType.FOLD))

        val result = foldAbsent(flopChecked, setOf(turnFirst), seeds)
        val mark = result.outbound.drop(flopChecked.outbound.size)
            .map { it.message }
            .filterIsInstance<ServerMessage.ActedForAbsent>()
            .first()

        assertEquals(ActionType.CHECK, mark.action)
        assertEquals(turnFirst, mark.seat)
    }

    @Test
    fun theTwoOutcomesAreMarkedDifferently() {
        val foldStep = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val foldSeat = foldStep.runner.hand!!.state.seatToAct!!
        assertTrue(legalActions(foldStep.runner.hand!!.state).allows(ActionType.FOLD))

        val foldResult = foldAbsent(foldStep, setOf(foldSeat), seeds)
        val foldMark = foldResult.outbound.drop(foldStep.outbound.size)
            .map { it.message }
            .filterIsInstance<ServerMessage.ActedForAbsent>()
            .first()

        val checkStep = startDuel(oneHand, buttonSeat = 0, seed = 7L)
        val button = checkStep.runner.hand!!.state.seatToAct!!
        val bigBlind = 1 - button
        val afterCall = actOn(checkStep, button, PlayerAction.Call(button))
        assertFalse(legalActions(afterCall.runner.hand!!.state).allows(ActionType.FOLD))

        val checkResult = foldAbsent(afterCall, setOf(bigBlind), seeds)
        val checkMark = checkResult.outbound.drop(afterCall.outbound.size)
            .map { it.message }
            .filterIsInstance<ServerMessage.ActedForAbsent>()
            .first()

        assertEquals(ActionType.FOLD, foldMark.action)
        assertEquals(ActionType.CHECK, checkMark.action)
    }
}
