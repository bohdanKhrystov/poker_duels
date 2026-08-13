package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoomPresenceTest {
    private val host = PlayerId("host")
    private val guest = PlayerId("guest")
    private val code = RoomCode("2B7KMNPQ")

    private fun waitingRoom(): Room = Room.open(code, host, DuelFormat.DEFAULT, now = 1_000L)

    private fun playingRoom(): Room = (waitingRoom().join(guest, now = 2_000L) as JoinResult.Seated).room

    @Test
    fun aFreshRoomHasNobodyGone() {
        val waiting = waitingRoom()
        val playing = playingRoom()

        assertTrue(waiting.gracePeriods.isEmpty())
        assertTrue(waiting.absentSeats.isEmpty())
        assertFalse(waiting.isPaused)

        assertTrue(playing.gracePeriods.isEmpty())
        assertTrue(playing.absentSeats.isEmpty())
        assertFalse(playing.isPaused)
    }

    @Test
    fun aSeatInsideItsWindowPausesTheRoom() {
        val room = playingRoom().copy(gracePeriods = mapOf(1 to 5_000L))

        assertTrue(room.isPaused)
    }

    @Test
    fun aSeatWhoseWindowRanOutDoesNotPauseTheRoom() {
        val room = playingRoom().copy(absentSeats = setOf(1))

        assertFalse(room.isPaused)
    }

    @Test
    fun aSeatCannotBeBothCountingDownAndAbsent() {
        assertThrows(IllegalArgumentException::class.java) {
            playingRoom().copy(gracePeriods = mapOf(1 to 5_000L), absentSeats = setOf(1))
        }
    }

    @Test
    fun anUnseatedSeatCannotBeGone() {
        assertThrows(IllegalArgumentException::class.java) {
            waitingRoom().copy(gracePeriods = mapOf(1 to 5_000L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            waitingRoom().copy(absentSeats = setOf(1))
        }
    }

    @Test
    fun aSeatOutsideTheTableIsRefused() {
        assertThrows(IllegalArgumentException::class.java) {
            playingRoom().copy(absentSeats = setOf(2))
        }
        assertThrows(IllegalArgumentException::class.java) {
            playingRoom().copy(gracePeriods = mapOf(-1 to 5_000L))
        }
    }

    @Test
    fun aNegativeDeadlineIsRefused() {
        assertThrows(IllegalArgumentException::class.java) {
            playingRoom().copy(gracePeriods = mapOf(0 to -1L))
        }
    }

    @Test
    fun aDisconnectStartsTheWindowAndPausesTheRoom() {
        val before = playingRoom()
        assertTrue(before.gracePeriods.isEmpty())
        assertFalse(before.isPaused)

        val after = before.disconnect(1, 9_000L)

        assertEquals(mapOf(1 to 9_000L), after.gracePeriods)
        assertTrue(after.isPaused)
    }

    @Test
    fun aSecondDisconnectRestartsTheWindow() {
        val firstWindow = playingRoom().disconnect(1, 9_000L)
        assertEquals(mapOf(1 to 9_000L), firstWindow.gracePeriods)

        val secondWindow = firstWindow.disconnect(1, 20_000L)

        assertEquals(mapOf(1 to 20_000L), secondWindow.gracePeriods)
    }

    @Test
    fun aDisconnectAfterExpiryPutsTheSeatBackInAWindow() {
        val absent = playingRoom().copy(absentSeats = setOf(1))
        assertEquals(setOf(1), absent.absentSeats)
        assertTrue(absent.gracePeriods.isEmpty())

        val countingAgain = absent.disconnect(1, 9_000L)

        assertTrue(countingAgain.absentSeats.isEmpty())
        assertEquals(mapOf(1 to 9_000L), countingAgain.gracePeriods)
    }

    @Test
    fun disconnectingASeatTheRoomDoesNotHoldThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            waitingRoom().disconnect(1, 9_000L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            playingRoom().disconnect(2, 9_000L)
        }
    }

    @Test
    fun aReconnectClearsBothTheWindowAndTheAbsence() {
        val counting = playingRoom().disconnect(1, 9_000L)
        assertEquals(mapOf(1 to 9_000L), counting.gracePeriods)

        val reconnectedFromWindow = counting.reconnect(1)

        assertTrue(reconnectedFromWindow.gracePeriods.isEmpty())
        assertTrue(reconnectedFromWindow.absentSeats.isEmpty())
        assertFalse(reconnectedFromWindow.isPaused)

        val absent = playingRoom().copy(absentSeats = setOf(1))
        assertEquals(setOf(1), absent.absentSeats)

        val reconnectedFromAbsence = absent.reconnect(1)

        assertTrue(reconnectedFromAbsence.gracePeriods.isEmpty())
        assertTrue(reconnectedFromAbsence.absentSeats.isEmpty())
        assertFalse(reconnectedFromAbsence.isPaused)
    }

    @Test
    fun reconnectingASeatNobodyWasWaitingForChangesNothing() {
        val room = playingRoom()
        assertTrue(room.gracePeriods.isEmpty())
        assertTrue(room.absentSeats.isEmpty())

        assertEquals(playingRoom(), room.reconnect(1))
    }

    @Test
    fun expireGraceMovesOnlyTheWindowsThatRanOut() {
        val bothCounting = playingRoom().disconnect(0, 5_000L).disconnect(1, 20_000L)
        assertEquals(mapOf(0 to 5_000L, 1 to 20_000L), bothCounting.gracePeriods)
        assertTrue(bothCounting.absentSeats.isEmpty())

        val afterExpiry = bothCounting.expireGrace(5_000L)

        assertEquals(setOf(0), afterExpiry.absentSeats)
        assertEquals(mapOf(1 to 20_000L), afterExpiry.gracePeriods)
    }

    @Test
    fun expireGraceLeavesARoomWithTimeLeftAlone() {
        val counting = playingRoom().disconnect(1, 20_000L)
        assertEquals(mapOf(1 to 20_000L), counting.gracePeriods)
        assertTrue(counting.isPaused)

        val afterExpiry = counting.expireGrace(19_999L)

        assertEquals(counting, afterExpiry)
        assertTrue(afterExpiry.isPaused)
    }

    @Test
    fun expireGraceIsIdempotent() {
        val counting = playingRoom().disconnect(1, 5_000L)
        assertEquals(mapOf(1 to 5_000L), counting.gracePeriods)

        val once = counting.expireGrace(5_000L)
        assertEquals(setOf(1), once.absentSeats)

        val twice = once.expireGrace(5_000L)

        assertEquals(once, twice)
    }
}
