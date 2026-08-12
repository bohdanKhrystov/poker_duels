package duels.poker.server.room

import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

/** A [RoomCodeSource] that hands out the given codes in order, then repeats the last forever. */
private class ScriptedCodes(vararg codes: String) : RoomCodeSource {
    private val queue = codes.toMutableList()

    override fun newRoomCode(): RoomCode {
        val value = if (queue.size > 1) queue.removeAt(0) else queue.first()
        return RoomCode(value)
    }
}

private fun newPlayerId(): PlayerId = PlayerId(UUID.randomUUID().toString())

internal class RoomRegistryTest {

    @Test
    fun createStoresARoomFoundByItsCode() {
        val registry = RoomRegistry(ScriptedCodes("2B7KMNPQ"), MutableClock())

        val created = registry.create(newPlayerId())

        assertEquals(created, registry.get(created.code))
        assertEquals(RoomState.WAITING, created.state)
        assertEquals(1, registry.size)
    }

    @Test
    fun createStampsTheRoomWithTheClock() {
        val registry = RoomRegistry(ScriptedCodes("2B7KMNPQ"), MutableClock(4_242))

        val created = registry.create(newPlayerId())

        assertEquals(4_242L, created.lastActivityAt)
    }

    @Test
    fun everyRoomGetsItsOwnCode() {
        val registry = RoomRegistry(RandomRoomCodeSource(), MutableClock())

        val first = registry.create(newPlayerId())
        val second = registry.create(newPlayerId())

        assertNotEquals(first.code, second.code)
        assertEquals(2, registry.size)
    }

    @Test
    fun createRetriesWhenTheCodeSourceRepeatsItself() {
        val registry = RoomRegistry(ScriptedCodes("2B7KMNPQ", "2B7KMNPQ", "3C8MNPQR"), MutableClock())

        registry.create(newPlayerId())
        val second = registry.create(newPlayerId())

        assertEquals(RoomCode("3C8MNPQR"), second.code)
        assertEquals(2, registry.size)
    }

    @Test
    fun createGivesUpAfterTooManyCollisions() {
        val registry = RoomRegistry(ScriptedCodes("2B7KMNPQ"), MutableClock())

        registry.create(newPlayerId())

        assertThrows(IllegalStateException::class.java) { registry.create(newPlayerId()) }
    }

    @Test
    fun getReturnsNullForAnUnknownCode() {
        val registry = RoomRegistry(ScriptedCodes("2B7KMNPQ"), MutableClock())

        assertNull(registry.get(RoomCode("ZZZZZZZZ")))
    }

    @Test
    fun importsNoTransportOrProtocolTypes() {
        val source = File("src/main/kotlin/duels/poker/server/room/RoomRegistry.kt").readText()

        assertFalse(source.contains("import io.ktor"))
        assertFalse(source.contains("import duels.poker.server.protocol"))
    }
}
