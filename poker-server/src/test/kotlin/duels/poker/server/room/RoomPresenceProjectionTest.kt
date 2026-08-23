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

        assertEquals(ServerMessage.OpponentPresence(SeatPresence.PRESENT), room.presenceOf(0, 5_000L))
        assertEquals(ServerMessage.OpponentPresence(SeatPresence.PRESENT), room.presenceOf(1, 5_000L))
    }

    @Test
    fun aSeatInsideItsWindowIsAwayWithWhatIsLeft() {
        val room = playingRoom().copy(gracePeriods = mapOf(1 to 30_000L))

        assertEquals(
            ServerMessage.OpponentPresence(SeatPresence.AWAY, graceRemainingMillis = 25_000L),
            room.presenceOf(1, 5_000L),
        )
        assertEquals(ServerMessage.OpponentPresence(SeatPresence.PRESENT), room.presenceOf(0, 5_000L))
    }

    @Test
    fun theOtherSeatsWindowIsTheOneReported() {
        val room = playingRoom().copy(gracePeriods = mapOf(0 to 9_000L, 1 to 30_000L))

        assertEquals(
            ServerMessage.OpponentPresence(SeatPresence.AWAY, graceRemainingMillis = 8_000L),
            room.presenceOf(0, 1_000L),
        )
        assertEquals(
            ServerMessage.OpponentPresence(SeatPresence.AWAY, graceRemainingMillis = 29_000L),
            room.presenceOf(1, 1_000L),
        )
    }

    @Test
    fun aWindowThatHasRunOutIsAwayWithZero() {
        val room = playingRoom().copy(gracePeriods = mapOf(1 to 30_000L))

        assertEquals(
            ServerMessage.OpponentPresence(SeatPresence.AWAY, graceRemainingMillis = 0L),
            room.presenceOf(1, 45_000L),
        )
    }

    @Test
    fun aWindowEndingExactlyNowIsAwayWithZero() {
        val room = playingRoom().copy(gracePeriods = mapOf(1 to 30_000L))

        assertEquals(
            ServerMessage.OpponentPresence(SeatPresence.AWAY, graceRemainingMillis = 0L),
            room.presenceOf(1, 30_000L),
        )
    }

    @Test
    fun aSeatWhoseWindowRanOutIsAbsent() {
        val room = playingRoom().copy(absentSeats = setOf(1))

        assertEquals(ServerMessage.OpponentPresence(SeatPresence.ABSENT), room.presenceOf(1, 5_000L))
        assertEquals(ServerMessage.OpponentPresence(SeatPresence.PRESENT), room.presenceOf(0, 5_000L))
    }

    @Test
    fun bothSeatsGoneAreBothAbsent() {
        val room = playingRoom().copy(absentSeats = setOf(0, 1))

        assertEquals(ServerMessage.OpponentPresence(SeatPresence.ABSENT), room.presenceOf(0, 5_000L))
        assertEquals(ServerMessage.OpponentPresence(SeatPresence.ABSENT), room.presenceOf(1, 5_000L))
    }

    @Test
    fun aSeatOutsideTheTableIsRefused() {
        val room = playingRoom()

        assertThrows(IllegalArgumentException::class.java) { room.presenceOf(2, 0L) }
        assertThrows(IllegalArgumentException::class.java) { room.presenceOf(-1, 0L) }
    }

    @Test
    fun presenceOfStoresNothing() {
        val room = playingRoom().copy(gracePeriods = mapOf(1 to 30_000L))

        room.presenceOf(1, 5_000L)
        room.presenceOf(1, 20_000L)

        assertEquals(room, room)
        assertEquals(
            ServerMessage.OpponentPresence(SeatPresence.AWAY, graceRemainingMillis = 10_000L),
            room.presenceOf(1, 20_000L),
        )
    }
}
