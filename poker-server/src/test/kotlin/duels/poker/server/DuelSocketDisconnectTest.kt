package duels.poker.server

import duels.poker.engine.game.PlayerAction
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.CreateRoom
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.JoinRoom
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ProtocolError
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import duels.poker.server.room.RandomRoomCodeSource
import duels.poker.server.room.Room
import duels.poker.server.room.RoomCode
import duels.poker.server.room.RoomRegistry
import duels.poker.server.room.RoomTimeouts
import duels.poker.server.session.SessionRegistry
import duels.poker.server.session.testDeps
import duels.poker.server.time.MutableClock
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** A fixed seed, so a test that reaches the opening hand sees the same cards every run. */
private val fixedSeeds = HandSeedSource { 7L }

/**
 * Deliberately not [RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS]: a test that checks a grace
 * deadline against this constant, rather than a hard-coded literal, is proving the value travelled
 * from the registry's configured timeouts.
 */
private const val TEST_DISCONNECT_GRACE_MILLIS = 45_000L

/** A [RoomRegistry] over [clock], with a distinctive, test-only disconnect grace window. */
private fun testRoomRegistry(clock: MutableClock): RoomRegistry =
    RoomRegistry(
        RandomRoomCodeSource(),
        clock,
        RoomTimeouts(
            waitingMillis = RoomTimeouts.DEFAULT_WAITING_MILLIS,
            finishedMillis = RoomTimeouts.DEFAULT_FINISHED_MILLIS,
            disconnectGraceMillis = TEST_DISCONNECT_GRACE_MILLIS,
        ),
        seeds = fixedSeeds,
    )

/** Reads the next frame off [this] session as a decoded [ServerMessage]. */
private suspend fun DefaultClientWebSocketSession.nextServerMessage(): ServerMessage {
    val frame = incoming.receive() as Frame.Text
    return protocolJson.decodeFromString(frame.readText())
}

/**
 * Reads every [ServerMessage] already queued, or arriving within 300 milliseconds, for [this]
 * session — the opening hand's broadcast plus turn prompt arrive as a burst whose exact count this
 * file has no opinion on; see `DuelSocketRoomTest`'s copy of this helper.
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

/** The two connected sockets of a started duel, and the room code they share. */
private data class DuelSetup(
    val host: DefaultClientWebSocketSession,
    val guest: DefaultClientWebSocketSession,
    val code: RoomCode,
)

/**
 * Opens a room as `"host"`, joins it as `"guest"`, and drains the opening hand's frames from both
 * sockets, so a later read on either sees only what happens after this call.
 */
private suspend fun HttpClient.startDuel(): DuelSetup {
    val (host, created) = openRoomAsHost("host")
    val (guest, _) = joinRoom("guest", created.code)
    host.drainServerMessages()
    guest.drainServerMessages()
    return DuelSetup(host, guest, RoomCode(created.code))
}

/** Waits, without a fixed sleep, for the room [code] names in [rooms] to satisfy [until]. */
private suspend fun awaitRoom(rooms: RoomRegistry, code: RoomCode, until: (Room) -> Boolean) =
    withTimeout(5.seconds) {
        while (rooms.get(code)?.let(until) != true) delay(10)
    }

/** Waits, without a fixed sleep, for [sessions] to reach [expected] entries. */
private suspend fun awaitSize(sessions: SessionRegistry, expected: Int) =
    withTimeout(5.seconds) {
        while (sessions.size != expected) delay(10)
    }

/**
 * `TASK-020813`: a socket whose connection closes starts its seat's disconnect grace window
 * (`ADR-0013`) in the room it occupied — unless the close was `ADR-0018` adoption by that same
 * player's newer socket, or the socket never occupied a room at all.
 */
