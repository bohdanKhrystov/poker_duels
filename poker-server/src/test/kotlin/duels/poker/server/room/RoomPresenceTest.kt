package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.session.PlayerId
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
}
