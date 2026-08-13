package duels.poker.server.duel

import duels.poker.engine.duel.BlindLevel
import duels.poker.engine.duel.BlindSchedule
import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.HandStarted
import duels.poker.server.protocol.ServerMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DuelStartTest {
    @Test
    fun theFirstHandIsHandOneWithTheOpeningButton() {
        val step = startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L)
        assertEquals(1, step.runner.hand!!.log.handNumber)
        assertEquals(0, step.runner.log.buttonSeat)
        assertEquals(0, step.runner.match.handsPlayed)
    }

    @Test
    fun theHandLogRecordsTheSeedItWasDealtFrom() {
        val step = startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L)
        assertEquals(7L, step.runner.hand!!.log.seed)
    }

    @Test
    fun theHandLogTakesItsStacksAndBlindsFromTheMatch() {
        val step = startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L)
        val log = step.runner.hand!!.log
        assertEquals(listOf(10_000, 10_000), log.stacks)
        assertEquals(50, log.smallBlind)
        assertEquals(100, log.bigBlind)
    }

    @Test
    fun theSameSeedOpensTheSameHand() {
        val step1 = startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L)
        val step2 = startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L)
        assertEquals(step1.runner.hand!!.log.events, step2.runner.hand!!.log.events)
        assertEquals(step1.outbound, step2.outbound)
    }

    @Test
    fun adifferentSeedOpensAdifferentHand() {
        val step1 = startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L)
        val step2 = startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 8L)
        assertTrue(
            step1.runner.hand!!.log.events != step2.runner.hand!!.log.events,
            "different seeds should produce different events",
        )
    }

    @Test
    fun bothSeatsAreToldTheHandHasStarted() {
        val step = startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L)
        val snapshots = step.outbound.filterIsInstance<Addressed>()
            .filter { it.message is ServerMessage.Snapshot }
        assertEquals(2, snapshots.size, "should have one Snapshot for each seat")
        assertTrue(
            snapshots.all { it.seat in 0..1 },
            "snapshots should be addressed to seats 0 and 1",
        )

        val eventsFrames = step.outbound.filterIsInstance<Addressed>()
            .filter { it.message is ServerMessage.Events }
        assertTrue(
            eventsFrames.isNotEmpty(),
            "should have at least one Events frame",
        )
        assertTrue(
            eventsFrames.all { (it.message as ServerMessage.Events).events.any { e -> e is HandStarted } },
            "each Events frame should contain a HandStarted",
        )
    }

    @Test
    fun exactlyOneSeatIsPromptedToAct() {
        val step = startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L)
        val yourTurns = step.outbound.filterIsInstance<Addressed>()
            .filter { it.message is ServerMessage.YourTurn }
        assertEquals(1, yourTurns.size, "should have exactly one YourTurn")
        assertEquals(
            step.runner.hand!!.state.seatToAct,
            yourTurns[0].seat,
            "YourTurn should be addressed to the seat on turn",
        )
    }

    @Test
    fun theOpeningHandAlwaysHasASeatToAct() {
        // Tightest legal format: startingStack = 100, big blind = 100
        val tightFormat = DuelFormat(
            startingStack = 100,
            blinds = BlindSchedule(listOf(BlindLevel(50, 100)), 10),
            endCondition = EndCondition.Freezeout,
        )
        val step = startDuel(tightFormat, buttonSeat = 0, seed = 7L)
        assertNotNull(
            step.runner.hand!!.state.seatToAct,
            "opening hand should always have a seat to act",
        )
    }

    @Test
    fun theMatchLogStartsEmptyAndUnfinished() {
        val step = startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L)
        assertTrue(step.runner.log.hands.isEmpty(), "log.hands should be empty")
        assertTrue(step.runner.log.events.isEmpty(), "log.events should be empty")
        assertEquals(null, step.runner.outcome, "outcome should be null")
    }
}
