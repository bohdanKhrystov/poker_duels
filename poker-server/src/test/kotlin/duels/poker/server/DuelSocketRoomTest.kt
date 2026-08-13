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
import duels.poker.server.session.testDeps
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Field
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** A fixed seed, so a test that reaches the opening hand sees the same cards every run. */
private val fixedSeeds = HandSeedSource { 7L }

/** A [RoomRegistry] whose opening hand is reproducible — the code itself is still random. */
private fun testRoomRegistry(): RoomRegistry = RoomRegistry(RandomRoomCodeSource(), SystemClock, seeds = fixedSeeds)

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
    return host to (host.nextServerMessage() as ServerMessage.RoomJoined)
}

/** Opens a `/ws` connection for [deviceId], completes its handshake, and attempts to join [code]. */
private suspend fun HttpClient.joinRoom(
    deviceId: String,
    code: String,
): Pair<DefaultClientWebSocketSession, ServerMessage> {
    val session = webSocketSession("/ws")
    session.completeHandshake(deviceId)
    session.send(Frame.Text(ProtocolCodec.encode(JoinRoom(code))))
    return session to session.nextServerMessage()
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
}
