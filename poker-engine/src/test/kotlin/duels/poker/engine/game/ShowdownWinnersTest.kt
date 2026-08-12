package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

private fun showdownState(
    board: String,
    holeCards0: String,
    holeCards1: String,
): GameState {
    val seats = listOf(
        Seat(index = 0, stack = START_STACK, holeCards = cards(holeCards0)),
        Seat(index = 1, stack = START_STACK, holeCards = cards(holeCards1)),
    )
    return handState(seats).copy(street = Street.SHOWDOWN, board = Board(cards(board)), seatToAct = null)
}

internal class ShowdownWinnersTest {
    @Test
    fun theBetterHandTakesIt() {
        val state = showdownState(board = "As Kd 7c 2h 9s", holeCards0 = "Qh Jc", holeCards1 = "Td 8c")

        assertEquals(listOf(0), showdownWinners(state))
    }

    @Test
    fun eitherSeatCanWin() {
        val state = showdownState(board = "As Kd 7c 2h 9s", holeCards0 = "Td 8c", holeCards1 = "Qh Jc")

        assertEquals(listOf(1), showdownWinners(state))
    }

    @Test
    fun aKickerDecidesIt() {
        val state = showdownState(board = "Ah Kc 7d 4s 2c", holeCards0 = "Ad Qs", holeCards1 = "Ac Js")

        assertEquals(listOf(0), showdownWinners(state))
    }

    @Test
    fun theWheelBeatsAPair() {
        val state = showdownState(board = "2c 3d 4h 5s Kc", holeCards0 = "Ad 9h", holeCards1 = "Kd Qs")

        assertEquals(listOf(0), showdownWinners(state))
    }

    @Test
    fun aBoardThatPlaysSplits() {
        val state = showdownState(board = "As Kd Qc Jh Ts", holeCards0 = "2c 3d", holeCards1 = "4h 5s")

        assertEquals(listOf(0, 1), showdownWinners(state))
    }

    @Test
    fun anIncompleteBoardIsRefused() {
        val seats = listOf(
            Seat(index = 0, stack = START_STACK, holeCards = cards("Qh Jc")),
            Seat(index = 1, stack = START_STACK, holeCards = cards("Td 8c")),
        )
        val state = handState(seats).copy(street = Street.FLOP, board = Board(cards("As Kd 7c")))

        assertThrows(IllegalArgumentException::class.java) { showdownWinners(state) }
    }

    @Test
    fun aFoldedSeatIsRefused() {
        val seats = listOf(
            Seat(index = 0, stack = START_STACK, holeCards = cards("Qh Jc"), hasFolded = true),
            Seat(index = 1, stack = START_STACK, holeCards = cards("Td 8c")),
        )
        val state = handState(seats).copy(street = Street.SHOWDOWN, board = Board(cards("As Kd 7c 2h 9s")), seatToAct = null)

        assertThrows(IllegalArgumentException::class.java) { showdownWinners(state) }
    }

    @Test
    fun aSeatWithoutCardsIsRefused() {
        val seats = listOf(
            Seat(index = 0, stack = START_STACK, holeCards = cards("Qh Jc")),
            Seat(index = 1, stack = START_STACK),
        )
        val state = handState(seats).copy(street = Street.SHOWDOWN, board = Board(cards("As Kd 7c 2h 9s")), seatToAct = null)

        assertThrows(IllegalArgumentException::class.java) { showdownWinners(state) }
    }
}
