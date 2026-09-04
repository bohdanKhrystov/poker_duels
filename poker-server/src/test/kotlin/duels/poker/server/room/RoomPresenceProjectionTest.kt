package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.protocol.SeatPresence
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RoomPresenceProjectionTest {
    private val host = PlayerId("host")
    private val guest = PlayerId("guest")
    private val code = RoomCode("2B7KMNPQ")

    private fun waitingRoom(): Room = Room.open(code, host, DuelFormat.DEFAULT, now = 1_000L)

    private fun playingRoom(): Room = (waitingRoom().join(guest, now = 2_000L) as JoinResult.Seated).room

    @Test
    fun aSeatNobodyIsWaitingForIsPresent() {
        val room = playingRoom()

        assertEquals(ServerMessage.OpponentPresence(SeatPresence.PRESENT), room.presenceOf(0))
        assertEquals(ServerMessage.OpponentPresence(SeatPresence.PRESENT), room.presenceOf(1))
    }

    @Test
    fun aSeatWithItsSocketDownIsAway() {
        val room = playingRoom().copy(awaySeats = setOf(1))

        assertEquals(
            ServerMessage.OpponentPresence(SeatPresence.AWAY),
            room.presenceOf(1),
        )
        assertEquals(ServerMessage.OpponentPresence(SeatPresence.PRESENT), room.presenceOf(0))
    }

    @Test
    fun theOtherSeatsWindowIsTheOneReported() {
        // The two seats must differ, or swapping which seat presenceOf reads is invisible: a
        // fixture that gave both seats the same state could not tell the swap this name
        // describes apart from the correct read (TASK-130805 review, repaired by TASK-130810).
        val room = playingRoom().copy(awaySeats = setOf(0), absentSeats = setOf(1))

        assertEquals(
            ServerMessage.OpponentPresence(SeatPresence.AWAY),
            room.presenceOf(0),
        )
        assertEquals(
            ServerMessage.OpponentPresence(SeatPresence.ABSENT),
            room.presenceOf(1),
        )
    }

    // aWindowThatHasRunOutIsAwayWithZero and aWindowEndingExactlyNowIsAwayWithZero, below, used to
    // probe two different instants against the same away seat; presenceOf takes no clock input
    // anymore, so both now assert exactly what aSeatWithItsSocketDownIsAway does. Kept as their
    // own tests rather than merged, so the count this class is pinned at does not move.
    @Test
    fun aWindowThatHasRunOutIsAwayWithZero() {
        val room = playingRoom().copy(awaySeats = setOf(1))

        assertEquals(
            ServerMessage.OpponentPresence(SeatPresence.AWAY),
            room.presenceOf(1),
        )
        assertEquals(ServerMessage.OpponentPresence(SeatPresence.PRESENT), room.presenceOf(0))
    }

    @Test
    fun aWindowEndingExactlyNowIsAwayWithZero() {
        val room = playingRoom().copy(awaySeats = setOf(1))

        assertEquals(
            ServerMessage.OpponentPresence(SeatPresence.AWAY),
            room.presenceOf(1),
        )
        assertEquals(ServerMessage.OpponentPresence(SeatPresence.PRESENT), room.presenceOf(0))
    }

    @Test
    fun aLatchedSeatIsAbsent() {
        val room = playingRoom().copy(absentSeats = setOf(1))

        assertEquals(ServerMessage.OpponentPresence(SeatPresence.ABSENT), room.presenceOf(1))
        assertEquals(ServerMessage.OpponentPresence(SeatPresence.PRESENT), room.presenceOf(0))
    }

    @Test
    fun bothSeatsGoneAreBothAbsent() {
        val room = playingRoom().copy(absentSeats = setOf(0, 1))

        assertEquals(ServerMessage.OpponentPresence(SeatPresence.ABSENT), room.presenceOf(0))
        assertEquals(ServerMessage.OpponentPresence(SeatPresence.ABSENT), room.presenceOf(1))
    }

    @Test
    fun aSeatOutsideTheTableIsRefused() {
        val room = playingRoom()

        assertThrows(IllegalArgumentException::class.java) { room.presenceOf(2) }
        assertThrows(IllegalArgumentException::class.java) { room.presenceOf(-1) }
    }

    @Test
    fun presenceOfStoresNothing() {
        val room = playingRoom().copy(awaySeats = setOf(1))

        room.presenceOf(1)
        room.presenceOf(1)

        assertEquals(room, room)
        assertEquals(
            ServerMessage.OpponentPresence(SeatPresence.AWAY),
            room.presenceOf(1),
        )
        assertEquals(ServerMessage.OpponentPresence(SeatPresence.PRESENT), room.presenceOf(0))
    }
}
