package duels.poker.engine.game

import duels.poker.engine.card.cards
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BoardTest {
    @Test
    fun acceptsTheFourLegalSizes() {
        // 0 cards
        val empty = Board(emptyList())
        assertEquals(0, empty.size)

        // 3 cards
        val flop = Board(cards("As Kd Qh"))
        assertEquals(3, flop.size)

        // 4 cards
        val turn = Board(cards("As Kd Qh Jc"))
        assertEquals(4, turn.size)

        // 5 cards
        val river = Board(cards("As Kd Qh Jc Ts"))
        assertEquals(5, river.size)
    }

    @Test
    fun rejectsAnyOtherSize() {
        // 1 card
        assertThrows(IllegalArgumentException::class.java) {
            Board(cards("As"))
        }

        // 2 cards
        assertThrows(IllegalArgumentException::class.java) {
            Board(cards("As Kd"))
        }

        // 6 cards
        assertThrows(IllegalArgumentException::class.java) {
            Board(cards("As Kd Qh Jc Ts 2c"))
        }
    }

    @Test
    fun rejectsADuplicateCard() {
        assertThrows(IllegalArgumentException::class.java) {
            Board(cards("As As Kd"))
        }
    }

    @Test
    fun dealtExtendsTheBoard() {
        // Start with empty board
        val empty = Board.EMPTY
        assertEquals(0, empty.size)

        // Deal the flop
        val flop = empty.dealt(cards("As Kd Qh"))
        assertEquals(3, flop.size)
        assertEquals(cards("As Kd Qh"), flop.cards)

        // Deal the turn
        val turn = flop.dealt(cards("2c"))
        assertEquals(4, turn.size)
        assertEquals(cards("As Kd Qh 2c"), turn.cards)
    }

    @Test
    fun dealtRejectsAnIllegalResult() {
        // Dealing one card onto empty board should throw
        assertThrows(IllegalArgumentException::class.java) {
            Board.EMPTY.dealt(cards("As"))
        }
    }

    @Test
    fun emptyIsTheZeroCardBoard() {
        assertEquals(0, Board.EMPTY.size)
    }
}
