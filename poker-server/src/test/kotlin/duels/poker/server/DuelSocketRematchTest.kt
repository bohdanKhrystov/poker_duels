package duels.poker.server

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.PlayerAction
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.JoinRoom
import duels.poker.server.protocol.OfferRematch
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import duels.poker.server.room.RandomRoomCodeSource
import duels.poker.server.room.RoomRegistry
import duels.poker.server.session.DeviceId
import duels.poker.server.session.SocketDependencies
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** A fixed seed, so every hand this file deals is reproducible run to run. */
private val fixedSeeds = HandSeedSource { 7L }

/** A [RoomRegistry] whose every hand draws from [seeds] — the room code itself is still random. */
private fun testRoomRegistry(seeds: HandSeedSource = fixedSeeds): RoomRegistry =
    RoomRegistry(RandomRoomCodeSource(), SystemClock, seeds = seeds)

/** Reads the next frame off [this] session as a decoded [ServerMessage]. */
private suspend fun DefaultClientWebSocketSession.nextServerMessage(): ServerMessage {
    val frame = incoming.receive() as Frame.Text
    return protocolJson.decodeFromString(frame.readText())
}

/**
 * Reads every [ServerMessage] already queued, or arriving within 300 milliseconds, for [this]
 * session — used once a message is known to trigger a burst of frames whose exact count this
 * file does not hard-code. The bounded wait is what tells "nothing else is coming" apart from
 * "the next frame just has not arrived yet"; the in-memory test transport makes 300ms ample for
 * frames the server already sent.
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

/** Sends [action] as an [Act] answering the decision point named by [handNumber] and [actionSequence]. */
private suspend fun DefaultClientWebSocketSession.sendAct(handNumber: Int, actionSequence: Int, action: PlayerAction) {
    send(Frame.Text(ProtocolCodec.encode(Act(handNumber, actionSequence, action))))
}

/** Opens a `/ws` connection for [deviceId], completes its handshake, and joins the room at [code]. */
private suspend fun HttpClient.enterRoom(
    deviceId: String,
    code: String,
): Pair<DefaultClientWebSocketSession, ServerMessage.RoomJoined> {
    val session = webSocketSession("/ws")
    session.completeHandshake(deviceId)
    session.send(Frame.Text(ProtocolCodec.encode(JoinRoom(code))))
    return session to (session.nextServerMessage() as ServerMessage.RoomJoined)
}

/** Two sockets already sitting on a room whose duel has just finished. */
private data class FinishedDuel(
    val host: DefaultClientWebSocketSession,
    val guest: DefaultClientWebSocketSession,
)

/**
 * Pre-creates a room directly on [deps]'s [RoomRegistry] — the only way a test picks a one-hand
 * format, since [duels.poker.server.protocol.CreateRoom] always opens [DuelFormat.DEFAULT] — hands
 * a host and a guest device onto it over the wire through [JoinRoom], exactly as real clients
 * would, then folds the host's only decision: hand 1's fold ends that hand, and
 * [EndCondition.FixedHands] `(1)` ends the duel there. Both sockets are drained before returning,
 * so a later read on either sees only what happens after this call.
 */
private suspend fun HttpClient.finishedDuel(deps: SocketDependencies): FinishedDuel {
    val format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
    val host = deps.directory.resolve(DeviceId("host"))
    val room = deps.rooms.create(host.id, format)

    val (hostSession, _) = enterRoom("host", room.code.value)
    val (guestSession, _) = enterRoom("guest", room.code.value)

    // the room always opens with the host on the button (Room.open), hence on turn first.
    val hostOpening = hostSession.drainServerMessages()
    guestSession.drainServerMessages()
    val yourTurn = hostOpening.filterIsInstance<ServerMessage.YourTurn>().single()

    // folding hand 1's only decision ends that hand, and FixedHands(1) ends the duel there.
    hostSession.sendAct(yourTurn.handNumber, yourTurn.actionSequence, PlayerAction.Fold(0))
    hostSession.drainServerMessages()
    guestSession.drainServerMessages()

    return FinishedDuel(hostSession, guestSession)
}

/**
 * True once none of [messages] is a sign a duel started: a [ServerMessage.Snapshot], an
 * [ServerMessage.Events], or a [ServerMessage.YourTurn].
 */
private fun noDuelStarted(messages: List<ServerMessage>): Boolean =
    messages.none { it is ServerMessage.Snapshot || it is ServerMessage.Events || it is ServerMessage.YourTurn }

/**
 * `TASK-021302`: `ADR-0044` §2's first half over the wire. One seat's [OfferRematch] puts exactly
 * one [ServerMessage.RematchOffered], naming the offering seat, on **both** sockets of a finished
 * duel, and starts no new duel by itself.
 */
class DuelSocketRematchTest {
    @Test
    fun oneOfferPutsOneRematchOfferedNamingThatSeatOnBothSockets() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val duel = client.finishedDuel(deps)

            duel.host.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))

            val hostAfter = duel.host.drainServerMessages()
            val guestAfter = duel.guest.drainServerMessages()

            val hostOffers = hostAfter.filterIsInstance<ServerMessage.RematchOffered>()
            val guestOffers = guestAfter.filterIsInstance<ServerMessage.RematchOffered>()

            // asserted per socket, not on the pooled total — a frame delivered twice to one
            // socket and never to the other would otherwise still sum to two.
            assertEquals(1, hostOffers.size)
            assertEquals(1, guestOffers.size)
            assertEquals(0, hostOffers.single().seat)
            assertEquals(0, guestOffers.single().seat)

            assertTrue(noDuelStarted(hostAfter), "host saw a message only a started duel would send: $hostAfter")
            assertTrue(noDuelStarted(guestAfter), "guest saw a message only a started duel would send: $guestAfter")
        }
    }
}
