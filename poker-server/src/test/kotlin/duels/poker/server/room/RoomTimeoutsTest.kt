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
}
