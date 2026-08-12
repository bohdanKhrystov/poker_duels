package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.MatchState
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoomTest {

    private val host = PlayerId("host")

    private fun openRoom(): Room = Room.open(RoomCode("2B7KMNPQ"), host, DuelFormat.DEFAULT, now = 1_000L)

    @Test
    fun openStartsAWaitingRoomWithNoGuest() {
        val room = openRoom()

        assertEquals(RoomState.WAITING, room.state)
        assertNull(room.guest)
        assertNull(room.match)
        assertTrue(room.rematchOffers.isEmpty())
        assertEquals(0, room.openingButtonSeat)
        assertEquals(1_000L, room.lastActivityAt)
    }

    @Test
    fun theHostSitsInSeatZeroAndAStrangerHasNoSeat() {
        val room = openRoom()

        assertEquals(0, room.seatOf(host))
        assertNull(room.seatOf(PlayerId("nobody")))
        assertEquals(setOf(host), room.players)
    }

    @Test
    fun aRoomWhoseGuestIsTheHostIsRejected() {
        val room = openRoom()

        assertThrows(IllegalArgumentException::class.java) {
            room.copy(guest = host)
        }
    }

    @Test
    fun aWaitingRoomMayNotCarryAMatch() {
        val room = openRoom()

        assertThrows(IllegalArgumentException::class.java) {
            room.copy(match = MatchState.start(DuelFormat.DEFAULT))
        }
    }

    @Test
    fun aPlayingRoomWithoutAGuestIsRejected() {
        val room = openRoom()

        assertThrows(IllegalArgumentException::class.java) {
            room.copy(state = RoomState.PLAYING)
        }
    }

    @Test
    fun anOfferOutsideAFinishedRoomIsRejected() {
        val room = openRoom()

        assertThrows(IllegalArgumentException::class.java) {
            room.copy(rematchOffers = setOf(host))
        }
    }

    @Test
    fun anOfferFromSomebodyWithNoSeatIsRejected() {
        val guest = PlayerId("guest")
        val finished = openRoom().copy(
            guest = guest,
            state = RoomState.FINISHED,
            match = MatchState.start(DuelFormat.DEFAULT),
        )

        assertThrows(IllegalArgumentException::class.java) {
            finished.copy(rematchOffers = setOf(PlayerId("nobody")))
        }
    }
}
