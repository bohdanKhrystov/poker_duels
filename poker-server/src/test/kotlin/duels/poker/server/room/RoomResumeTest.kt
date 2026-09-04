package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.PlayerAction
import duels.poker.server.duel.Addressed
import duels.poker.server.duel.decisionPointOf
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.SeatPresence
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

private fun newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())

private fun codeSource(vararg codes: String): RoomCodeSource {
    val iterator = codes.iterator()
    return RoomCodeSource { RoomCode(iterator.next()) }
}

private val TEST_TIMEOUTS = RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000)

private val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))

/** Builds the finishing [Act] frame — a fold on the seat currently on turn. */
private fun foldFrame(room: Room): Act {
    val hand = room.runner!!.hand!!
    return Act(
        handNumber = hand.state.handNumber,
        actionSequence = decisionPointOf(hand.log.events)!!.sequence,
        action = PlayerAction.Fold(hand.state.seatToAct!!),
    )
}

internal class RoomResumeTest {

    @Test
    fun aReturningPlayerIsToldItsSeatAndItsOwnState(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, guest)

        val resumption = registry.resume(room.code, guest)

        assertEquals(1, resumption!!.seat)
        assertTrue(resumption.outbound.isNotEmpty())
        assertTrue(resumption.outbound.filterNot { it.message is ServerMessage.OpponentPresence }.all { it.seat == 1 })
        assertTrue(resumption.outbound.any { it.message is ServerMessage.Snapshot })
    }

    @Test
    fun aResumeStopsTheCountdown(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, guest)

        registry.resume(room.code, guest)

        assertEquals(emptySet<Int>(), registry.get(room.code)!!.awaySeats)
        assertFalse(registry.get(room.code)!!.awaySeats.isNotEmpty())
    }

    @Test
    fun astrangerMayNotTakeAHeldSeat(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, guest)

        val result = registry.resume(room.code, PlayerId("stranger"))

        assertNull(result)
        assertEquals(setOf(1), registry.get(room.code)!!.awaySeats)
    }

    @Test
    fun aPlayerWhoNeverDroppedMayStillResume(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        val resumption = registry.resume(room.code, guest)

        assertEquals(1, resumption!!.seat)
    }

    @Test
    fun aFinishedRoomResumesAsItsOutcome(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host, oneHand)
        registry.join(room.code, guest)
        val seat = foldFrame(registry.get(room.code)!!).action.seat
        registry.act(room.code) { it.act(seat, foldFrame(it), registry.handSeeds) }
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)

        val resumption = registry.resume(room.code, host)

        // A finished room resumes with exactly two frames: DuelFinished and OpponentPresence.
        // The host resumes without having been away, so presenceOf(guest) reads PRESENT, the same as
        // presenceOf(host) would — a value mixup between the two is undetectable here. That mixup
        // is caught on the running-room path by theReturningSeatIsToldTheOpponentIsAway, where
        // the two presences differ. This assertion proves the frame exists, is addressed correctly,
        // and there is exactly one of each frame type.
        assertEquals(2, resumption!!.outbound.size)

        val duelFinished = resumption.outbound.filter { it.message is ServerMessage.DuelFinished }
        assertEquals(1, duelFinished.size)
        assertEquals(0, duelFinished.single().seat)

        val opponentPresence = resumption.outbound.filter { it.message is ServerMessage.OpponentPresence }
        assertEquals(1, opponentPresence.size)
        assertEquals(0, opponentPresence.single().seat)
        val presence = (opponentPresence.single().message as ServerMessage.OpponentPresence).presence
        assertEquals(SeatPresence.PRESENT, presence)

        assertTrue(resumption.outbound.none { it.message is ServerMessage.Snapshot })
    }

    @Test
    fun theReturningSeatIsToldTheOpponentIsPresent(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, guest)

        val resumption = registry.resume(room.code, guest)

        assertTrue(resumption!!.outbound.contains(Addressed(1, ServerMessage.OpponentPresence(SeatPresence.PRESENT))))
    }

    @Test
    fun theSeatThatStayedIsToldTheOtherIsBack(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, guest)

        val resumption = registry.resume(room.code, guest)

        assertTrue(resumption!!.outbound.contains(Addressed(0, ServerMessage.OpponentPresence(SeatPresence.PRESENT))))
    }

    /**
     * The mirror of [theReturningSeatIsToldTheOpponentIsPresent] and
     * [theSeatThatStayedIsToldTheOtherIsBack] together: both above disconnect and resume the
     * guest, so this drops and returns the host instead, checking both addresses the two above
     * split across two tests.
     */
    @Test
    fun aReturningHostIsToldAndTellsTheGuest(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, host)

        val resumption = registry.resume(room.code, host)

        assertTrue(resumption!!.outbound.contains(Addressed(0, ServerMessage.OpponentPresence(SeatPresence.PRESENT))))
        assertTrue(resumption.outbound.contains(Addressed(1, ServerMessage.OpponentPresence(SeatPresence.PRESENT))))
    }

    // The returning seat here is the host (0), not the guest (1) every scenario above resumes as:
    // buttonSeat is fixed at 0 throughout this suite, so a "the returning seat" frame that hardcoded
    // seat 1 (or an "other seat" frame that hardcoded seat 0) would pass every test above by
    // coincidence alone. This one, and the two below, resume as the host instead, so the address
    // itself — not just the presence value — is what a mistake here would get wrong.
    @Test
    fun nobodyIsToldAboutASeatNobodyWasWaitingFor(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        val resumption = registry.resume(room.code, host)

        assertTrue(resumption!!.outbound.none { it.seat == 1 })
        assertTrue(resumption.outbound.contains(Addressed(0, ServerMessage.OpponentPresence(SeatPresence.PRESENT))))
    }

    @Test
    fun theReturningSeatIsToldTheOpponentIsAway(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, guest)
        clock.advance(5_000)

        val resumption = registry.resume(room.code, host)

        assertTrue(
            resumption!!.outbound.contains(
                Addressed(0, ServerMessage.OpponentPresence(SeatPresence.AWAY)),
            ),
        )
    }

    @Test
    fun theReturningSeatIsToldTheOpponentIsAbsent(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, guest)

        // The guest is away but not yet on turn — the host always opens a fresh room on turn
        // (Room.open) — so the guest's own deadline only arms once the host's call passes the
        // turn to it.
        val opening = registry.get(room.code)!!
        val hand = opening.runner!!.hand!!
        val openingFrame = Act(
            handNumber = hand.state.handNumber,
            actionSequence = decisionPointOf(hand.log.events)!!.sequence,
            action = PlayerAction.Call(hand.state.seatToAct!!),
        )
        registry.act(room.code) { r -> r.act(openingFrame.action.seat, openingFrame, registry.handSeeds) }
        val deadline = registry.get(room.code)!!.turnDeadline!!.expiresAt

        clock.advance(deadline - clock.nowMillis())
        registry.expireTurnClocks()

        val resumption = registry.resume(room.code, host)

        assertTrue(resumption!!.outbound.contains(Addressed(0, ServerMessage.OpponentPresence(SeatPresence.ABSENT))))
    }

    @Test
    fun thePresenceFollowsTheResumedFrames(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, guest)

        val resumption = registry.resume(room.code, guest)

        val snapshotIndex = resumption!!.outbound.indexOfFirst { it.message is ServerMessage.Snapshot }
        assertTrue(snapshotIndex >= 0)
        val presenceIndices =
            resumption.outbound.withIndex()
                .filter { it.value.message is ServerMessage.OpponentPresence }
                .map { it.index }
        assertTrue(presenceIndices.isNotEmpty())
        assertTrue(presenceIndices.all { it > snapshotIndex })
    }

    @Test
    fun noEventsAreReplayed(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, guest)

        val resumption = registry.resume(room.code, guest)

        assertTrue(resumption!!.outbound.none { it.message is ServerMessage.Events })
    }

    @Test
    fun aWaitingRoomHasNothingToResume(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val room = registry.create(host)

        val result = registry.resume(room.code, host)

        assertNull(result)
    }

    @Test
    fun anAbandonedRoomHasNothingToResume(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.abandon(room.code)

        val result = registry.resume(room.code, guest)

        assertNull(result)
    }

    @Test
    fun anUnknownCodeAnswersNull(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        registry.create(host)

        val result = registry.resume(RoomCode("ZZZZZZZZ"), host)

        assertNull(result)
    }

    @Test
    fun aResumeDoesNotDisturbTheOtherSeatsCountdown(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, host)
        clock.advance(5_000)
        registry.disconnect(room.code, guest)

        val resumption = registry.resume(room.code, guest)

        assertEquals(1, resumption!!.seat)
        assertEquals(setOf(0), registry.get(room.code)!!.awaySeats)
        assertTrue(registry.get(room.code)!!.awaySeats.isNotEmpty())
    }

    @Test
    fun aResumingSeatIsToldTheLiveDeadline(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)
        registry.disconnect(room.code, guest)

        val resumption = registry.resume(room.code, guest)

        // The live decision point, read independently off the runner, the same way `foldFrame`
        // above does — not assumed to belong to the resuming seat, since the frame's own `seat`
        // names whoever is actually on turn, and `Addressed.seat` alone names the recipient.
        val hand = registry.get(room.code)!!.runner!!.hand!!
        val clocks = resumption!!.outbound.filter { it.message is ServerMessage.TurnClock }
        assertEquals(1, clocks.size)
        val frame = clocks.single()
        assertEquals(1, frame.seat)
        val turnClock = frame.message as ServerMessage.TurnClock
        assertEquals(hand.state.seatToAct!!, turnClock.seat)
        assertEquals(hand.state.handNumber, turnClock.handNumber)
        assertEquals(decisionPointOf(hand.log.events)!!.sequence, turnClock.actionSequence)
    }

    @Test
    fun aResumingSeatIsToldTheClockEvenWhenItNeverLeft(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host)
        registry.join(room.code, guest)

        val resumption = registry.resume(room.code, guest)

        assertEquals(1, resumption!!.outbound.count { it.message is ServerMessage.TurnClock })
    }

    @Test
    fun aFinishedRoomsResumeStatesNoClock(): Unit = runBlocking {
        val clock = MutableClock()
        val registry = RoomRegistry(codeSource("2B7KMNPQ"), clock, TEST_TIMEOUTS)
        val host = newPlayerId()
        val guest = newPlayerId()
        val room = registry.create(host, oneHand)
        registry.join(room.code, guest)
        val seat = foldFrame(registry.get(room.code)!!).action.seat
        registry.act(room.code) { it.act(seat, foldFrame(it), registry.handSeeds) }
        assertEquals(RoomState.FINISHED, registry.get(room.code)!!.state)

        val resumption = registry.resume(room.code, host)

        assertTrue(resumption!!.outbound.none { it.message is ServerMessage.TurnClock })
    }
}