class DuelSocketDisconnectTest {
    @Test
    fun closingASocketStartsThatSeatsWindow(): Unit = testApplication {
        val rooms = testRoomRegistry(MutableClock())
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val setup = client.startDuel()

            setup.guest.close()
            awaitRoom(rooms, setup.code) { it.isPaused }

            assertEquals(setOf(1), rooms.get(setup.code)!!.gracePeriods.keys)
            assertTrue(rooms.get(setup.code)!!.isPaused)
        }
    }

    @Test
    fun theWindowIsTheConfiguredOne(): Unit = testApplication {
        val clock = MutableClock(2_000L)
        val rooms = testRoomRegistry(clock)
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val setup = client.startDuel()

            setup.guest.close()
            awaitRoom(rooms, setup.code) { it.isPaused }

            val deadline = rooms.get(setup.code)!!.gracePeriods.getValue(1)
            assertEquals(clock.nowMillis() + TEST_DISCONNECT_GRACE_MILLIS, deadline)
        }
    }

    @Test
    fun theOpponentCannotActWhileTheDuelIsPaused(): Unit = testApplication {
        val rooms = testRoomRegistry(MutableClock())
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val setup = client.startDuel()
            val runnerBefore = rooms.get(setup.code)!!.runner

            setup.guest.close()
            awaitRoom(rooms, setup.code) { it.isPaused }

            setup.host.send(Frame.Text(ProtocolCodec.encode(Act(1, 0, PlayerAction.Fold(0)))))
            val failure = setup.host.nextServerMessage() as ServerMessage.Failure

            assertEquals(ProtocolError.DUEL_PAUSED, failure.error)
            assertSame(runnerBefore, rooms.get(setup.code)!!.runner)
        }
    }

    @Test
    fun aSecondSocketForTheSameDeviceDoesNotPauseTheDuel(): Unit = testApplication {
        val rooms = testRoomRegistry(MutableClock())
        val sessions = SessionRegistry()
        application {
            module()
            duelSocket(testDeps(rooms = rooms, sessions = sessions))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val setup = client.startDuel()

            val second = client.webSocketSession("/ws")
            second.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "guest"))))
            second.nextServerMessage()

            val closeReason = setup.guest.closeReason.await()
            assertEquals(SEAT_ADOPTED, closeReason?.message)

            // closeReason is the client's own view of the close, resolved once it sees the close
            // frame the server sends *before* running the old connection's finally block — where
            // the disconnect guard this test exists to prove actually runs. Asserting straight off
            // it can race that block. sessions.remove is that finally block's first statement, so
            // waiting for the count to fall back to its pre-adoption value (host + one guest)
            // waits for the server's own cleanup to have started, not the client's view of it.
            awaitSize(sessions, 2)

            // RoomRegistry.disconnect is still a genuine suspending call inside that same finally
            // block — it takes the room's own mutex — so sessions settling only proves the block
            // *started*, not that this last, suspending statement has completed. Giving the wrong
            // outcome a bounded, polled window to appear is what makes "still unpaused" a settled
            // answer rather than a read that simply beat the suspending call to the punch; a room
            // that really did get wrongly paused shows it well within this window.
            withTimeoutOrNull(500.milliseconds) {
                while (rooms.get(setup.code)?.isPaused != true) delay(10)
            }

            assertFalse(rooms.get(setup.code)!!.isPaused)
            assertTrue(rooms.get(setup.code)!!.gracePeriods.isEmpty())
        }
    }

    @Test
    fun aSocketThatEnteredNoRoomPausesNothing(): Unit = testApplication {
        val rooms = testRoomRegistry(MutableClock())
        val sessions = SessionRegistry()
        application {
            module()
            duelSocket(testDeps(rooms = rooms, sessions = sessions))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val setup = client.startDuel()
            awaitSize(sessions, 2)

            val third = client.webSocketSession("/ws")
            third.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "third"))))
            third.nextServerMessage()
            awaitSize(sessions, 3)

            third.close()
            awaitSize(sessions, 2)

            assertFalse(rooms.get(setup.code)!!.isPaused)
        }
    }
}
