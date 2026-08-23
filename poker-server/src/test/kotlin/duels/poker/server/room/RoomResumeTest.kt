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

private val TEST_TIMEOUTS = RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000, disconnectGraceMillis = 30_000)

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

        assertEquals(emptyMap<Int, Long>(), registry.get(room.code)!!.gracePeriods)
        assertFalse(registry.get(room.code)!!.isPaused)
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
        assertEquals(mapOf(1 to 30_000L), registry.get(room.code)!!.gracePeriods)
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

        val duelFinished = resumption!!.outbound.filter { it.message is ServerMessage.DuelFinished }
        assertEquals(1, duelFinished.size)
        assertEquals(0, duelFinished.single().seat)
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
                Addressed(0, ServerMessage.OpponentPresence(SeatPresence.AWAY, graceRemainingMillis = 25_000)),
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
        clock.advance(TEST_TIMEOUTS.disconnectGraceMillis)
        registry.expireGracePeriods()

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
        assertEquals(mapOf(0 to 30_000L), registry.get(room.code)!!.gracePeriods)
        assertTrue(registry.get(room.code)!!.isPaused)
    }
}
