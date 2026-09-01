package duels.poker.server.session

import duels.poker.server.room.RoomCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConnectionDirectoryTest {
    private val player = PlayerId("alice")
    private val room = RoomCode("ABCDEFGH")
    private val otherRoom = RoomCode("ZYXWVTSR")

    /** A [RoomMembership] already in [room], the way a connection that has entered it would be. */
    private fun seated(): RoomMembership = RoomMembership().apply { code = room }

    @Test
    fun aregisteredWriterIsFoundByItsPlayer() {
        val directory = ConnectionDirectory()
        val writer = ConnectionWriter()

        directory.register(player, writer, seated())

        assertSame(writer, directory.writerFor(player, room))
    }

    @Test
    fun anunknownPlayerHasNoWriter() {
        val directory = ConnectionDirectory()

        assertNull(directory.writerFor(player, room))
    }

    @Test
    fun asecondRegistrationReplacesTheFirst() {
        val directory = ConnectionDirectory()
        val first = ConnectionWriter()
        val second = ConnectionWriter()

        directory.register(player, first, seated())
        directory.register(player, second, seated())

        assertSame(second, directory.writerFor(player, room))
        assertEquals(1, directory.size)
    }

    @Test
    fun forgetRemovesOnlyTheWriterItNames() {
        val directory = ConnectionDirectory()
        val first = ConnectionWriter()
        val second = ConnectionWriter()
        directory.register(player, first, seated())
        directory.register(player, second, seated())

        val removed = directory.forget(player, first)

        assertFalse(removed)
        assertSame(second, directory.writerFor(player, room))
    }

    @Test
    fun forgetRemovesTheWriterItDoesName() {
        val directory = ConnectionDirectory()
        val writer = ConnectionWriter()
        directory.register(player, writer, seated())

        val removed = directory.forget(player, writer)

        assertTrue(removed)
        assertNull(directory.writerFor(player, room))
    }

    @Test
    fun forgettingTwiceRemovesNothingTheSecondTime() {
        val directory = ConnectionDirectory()
        val writer = ConnectionWriter()
        directory.register(player, writer, seated())

        val first = directory.forget(player, writer)
        val second = directory.forget(player, writer)

        assertTrue(first)
        assertFalse(second)
    }

    @Test
    fun awriterIsFoundOnlyForTheRoomItsConnectionIsIn() {
        val directory = ConnectionDirectory()
        val writer = ConnectionWriter()
        directory.register(player, writer, seated())

        assertSame(writer, directory.writerFor(player, room))
        assertNull(directory.writerFor(player, otherRoom))
    }

    @Test
    fun aconnectionInNoRoomHasNoWriterForAnyRoom() {
        val directory = ConnectionDirectory()
        val writer = ConnectionWriter()
        directory.register(player, writer, RoomMembership())

        assertNull(directory.writerFor(player, room))
        assertNull(directory.writerFor(player, otherRoom))
        assertEquals(1, directory.size)
    }

    @Test
    fun alookupFollowsTheRoomTheConnectionMovesTo() {
        val directory = ConnectionDirectory()
        val writer = ConnectionWriter()
        val membership = seated()
        directory.register(player, writer, membership)

        membership.code = otherRoom

        assertNull(directory.writerFor(player, room))
        assertSame(writer, directory.writerFor(player, otherRoom))
    }
}
