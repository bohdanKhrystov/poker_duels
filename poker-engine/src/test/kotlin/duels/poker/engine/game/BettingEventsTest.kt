package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BettingEventsTest {

    @Test
    fun everyBettingEventIsAGameEventWithASeat() {
        val folded = PlayerFolded(sequence = 0, seat = 1)
        assertTrue(folded is GameEvent)
        assertTrue(folded is BettingEvent)
        assertEquals(1, folded.seat)
        assertEquals(EVENT_SCHEMA_VERSION, folded.version)

        val checked = PlayerChecked(sequence = 1, seat = 1)
        assertTrue(checked is GameEvent)
        assertTrue(checked is BettingEvent)
        assertEquals(1, checked.seat)
        assertEquals(EVENT_SCHEMA_VERSION, checked.version)

        val called = PlayerCalled(sequence = 2, seat = 1, to = 100)
        assertTrue(called is GameEvent)
        assertTrue(called is BettingEvent)
        assertEquals(1, called.seat)
        assertEquals(EVENT_SCHEMA_VERSION, called.version)

        val bet = PlayerBet(sequence = 3, seat = 1, to = 200)
        assertTrue(bet is GameEvent)
        assertTrue(bet is BettingEvent)
        assertEquals(1, bet.seat)
        assertEquals(EVENT_SCHEMA_VERSION, bet.version)

        val raised = PlayerRaised(sequence = 4, seat = 1, to = 400)
        assertTrue(raised is GameEvent)
        assertTrue(raised is BettingEvent)
        assertEquals(1, raised.seat)
        assertEquals(EVENT_SCHEMA_VERSION, raised.version)

        val allIn = PlayerAllIn(sequence = 5, seat = 1, to = 5000)
        assertTrue(allIn is GameEvent)
        assertTrue(allIn is BettingEvent)
        assertEquals(1, allIn.seat)
        assertEquals(EVENT_SCHEMA_VERSION, allIn.version)
    }

    @Test
    fun totalsAreStreetTotals() {
        val bet = PlayerBet(2, 0, to = 300)
        assertEquals(300, bet.to)

        val raised = PlayerRaised(3, 1, to = 900)
        assertEquals(900, raised.to)

        val called = PlayerCalled(4, 0, to = 900)
        assertEquals(900, called.to)

        val allIn = PlayerAllIn(5, 1, to = 4_000)
        assertEquals(4_000, allIn.to)
    }

    @Test
    fun foldAndCheckCarryNoAmount() {
        val folded = PlayerFolded(1, 0)
        assertEquals(1, folded.sequence)
        assertEquals(0, folded.seat)

        val checked = PlayerChecked(1, 0)
        assertEquals(1, checked.sequence)
        assertEquals(0, checked.seat)
    }

    @Test
    fun rejectsANonPositiveTotal() {
        assertThrows(IllegalArgumentException::class.java) {
            PlayerBet(1, 0, to = 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            PlayerRaised(1, 0, to = -1)
        }
    }

    @Test
    fun rejectsAnInvalidSeatOrSequence() {
        assertThrows(IllegalArgumentException::class.java) {
            PlayerBet(1, 2, to = 100)
        }

        assertThrows(IllegalArgumentException::class.java) {
            PlayerRaised(1, 0, to = 100).copy(seat = 2)
        }

        assertThrows(IllegalArgumentException::class.java) {
            PlayerFolded(-1, 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            PlayerChecked(1, -1)
        }
    }
}
