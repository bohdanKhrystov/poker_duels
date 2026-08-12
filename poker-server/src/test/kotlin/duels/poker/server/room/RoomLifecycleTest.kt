package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RoomLifecycleTest {
    private val host = PlayerId("host")
    private val guest = PlayerId("guest")
    private val code = RoomCode("ABCDEFGH")

    private fun playingRoom(now: Long = 1_000L): Room {
        val waitingRoom = Room.open(code, host, DuelFormat.DEFAULT, now)
        val result = waitingRoom.join(guest, now = 1_000L)
        return (result as JoinResult.Seated).room
    }

    @Test
    fun finishMovesAPlayingRoomToFinishedAndKeepsTheMatch() {
        val room = playingRoom()
        val matchBefore = room.match

        val finished = room.finish(now = 2_000L)

        assertEquals(RoomState.FINISHED, finished.state)
        assertEquals(matchBefore, finished.match)
        assertEquals(guest, finished.guest)
        assertEquals(2_000L, finished.lastActivityAt)
    }

    @Test
    fun finishingARoomThatIsNotPlayingThrows() {
        val waitingRoom = Room.open(code, host, DuelFormat.DEFAULT, now = 1_000L)

        assertThrows(IllegalStateException::class.java) {
            waitingRoom.finish(now = 2_000L)
        }

        val playingRoom = playingRoom()
        val finishedRoom = playingRoom.finish(now = 2_000L)

        assertThrows(IllegalStateException::class.java) {
            finishedRoom.finish(now = 3_000L)
        }
    }

    @Test
    fun abandonWorksFromWaitingPlayingAndFinished() {
        // From WAITING
        val waitingRoom = Room.open(code, host, DuelFormat.DEFAULT, now = 1_000L)
        val abandonedFromWaiting = waitingRoom.abandon(now = 2_000L)
        assertEquals(RoomState.ABANDONED, abandonedFromWaiting.state)
        assertEquals(2_000L, abandonedFromWaiting.lastActivityAt)

        // From PLAYING
        val playingRoom = playingRoom()
        val abandonedFromPlaying = playingRoom.abandon(now = 3_000L)
        assertEquals(RoomState.ABANDONED, abandonedFromPlaying.state)
        assertEquals(3_000L, abandonedFromPlaying.lastActivityAt)

        // From FINISHED
        val finishedRoom = playingRoom.finish(now = 4_000L)
        val abandonedFromFinished = finishedRoom.abandon(now = 5_000L)
        assertEquals(RoomState.ABANDONED, abandonedFromFinished.state)
        assertEquals(5_000L, abandonedFromFinished.lastActivityAt)
    }

    @Test
    fun abandonClearsAnyRematchOffer() {
        val playingRoom = playingRoom()
        val finishedRoom = playingRoom.finish(now = 2_000L)
        val roomWithOffer = finishedRoom.copy(rematchOffers = setOf(host))

        val abandoned = roomWithOffer.abandon(now = 3_000L)

        assertEquals(RoomState.ABANDONED, abandoned.state)
        assertEquals(true, abandoned.rematchOffers.isEmpty())
    }

    @Test
    fun abandoningAnAbandonedRoomChangesNothing() {
        val playingRoom = playingRoom()
        val abandonedOnce = playingRoom.abandon(now = 5_000L)

        val abandonedTwice = abandonedOnce.abandon(now = 9_999L)

        assertEquals(abandonedOnce, abandonedTwice)
        assertEquals(5_000L, abandonedTwice.lastActivityAt)
    }

    @Test
    fun touchOnlyMovesTheClock() {
        val room = playingRoom(now = 1_000L)

        val touched = room.touch(5_000L)

        assertEquals(room.copy(lastActivityAt = 5_000L), touched)
    }
}
