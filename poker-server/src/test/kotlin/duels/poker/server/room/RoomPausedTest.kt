package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.PlayerAction
import duels.poker.server.duel.Addressed
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.duel.decisionPointOf
import duels.poker.server.duel.startDuel
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.ProtocolError
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

private val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
private val seeds = HandSeedSource { 7L }

private fun playingRoom(): Room {
    val opened = Room.open(RoomCode("2B7KMNPQ"), PlayerId("host"), oneHand, now = 1_000L)
    val seated = (opened.join(PlayerId("guest"), now = 2_000L) as JoinResult.Seated).room
    val started = startDuel(seated.format, seated.openingButtonSeat, seed = 7L)
    return seated.copy(runner = started.runner, duelId = UUID.randomUUID())
}

/** Builds the [Act] frame for whoever is on turn in [room]'s live hand. */
private fun callFrame(room: Room): Act {
    val hand = room.runner!!.hand!!
    return Act(
        handNumber = hand.state.handNumber,
        actionSequence = decisionPointOf(hand.log.events)!!.sequence,
        action = PlayerAction.Call(hand.state.seatToAct!!),
    )
}

internal class RoomPausedTest {

    @Test
    fun theOpponentOfADroppedSeatIsRefused() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val frame = callFrame(room)
        val paused = room.copy(gracePeriods = mapOf(1 - onTurn to 9_000L))

        val step = paused.act(onTurn, frame, seeds)

        assertNotNull(step)
        assertEquals(
            listOf(Addressed(onTurn, ServerMessage.Failure(ProtocolError.DUEL_PAUSED))),
            step!!.outbound,
        )
    }

    @Test
    fun aRefusedActionMovesTheDuelNowhere() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val frame = callFrame(room)
        val paused = room.copy(gracePeriods = mapOf(1 - onTurn to 9_000L))

        val step = paused.act(onTurn, frame, seeds)

        // Identity, not equality: nothing was applied, so the runner handed back must be the
        // very same instance that went in, not merely one that happens to look the same.
        assertSame(room.runner, step!!.runner)
    }

    @Test
    fun nothingIsSentToTheSeatThatIsGone() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val frame = callFrame(room)
        val paused = room.copy(gracePeriods = mapOf(1 - onTurn to 9_000L))

        val step = paused.act(onTurn, frame, seeds)

        assertTrue(step!!.outbound.none { it.seat == 1 - onTurn })
    }

    @Test
    fun aSeatInsideItsOwnWindowIsRefusedToo() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val frame = callFrame(room)
        // Pausing is keyed to the room, not to who is speaking: the seat about to act is the
        // one counting down here, not its opponent, and it is refused all the same.
        val paused = room.copy(gracePeriods = mapOf(onTurn to 9_000L))

        val step = paused.act(onTurn, frame, seeds)

        assertEquals(
            listOf(Addressed(onTurn, ServerMessage.Failure(ProtocolError.DUEL_PAUSED))),
            step!!.outbound,
        )
        assertSame(room.runner, step.runner)
    }

    @Test
    fun aRoomWhoseSeatIsAlreadyAbsentIsNotPaused() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val frame = callFrame(room)
        // The falsifying case: the opponent's window has already run out into absentSeats,
        // gracePeriods is empty, and isPaused is therefore false — the legal Call must be
        // applied for real, proving the branch above is not vacuously refusing everything.
        val absent = room.copy(absentSeats = setOf(1 - onTurn))

        val step = absent.act(onTurn, frame, seeds)

        assertNotNull(step)
        assertNotSame(room.runner, step!!.runner)
        assertTrue(step.outbound.none { it.message is ServerMessage.Failure })
    }

    @Test
    fun aFinishedRoomStillAnswersNull() {
        val room = playingRoom()
        val onTurn = room.runner!!.hand!!.state.seatToAct!!
        val frame = callFrame(room)
        val finished = room.copy(state = RoomState.FINISHED, gracePeriods = mapOf(0 to 9_000L))

        val step = finished.act(onTurn, frame, seeds)

        assertNull(step)
    }

    @Test
    fun aRoomWithNoRunnerStillAnswersNull() {
        val room = Room.open(RoomCode("2B7KMNPQ"), PlayerId("host"), oneHand, now = 1_000L)
        val frame = Act(handNumber = 1, actionSequence = 0, action = PlayerAction.Check(0))

        val step = room.act(0, frame, seeds)

        assertNull(step)
    }
}
