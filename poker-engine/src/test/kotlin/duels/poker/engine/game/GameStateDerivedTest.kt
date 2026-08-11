package duels.poker.engine.game

import duels.poker.engine.card.Deck
import duels.poker.engine.card.cards
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GameStateDerivedTest {
    private val deck = Deck.full()
    private val rng = SplitMix64Rng(1L)

    private fun seat0(stack: Int = 1000, committedThisStreet: Int = 0) =
        Seat(index = 0, stack = stack, committedThisStreet = committedThisStreet, committedThisHand = committedThisStreet)

    private fun seat1(stack: Int = 1000, committedThisStreet: Int = 0) =
        Seat(index = 1, stack = stack, committedThisStreet = committedThisStreet, committedThisHand = committedThisStreet)

    private fun state(
        seats: List<Seat> = listOf(seat0(), seat1()),
        street: Street = Street.PREFLOP,
        board: Board = Board.EMPTY,
        pot: Int = 0,
        betToMatch: Int = 20,
        minRaiseTo: Int = 40,
        seatToAct: Int? = 0,
    ) = GameState(
        handNumber = 1,
        buttonSeat = 0,
        street = street,
        seats = seats,
        board = board,
        pot = pot,
        betToMatch = betToMatch,
        minRaiseTo = minRaiseTo,
        seatToAct = seatToAct,
        smallBlind = 10,
        bigBlind = 20,
        eventCount = 0,
        deck = deck,
        rng = rng,
    )

    @Test
    fun potTotalAddsStreetCommitmentsToThePot() {
        val s = state(
            seats = listOf(seat0(committedThisStreet = 100), seat1(committedThisStreet = 300)),
            pot = 400,
        )

        assertEquals(800, s.potTotal)
    }

    @Test
    fun chipsInPlayCountsStacksAndPot() {
        val s = state(
            seats = listOf(seat0(stack = 1000, committedThisStreet = 100), seat1(stack = 1000, committedThisStreet = 100)),
            pot = 200,
        )

        assertEquals(2400, s.chipsInPlay)
    }

    @Test
    fun chipsInPlayIsUnchangedByACommit() {
        val s = state()

        assertEquals(s.chipsInPlay, s.withSeat(0) { it.commit(250) }.chipsInPlay)
    }

    @Test
    fun isHandOverOnlyOnComplete() {
        val complete = state(street = Street.COMPLETE, board = Board.EMPTY, seatToAct = null)
        val preflop = state(street = Street.PREFLOP)
        val showdown = state(street = Street.SHOWDOWN, board = Board(cards("2c 3c 4c 5c 6c")), seatToAct = null)

        assertEquals(true, complete.isHandOver)
        assertEquals(false, preflop.isHandOver)
        assertEquals(false, showdown.isHandOver)
    }

    @Test
    fun seatReturnsTheSeatWithThatIndex() {
        val s = state()

        assertEquals(1, s.seat(1).index)
    }

    @Test
    fun seatRejectsAnOutOfRangeIndex() {
        val s = state()

        assertThrows(IllegalArgumentException::class.java) { s.seat(2) }
    }

    @Test
    fun toCallIsTheDifferenceToTheCurrentBet() {
        val s = state(
            seats = listOf(seat0(committedThisStreet = 100), seat1()),
            betToMatch = 300,
        )

        assertEquals(200, s.toCall(0))
    }

    @Test
    fun toCallIsZeroWhenAlreadyMatched() {
        val s = state(
            seats = listOf(seat0(committedThisStreet = 300), seat1()),
            betToMatch = 300,
        )

        assertEquals(0, s.toCall(0))
    }

    @Test
    fun toCallIsCappedAtTheStack() {
        val s = state(
            seats = listOf(seat0(stack = 400, committedThisStreet = 0), seat1()),
            betToMatch = 900,
        )

        assertEquals(400, s.toCall(0))
    }

    @Test
    fun withSeatReplacesOnlyThatSeat() {
        val s = state()

        val updated = s.withSeat(1) { it.award(500) }

        assertEquals(1500, updated.seat(1).stack)
        assertEquals(s.seat(0), updated.seat(0))
        assertEquals(0, updated.seats[0].index)
    }
}
