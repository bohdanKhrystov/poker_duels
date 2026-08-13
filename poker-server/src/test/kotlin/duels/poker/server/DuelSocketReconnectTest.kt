package duels.poker.server

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
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
import duels.poker.server.session.DeviceId
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** A fixed seed, so a test that reaches the opening hand sees the same cards every run. */
private val fixedSeeds = HandSeedSource { 7L }

/** A [RoomRegistry] over [clock] — the test's own hand on time — whose opening hand is reproducible. */
private fun testRoomRegistry(clock: MutableClock): RoomRegistry =
    RoomRegistry(RandomRoomCodeSource(), clock, seeds = fixedSeeds)

/** Reads the next frame off [this] session as a decoded [ServerMessage]. */
private suspend fun DefaultClientWebSocketSession.nextServerMessage(): ServerMessage {
    val frame = incoming.receive() as Frame.Text
    return protocolJson.decodeFromString(frame.readText())
}

/**
 * Reads every [ServerMessage] already queued, or arriving within 300 milliseconds, for [this]
 * session — see `DuelSocketRoomTest`'s copy of this helper for why the wait is bounded rather than
 * a hard-coded frame count.
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

/** The two connected sockets of a started duel, its room code, and each seat's opening frames. */
private data class ReconnectSetup(
    val host: DefaultClientWebSocketSession,
    val guest: DefaultClientWebSocketSession,
    val code: RoomCode,
    val hostOpening: List<ServerMessage>,
    val guestOpening: List<ServerMessage>,
)

/**
 * Opens a room as `"host"`, joins it as `"guest"`, and drains the opening hand's frames from both
 * sockets, so a later read on either sees only what happens after this call — a duel is live
 * before this returns, with nothing yet dropped.
 */
private suspend fun HttpClient.startDuel(): ReconnectSetup {
    val (host, created) = openRoomAsHost("host")
    val (guest, _) = joinRoom("guest", created.code)
    val hostOpening = host.drainServerMessages()
    val guestOpening = guest.drainServerMessages()
    return ReconnectSetup(host, guest, RoomCode(created.code), hostOpening, guestOpening)
}

/** Waits, without a fixed sleep, for the room [code] names in [rooms] to satisfy [until]. */
private suspend fun awaitRoom(rooms: RoomRegistry, code: RoomCode, until: (Room) -> Boolean) =
    withTimeout(5.seconds) {
        while (rooms.get(code)?.let(until) != true) delay(10)
    }

/**
 * Closes [guest]'s connection and waits for the room [code] names in [rooms] to actually be
 * paused, rather than trusting [guest]'s own view of its close.
 *
 * `TASK-020813`'s regression test asserted straight off a client-side `closeReason`, and caught
 * its own bug only 8 times in 15: that signal resolves once the client sees the close frame the
 * server sends *before* running the `finally` block that starts this window, so it proves nothing
 * about whether `RoomRegistry.disconnect` — the call every test in this file depends on having
 * completed — has actually returned. Polling the registry itself, the server's own state, does.
 */
private suspend fun dropGuest(rooms: RoomRegistry, guest: DefaultClientWebSocketSession, code: RoomCode) {
    guest.close()
    awaitRoom(rooms, code) { it.isPaused }
}

/**
 * Reconnects `"guest"` on a brand new socket — completing the handshake with the same device id,
 * then sending the [JoinRoom] it already knows how to send (`ADR-0018`) — and returns every frame
 * that arrives for it, starting with the [ServerMessage.RoomJoined] a resumed seat is always told
 * first. That reply is itself the proof any server-side resumption work already finished: it is
 * sent only after `RoomRegistry.resume` has already returned.
 */
private suspend fun HttpClient.reconnectGuest(code: RoomCode): List<ServerMessage> {
    val session = webSocketSession("/ws")
    session.completeHandshake("guest")
    session.send(Frame.Text(ProtocolCodec.encode(JoinRoom(code.value))))
    val joined = session.nextServerMessage()
    return listOf(joined) + session.drainServerMessages()
}

/**
 * `TASK-020814`: a `JoinRoom` from a player who already holds a seat resumes it — the window
 * stops, the socket is told which seat it has, and the state it is entitled to arrives through the
 * projection layer — while a `JoinRoom` from any other device is refused exactly as it always has
 * been (`TASK-020731`, `TASK-020734`), leaving the held seat's window untouched.
 */
