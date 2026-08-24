package duels.poker.server.auth

import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecordingDeviceBindingsTest {
    @Test
    fun aFreshDoubleHasRecordedNothing(): Unit = runBlocking {
        val double = RecordingDeviceBindings()
        assertEquals(0, double.revokeCalls.size)
    }

    @Test
    fun oneRevokeRecordsExactlyOneCallWithBothArguments(): Unit = runBlocking {
        val double = RecordingDeviceBindings()
        double.revoke(PlayerId("p-1"), SessionToken("t-1"))
        assertEquals(1, double.revokeCalls.size)
        assertEquals(RevokeCall(PlayerId("p-1"), SessionToken("t-1")), double.revokeCalls[0])
    }

    @Test
    fun twoRevokesRecordTwoCallsInOrder(): Unit = runBlocking {
        val double = RecordingDeviceBindings()
        double.revoke(PlayerId("p-1"), SessionToken("t-1"))
        double.revoke(PlayerId("p-2"), SessionToken("t-2"))
        assertEquals(2, double.revokeCalls.size)
        assertEquals(RevokeCall(PlayerId("p-1"), SessionToken("t-1")), double.revokeCalls[0])
        assertEquals(RevokeCall(PlayerId("p-2"), SessionToken("t-2")), double.revokeCalls[1])
    }
}
