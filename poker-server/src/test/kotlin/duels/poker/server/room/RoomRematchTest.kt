package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoomRematchTest {
    private val host = PlayerId("host")
    private val guest = PlayerId("guest")
    private val code = RoomCode("ABCDEFGH")

    private fun finishedRoom(): Room {
        val waitingRoom = Room.open(code, host, DuelFormat.DEFAULT, now = 1_000L)
        val seated = (waitingRoom.join(guest, 1_000L) as JoinResult.Seated).room
        return seated.finish(2_000L)
    }

    @Test
    fun oneOfferLeavesTheRoomFinished() {
        val room = finishedRoom()

        val result = room.offerRematch(host, now = 3_000L)

        assertTrue(result is RematchResult.Offered)
        val offered = (result as RematchResult.Offered).room
        assertEquals(RoomState.FINISHED, offered.state)
        assertEquals(setOf(host), offered.rematchOffers)
        assertEquals(room.match, offered.match)
    }

    @Test
    fun bothOffersReturnTheRoomToPlaying() {
        val room = finishedRoom()
        val afterHost = (room.offerRematch(host, now = 3_000L) as RematchResult.Offered).room

        val result = afterHost.offerRematch(guest, now = 4_000L)

        assertTrue(result is RematchResult.Agreed)
        val agreed = (result as RematchResult.Agreed).room
        assertEquals(RoomState.PLAYING, agreed.state)
        assertTrue(agreed.rematchOffers.isEmpty())
        assertEquals(4_000L, agreed.lastActivityAt)
    }

    @Test
    fun theRematchButtonSitsOnTheOtherSeat() {
        val room = finishedRoom()
        val afterHost = (room.offerRematch(host, now = 3_000L) as RematchResult.Offered).room

        val agreed = (afterHost.offerRematch(guest, now = 4_000L) as RematchResult.Agreed).room

        assertEquals(1, agreed.openingButtonSeat)
        assertEquals(1, agreed.match!!.buttonSeat)
        assertEquals(0, agreed.match!!.handsPlayed)
        assertTrue(agreed.match!!.stacks.all { it == agreed.format.startingStack })
    }

    @Test
    fun aSecondRematchReturnsTheButtonToTheHost() {
        val room = finishedRoom()
        val afterHost = (room.offerRematch(host, now = 3_000L) as RematchResult.Offered).room
        val firstRematch = (afterHost.offerRematch(guest, now = 4_000L) as RematchResult.Agreed).room

        val secondFinished = firstRematch.finish(now = 5_000L)
        val secondAfterHost =
            (secondFinished.offerRematch(host, now = 6_000L) as RematchResult.Offered).room
        val secondAgreed =
            (secondAfterHost.offerRematch(guest, now = 7_000L) as RematchResult.Agreed).room

        assertEquals(0, secondAgreed.openingButtonSeat)
        assertEquals(0, secondAgreed.match!!.buttonSeat)
    }

    @Test
    fun offeringTwiceFromOneSeatIsRefused() {
        val room = finishedRoom()
        val afterHost = (room.offerRematch(host, now = 3_000L) as RematchResult.Offered).room

        val result = afterHost.offerRematch(host, now = 4_000L)

        assertEquals(RematchResult.Refused(RematchRefusal.ALREADY_OFFERED), result)
        assertEquals(setOf(host), afterHost.rematchOffers)
    }

    @Test
    fun aStrangerCannotOfferARematch() {
        val room = finishedRoom()

        val result = room.offerRematch(PlayerId("nobody"), now = 3_000L)

        assertEquals(RematchResult.Refused(RematchRefusal.NOT_A_PLAYER), result)
    }

    @Test
    fun aRematchOfferBeforeTheDuelEndsIsRefused() {
        val playingRoom =
            (Room.open(code, host, DuelFormat.DEFAULT, now = 1_000L).join(guest, 1_000L) as JoinResult.Seated).room
        val waitingRoom = Room.open(code, host, DuelFormat.DEFAULT, now = 1_000L)

        assertEquals(
            RematchResult.Refused(RematchRefusal.NOT_FINISHED),
            playingRoom.offerRematch(host, now = 2_000L),
        )
        assertEquals(
            RematchResult.Refused(RematchRefusal.NOT_FINISHED),
            waitingRoom.offerRematch(host, now = 2_000L),
        )
    }
}
