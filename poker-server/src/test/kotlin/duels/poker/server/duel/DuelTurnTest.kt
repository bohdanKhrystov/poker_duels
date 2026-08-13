package duels.poker.server.duel

import duels.poker.engine.game.ActionOn
import duels.poker.engine.game.startHand
import duels.poker.engine.random.SplitMix64Rng
import duels.poker.server.protocol.ServerMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DuelTurnTest {
    @Test
    fun theSeatOnTurnIsTheOneAddressed() {
        val fixture = startHand(
            handNumber = 1,
            buttonSeat = 0,
            stacks = listOf(1_000, 1_000),
            smallBlind = 50,
            bigBlind = 100,
            rng = SplitMix64Rng(7),
        )
        val state = fixture.newState
        val events = fixture.events

        val result = turnFor(state, events)

        assertEquals(state.seatToAct, result!!.seat, "The turn should be addressed to the seat on turn")
    }

    @Test
    fun theOtherSeatGetsNoTurnFrame() {
        val fixture = startHand(
            handNumber = 1,
            buttonSeat = 0,
            stacks = listOf(1_000, 1_000),
            smallBlind = 50,
            bigBlind = 100,
            rng = SplitMix64Rng(7),
        )
        val state = fixture.newState
        val events = fixture.events

        val result = framesFor(state, events, events)

        val yourTurnFrames = result.filter { it.message is ServerMessage.YourTurn }
        assertEquals(1, yourTurnFrames.size, "Should have exactly one YourTurn frame")
        assertEquals(state.seatToAct, yourTurnFrames[0].seat, "YourTurn should be addressed to the seat on turn")
    }

    @Test
    fun theActionSequenceIsTheLastActionOnsSequence() {
        val fixture = startHand(
            handNumber = 1,
            buttonSeat = 0,
            stacks = listOf(1_000, 1_000),
            smallBlind = 50,
            bigBlind = 100,
            rng = SplitMix64Rng(7),
        )
        val state = fixture.newState
        val events = fixture.events

        val result = turnFor(state, events)

        val lastActionOn = events.filterIsInstance<ActionOn>().last()
        assertEquals(lastActionOn.sequence, (result!!.message as ServerMessage.YourTurn).actionSequence)
        assertEquals(state.handNumber, (result.message as ServerMessage.YourTurn).handNumber)
    }

    @Test
    fun theLegalActionsAreTheEnginesOwn() {
        val fixture = startHand(
            handNumber = 1,
            buttonSeat = 0,
            stacks = listOf(1_000, 1_000),
            smallBlind = 50,
            bigBlind = 100,
            rng = SplitMix64Rng(7),
        )
        val state = fixture.newState
        val events = fixture.events

        val result = turnFor(state, events)

        val legalActionsFromEngine = duels.poker.engine.game.legalActions(state)
        assertEquals(legalActionsFromEngine, (result!!.message as ServerMessage.YourTurn).legalActions)
    }

    @Test
    fun aHandWithNobodyToActPromptsNobody() {
        val fixture = startHand(
            handNumber = 1,
            buttonSeat = 0,
            stacks = listOf(1_000, 1_000),
            smallBlind = 50,
            bigBlind = 100,
            rng = SplitMix64Rng(7),
        )
        val state = fixture.newState.copy(seatToAct = null)
        val events = fixture.events

        val turnResult = turnFor(state, events)
        val framesResult = framesFor(state, events, events)

        assertNull(turnResult, "turnFor should return null when seatToAct is null")
        val yourTurnFrames = framesResult.filter { it.message is ServerMessage.YourTurn }
        assertEquals(0, yourTurnFrames.size, "framesFor should contain no YourTurn when seatToAct is null")
    }

    @Test
    fun aDecisionPointNamingAnotherSeatFailsLoudly() {
        val fixture = startHand(
            handNumber = 1,
            buttonSeat = 0,
            stacks = listOf(1_000, 1_000),
            smallBlind = 50,
            bigBlind = 100,
            rng = SplitMix64Rng(7),
        )
        val state = fixture.newState
        val events = fixture.events

        // Create a modified events list where the last ActionOn names the opposite seat
        val wrongSeat = 1 - state.seatToAct!!
        val lastActionOn = events.filterIsInstance<ActionOn>().last()
        val modifiedEvents = events.dropLast(1) + ActionOn(sequence = lastActionOn.sequence, seat = wrongSeat)

        assertFailsWith<IllegalStateException> {
            turnFor(state, modifiedEvents)
        }
    }

    @Test
    fun framesForPutsTheTurnAfterTheSnapshot() {
        val fixture = startHand(
            handNumber = 1,
            buttonSeat = 0,
            stacks = listOf(1_000, 1_000),
            smallBlind = 50,
            bigBlind = 100,
            rng = SplitMix64Rng(7),
        )
        val state = fixture.newState
        val events = fixture.events

        val result = framesFor(state, events, events)

        val snapshotIndices = result.mapIndexed { index, addressed ->
            if (addressed.message is ServerMessage.Snapshot) index else -1
        }.filter { it >= 0 }

        val yourTurnIndices = result.mapIndexed { index, addressed ->
            if (addressed.message is ServerMessage.YourTurn) index else -1
        }.filter { it >= 0 }

        if (yourTurnIndices.isNotEmpty()) {
            assertTrue(
                yourTurnIndices.all { turnIndex ->
                    snapshotIndices.all { snapshotIndex -> turnIndex > snapshotIndex }
                },
                "YourTurn frame should come after all Snapshot frames",
            )
        }
    }
}
