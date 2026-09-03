package duels.poker.server.room

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class RoomTimeoutsTest {
    @Test
    fun theDefaultsAreTheDeclaredConstants() {
        assertEquals(RoomTimeouts.DEFAULT_WAITING_MILLIS, RoomTimeouts.DEFAULT.waitingMillis)
        assertEquals(RoomTimeouts.DEFAULT_FINISHED_MILLIS, RoomTimeouts.DEFAULT.finishedMillis)
        assertEquals(RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS, RoomTimeouts.DEFAULT.disconnectGraceMillis)
        assert(RoomTimeouts.DEFAULT.waitingMillis > 0)
        assert(RoomTimeouts.DEFAULT.finishedMillis > 0)
        assert(RoomTimeouts.DEFAULT.disconnectGraceMillis > 0)
    }

    @Test
    fun rejectsANonPositiveWaitingTimeout() {
        assertThrows<IllegalArgumentException> { RoomTimeouts(0, 1) }
        assertThrows<IllegalArgumentException> { RoomTimeouts(-1, 1) }
    }

    @Test
    fun rejectsANonPositiveFinishedTimeout() {
        assertThrows<IllegalArgumentException> { RoomTimeouts(1, 0) }
    }

    @Test
    fun rejectsANonPositiveGraceWindow() {
        assertThrows<IllegalArgumentException> { RoomTimeouts(1, 1, 0) }
        assertThrows<IllegalArgumentException> { RoomTimeouts(1, 1, -1) }
    }

    @Test
    fun theGraceWindowDefaultsWhenNotNamed() {
        assertEquals(RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS, RoomTimeouts(1, 1).disconnectGraceMillis)
    }

    @Test
    fun theShippedTurnAllowanceIsThirtySeconds() {
        assertEquals(30_000L, RoomTimeouts.DEFAULT_TURN_MILLIS)
        assertEquals(RoomTimeouts.DEFAULT_TURN_MILLIS, RoomTimeouts.DEFAULT.turnMillis)
    }

    @Test
    fun theShippedTimebankIsThreeMinutes() {
        assertEquals(180_000L, RoomTimeouts.DEFAULT_TIMEBANK_MILLIS)
        assertEquals(RoomTimeouts.DEFAULT_TIMEBANK_MILLIS, RoomTimeouts.DEFAULT.timebankMillis)
    }

    @Test
    fun aNonPositiveTurnAllowanceIsRefused() {
        assertThrows<IllegalArgumentException> { RoomTimeouts(1, 1, turnMillis = 0) }
        assertThrows<IllegalArgumentException> { RoomTimeouts(1, 1, turnMillis = -1) }
    }

    @Test
    fun aNonPositiveTimebankIsRefused() {
        assertThrows<IllegalArgumentException> { RoomTimeouts(1, 1, timebankMillis = 0) }
        assertThrows<IllegalArgumentException> { RoomTimeouts(1, 1, timebankMillis = -1) }
    }

    @Test
    fun aReapingTestNeedStateNoClock() {
        val timeout = RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000)
        assertEquals(RoomTimeouts.DEFAULT_TURN_MILLIS, timeout.turnMillis)
        assertEquals(RoomTimeouts.DEFAULT_TIMEBANK_MILLIS, timeout.timebankMillis)
    }
}
