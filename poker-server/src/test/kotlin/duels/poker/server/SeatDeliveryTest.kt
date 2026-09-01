package duels.poker.server

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.duel.Addressed
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.SeatPresence
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.room.JoinResult
import duels.poker.server.room.Room
import duels.poker.server.room.RoomCode
import duels.poker.server.session.ConnectionDirectory
import duels.poker.server.session.ConnectionWriter
import duels.poker.server.session.PlayerId
import duels.poker.server.session.RoomMembership
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * [deliver] is the boundary the epic is most able to break quietly: a frame addressed to one seat
 * reaching the other seat's writer is the hole-card leak the whole design exists to prevent.
 *
 * No Ktor and no socket here. A [ConnectionWriter] is constructible on its own, and a coroutine
 * draining it with [ConnectionWriter.writeAll] collects exactly what [deliver] wrote to it — so
 * "seat 1's frame went to seat 1" is asserted directly from what each writer received.
 */
internal class SeatDeliveryTest {
    private val hostId = PlayerId("host-player")
    private val guestId = PlayerId("guest-player")
    private val roomCode = RoomCode("ABCDEFGH")

    /** A [duels.poker.server.room.RoomState.PLAYING] room with both seats filled. */
    private fun seatedRoom(): Room {
        val waiting = Room.open(roomCode, hostId, DuelFormat.DEFAULT, now = 0L)
        return (waiting.join(guestId, now = 0L) as JoinResult.Seated).room
    }

    /** A message distinguishable by [label] alone — its content plays no part in delivery. */
    private fun messageFor(label: String): ServerMessage = ServerMessage.Welcome(playerId = label, deviceId = label)

    /**
     * Registers one [ConnectionWriter] per entry of [writers], runs [deliver] with [frames]
     * against them, then closes and drains every registered writer and returns what each
     * collected, keyed by the same [PlayerId].
     *
     * Closing and draining happen only after [deliver] returns, so a bug that writes a frame to
     * the wrong writer still lands in that writer's own list rather than being lost to a drain
     * that finished too early.
     *
     * [connectedTo] names the room each writer's connection is in. A player absent from it is
     * registered in [room]'s own code, so every test written before rooms were scoped keeps
     * registering exactly the connection [deliver] expects to find there.
     */
    private suspend fun deliverAndDrain(
        frames: List<Addressed>,
        room: Room,
        writers: Map<PlayerId, ConnectionWriter>,
        connectedTo: Map<PlayerId, RoomCode?> = emptyMap(),
    ): Map<PlayerId, List<String>> = coroutineScope {
        val connections = ConnectionDirectory()
        val sinks: Map<PlayerId, MutableList<String>> = writers.mapValues { mutableListOf() }
        val drains = writers.map { (player, writer) ->
            val membership = RoomMembership().apply {
                code = if (player in connectedTo) connectedTo.getValue(player) else room.code
            }
            connections.register(player, writer, membership)
            launch { writer.writeAll { frame -> sinks.getValue(player).add(frame) } }
        }

        deliver(frames, room, connections)
        writers.values.forEach { it.close() }
        drains.forEach { it.join() }

        sinks
    }

    @Test
    fun eachSeatGetsOnlyItsOwnFrames(): Unit = runBlocking {
        val seat0 = Addressed(0, messageFor("seat-0-frame"))
        val seat1 = Addressed(1, messageFor("seat-1-frame"))

        val sinks = deliverAndDrain(
            frames = listOf(seat0, seat1),
            room = seatedRoom(),
            writers = mapOf(hostId to ConnectionWriter(), guestId to ConnectionWriter()),
        )

        assertEquals(listOf(ProtocolCodec.encode(seat0.message)), sinks.getValue(hostId))
        assertEquals(listOf(ProtocolCodec.encode(seat1.message)), sinks.getValue(guestId))
    }

    @Test
    fun aframeIsEncodedOncePerAddressed(): Unit = runBlocking {
        val first = Addressed(0, messageFor("first"))
        val second = Addressed(0, messageFor("second"))

        val sinks = deliverAndDrain(
            frames = listOf(first, second),
            room = seatedRoom(),
            writers = mapOf(hostId to ConnectionWriter()),
        )

        assertEquals(
            listOf(ProtocolCodec.encode(first.message), ProtocolCodec.encode(second.message)),
            sinks.getValue(hostId),
        )
    }

