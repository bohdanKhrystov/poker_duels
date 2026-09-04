package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Assertions.assertEquals
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

        assertTrue(waiting.awaySeats.isEmpty())
        assertTrue(waiting.absentSeats.isEmpty())

        assertTrue(playing.awaySeats.isEmpty())
        assertTrue(playing.absentSeats.isEmpty())
    }

    @Test
    fun aSeatMarkedAwayShowsUpInAwaySeats() {
        val room = playingRoom().copy(awaySeats = setOf(1))

        assertTrue(room.awaySeats.isNotEmpty())
    }

    @Test
    fun anAbsentSeatDoesNotShowUpInAwaySeats() {
        val room = playingRoom().copy(absentSeats = setOf(1))

        assertTrue(room.awaySeats.isEmpty())
    }

    @Test
    fun aSeatCannotBeBothAwayAndAbsent() {
        assertThrows(IllegalArgumentException::class.java) {
            playingRoom().copy(awaySeats = setOf(1), absentSeats = setOf(1))
        }
    }

    @Test
    fun anUnseatedSeatCannotBeGone() {
        assertThrows(IllegalArgumentException::class.java) {
            waitingRoom().copy(awaySeats = setOf(1))
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
            playingRoom().copy(awaySeats = setOf(-1))
        }
    }

    @Test
    fun aDisconnectMarksTheSeatAway() {
        val before = playingRoom()
        assertTrue(before.awaySeats.isEmpty())

        val after = before.disconnect(1)

        assertEquals(setOf(1), after.awaySeats)
    }

    @Test
    fun aSecondDisconnectLeavesAnAlreadyAwaySeatAway() {
        val firstDrop = playingRoom().disconnect(1)
        assertEquals(setOf(1), firstDrop.awaySeats)

        val secondDrop = firstDrop.disconnect(1)

        assertEquals(setOf(1), secondDrop.awaySeats)
    }

    @Test
    fun aDisconnectAfterAbsenceMarksTheSeatAwayAgain() {
        val absent = playingRoom().copy(absentSeats = setOf(1))
        assertEquals(setOf(1), absent.absentSeats)
        assertTrue(absent.awaySeats.isEmpty())

        val awayAgain = absent.disconnect(1)

        assertTrue(awayAgain.absentSeats.isEmpty())
        assertEquals(setOf(1), awayAgain.awaySeats)
    }

    @Test
    fun disconnectingASeatTheRoomDoesNotHoldThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            waitingRoom().disconnect(1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            playingRoom().disconnect(2)
        }
    }

    @Test
    fun aReconnectClearsBothAwayAndAbsence() {
        val away = playingRoom().disconnect(1)
        assertEquals(setOf(1), away.awaySeats)

        val reconnectedFromAway = away.reconnect(1)

        assertTrue(reconnectedFromAway.awaySeats.isEmpty())
        assertTrue(reconnectedFromAway.absentSeats.isEmpty())

        val absent = playingRoom().copy(absentSeats = setOf(1))
        assertEquals(setOf(1), absent.absentSeats)

        val reconnectedFromAbsence = absent.reconnect(1)

        assertTrue(reconnectedFromAbsence.awaySeats.isEmpty())
        assertTrue(reconnectedFromAbsence.absentSeats.isEmpty())
    }

    @Test
    fun reconnectingASeatNobodyWasWaitingForChangesNothing() {
        val room = playingRoom()
        assertTrue(room.awaySeats.isEmpty())
        assertTrue(room.absentSeats.isEmpty())

        assertEquals(playingRoom(), room.reconnect(1))
    }
}
