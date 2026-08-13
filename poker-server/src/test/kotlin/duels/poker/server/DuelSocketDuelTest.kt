package duels.poker.server

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.EndCondition
import duels.poker.engine.game.HandRevealed
import duels.poker.engine.game.HandStarted
import duels.poker.engine.game.HoleCardsDealt
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.game.Rejection
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.JoinRoom
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ProtocolError
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
import org.junit.jupiter.api.Assertions.assertNull
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

/** Two sockets, each already told the seat it holds in the same live duel. */
private data class SeatedDuel(
    val code: String,
    val host: DefaultClientWebSocketSession,
    val hostSeat: Int,
    val guest: DefaultClientWebSocketSession,
    val guestSeat: Int,
)

/**
 * Pre-creates a room directly on [deps]'s [RoomRegistry] — the only way a test picks [format],
 * since [duels.poker.server.protocol.CreateRoom] always opens [DuelFormat.DEFAULT] — then hands a
 * host and a guest device onto it over the wire through [duels.poker.server.protocol.JoinRoom],
 * exactly as real clients would. The host's `ALREADY_SEATED` join and the guest's fresh one leave
 * each side's own `RoomJoined` reply already consumed, but nothing else drained.
 */
private suspend fun HttpClient.openADuel(deps: SocketDependencies, format: DuelFormat = DuelFormat.DEFAULT): SeatedDuel {
    val host = deps.directory.resolve(DeviceId("host"))
    val room = deps.rooms.create(host.id, format)

    val (hostSession, hostJoined) = enterRoom("host", room.code.value)
    val (guestSession, guestJoined) = enterRoom("guest", room.code.value)

    return SeatedDuel(room.code.value, hostSession, hostJoined.seat, guestSession, guestJoined.seat)
}

/**
 * For each index in [messages] holding a [ServerMessage.Snapshot], the seats a [HandRevealed] has
 * named so far **in the current hand only** — reset on every [HandStarted]. This mirrors exactly
 * what a real client could compute from what it was sent; nothing here reads engine state.
 */
private fun revealedAtEachSnapshot(messages: List<ServerMessage>): Map<Int, Set<Int>> {
    val result = mutableMapOf<Int, Set<Int>>()
    var revealed = mutableSetOf<Int>()
    messages.forEachIndexed { index, message ->
        when (message) {
            is ServerMessage.Events -> message.events.forEach { event ->
                when (event) {
                    is HandStarted -> revealed = mutableSetOf()
                    is HandRevealed -> revealed.add(event.seat)
                    else -> Unit
                }
            }
            is ServerMessage.Snapshot -> result[index] = revealed.toSet()
            else -> Unit
        }
    }
    return result
}

/**
 * `TASK-020715`: an `Act` frame on a socket seated in a live room reaches the duel through that
 * room's own mutex, and every frame the duel produces — including the `DuelFinished` that ends
 * it — reaches only the seat the engine's projection layer addressed it to. This is the last
 * ticket of `STORY-0207`: after it, an action arriving on a socket reaches
 * `DefaultPokerEngine.handle`, and both players get back exactly the view the engine says each is
 * entitled to.
 */
class DuelSocketDuelTest {
    @Test
    fun theSeatOnTurnIsTheOnlyOneToldItIsItsTurn() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val duel = client.openADuel(deps)
            val hostTurns = duel.host.drainServerMessages().filterIsInstance<ServerMessage.YourTurn>()
            val guestTurns = duel.guest.drainServerMessages().filterIsInstance<ServerMessage.YourTurn>()

