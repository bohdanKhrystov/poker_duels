package duels.poker.server

import duels.poker.server.duel.HandSeedSource
import duels.poker.server.protocol.CreateRoom
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.JoinRoom
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ProtocolError
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import duels.poker.server.room.RandomRoomCodeSource
import duels.poker.server.room.RoomCode
import duels.poker.server.room.RoomRegistry
import duels.poker.server.room.RoomState
import duels.poker.server.session.testDeps
import duels.poker.server.time.MutableClock
import duels.poker.server.time.ServerClock
import duels.poker.server.time.SystemClock
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.reflect.Field
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** A fixed seed, so a test that reaches the opening hand sees the same cards every run. */
private val fixedSeeds = HandSeedSource { 7L }

/** A [RoomRegistry] whose opening hand is reproducible — the code itself is still random. */
private fun testRoomRegistry(clock: ServerClock = SystemClock): RoomRegistry =
    RoomRegistry(RandomRoomCodeSource(), clock, seeds = fixedSeeds)

/** Reads the next frame off [this] session as a decoded [ServerMessage]. */
private suspend fun DefaultClientWebSocketSession.nextServerMessage(): ServerMessage {
    val frame = incoming.receive() as Frame.Text
    return protocolJson.decodeFromString(frame.readText())
}

/**
 * Reads every [ServerMessage] already queued, or arriving within 300 milliseconds, for [this]
 * session.
 *
 * Used once a message is known to trigger a burst of frames (the opening hand's broadcast plus
 * turn prompt) whose exact count this file does not hard-code — [duels.poker.server.duel.DuelStart]
 * owns that shape, not this test. The bounded wait is what tells "nothing else is coming" apart
 * from "the next frame just has not arrived yet"; the in-memory test transport makes 300ms ample
 * for frames the server already sent.
 */
private suspend fun DefaultClientWebSocketSession.drainServerMessages(): List<ServerMessage> {
    val messages = mutableListOf<ServerMessage>()
    while (true) {
        val frame = withTimeoutOrNull(300.milliseconds) { incoming.receive() } ?: break
        messages.add(protocolJson.decodeFromString((frame as Frame.Text).readText()))
    }
    return messages
}

/** Completes the handshake for [this] session and discards the [ServerMessage.Welcome]. */
private suspend fun DefaultClientWebSocketSession.completeHandshake(deviceId: String) {
    send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = deviceId))))
    nextServerMessage()
}

/** Opens a `/ws` connection for [deviceId], completes its handshake, and opens a fresh room. */
private suspend fun HttpClient.openRoomAsHost(
    deviceId: String,
): Pair<DefaultClientWebSocketSession, ServerMessage.RoomJoined> {
    val host = webSocketSession("/ws")
    host.completeHandshake(deviceId)
    host.send(Frame.Text(ProtocolCodec.encode(CreateRoom)))
    val joined = host.nextServerMessage() as ServerMessage.RoomJoined
    // Every RoomJoined is followed by the seats' names; consumed here so a test reads the
    // frames it is about, not the one that names the table.
    check(host.nextServerMessage() is ServerMessage.SeatNames) { "a RoomJoined is followed by SeatNames" }
    return host to joined
}

/** Opens a `/ws` connection for [deviceId], completes its handshake, and attempts to join [code]. */
private suspend fun HttpClient.joinRoom(
    deviceId: String,
    code: String,
): Pair<DefaultClientWebSocketSession, ServerMessage> {
    val session = webSocketSession("/ws")
    session.completeHandshake(deviceId)
    session.send(Frame.Text(ProtocolCodec.encode(JoinRoom(code))))
    val response = session.nextServerMessage()
    if (response is ServerMessage.RoomJoined) {
        check(session.nextServerMessage() is ServerMessage.SeatNames) { "a RoomJoined is followed by SeatNames" }
    }
    return session to response
}

/** Removes a room from the registry using reflection to access its internal state. */
private fun RoomRegistry.removeRoom(code: RoomCode) {
    val roomsField: Field = RoomRegistry::class.java.getDeclaredField("rooms")
    roomsField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val roomsMap = roomsField.get(this) as? java.util.concurrent.ConcurrentHashMap<RoomCode, *>
    roomsMap?.remove(code)
}

