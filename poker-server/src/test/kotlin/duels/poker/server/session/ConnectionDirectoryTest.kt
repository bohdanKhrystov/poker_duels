package duels.poker.server.session

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConnectionDirectoryTest {
    private val player = PlayerId("alice")

    @Test
    fun aregisteredWriterIsFoundByItsPlayer() {
        val directory = ConnectionDirectory()
        val writer = ConnectionWriter()

        directory.register(player, writer)

        assertSame(writer, directory.writerFor(player))
    }

    @Test
    fun anunknownPlayerHasNoWriter() {
        val directory = ConnectionDirectory()

        assertNull(directory.writerFor(player))
    }

    @Test
    fun asecondRegistrationReplacesTheFirst() {
        val directory = ConnectionDirectory()
        val first = ConnectionWriter()
        val second = ConnectionWriter()

        directory.register(player, first)
        directory.register(player, second)

        assertSame(second, directory.writerFor(player))
        assertEquals(1, directory.size)
    }

    @Test
    fun forgetRemovesOnlyTheWriterItNames() {
        val directory = ConnectionDirectory()
        val first = ConnectionWriter()
        val second = ConnectionWriter()
        directory.register(player, first)
        directory.register(player, second)

        val removed = directory.forget(player, first)

        assertFalse(removed)
        assertSame(second, directory.writerFor(player))
    }

    @Test
    fun forgetRemovesTheWriterItDoesName() {
        val directory = ConnectionDirectory()
        val writer = ConnectionWriter()
        directory.register(player, writer)

        val removed = directory.forget(player, writer)

        assertTrue(removed)
        assertNull(directory.writerFor(player))
    }

    @Test
    fun forgettingTwiceRemovesNothingTheSecondTime() {
        val directory = ConnectionDirectory()
        val writer = ConnectionWriter()
        directory.register(player, writer)

        val first = directory.forget(player, writer)
        val second = directory.forget(player, writer)

        assertTrue(first)
        assertFalse(second)
    }
}
