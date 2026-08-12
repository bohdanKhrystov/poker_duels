package duels.poker.server.room

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class RoomTimeoutsTest {
    @Test
    fun theDefaultsAreTheDeclaredConstants() {
        assertEquals(RoomTimeouts.DEFAULT_WAITING_MILLIS, RoomTimeouts.DEFAULT.waitingMillis)
        assertEquals(RoomTimeouts.DEFAULT_FINISHED_MILLIS, RoomTimeouts.DEFAULT.finishedMillis)
        assert(RoomTimeouts.DEFAULT.waitingMillis > 0)
        assert(RoomTimeouts.DEFAULT.finishedMillis > 0)
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
}
