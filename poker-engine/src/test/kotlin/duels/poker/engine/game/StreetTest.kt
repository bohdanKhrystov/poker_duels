package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StreetTest {
    @Test
    fun boardCardsGrowWithTheStreet() {
        assertEquals(0, Street.PREFLOP.boardCards)
        assertEquals(3, Street.FLOP.boardCards)
        assertEquals(4, Street.TURN.boardCards)
        assertEquals(5, Street.RIVER.boardCards)
        assertEquals(5, Street.SHOWDOWN.boardCards)
        assertEquals(5, Street.COMPLETE.boardCards)
    }

    @Test
    fun nextFollowsDeclarationOrder() {
        assertEquals(Street.FLOP, Street.PREFLOP.next)
        assertEquals(Street.TURN, Street.FLOP.next)
        assertEquals(Street.RIVER, Street.TURN.next)
        assertEquals(Street.SHOWDOWN, Street.RIVER.next)
        assertEquals(Street.COMPLETE, Street.SHOWDOWN.next)
    }

    @Test
    fun completeHasNoSuccessor() {
        assertNull(Street.COMPLETE.next)
    }

    @Test
    fun bettingRunsFromPreflopToTheRiver() {
        assertTrue(Street.PREFLOP.isBetting)
        assertTrue(Street.FLOP.isBetting)
        assertTrue(Street.TURN.isBetting)
        assertTrue(Street.RIVER.isBetting)
        assertFalse(Street.SHOWDOWN.isBetting)
        assertFalse(Street.COMPLETE.isBetting)
    }
}
