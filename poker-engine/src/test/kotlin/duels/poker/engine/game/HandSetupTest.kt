package duels.poker.engine.game

import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Tests for [startHand]: posting both blinds, as events and as state. */
class HandSetupTest {
    @Test
    fun theButtonPostsTheSmallBlind() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        val seat0 = result.newState.seat(0)
        assertEquals(50, seat0.committedThisStreet)
        assertEquals(9_950, seat0.stack)
    }

    @Test
    fun theNonButtonPostsTheBigBlind() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        val seat1 = result.newState.seat(1)
        assertEquals(100, seat1.committedThisStreet)
        assertEquals(9_900, seat1.stack)
    }

    @Test
    fun theBlindsFollowTheButtonToTheOtherSeat() {
        val result = startHand(1, buttonSeat = 1, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        assertEquals(50, result.newState.seat(1).committedThisStreet)
        assertEquals(100, result.newState.seat(0).committedThisStreet)
    }

    @Test
    fun theOpeningEventsAreExact() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        assertEquals(6, result.events.size)

        val handStarted = result.events[0]
        assertTrue(handStarted is HandStarted)
        assertEquals(0, handStarted.sequence)

        val smallBlindPosted = result.events[1]
        assertTrue(smallBlindPosted is BlindPosted)
        assertEquals(1, smallBlindPosted.sequence)

        val bigBlindPosted = result.events[2]
        assertTrue(bigBlindPosted is BlindPosted)
        assertEquals(2, bigBlindPosted.sequence)
    }

    @Test
    fun theBigBlindIsTheBarAndSetsTheMinimumRaise() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        assertEquals(100, result.newState.betToMatch)
        assertEquals(200, result.newState.minRaiseTo)
    }

    @Test
    fun aShortStackPostsItsBlindAllIn() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 60), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        val seat1 = result.newState.seat(1)
        assertEquals(60, seat1.committedThisStreet)
        assertEquals(0, seat1.stack)
        assertTrue(seat1.isAllIn)
        // DEC-003: the bar is the amount actually posted, not the nominal blind — do not "fix" this back to 100.
        assertEquals(60, result.newState.betToMatch)
    }

    @Test
    fun chipsAreConserved() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        assertEquals(20_000, result.newState.chipsInPlay)
        assertEquals(150, result.newState.potTotal)
    }

    @Test
    fun theEventsDescribeTheState() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        val opening = GameState(
            handNumber = 1,
            buttonSeat = 0,
            street = Street.PREFLOP,
            seats = listOf(Seat(index = 0, stack = 10_000), Seat(index = 1, stack = 10_000)),
            board = Board.EMPTY,
            pot = 0,
            betToMatch = 0,
            minRaiseTo = 100,
            seatToAct = null,
            smallBlind = 50,
            bigBlind = 100,
            eventCount = 0,
            deck = result.newState.deck,
            rng = result.newState.rng,
        )

        val folded = StateProjection.fold(opening, result.events)

        assertEquals(folded, result.newState)
    }

    @Test
    fun theDeckIsShuffledAndFull() {
        val result = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(1L))

        assertEquals(48, result.newState.deck.remaining)

        val other = startHand(1, buttonSeat = 0, stacks = listOf(10_000, 10_000), smallBlind = 50, bigBlind = 100, rng = SplitMix64Rng(2L))

        val firstCard = result.newState.deck.deal(1).cards.first()
        val otherFirstCard = other.newState.deck.deal(1).cards.first()
        assertNotEquals(firstCard, otherFirstCard)
    }
}