    @Test
    fun aseatWithNoWriterIsSkippedAndTheOtherStillReceives(): Unit = runBlocking {
        val seat0 = Addressed(0, messageFor("host-frame"))
        // The guest has no registered writer below. This frame must still be built and handed to
        // deliver — otherwise a missing entry in the guest's sink would prove nothing, since none
        // would have arrived even from a broadcasting bug that ignored the missing writer.
        val seat1 = Addressed(1, messageFor("guest-frame"))

        val sinks = deliverAndDrain(
            frames = listOf(seat0, seat1),
            room = seatedRoom(),
            writers = mapOf(hostId to ConnectionWriter()),
        )

        // If seat 1's frame had leaked to the only live writer instead of being skipped, it would
        // show up here as a second entry — exact equality catches that, not just presence of seat 0.
        assertEquals(listOf(ProtocolCodec.encode(seat0.message)), sinks.getValue(hostId))
    }

    @Test
    fun aroomWithNoGuestDropsSeatOnesFrames(): Unit = runBlocking {
        val waitingRoom = Room.open(roomCode, hostId, DuelFormat.DEFAULT, now = 0L)
        val seat0 = Addressed(0, messageFor("host-frame"))
        // No guest is seated in a WAITING room, yet this frame is still built and delivered, so a
        // bug that fell back to some other seat for an unseated one would be visible below.
        val seat1 = Addressed(1, messageFor("nobody-is-seated-here"))

        val sinks = deliverAndDrain(
            frames = listOf(seat0, seat1),
            room = waitingRoom,
            writers = mapOf(hostId to ConnectionWriter()),
        )

        assertEquals(listOf(ProtocolCodec.encode(seat0.message)), sinks.getValue(hostId))
    }

    @Test
    fun nothingIsWrittenToASeatTheFrameDoesNotName(): Unit = runBlocking {
        val hostFrames = listOf(messageFor("host-a"), messageFor("host-b")).map { Addressed(0, it) }
        val guestFrames = listOf(messageFor("guest-a"), messageFor("guest-b")).map { Addressed(1, it) }
        val mixed = listOf(hostFrames[0], guestFrames[0], hostFrames[1], guestFrames[1])

        val sinks = deliverAndDrain(
            frames = mixed,
            room = seatedRoom(),
            writers = mapOf(hostId to ConnectionWriter(), guestId to ConnectionWriter()),
        )

        val hostEncoded = hostFrames.map { ProtocolCodec.encode(it.message) }
        val guestEncoded = guestFrames.map { ProtocolCodec.encode(it.message) }

        assertEquals(hostEncoded, sinks.getValue(hostId))
        assertEquals(guestEncoded, sinks.getValue(guestId))
        hostEncoded.forEach { text -> assertFalse(sinks.getValue(guestId).contains(text)) }
        guestEncoded.forEach { text -> assertFalse(sinks.getValue(hostId).contains(text)) }
    }

    @Test
    fun aframeIsNotDeliveredToAConnectionInAnotherRoom(): Unit = runBlocking {
        val elsewhere = RoomCode("ZYXWVTSR")
        val seat0 = Addressed(0, messageFor("host-frame"))
        val seat1 = Addressed(1, messageFor("guest-frame"))

        val sinks = deliverAndDrain(
            frames = listOf(seat0, seat1),
            room = seatedRoom(),
            writers = mapOf(hostId to ConnectionWriter(), guestId to ConnectionWriter()),
            connectedTo = mapOf(guestId to elsewhere),
        )

        assertEquals(listOf(ProtocolCodec.encode(seat0.message)), sinks.getValue(hostId))
        assertEquals(emptyList<String>(), sinks.getValue(guestId))
    }

    @Test
    fun aframeIsNotDeliveredToAConnectionInNoRoom(): Unit = runBlocking {
        val seat0 = Addressed(0, messageFor("host-frame"))
        val seat1 = Addressed(1, messageFor("guest-frame"))

        val sinks = deliverAndDrain(
            frames = listOf(seat0, seat1),
            room = seatedRoom(),
            writers = mapOf(hostId to ConnectionWriter(), guestId to ConnectionWriter()),
            connectedTo = mapOf(guestId to null),
        )

        assertEquals(listOf(ProtocolCodec.encode(seat0.message)), sinks.getValue(hostId))
        assertEquals(emptyList<String>(), sinks.getValue(guestId))
    }

    @Test
    fun apresenceFrameCrossesNoRoomEither(): Unit = runBlocking {
        val elsewhere = RoomCode("ZYXWVTSR")
        val seat0 = Addressed(0, messageFor("host-frame"))
        val presence = Addressed(1, ServerMessage.OpponentPresence(SeatPresence.AWAY, graceRemainingMillis = 60000L))

        val sinks = deliverAndDrain(
            frames = listOf(seat0, presence),
            room = seatedRoom(),
            writers = mapOf(hostId to ConnectionWriter(), guestId to ConnectionWriter()),
            connectedTo = mapOf(guestId to elsewhere),
        )

        assertEquals(listOf(ProtocolCodec.encode(seat0.message)), sinks.getValue(hostId))
        assertEquals(emptyList<String>(), sinks.getValue(guestId))
    }
}
