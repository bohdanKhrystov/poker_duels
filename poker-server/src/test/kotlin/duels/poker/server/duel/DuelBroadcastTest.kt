package duels.poker.server.duel

import duels.poker.engine.game.HandRevealed
import duels.poker.engine.game.startHand
import duels.poker.engine.random.SplitMix64Rng
import duels.poker.server.protocol.ServerMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DuelBroadcastTest {
    @Test
    fun eachSeatGetsASnapshotOfItsOwnView() {
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

        val result = broadcast(state, events, events)

        val snapshots = result.filter { it.message is ServerMessage.Snapshot }
        assertEquals(2, snapshots.size, "Should have exactly two Snapshot frames")

        snapshots.forEach { addressed ->
            val snapshot = addressed.message as ServerMessage.Snapshot
            assertEquals(addressed.seat, snapshot.view.viewerSeat, "Each snapshot's viewerSeat must match the addressed seat")
        }
    }

    @Test
    fun aSnapshotShowsOnlyTheRecipientsHoleCards() {
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

        val result = broadcast(state, events, events)

        val seat0Snapshot = (result.find { it.seat == 0 && it.message is ServerMessage.Snapshot }?.message as ServerMessage.Snapshot).view
        val seat1Snapshot = (result.find { it.seat == 1 && it.message is ServerMessage.Snapshot }?.message as ServerMessage.Snapshot).view

        // Seat 0's snapshot should show seat 0's hole cards but not seat 1's
        assertEquals(2, seat0Snapshot.seats[0].holeCards.size, "Seat 0's snapshot should show its own 2 hole cards")
        assertEquals(0, seat0Snapshot.seats[1].holeCards.size, "Seat 0's snapshot should not show opponent's hole cards")

        // Seat 1's snapshot should show seat 1's hole cards but not seat 0's
        assertEquals(0, seat1Snapshot.seats[0].holeCards.size, "Seat 1's snapshot should not show opponent's hole cards")
        assertEquals(2, seat1Snapshot.seats[1].holeCards.size, "Seat 1's snapshot should show its own 2 hole cards")
    }

    @Test
    fun holeCardsDealtReachesOnlyTheSeatItNames() {
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

        val result = broadcast(state, events, events)

        val seat0Events = result.filter { it.seat == 0 && it.message is ServerMessage.Events }.map { it.message as ServerMessage.Events }
        val seat1Events = result.filter { it.seat == 1 && it.message is ServerMessage.Events }.map { it.message as ServerMessage.Events }

        // Collect all HoleCardsDealt events for each seat
        val seat0HoleCardsDealt = seat0Events.flatMap { it.events }.filterIsInstance<duels.poker.engine.game.HoleCardsDealt>()
        val seat1HoleCardsDealt = seat1Events.flatMap { it.events }.filterIsInstance<duels.poker.engine.game.HoleCardsDealt>()

        // Seat 0 should only see HoleCardsDealt for seat 0
        seat0HoleCardsDealt.forEach {
            assertEquals(0, it.seat, "Seat 0's Events should only contain HoleCardsDealt for seat 0")
        }

        // Seat 1 should only see HoleCardsDealt for seat 1
        seat1HoleCardsDealt.forEach {
            assertEquals(1, it.seat, "Seat 1's Events should only contain HoleCardsDealt for seat 1")
        }
    }

    @Test
    fun aSeatWithNothingVisibleGetsNoEventsFrame() {
        val fixture = startHand(
            handNumber = 1,
            buttonSeat = 0,
            stacks = listOf(1_000, 1_000),
            smallBlind = 50,
            bigBlind = 100,
            rng = SplitMix64Rng(7),
        )
        val state = fixture.newState

        // Find the HoleCardsDealt event for seat 1 only
        val seat1OnlyHoleCards = fixture.events.filter {
            it is duels.poker.engine.game.HoleCardsDealt && it.seat == 1
        }

        val result = broadcast(state, seat1OnlyHoleCards, fixture.events)

        val seat0Frames = result.filter { it.seat == 0 }
        val seat0EventsFrames = seat0Frames.filter { it.message is ServerMessage.Events }

        assertEquals(0, seat0EventsFrames.size, "Seat 0 should have no Events frame when it has no visible events")
        assertEquals(1, seat0Frames.filter { it.message is ServerMessage.Snapshot }.size, "Seat 0 should still have a Snapshot")
    }

    @Test
    fun aRevealedSeatIsShownToBothSeats() {
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

        // Create a HandRevealed event for seat 1
        val handRevealed = HandRevealed(sequence = events.size, seat = 1, cards = state.seat(1).holeCards)
        val handEventsWithReveal = events + handRevealed

        val result = broadcast(state, fixture.events, handEventsWithReveal)

        val seat0Snapshot = (result.find { it.seat == 0 && it.message is ServerMessage.Snapshot }?.message as ServerMessage.Snapshot).view

        // Seat 0's snapshot should now show seat 1's hole cards because it was revealed
        assertEquals(2, seat0Snapshot.seats[1].holeCards.size, "Seat 0's snapshot should show seat 1's revealed hole cards")
    }

    @Test
    fun framesAreOrderedEventsThenSnapshotWithinASeat() {
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

        val result = broadcast(state, events, events)

        // Expected order: Events(0), Snapshot(0), Events(1), Snapshot(1)
        // But only if there are visible events; otherwise just Snapshot

        // Group by seat and check the order within each seat
        val seat0Frames = result.filter { it.seat == 0 }
        val seat1Frames = result.filter { it.seat == 1 }

        // Within each seat, Events (if any) should come before Snapshot
        if (seat0Frames.size > 1) {
            assertEquals(ServerMessage.Events::class, seat0Frames[0].message::class, "Seat 0's first frame should be Events")
            assertEquals(ServerMessage.Snapshot::class, seat0Frames[1].message::class, "Seat 0's second frame should be Snapshot")
        } else {
            assertTrue(seat0Frames[0].message is ServerMessage.Snapshot, "Seat 0 should have Snapshot when no Events")
        }

        if (seat1Frames.size > 1) {
            assertEquals(ServerMessage.Events::class, seat1Frames[0].message::class, "Seat 1's first frame should be Events")
            assertEquals(ServerMessage.Snapshot::class, seat1Frames[1].message::class, "Seat 1's second frame should be Snapshot")
        } else {
            assertTrue(seat1Frames[0].message is ServerMessage.Snapshot, "Seat 1 should have Snapshot when no Events")
        }

        // Overall order should be seat 0's frames first, then seat 1's
        var seat0Done = false
        var foundSeat1 = false
        for (frame in result) {
            if (frame.seat == 0) {
                require(!foundSeat1) { "Seat 0 frames should come before seat 1 frames" }
            } else {
                seat0Done = true
                foundSeat1 = true
            }
        }
    }
}