/**
 * `TASK-020731`: a socket can open a room and be told its code, a second socket can join it by
 * that code, and the moment the second seat is taken both sockets receive the opening hand — each
 * seeing only what the engine says it may.
 */
class DuelSocketRoomTest {
    @Test
    fun createRoomAnswersWithACodeAndSeatZero() = testApplication {
        val rooms = testRoomRegistry()
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (_, created) = client.openRoomAsHost("host")

            assertEquals(0, created.seat)
            assertNotNull(rooms.get(RoomCode(created.code)))
        }
    }

    @Test
    fun joinRoomSeatsTheGuestInSeatOne() = testApplication {
        application {
            module()
            duelSocket(testDeps(rooms = testRoomRegistry()))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (_, created) = client.openRoomAsHost("host")
            val (_, response) = client.joinRoom("guest", created.code)
            val joined = response as ServerMessage.RoomJoined

            assertEquals(created.code, joined.code)
            assertEquals(1, joined.seat)
        }
    }

    @Test
    fun bothSeatsAreToldEachOthersNamesOnceTheTableIsFull() = testApplication {
        // The directory mints player ids from device ids; the lookup below names players by the
        // id the socket resolved, so the two tables agree by construction rather than by string.
        val directory = duels.poker.server.session.InMemoryPlayerDirectory()
        val names = duels.poker.server.session.DisplayNameLookup { player ->
            when (player) {
                directory.findOrNull(duels.poker.server.session.DeviceId("host"))?.id -> "Ada"
                else -> null
            }
        }
        application {
            module()
            duelSocket(testDeps(directory = directory, rooms = testRoomRegistry(), displayNames = names))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val host = client.webSocketSession("/ws")
            host.completeHandshake("host")
            host.send(Frame.Text(ProtocolCodec.encode(CreateRoom)))
            val created = host.nextServerMessage() as ServerMessage.RoomJoined
            // Straight after the host's own RoomJoined: the host's name, and no guest yet.
            assertEquals(ServerMessage.SeatNames(listOf("Ada", null)), host.nextServerMessage())

            val guest = client.webSocketSession("/ws")
            guest.completeHandshake("guest")
            guest.send(Frame.Text(ProtocolCodec.encode(JoinRoom(created.code))))
            assertTrue(guest.nextServerMessage() is ServerMessage.RoomJoined)

            // The moment the second seat is taken, both sockets are told both names — the
            // guest has none, and reads as null rather than as a stand-in string — and the
            // names precede the opening hand, so a table never draws a nameless rival for a
            // frame and then corrects itself.
            val expected = ServerMessage.SeatNames(listOf("Ada", null))
            val guestFrames = guest.drainServerMessages()
            val hostFrames = host.drainServerMessages()
            assertEquals(expected, guestFrames.first())
            assertEquals(expected, hostFrames.first())
            assertTrue(guestFrames.any { it is ServerMessage.Snapshot })
            assertTrue(hostFrames.any { it is ServerMessage.Snapshot })
        }
    }

    @Test
    fun anUnknownCodeIsRefused() = testApplication {
        application {
            module()
            duelSocket(testDeps(rooms = testRoomRegistry()))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (_, response) = client.joinRoom("solo", "ZZZZZZZZ")
            val reply = response as ServerMessage.Failure

            assertEquals(ProtocolError.UNKNOWN_ROOM, reply.error)
        }
    }

    @Test
    fun amalformedCodeIsRefusedTheSameWay() = testApplication {
        application {
            module()
            duelSocket(testDeps(rooms = testRoomRegistry()))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (_, response) = client.joinRoom("solo", "!")
            val reply = response as ServerMessage.Failure

            assertEquals(ProtocolError.UNKNOWN_ROOM, reply.error)
        }
    }

    @Test
    fun athirdClientFindsTheRoomFull() = testApplication {
        application {
            module()
            duelSocket(testDeps(rooms = testRoomRegistry()))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (_, created) = client.openRoomAsHost("host")
            client.joinRoom("guest", created.code)

            val (_, response) = client.joinRoom("third", created.code)
            val reply = response as ServerMessage.Failure

            assertEquals(ProtocolError.ROOM_FULL, reply.error)
        }
    }

    @Test
    fun thehostRejoiningItsOwnRoomIsToldItsSeat() = testApplication {
        application {
            module()
            duelSocket(testDeps(rooms = testRoomRegistry()))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (host, created) = client.openRoomAsHost("host")

            host.send(Frame.Text(ProtocolCodec.encode(JoinRoom(created.code))))
            val rejoined = host.nextServerMessage() as ServerMessage.RoomJoined

            assertEquals(created.code, rejoined.code)
            assertEquals(0, rejoined.seat)
        }
    }

    @Test
    fun bothSeatsReceiveTheOpeningHand() = testApplication {
        application {
            module()
            duelSocket(testDeps(rooms = testRoomRegistry()))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (host, created) = client.openRoomAsHost("host")
            val (guest, _) = client.joinRoom("guest", created.code)

            val hostMessages = host.drainServerMessages()
            val guestMessages = guest.drainServerMessages()

            assertTrue(hostMessages.any { it is ServerMessage.Snapshot })
            assertTrue(guestMessages.any { it is ServerMessage.Snapshot })

            val yourTurns =
                hostMessages.count { it is ServerMessage.YourTurn } +
                    guestMessages.count { it is ServerMessage.YourTurn }
            assertEquals(1, yourTurns)
        }
    }

    @Test
    fun neitherSeatSeesTheOthersHoleCards() = testApplication {
        application {
            module()
            duelSocket(testDeps(rooms = testRoomRegistry()))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (host, created) = client.openRoomAsHost("host")
            val (guest, _) = client.joinRoom("guest", created.code)

            val hostView = host.drainServerMessages().filterIsInstance<ServerMessage.Snapshot>().single().view
            val guestView = guest.drainServerMessages().filterIsInstance<ServerMessage.Snapshot>().single().view

            assertTrue(hostView.opponent.holeCards.isEmpty())
            assertEquals(2, hostView.viewer.holeCards.size)

            assertTrue(guestView.opponent.holeCards.isEmpty())
            assertEquals(2, guestView.viewer.holeCards.size)
        }
    }

    @Test
    fun aRejoinWhoseRoomVanishedIsRefusedNotFatal(): Unit = testApplication {
        val rooms = testRoomRegistry()
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (host, created) = client.openRoomAsHost("host")
            val roomCode = RoomCode(created.code)

            // Verify the room exists in the registry
            assertNotNull(rooms.get(roomCode))

            // Remove the room from the registry to simulate it vanishing
            rooms.removeRoom(roomCode)

            // Attempt to rejoin the same room whose code still exists but room was removed
            host.send(Frame.Text(ProtocolCodec.encode(JoinRoom(created.code))))
            val response = host.nextServerMessage()

            // Should receive Failure(UNKNOWN_ROOM), not an exception that closes the socket
            val failure = response as ServerMessage.Failure
            assertEquals(ProtocolError.UNKNOWN_ROOM, failure.error)

            // Socket should still be open; send another frame to verify
            host.send(Frame.Text(ProtocolCodec.encode(JoinRoom(created.code))))
            val secondResponse = host.nextServerMessage()
            assertEquals(ProtocolError.UNKNOWN_ROOM, (secondResponse as ServerMessage.Failure).error)
        }
    }

    @Test
    fun aRejoinToAnExistingRoomStillAnswersRoomJoined(): Unit = testApplication {
        val rooms = testRoomRegistry()
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (host, created) = client.openRoomAsHost("host")
            val roomCode = RoomCode(created.code)

            // Verify the room still exists in the registry
            assertNotNull(rooms.get(roomCode))

            // Rejoin the same room without removing it
            host.send(Frame.Text(ProtocolCodec.encode(JoinRoom(created.code))))
            val response = host.nextServerMessage() as ServerMessage.RoomJoined

            // Should get RoomJoined with the same code and seat
            assertEquals(created.code, response.code)
            assertEquals(0, response.seat)
        }
    }

    /**
     * `ADR-0124` §§1, 3: a second `CreateRoom` from the socket that already holds a `WAITING`
     * room is answered with that room, not a fresh one — one socket, one held room, `rooms.size`
     * never grows past `1`.
     */
    @Test
    fun aSecondCreateRoomHandsBackTheRoomTheHostAlreadyHolds() = testApplication {
        val rooms = testRoomRegistry()
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (host, first) = client.openRoomAsHost("host")

            host.send(Frame.Text(ProtocolCodec.encode(CreateRoom)))
            val second = host.nextServerMessage() as ServerMessage.RoomJoined

            assertEquals(first.code, second.code)
            assertEquals(first.seat, second.seat)
            assertEquals(1, rooms.size)
        }
    }

    /**
     * `ADR-0124` §4: handing a `WAITING` room back is not a re-stamp. The clock advances between
     * the two presses, so a fresh room minted by the second press would carry a code
     * [rooms.get] resolves to a room stamped `61_000` — the [MutableClock]'s value at that
     * press. The reading is taken off the **second** reply's own code, never the first press's
     * stashed one: a stale room the second press ignored would still read `1_000` no matter how
     * many fresh rooms a bug opened beside it, so only the code and room the second reply itself
     * names can tell "handed back" from "opened again". Both presses naming the **same** code is
     * the first half of that proof — codes are minted fresh and unique per [Room.open], so a
     * second, distinct room can never carry it — and that same code's room still stamped
     * `1_000` is the second: a returned room does not restart its own ten minutes.
     */
    @Test
    fun aReturnedRoomKeepsTheStampItWasOpenedWith() = testApplication {
        val clock = MutableClock(1_000)
        val rooms = testRoomRegistry(clock)
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (host, created) = client.openRoomAsHost("host")
            clock.advance(60_000)

            host.send(Frame.Text(ProtocolCodec.encode(CreateRoom)))
            val second = host.nextServerMessage() as ServerMessage.RoomJoined

            assertEquals(created.code, second.code)
            assertEquals(1_000L, rooms.get(RoomCode(second.code))!!.lastActivityAt)
        }
    }

    /**
     * `ADR-0133` §9: the `PLAYING` case must keep falling through to `create` — this story hands
     * back only a `WAITING` room. A host of a room a guest has already turned `PLAYING` is given a
     * brand new room by a second `CreateRoom`, the first stays `PLAYING`, and the registry now
     * holds both.
     */
    @Test
    fun aHolderOfAPlayingRoomStillGetsAFreshRoom() = testApplication {
        val rooms = testRoomRegistry()
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (host, created) = client.openRoomAsHost("host")
            client.joinRoom("guest", created.code)
            host.drainServerMessages()

            host.send(Frame.Text(ProtocolCodec.encode(CreateRoom)))
            val second = host.nextServerMessage() as ServerMessage.RoomJoined

            assertNotEquals(created.code, second.code)
            assertEquals(2, rooms.size)
            assertEquals(RoomState.PLAYING, rooms.get(RoomCode(created.code))!!.state)
        }
    }

    /**
     * Structural, on purpose: within this story a returned `WAITING` room and a freshly opened one
     * carry the same seat, `0`, so no behavioural test can tell a derived seat from the constant it
     * replaced. This reads `replyToCreateRoom`'s own body and asserts it derives the seat rather
     * than hard-coding it, and that the shipped `RoomJoined(created.code.value, 0)` line is gone.
     */
    @Test
    fun theCreateRoomAnswerReadsItsSeatOffTheRoom() {
        val source = File("src/main/kotlin/duels/poker/server/DuelSocket.kt").readText()
        val marker = "private suspend fun ConnectionWriter.replyToCreateRoom("
        val start = source.indexOf(marker)
        assertTrue(start >= 0)
        val end = source.indexOf("\n}\n", start)
        assertTrue(end >= 0)
        val body = source.substring(start, end)

        assertTrue(body.contains("heldOrOpen("))
        assertTrue(body.contains("seatOf(session.player.id)"))
        assertFalse(Regex("""RoomJoined\([^)]*,\s*0\s*\)""").containsMatchIn(body))
    }
}