            assertEquals(1, hostTurns.size + guestTurns.size)
            val prompted = (hostTurns + guestTurns).single()
            val promptedSocketSeat = if (hostTurns.isNotEmpty()) duel.hostSeat else duel.guestSeat
            assertEquals(promptedSocketSeat, prompted.legalActions.seat)
        }
    }

    @Test
    fun anActFromTheSeatOnTurnMovesTheDuelAndBothSeatsSeeIt() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val duel = client.openADuel(deps)
            // the room always opens with the host on the button (Room.open), hence on turn first.
            val opening = duel.host.drainServerMessages()
            duel.guest.drainServerMessages()
            val yourTurn = opening.filterIsInstance<ServerMessage.YourTurn>().single()

            duel.host.sendAct(yourTurn.handNumber, yourTurn.actionSequence, PlayerAction.Fold(duel.hostSeat))

            val hostAfter = duel.host.drainServerMessages()
            val guestAfter = duel.guest.drainServerMessages()

            assertTrue(hostAfter.any { it is ServerMessage.Snapshot })
            assertTrue(guestAfter.any { it is ServerMessage.Snapshot })
        }
    }

    @Test
    fun anActFromTheSeatNotOnTurnIsAnsweredToThatSocketAlone() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val duel = client.openADuel(deps)
            val opening = duel.host.drainServerMessages()
            duel.guest.drainServerMessages()
            val yourTurn = opening.filterIsInstance<ServerMessage.YourTurn>().single()

            // the guest is not on turn — the host is — no matter what seat the frame names.
            duel.guest.sendAct(yourTurn.handNumber, yourTurn.actionSequence, PlayerAction.Fold(duel.guestSeat))

            val reply = duel.guest.nextServerMessage() as ServerMessage.Rejected
            assertTrue(reply.rejection is Rejection.NotYourTurn)

            assertNull(withTimeoutOrNull(200.milliseconds) { duel.host.incoming.receive() })
        }
    }

    @Test
    fun anActFromASocketInNoRoomIsStillRefused() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            // a live duel elsewhere on the same server must not matter to this refusal.
            client.openADuel(deps)

            val solo = client.webSocketSession("/ws")
            solo.completeHandshake("third")

            val act = Act(handNumber = 1, actionSequence = 0, action = PlayerAction.Fold(seat = 0))
            solo.send(Frame.Text(ProtocolCodec.encode(act)))
            val reply = solo.nextServerMessage() as ServerMessage.Failure

            assertEquals(ProtocolError.NOT_IN_DUEL, reply.error)
        }
    }

    @Test
    fun theSeatOnTheFrameIsIgnored() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val duel = client.openADuel(deps)
            val opening = duel.host.drainServerMessages()
            duel.guest.drainServerMessages()
            val yourTurn = opening.filterIsInstance<ServerMessage.YourTurn>().single()

            // the host really is on turn, but the frame claims to move the guest's seat.
            duel.host.sendAct(yourTurn.handNumber, yourTurn.actionSequence, PlayerAction.Fold(duel.guestSeat))

            val reply = duel.host.nextServerMessage() as ServerMessage.Rejected
            assertTrue(reply.rejection is Rejection.NotYourTurn)

            assertNull(withTimeoutOrNull(200.milliseconds) { duel.guest.incoming.receive() })
        }
    }

    @Test
    fun bothSocketsAreToldWhenTheDuelEnds() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
            val duel = client.openADuel(deps, format)
            val opening = duel.host.drainServerMessages()
            duel.guest.drainServerMessages()
            val yourTurn = opening.filterIsInstance<ServerMessage.YourTurn>().single()

            // folding hand 1's only decision ends that hand, and FixedHands(1) ends the duel there.
            duel.host.sendAct(yourTurn.handNumber, yourTurn.actionSequence, PlayerAction.Fold(duel.hostSeat))

            val hostAfter = duel.host.drainServerMessages()
            val guestAfter = duel.guest.drainServerMessages()

            val hostFinished = hostAfter.filterIsInstance<ServerMessage.DuelFinished>()
            val guestFinished = guestAfter.filterIsInstance<ServerMessage.DuelFinished>()

            assertEquals(1, hostFinished.size)
            assertEquals(1, guestFinished.size)
            assertEquals(hostFinished.single().outcome, guestFinished.single().outcome)
            assertEquals(duel.guestSeat, hostFinished.single().outcome.winner)
        }
    }

    @Test
    fun neitherSocketEverSawTheOthersHoleCards() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val duel = client.openADuel(deps)
            val hostMessages = mutableListOf<ServerMessage>()
            val guestMessages = mutableListOf<ServerMessage>()
            hostMessages += duel.host.drainServerMessages()
            guestMessages += duel.guest.drainServerMessages()

            // hand 1: the host (opening button) folds — the guest becomes button for hand 2.
            val firstTurn = hostMessages.filterIsInstance<ServerMessage.YourTurn>().single()
            duel.host.sendAct(firstTurn.handNumber, firstTurn.actionSequence, PlayerAction.Fold(duel.hostSeat))
            hostMessages += duel.host.drainServerMessages()
            guestMessages += duel.guest.drainServerMessages()

            // hand 2: the guest, now on the button, folds too.
            val secondTurn = guestMessages.filterIsInstance<ServerMessage.YourTurn>().single()
            duel.guest.sendAct(secondTurn.handNumber, secondTurn.actionSequence, PlayerAction.Fold(duel.guestSeat))
            hostMessages += duel.host.drainServerMessages()
            guestMessages += duel.guest.drainServerMessages()

            for ((seat, messages) in listOf(duel.hostSeat to hostMessages, duel.guestSeat to guestMessages)) {
                val opponent = 1 - seat
                val revealedByIndex = revealedAtEachSnapshot(messages)
                var sawOwnCards = false

                messages.forEachIndexed { index, message ->
                    when (message) {
                        is ServerMessage.Snapshot -> {
                            if (message.view.viewer.holeCards.isNotEmpty()) sawOwnCards = true
                            if (opponent !in revealedByIndex.getValue(index)) {
                                assertTrue(
                                    message.view.opponent.holeCards.isEmpty(),
                                    "seat $seat's snapshot at frame $index shows opponent " +
                                        "(seat $opponent) holeCards before any reveal",
                                )
                            }
                        }

                        is ServerMessage.Events -> {
                            val leaked = message.events.filterIsInstance<HoleCardsDealt>().filter { it.seat == opponent }
                            assertTrue(
                                leaked.isEmpty(),
                                "seat $seat's Events frame at frame $index carries HoleCardsDealt " +
                                    "for opponent (seat $opponent): $leaked",
                            )
                        }

                        else -> Unit
                    }
                }

                assertTrue(sawOwnCards, "seat $seat never saw its own hole cards — fixture proves nothing")
            }
        }
    }
}