class DuelSocketReconnectTest {
    @Test
    fun aReturningSocketIsToldItsSeat(): Unit = testApplication {
        val rooms = testRoomRegistry(MutableClock())
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val setup = client.startDuel()
            dropGuest(rooms, setup.guest, setup.code)

            val joined = client.reconnectGuest(setup.code).first() as ServerMessage.RoomJoined

            assertEquals(setup.code.value, joined.code)
            assertEquals(1, joined.seat)
        }
    }

    @Test
    fun aReturningSocketSeesItsOwnCardsAndNotTheOpponents(): Unit = testApplication {
        val rooms = testRoomRegistry(MutableClock())
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val setup = client.startDuel()
            dropGuest(rooms, setup.guest, setup.code)

            val frames = client.reconnectGuest(setup.code)
            val view = frames.filterIsInstance<ServerMessage.Snapshot>().single().view

            assertEquals(2, view.viewer.holeCards.size)
            assertTrue(view.opponent.holeCards.isEmpty())
        }
    }

    @Test
    fun aReturningSocketResumesTheSameState(): Unit = testApplication {
        val rooms = testRoomRegistry(MutableClock())
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val setup = client.startDuel()
            val dealtView = setup.guestOpening.filterIsInstance<ServerMessage.Snapshot>().single().view
            dropGuest(rooms, setup.guest, setup.code)

            val frames = client.reconnectGuest(setup.code)
            val resumedView = frames.filterIsInstance<ServerMessage.Snapshot>().single().view

            // No action happened between the deal and the drop, so resuming must hand back the
            // very same view rather than one the projection layer re-derived from scratch.
            assertEquals(dealtView, resumedView)
        }
    }

    @Test
    fun theDuelIsRunningAgainAfterAReconnect(): Unit = testApplication {
        val rooms = testRoomRegistry(MutableClock())
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val setup = client.startDuel()
            // the room always opens with the host on the button (Room.open), hence on turn first.
            val yourTurn = setup.hostOpening.filterIsInstance<ServerMessage.YourTurn>().single()
            dropGuest(rooms, setup.guest, setup.code)

            client.reconnectGuest(setup.code)
            awaitRoom(rooms, setup.code) { !it.isPaused }

            assertFalse(rooms.get(setup.code)!!.isPaused)

            setup.host.send(
                Frame.Text(
                    ProtocolCodec.encode(Act(yourTurn.handNumber, yourTurn.actionSequence, PlayerAction.Fold(0))),
                ),
            )
            val afterFold = setup.host.drainServerMessages()

            assertFalse(afterFold.any { it is ServerMessage.Failure && it.error == ProtocolError.DUEL_PAUSED })
            assertTrue(afterFold.any { it is ServerMessage.Snapshot })
        }
    }

    @Test
    fun anotherDeviceMayNotTakeAHeldSeat(): Unit = testApplication {
        val rooms = testRoomRegistry(MutableClock())
        application {
            module()
            duelSocket(testDeps(rooms = rooms))
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val setup = client.startDuel()
            dropGuest(rooms, setup.guest, setup.code)
            val originalDeadline = rooms.get(setup.code)!!.gracePeriods.getValue(1)

            val (_, response) = client.joinRoom("third", setup.code.value)
            val failure = response as ServerMessage.Failure

            assertEquals(ProtocolError.ROOM_FULL, failure.error)
            // The point: a refusal that quietly cleared the window would still pass the line above.
            assertEquals(originalDeadline, rooms.get(setup.code)!!.gracePeriods.getValue(1))
        }
    }

    @Test
    fun aReconnectAfterTheDuelFinishedGetsTheFinishedState(): Unit = testApplication {
        val rooms = testRoomRegistry(MutableClock())
        val deps = testDeps(rooms = rooms)
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            // CreateRoom always opens DuelFormat.DEFAULT, so a one-hand duel is pre-created
            // directly on the registry and handed to the host and guest over the wire instead.
            val format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
            val host = deps.directory.resolve(DeviceId("host"))
            val room = rooms.create(host.id, format)

            val (hostSession, _) = client.joinRoom("host", room.code.value)
            val (guestSession, _) = client.joinRoom("guest", room.code.value)
            val hostOpening = hostSession.drainServerMessages()
            guestSession.drainServerMessages()

            // folding hand 1's only decision ends that hand, and FixedHands(1) ends the duel there.
            val yourTurn = hostOpening.filterIsInstance<ServerMessage.YourTurn>().single()
            hostSession.send(
                Frame.Text(
                    ProtocolCodec.encode(Act(yourTurn.handNumber, yourTurn.actionSequence, PlayerAction.Fold(0))),
                ),
            )
            hostSession.drainServerMessages()
            guestSession.drainServerMessages()

            dropGuest(rooms, guestSession, room.code)

            val frames = client.reconnectGuest(room.code)
            val joined = frames.first() as ServerMessage.RoomJoined
            val rest = frames.drop(1)

            assertEquals(room.code.value, joined.code)
            assertEquals(1, joined.seat)
            assertEquals(1, rest.count { it is ServerMessage.DuelFinished })
            assertTrue(rest.none { it is ServerMessage.Snapshot })
            assertTrue(rest.none { it is ServerMessage.YourTurn })
        }
    }
}
