package duels.poker.server.duel

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.server.protocol.ServerMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `resumeFrames` proves it never invents a view: everything it hands back must already be what
 * [PlayerView.of] and the per-seat filter (via [framesFor] and [finishedFrames]) would have
 * produced, addressed only to the seat that asked.
 */
class DuelResumeTest {
    private val liveRunner = startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L).runner
    private val onTurn = liveRunner.hand!!.state.seatToAct!!

    private val finishedRunner =
        playDuel(
            seed = 7L,
            format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1)),
        ).runner

    @Test
    fun aReturningSeatSeesItsOwnCards() {
        val snapshots = resumeFrames(liveRunner, 0).filter { it.message is ServerMessage.Snapshot }
        assertEquals(1, snapshots.size, "seat 0's resume should hold exactly one Snapshot")

        val view = (snapshots.single().message as ServerMessage.Snapshot).view
        assertEquals(2, view.viewer.holeCards.size, "seat 0 should see its own two hole cards")
    }

    @Test
    fun aReturningSeatNeverSeesTheOpponentsCards() {
        for (seat in 0..1) {
            val opponent = 1 - seat
            assertEquals(
                2,
                liveRunner.hand!!.state.seat(opponent).holeCards.size,
                "the opponent must actually hold two hole cards, or hiding them proves nothing",
            )

            val snapshots = resumeFrames(liveRunner, seat).filter { it.message is ServerMessage.Snapshot }
            assertEquals(1, snapshots.size, "seat $seat's resume should hold exactly one Snapshot")

            val view = (snapshots.single().message as ServerMessage.Snapshot).view
            assertTrue(view.opponent.holeCards.isEmpty(), "seat $seat must never see the opponent's hole cards")
        }
    }

    @Test
    fun nothingIsAddressedToTheOtherSeat() {
        assertTrue(resumeFrames(liveRunner, 0).all { it.seat == 0 }, "seat 0's resume must address only seat 0")
        assertTrue(resumeFrames(liveRunner, 1).all { it.seat == 1 }, "seat 1's resume must address only seat 1")
    }

    @Test
    fun theSeatOnTurnIsPrompted() {
        val turns = resumeFrames(liveRunner, onTurn).filter { it.message is ServerMessage.YourTurn }
        assertEquals(1, turns.size, "the seat on turn should hold exactly one YourTurn")

        val yourTurn = turns.single().message as ServerMessage.YourTurn
        val hand = liveRunner.hand!!
        val decisionPoint = decisionPointOf(hand.log.events)!!
        assertEquals(hand.state.handNumber, yourTurn.handNumber, "handNumber must match the hand's decision point")
        assertEquals(
            decisionPoint.sequence,
            yourTurn.actionSequence,
            "actionSequence must match the hand's decision point",
        )
    }

    @Test
    fun theSeatNotOnTurnIsNotPrompted() {
        val turns = resumeFrames(liveRunner, 1 - onTurn).filter { it.message is ServerMessage.YourTurn }
        assertTrue(turns.isEmpty(), "the seat not on turn must hold no YourTurn")
    }

    @Test
    fun noEventsAreReplayed() {
        assertTrue(
            liveRunner.hand!!.log.events.isNotEmpty(),
            "the fixture hand must have events, or this test proves nothing",
        )

        assertTrue(resumeFrames(liveRunner, 0).none { it.message is ServerMessage.Events }, "seat 0 must not replay events")
        assertTrue(resumeFrames(liveRunner, 1).none { it.message is ServerMessage.Events }, "seat 1 must not replay events")
    }

    @Test
    fun aFinishedDuelResumesAsItsOutcome() {
        assertNull(finishedRunner.hand, "the finished fixture must have no live hand")
        assertNotNull(finishedRunner.outcome, "the finished fixture must have an outcome")
        val outcome = finishedRunner.outcome!!

        for (seat in 0..1) {
            val frames = resumeFrames(finishedRunner, seat)
            assertEquals(1, frames.size, "seat $seat's resume of a finished duel should hold exactly one frame")

            val message = frames.single().message
            assertTrue(message is ServerMessage.DuelFinished, "seat $seat's resume should be a DuelFinished frame")
            assertEquals(outcome, (message as ServerMessage.DuelFinished).outcome, "the frame must carry the duel's outcome")
            assertTrue(frames.none { it.message is ServerMessage.Snapshot }, "a finished duel's resume must hold no Snapshot")
            assertTrue(frames.none { it.message is ServerMessage.YourTurn }, "a finished duel's resume must hold no YourTurn")
        }
    }

    @Test
    fun aSeatOutsideTheTableIsRefused() {
        assertFailsWith<IllegalArgumentException> { resumeFrames(liveRunner, 2) }
    }
}
