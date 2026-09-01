package duels.poker.server.session

import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import kotlin.test.assertTrue

class RoomMembershipTest {
    @Test
    fun theroomAConnectionIsInIsAVolatileField() {
        val field = RoomMembership::class.java.getDeclaredField("code")
        assertTrue(Modifier.isVolatile(field.modifiers))
    }
}
