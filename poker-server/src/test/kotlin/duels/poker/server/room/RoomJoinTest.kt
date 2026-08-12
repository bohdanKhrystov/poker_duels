package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RoomJoinTest {
    private val host = PlayerId("host")
    private val guest = PlayerId("guest")
    private val stranger = PlayerId("stranger")
    private val code = RoomCode("ABCDEFGH")

    private fun waitingRoom(now: Long = 1_000L): Room = Room.open(code, host, DuelFormat.DEFAULT, now)

    @Test
    fun aGuestJoiningAWaitingRoomIsSeatedAndTheDuelStarts() {
        val room = waitingRoom()

        val result = room.join(guest, now = 2_000L)

        val seated = result as JoinResult.Seated
        assertEquals(RoomState.PLAYING, seated.room.state)
        assertEquals(guest, seated.room.guest)
        assertEquals(0, seated.room.match!!.handsPlayed)
        assertEquals(0, seated.room.match!!.buttonSeat)
        assertEquals(2_000L, seated.room.lastActivityAt)
    }

    @Test
    fun theGuestSitsInSeatOne() {
        val room = waitingRoom()

        val seated = room.join(guest, now = 2_000L) as JoinResult.Seated

        assertEquals(1, seated.room.seatOf(guest))
        assertEquals(setOf(host, guest), seated.room.players)
    }

    @Test
    fun theHostCannotJoinTheirOwnRoom() {
        val room = waitingRoom()

        val result = room.join(host, now = 2_000L)

        assertEquals(JoinResult.Refused(RoomRefusal.ALREADY_SEATED), result)
        assertEquals(RoomState.WAITING, room.state)
        assertNull(room.guest)
    }

    @Test
    fun aThirdJoinerIsRefusedRoomFull() {
        val room = waitingRoom()
        val seatedRoom = (room.join(guest, now = 2_000L) as JoinResult.Seated).room

        val result = seatedRoom.join(stranger, now = 3_000L)

        assertEquals(JoinResult.Refused(RoomRefusal.ROOM_FULL), result)
    }

    @Test
    fun theSeatedGuestJoiningAgainIsAlreadySeatedNotFull() {
        val room = waitingRoom()
        val seatedRoom = (room.join(guest, now = 2_000L) as JoinResult.Seated).room

        val result = seatedRoom.join(guest, now = 3_000L)

        assertEquals(JoinResult.Refused(RoomRefusal.ALREADY_SEATED), result)
    }

    @Test
    fun joiningAFinishedRoomIsUnknownRoom() {
        val room = waitingRoom()
        val seatedRoom = (room.join(guest, now = 2_000L) as JoinResult.Seated).room
        val finishedRoom = seatedRoom.copy(state = RoomState.FINISHED)

        val guestResult = finishedRoom.join(guest, now = 4_000L)
        val hostResult = finishedRoom.join(host, now = 4_000L)
        val strangerResult = finishedRoom.join(stranger, now = 4_000L)

        assertEquals(JoinResult.Refused(RoomRefusal.UNKNOWN_ROOM), guestResult)
        assertEquals(JoinResult.Refused(RoomRefusal.UNKNOWN_ROOM), hostResult)
        assertEquals(JoinResult.Refused(RoomRefusal.UNKNOWN_ROOM), strangerResult)
    }

    @Test
    fun joiningAnAbandonedRoomIsUnknownRoom() {
        val room = waitingRoom()
        val abandonedRoom = room.copy(state = RoomState.ABANDONED)

        val result = abandonedRoom.join(stranger, now = 4_000L)

        assertEquals(JoinResult.Refused(RoomRefusal.UNKNOWN_ROOM), result)
    }
}
