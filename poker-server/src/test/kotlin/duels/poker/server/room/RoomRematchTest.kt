package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.PlayerAction
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.duel.decisionPointOf
import duels.poker.server.duel.startDuel
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoomRematchTest {
    private val host = PlayerId("host")
    private val guest = PlayerId("guest")
    private val code = RoomCode("ABCDEFGH")
    private val fixedSeeds = HandSeedSource { 7L }
    private val oneHand = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))

    private fun finishedRoom(): Room {
        val waitingRoom = Room.open(code, host, DuelFormat.DEFAULT, now = 1_000L)
        val seated = (waitingRoom.join(guest, 1_000L) as JoinResult.Seated).room
        return seated.finish(2_000L)
    }

    /** A registry that always mints [code], seeded so every opening hand it deals is reproducible. */
    private fun scriptedRegistry(): RoomRegistry {
        val codes = object : RoomCodeSource {
            override fun newRoomCode(): RoomCode = code
        }
        return RoomRegistry(codes, MutableClock(), seeds = fixedSeeds)
    }

    /**
     * Seats [guest] into a fresh room in [registry] and finishes it via [RoomRegistry.finish]'s
     * test-only affordance, without playing a hand to its end — the rematch tests below only care
     * about the room this leaves behind, at [code], not about how it came to be finished.
     */
    private suspend fun finishedRoomIn(registry: RoomRegistry) {
        registry.create(host)
        registry.join(code, guest)
        registry.finish(code)
    }

    /**
     * Builds the [Act] frame shoving all-in on the seat currently on turn — a non-terminal action
     * that leaves a real bet on the table, so the decision moves to the other seat, facing a
     * shove, instead of ending the hand.
     */
    private fun allInFrame(room: Room): Act {
        val hand = room.runner!!.hand!!
        return Act(
            handNumber = hand.state.handNumber,
            actionSequence = decisionPointOf(hand.log.events)!!.sequence,
            action = PlayerAction.AllIn(hand.state.seatToAct!!),
        )
    }

    /** Builds the [Act] frame folding the seat currently on turn, ending [oneHand]'s one hand. */
    private fun foldFrame(room: Room): Act {
        val hand = room.runner!!.hand!!
        return Act(
            handNumber = hand.state.handNumber,
            actionSequence = decisionPointOf(hand.log.events)!!.sequence,
            action = PlayerAction.Fold(hand.state.seatToAct!!),
        )
    }

    @Test
    fun oneOfferLeavesTheRoomFinished() {
        val room = finishedRoom()

        val result = room.offerRematch(host, now = 3_000L)

        assertTrue(result is RematchResult.Offered)
        val offered = (result as RematchResult.Offered).room
        assertEquals(RoomState.FINISHED, offered.state)
        assertEquals(setOf(host), offered.rematchOffers)
        assertEquals(room.match, offered.match)
    }

    @Test
    fun bothOffersReturnTheRoomToPlaying() {
        val room = finishedRoom()
        val afterHost = (room.offerRematch(host, now = 3_000L) as RematchResult.Offered).room

        val result = afterHost.offerRematch(guest, now = 4_000L)

        assertTrue(result is RematchResult.Agreed)
        val agreed = (result as RematchResult.Agreed).room
        assertEquals(RoomState.PLAYING, agreed.state)
        assertTrue(agreed.rematchOffers.isEmpty())
        assertEquals(4_000L, agreed.lastActivityAt)
    }

    @Test
    fun theRematchButtonSitsOnTheOtherSeat() {
        val room = finishedRoom()
        val afterHost = (room.offerRematch(host, now = 3_000L) as RematchResult.Offered).room

        val agreed = (afterHost.offerRematch(guest, now = 4_000L) as RematchResult.Agreed).room

        assertEquals(1, agreed.openingButtonSeat)
        assertEquals(1, agreed.match!!.buttonSeat)
        assertEquals(0, agreed.match!!.handsPlayed)
        assertTrue(agreed.match!!.stacks.all { it == agreed.format.startingStack })
    }

    @Test
    fun aSecondRematchReturnsTheButtonToTheHost() {
        val room = finishedRoom()
        val afterHost = (room.offerRematch(host, now = 3_000L) as RematchResult.Offered).room
        val firstRematch = (afterHost.offerRematch(guest, now = 4_000L) as RematchResult.Agreed).room

        val secondFinished = firstRematch.finish(now = 5_000L)
        val secondAfterHost =
            (secondFinished.offerRematch(host, now = 6_000L) as RematchResult.Offered).room
        val secondAgreed =
            (secondAfterHost.offerRematch(guest, now = 7_000L) as RematchResult.Agreed).room

        assertEquals(0, secondAgreed.openingButtonSeat)
        assertEquals(0, secondAgreed.match!!.buttonSeat)
    }

    @Test
    fun offeringTwiceFromOneSeatIsRefused() {
        val room = finishedRoom()
        val afterHost = (room.offerRematch(host, now = 3_000L) as RematchResult.Offered).room

        val result = afterHost.offerRematch(host, now = 4_000L)

        assertEquals(RematchResult.Refused(RematchRefusal.ALREADY_OFFERED), result)
        assertEquals(setOf(host), afterHost.rematchOffers)
    }

    @Test
    fun aStrangerCannotOfferARematch() {
        val room = finishedRoom()

        val result = room.offerRematch(PlayerId("nobody"), now = 3_000L)

        assertEquals(RematchResult.Refused(RematchRefusal.NOT_A_PLAYER), result)
    }

    @Test
    fun aRematchOfferBeforeTheDuelEndsIsRefused() {
        val playingRoom =
            (Room.open(code, host, DuelFormat.DEFAULT, now = 1_000L).join(guest, 1_000L) as JoinResult.Seated).room
        val waitingRoom = Room.open(code, host, DuelFormat.DEFAULT, now = 1_000L)

        assertEquals(
            RematchResult.Refused(RematchRefusal.NOT_FINISHED),
            playingRoom.offerRematch(host, now = 2_000L),
        )
        assertEquals(
            RematchResult.Refused(RematchRefusal.NOT_FINISHED),
            waitingRoom.offerRematch(host, now = 2_000L),
        )
    }

    @Test
    fun theRematchOpeningFramesReachBothSeats() = runBlocking {
        val registry = scriptedRegistry()
        finishedRoomIn(registry)

        registry.offerRematch(code, host)
        val agreed = registry.offerRematch(code, guest) as RematchResult.Agreed

        assertTrue(agreed.outbound.any { it.seat == 0 && it.message is ServerMessage.Snapshot })
        assertTrue(agreed.outbound.any { it.seat == 1 && it.message is ServerMessage.Snapshot })
    }

    @Test
    fun exactlyOneSeatIsToldItIsItsTurn() = runBlocking {
        val registry = scriptedRegistry()
        finishedRoomIn(registry)

        registry.offerRematch(code, host)
        val agreed = registry.offerRematch(code, guest) as RematchResult.Agreed

        assertEquals(1, agreed.outbound.count { it.message is ServerMessage.YourTurn })
    }

    @Test
    fun theFramesAreTheOnesTheRunnerProduced() = runBlocking {
        val registry = scriptedRegistry()
        finishedRoomIn(registry)

        registry.offerRematch(code, host)
        val agreed = registry.offerRematch(code, guest) as RematchResult.Agreed

        // Independently rebuilding the opening hand from the same inputs `withFreshRunner` used —
        // the agreed room's format and its now-flipped button seat, plus the fixed seed every hand
        // in this registry draws — must equal `agreed.outbound` exactly, byte for byte, or the
        // registry reordered, rebuilt or invented frames instead of just handing back the runner's.
        // The two `TurnClock` frames the fresh deadline owes both seats are not part of
        // `startDuel`'s own output, so they are checked separately below rather than folded in.
        val expected = startDuel(agreed.room.format, agreed.room.openingButtonSeat, seed = 7L)

        assertEquals(expected.outbound, agreed.outbound.dropLast(2))

        val clocks = agreed.outbound.takeLast(2)
        assertEquals(2, clocks.size)
        assertTrue(clocks.all { it.message is ServerMessage.TurnClock })
        assertEquals(setOf(0, 1), clocks.map { it.seat }.toSet())
    }

    @Test
    fun anAgreedRematchRefillsBothBanks() = runBlocking {
        val clock = MutableClock()
        val codes = object : RoomCodeSource {
            override fun newRoomCode(): RoomCode = code
        }
        val registry = RoomRegistry(codes, clock, seeds = fixedSeeds)
        registry.create(host, oneHand)
        registry.join(code, guest)

        // Two real decisions, each spending a different amount of bank for a different seat, so
        // that "both seats read the configured full bank after the rematch" cannot be explained
        // by a seat the rematch never touched having simply stayed full the whole time.
        val opener = registry.get(code)!!.runner!!.hand!!.state.seatToAct!!
        assertEquals(0, opener)
        clock.advance(RoomTimeouts.DEFAULT.turnMillis + 20_000L)
        registry.act(code) { it.act(opener, allInFrame(it), registry.handSeeds) }

        val responder = registry.get(code)!!.runner!!.hand!!.state.seatToAct!!
        assertEquals(1, responder)
        clock.advance(RoomTimeouts.DEFAULT.turnMillis + 15_000L)
        registry.act(code) { it.act(responder, foldFrame(it), registry.handSeeds) }

        assertEquals(RoomState.FINISHED, registry.get(code)!!.state)
        assertEquals(
            mapOf(
                0 to RoomTimeouts.DEFAULT.timebankMillis - 20_000L,
                1 to RoomTimeouts.DEFAULT.timebankMillis - 15_000L,
            ),
            registry.get(code)!!.timebankRemainingMillis,
        )

        registry.offerRematch(code, host)
        val agreed = registry.offerRematch(code, guest) as RematchResult.Agreed

        assertEquals(
            mapOf(0 to RoomTimeouts.DEFAULT.timebankMillis, 1 to RoomTimeouts.DEFAULT.timebankMillis),
            agreed.room.timebankRemainingMillis,
        )
    }

    @Test
    fun anAgreedRematchClocksItsOpeningDecision() = runBlocking {
        val registry = scriptedRegistry()
        finishedRoomIn(registry)

        registry.offerRematch(code, host)
        val agreed = registry.offerRematch(code, guest) as RematchResult.Agreed

        val clocks = agreed.outbound.takeLast(2)
        assertEquals(2, clocks.size)
        assertEquals(setOf(0, 1), clocks.map { it.seat }.toSet())
        for (frame in clocks) {
            val message = frame.message
            assertTrue(message is ServerMessage.TurnClock)
            assertEquals(RoomTimeouts.DEFAULT.turnMillis, (message as ServerMessage.TurnClock).turnRemainingMillis)
        }
    }
}
