package duels.poker.server.auth

import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NoAuthSessionsTest {
    @Test
    fun everyTokenIsUnknown(): Unit = runBlocking {
        val double = NoAuthSessions
        assertNull(double.playerOf(SessionToken("a")))
        assertNull(double.playerOf(SessionToken("b")))
    }

    @Test
    fun deletingAnUnknownTokenIsNotAnError(): Unit = runBlocking {
        val double = NoAuthSessions
        double.delete(SessionToken("a"))
        // If we get here, no exception was thrown.
    }

    @Test
    fun issuingThrows() {
        val double = NoAuthSessions
        assertThrows(UnsupportedOperationException::class.java) {
            runBlocking {
                double.issue(PlayerId("p1"))
            }
        }
    }
}
