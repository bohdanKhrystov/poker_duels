package duels.poker.engine.game

import duels.poker.engine.card.Deck
import duels.poker.engine.card.cards
import duels.poker.engine.random.SplitMix64Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GameStateTest {
    private val deck = Deck.full()
    private val rng = SplitMix64Rng(1L)

    private fun seat0() = Seat(index = 0, stack = 1000)
    private fun seat1() = Seat(index = 1, stack = 1000)

    private fun preflopState(
        seats: List<Seat> = listOf(seat0(), seat1()),
        buttonSeat: Int = 0,
        street: Street = Street.PREFLOP,
        board: Board = Board.EMPTY,
        pot: Int = 0,
        betToMatch: Int = 20,
        minRaiseTo: Int = 40,
        seatToAct: Int? = 0,
        smallBlind: Int = 10,
        bigBlind: Int = 20,
        eventCount: Int = 0,
    ) = GameState(
        handNumber = 1,
        buttonSeat = buttonSeat,
        street = street,
        seats = seats,
        board = board,
        pot = pot,
        betToMatch = betToMatch,
        minRaiseTo = minRaiseTo,
        seatToAct = seatToAct,
        smallBlind = smallBlind,
        bigBlind = bigBlind,
        eventCount = eventCount,
        deck = deck,
        rng = rng,
    )

    @Test
    fun buildsAPreflopState() {
        val state = preflopState()

        assertEquals(1, state.handNumber)
        assertEquals(0, state.buttonSeat)
        assertEquals(Street.PREFLOP, state.street)
        assertEquals(listOf(seat0(), seat1()), state.seats)
        assertEquals(Board.EMPTY, state.board)
        assertEquals(0, state.pot)
        assertEquals(20, state.betToMatch)
        assertEquals(40, state.minRaiseTo)
        assertEquals(0, state.seatToAct)
        assertEquals(10, state.smallBlind)
        assertEquals(20, state.bigBlind)
        assertEquals(0, state.eventCount)
        assertEquals(deck, state.deck)
        assertEquals(rng, state.rng)
    }

    @Test
    fun copyProducesAnEqualIndependentValue() {
        val state = preflopState()

        assertEquals(state, state.copy())

        val changed = state.copy(pot = 500)
        assertNotEquals(state, changed)
        assertEquals(0, state.pot)
    }

    @Test
    fun equalityReachesIntoSeatsAndBoard() {
        val a = preflopState(seats = listOf(seat0(), seat1()))
        val b = preflopState(seats = listOf(seat0(), seat1().copy(stack = 999)))
        val c = preflopState(seats = listOf(seat0(), seat1()))

        assertNotEquals(a, b)
        assertEquals(a, c)
    }

    @Test
    fun rejectsAnythingOtherThanTwoSeats() {
        assertThrows(IllegalArgumentException::class.java) {
            preflopState(seats = listOf(seat0()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            preflopState(seats = listOf(seat0(), seat1(), Seat(index = 1, stack = 500)))
        }
    }

    @Test
    fun rejectsSeatsOutOfIndexOrder() {
        assertThrows(IllegalArgumentException::class.java) {
            preflopState(seats = listOf(seat1(), seat0()))
        }
    }

    @Test
    fun rejectsAnOutOfRangeButtonOrActor() {
        assertThrows(IllegalArgumentException::class.java) {
            preflopState(buttonSeat = 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            preflopState(seatToAct = 2)
        }
    }

    @Test
    fun rejectsBlindsThatAreNotAscending() {
        assertThrows(IllegalArgumentException::class.java) {
            preflopState(smallBlind = 100, bigBlind = 100)
        }
        assertThrows(IllegalArgumentException::class.java) {
            preflopState(smallBlind = 0, bigBlind = 20)
        }
    }

    @Test
    fun rejectsANegativeEventCount() {
        assertThrows(IllegalArgumentException::class.java) {
            preflopState(eventCount = -1)
        }
    }

    @Test
    fun rejectsABoardThatContradictsTheStreet() {
        assertThrows(IllegalArgumentException::class.java) {
            preflopState(street = Street.FLOP, board = Board.EMPTY)
        }
        assertThrows(IllegalArgumentException::class.java) {
            preflopState(street = Street.PREFLOP, board = Board(cards("2c 3c 4c")))
        }
    }

    @Test
    fun allowsAnyBoardOnACompletedHand() {
        val state = preflopState(street = Street.COMPLETE, board = Board.EMPTY, seatToAct = null)

        assertEquals(Street.COMPLETE, state.street)
        assertEquals(Board.EMPTY, state.board)
    }
}
