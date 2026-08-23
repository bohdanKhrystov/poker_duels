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
import duels.poker.server.protocol.ProtocolError
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import duels.poker.server.room.RandomRoomCodeSource
import duels.poker.server.room.RoomCode
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
import java.lang.reflect.Field
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** A fixed seed, so every hand this file deals is reproducible run to run. */
private val fixedSeeds = HandSeedSource { 7L }

/** A [RoomRegistry] whose every hand draws from [seeds] — the room code itself is still random. */
private fun testRoomRegistry(seeds: HandSeedSource = fixedSeeds): RoomRegistry =
    RoomRegistry(RandomRoomCodeSource(), SystemClock, seeds = seeds)

/** Removes a room from the registry using reflection to access its internal state. */
private fun RoomRegistry.removeRoom(code: RoomCode) {
    val roomsField: Field = RoomRegistry::class.java.getDeclaredField("rooms")
    roomsField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val roomsMap = roomsField.get(this) as? java.util.concurrent.ConcurrentHashMap<RoomCode, *>
    roomsMap?.remove(code)
}

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
 * Two sockets seated on a fresh `FixedHands(1)` room, drained of their opening frames, with the
 * host's only decision point captured but not yet answered — the duel is still running.
 */
private data class RunningDuel(
    val host: DefaultClientWebSocketSession,
    val guest: DefaultClientWebSocketSession,
    val yourTurn: ServerMessage.YourTurn,
)

/**
 * Pre-creates a room directly on [deps]'s [RoomRegistry] — the only way a test picks a one-hand
 * format, since [duels.poker.server.protocol.CreateRoom] always opens [DuelFormat.DEFAULT] — and
 * hands a host and a guest device onto it over the wire through [JoinRoom], exactly as real clients
 * would. Both sockets are drained of their opening frames before returning, so a later read on
 * either sees only what happens after this call; the host's [ServerMessage.YourTurn] is handed back
 * rather than answered, which is what leaves the duel still running for a caller that needs exactly
 * that — [finishedDuel] is the caller that instead answers it right away.
 */
private suspend fun HttpClient.runningDuel(deps: SocketDependencies): RunningDuel {
    val format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
    val host = deps.directory.resolve(DeviceId("host"))
    val room = deps.rooms.create(host.id, format)

    val (hostSession, _) = enterRoom("host", room.code.value)
    val (guestSession, _) = enterRoom("guest", room.code.value)

    // the room always opens with the host on the button (Room.open), hence on turn first.
    val hostOpening = hostSession.drainServerMessages()
    guestSession.drainServerMessages()
    val yourTurn = hostOpening.filterIsInstance<ServerMessage.YourTurn>().single()

    return RunningDuel(hostSession, guestSession, yourTurn)
}

/**
 * Seats a duel via [runningDuel], then folds the host's only decision: hand 1's fold ends that
 * hand, and [EndCondition.FixedHands] `(1)` ends the duel there. Both sockets are drained again
 * before returning, so a later read on either sees only what happens after this call.
 */
private suspend fun HttpClient.finishedDuel(deps: SocketDependencies): FinishedDuel {
    val duel = runningDuel(deps)

    // folding hand 1's only decision ends that hand, and FixedHands(1) ends the duel there.
    duel.host.sendAct(duel.yourTurn.handNumber, duel.yourTurn.actionSequence, PlayerAction.Fold(0))
    duel.host.drainServerMessages()
    duel.guest.drainServerMessages()

    return FinishedDuel(duel.host, duel.guest)
}

/**
 * Like [finishedDuel], but keeps the host's opening [ServerMessage.Snapshot] from the first duel
 * instead of letting it fall away as ordinary setup noise. `TASK-021303`'s button-seat comparison
 * needs that real "before" frame, and [finishedDuel] is a fixture other tests depend on staying
 * exactly as it is, so it cannot be the one to hand it back.
 */
private suspend fun HttpClient.finishedDuelKeepingFirstOpeningSnapshot(
    deps: SocketDependencies,
): Pair<FinishedDuel, ServerMessage.Snapshot> {
    val format = DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(1))
    val host = deps.directory.resolve(DeviceId("host"))
    val room = deps.rooms.create(host.id, format)

    val (hostSession, _) = enterRoom("host", room.code.value)
    val (guestSession, _) = enterRoom("guest", room.code.value)

    // the room always opens with the host on the button (Room.open), hence on turn first.
    val hostOpening = hostSession.drainServerMessages()
    guestSession.drainServerMessages()
    val yourTurn = hostOpening.filterIsInstance<ServerMessage.YourTurn>().single()
    val openingSnapshot = hostOpening.filterIsInstance<ServerMessage.Snapshot>().single()

    // folding hand 1's only decision ends that hand, and FixedHands(1) ends the duel there.
    hostSession.sendAct(yourTurn.handNumber, yourTurn.actionSequence, PlayerAction.Fold(0))
    hostSession.drainServerMessages()
    guestSession.drainServerMessages()

    return FinishedDuel(hostSession, guestSession) to openingSnapshot
}

/**
 * Like [finishedDuel], but keeps the room's own [RoomCode] instead of letting it fall away.
 * `TASK-021305`'s reap needs that exact code to remove the exact room from the registry, and
 * [finishedDuel] is a fixture other tests depend on staying exactly as it is, so it cannot be the
 * one to hand it back.
 */
private suspend fun HttpClient.finishedDuelKeepingRoomCode(deps: SocketDependencies): Pair<FinishedDuel, RoomCode> {
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

    return FinishedDuel(hostSession, guestSession) to room.code
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

    /**
     * `TASK-021303`: `ADR-0044` §4 over the wire. The second seat's [OfferRematch] — the one that
     * completes agreement — starts a fresh duel on both sockets outright: no [ServerMessage.RematchOffered]
     * of its own, just the new duel's opening frames.
     */
    @Test
    fun theSecondOfferStartsAFreshDuelOnBothSockets() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val duel = client.finishedDuel(deps)

            duel.host.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            duel.host.drainServerMessages()
            duel.guest.drainServerMessages()

            duel.guest.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            val hostAfter = duel.host.drainServerMessages()
            val guestAfter = duel.guest.drainServerMessages()

            assertTrue(hostAfter.any { it is ServerMessage.Snapshot }, "host saw no Snapshot: $hostAfter")
            assertTrue(guestAfter.any { it is ServerMessage.Snapshot }, "guest saw no Snapshot: $guestAfter")

            // exactly one seat is on turn first in the fresh duel, so exactly one socket sees YourTurn —
            // asserted as a count of sockets, not of frames, so two YourTurn frames landing on the same
            // socket could not masquerade as "both seats got a turn".
            val socketsWithYourTurn =
                listOf(hostAfter, guestAfter).count { messages -> messages.any { it is ServerMessage.YourTurn } }
            assertEquals(1, socketsWithYourTurn)

            // checked against the frames just asserted non-empty above, not against silence: agreement
            // replaces RematchOffered with the duel's own frames, it does not simply send nothing.
            val hostOffers = hostAfter.filterIsInstance<ServerMessage.RematchOffered>()
            val guestOffers = guestAfter.filterIsInstance<ServerMessage.RematchOffered>()
            assertTrue(hostOffers.isEmpty(), "host saw a RematchOffered on agreement: $hostAfter")
            assertTrue(guestOffers.isEmpty(), "guest saw a RematchOffered on agreement: $guestAfter")
        }
    }

    /**
     * `TASK-021303`: `ADR-0044` §4's button claim. A room `RoomRegistry.create` opens starts at
     * `openingButtonSeat == 0`, and `Room.offerRematch` flips the fresh duel's button to
     * `1 - openingButtonSeat` — so the host's own socket should read `buttonSeat == 0` on the first
     * duel's opening [ServerMessage.Snapshot] and `buttonSeat == 1` on the rematch's.
     */
    @Test
    fun theRematchPutsTheButtonOnTheOtherSeat() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val (duel, firstOpeningSnapshot) = client.finishedDuelKeepingFirstOpeningSnapshot(deps)

            duel.host.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            duel.host.drainServerMessages()
            duel.guest.drainServerMessages()

            duel.guest.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            val hostAfter = duel.host.drainServerMessages()
            duel.guest.drainServerMessages()

            val rematchOpeningSnapshot = hostAfter.filterIsInstance<ServerMessage.Snapshot>().single()

            // both numbers, not only the second: a buttonSeat that was 1 all along would satisfy half
            // of this and prove nothing about the flip.
            assertEquals(0, firstOpeningSnapshot.view.buttonSeat)
            assertEquals(1, rematchOpeningSnapshot.view.buttonSeat)
        }
    }

    /**
     * `TASK-021304`: `ADR-0044` §3 over the wire. A second [OfferRematch] from a seat that already
     * offered is answered exactly like the first — one [ServerMessage.RematchOffered] naming that
     * seat, to that socket alone — never a [ServerMessage.Failure]: a double click cannot produce
     * an error state.
     */
    @Test
    fun aRepeatOfferIsAnsweredWithRematchOfferedAndNotAFailure() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val duel = client.finishedDuel(deps)

            duel.host.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            duel.host.drainServerMessages()
            duel.guest.drainServerMessages()

            duel.host.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            val hostAfter = duel.host.drainServerMessages()
            val guestAfter = duel.guest.drainServerMessages()

            val hostOffers = hostAfter.filterIsInstance<ServerMessage.RematchOffered>()
            assertEquals(1, hostOffers.size)
            assertEquals(0, hostOffers.single().seat)

            val hostFailures = hostAfter.filterIsInstance<ServerMessage.Failure>()
            assertTrue(hostFailures.isEmpty(), "host saw a Failure on a repeat offer: $hostAfter")
            assertTrue(guestAfter.isEmpty(), "guest saw something on the host's repeat offer: $guestAfter")
            assertTrue(noDuelStarted(hostAfter), "host saw a message only a started duel would send: $hostAfter")
        }
    }

    /**
     * `TASK-021304`: the flip side of the same rule. Had the repeat been recorded as a second,
     * distinct offer, the room would have agreed on it and the fresh duel would have started right
     * there, on the repeat — before the guest ever acted. Draining and asserting after every send,
     * not only once at the end, is what tells "the repeat started nothing" apart from "the repeat
     * started the duel, and the guest's own offer merely rode along with its opening frames".
     */
    @Test
    fun aRepeatOfferRecordsNothingSoTheOpponentsOfferIsStillTheOneThatStarts() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val duel = client.finishedDuel(deps)

            duel.host.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            duel.host.drainServerMessages()
            val guestAfterFirstHostOffer = duel.guest.drainServerMessages()
            assertTrue(
                noDuelStarted(guestAfterFirstHostOffer),
                "guest saw a message only a started duel would send after the host's first offer: $guestAfterFirstHostOffer",
            )

            // the repeat is the case that matters: a bug that records it as a second, distinct
            // offer would agree the room and start the fresh duel right here, before the guest
            // has sent anything at all.
            duel.host.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            duel.host.drainServerMessages()
            val guestAfterRepeatHostOffer = duel.guest.drainServerMessages()
            assertTrue(
                noDuelStarted(guestAfterRepeatHostOffer),
                "guest saw a message only a started duel would send after the host's repeat offer: $guestAfterRepeatHostOffer",
            )

            duel.guest.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            val guestAfterOwnOffer = duel.guest.drainServerMessages()
            assertTrue(
                guestAfterOwnOffer.any { it is ServerMessage.Snapshot },
                "guest's own offer should be the one that starts the fresh duel: $guestAfterOwnOffer",
            )
        }
    }

    /**
     * `TASK-021305`: `ADR-0044` §6's first bullet over the wire. Three different ways of holding
     * no seat in any room — never having entered one, having been refused entry to one that was
     * already full, and having been seated in one the registry no longer holds — all answer
     * [OfferRematch] with exactly one [ServerMessage.Failure]`(`[ProtocolError.UNKNOWN_ROOM]`)`,
     * and the three answers are indistinguishable from one another.
     *
     * The first two reach that refusal through the very same branch, and that is the point rather
     * than a weakness: `RoomMembership.code` is set only on a path that actually seats the player,
     * so a stranger who only knows a full room's code and a socket that never sent [duels.poker.server.protocol.JoinRoom]
     * at all are, as far as `replyToOfferRematch` can tell, the same state by construction. For that
     * same reason `RematchRefusal.NOT_A_PLAYER` is unreachable through the socket: its branch in
     * `replyToOfferRematch` exists only because `RoomRegistry` can return it and the `when` must
     * stay exhaustive, and this test does not attempt to reach it.
     */
    @Test
    fun theThreeWaysToHoldNoSeatAnswerOneIndistinguishableUnknownRoom() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            // 1. Entered no room at all: handshake only.
            val neverEntered = client.webSocketSession("/ws")
            neverEntered.completeHandshake("neverentered")

            // 2. Holds no seat: refused entry to a room that was already full when it tried —
            // ROOM_FULL seats nobody, so this socket never acquires a RoomMembership.code either.
            val fullRoomOwner = deps.directory.resolve(DeviceId("fullhost"))
            val fullRoom = deps.rooms.create(fullRoomOwner.id, DuelFormat.DEFAULT)
            client.enterRoom("fullhost", fullRoom.code.value)
            client.enterRoom("fullguest", fullRoom.code.value)

            val refusedEntry = client.webSocketSession("/ws")
            refusedEntry.completeHandshake("outsider")
            refusedEntry.send(Frame.Text(ProtocolCodec.encode(JoinRoom(fullRoom.code.value))))
            val joinReply = refusedEntry.nextServerMessage() as ServerMessage.Failure
            assertEquals(ProtocolError.ROOM_FULL, joinReply.error)

            // 3. Held a seat, but the room was reaped out from under it once the duel finished.
            val (reaped, reapedCode) = client.finishedDuelKeepingRoomCode(deps)
            deps.rooms.removeRoom(reapedCode)

            neverEntered.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            val neverEnteredAfter = neverEntered.drainServerMessages()

            refusedEntry.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            val refusedEntryAfter = refusedEntry.drainServerMessages()

            reaped.host.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            val reapedAfter = reaped.host.drainServerMessages()

            // the frame count is part of the claim: an extra frame on one of the three would itself
            // be a way to tell it apart from the other two, even if every frame it sent were correct.
            assertEquals(1, neverEnteredAfter.size, "entered no room: $neverEnteredAfter")
            assertEquals(1, refusedEntryAfter.size, "holds no seat: $refusedEntryAfter")
            assertEquals(1, reapedAfter.size, "room reaped: $reapedAfter")

            val expected = ServerMessage.Failure(ProtocolError.UNKNOWN_ROOM)
            val neverEnteredReply = neverEnteredAfter.single()
            val refusedEntryReply = refusedEntryAfter.single()
            val reapedReply = reapedAfter.single()

            assertEquals(expected, neverEnteredReply)
            assertEquals(expected, refusedEntryReply)
            assertEquals(expected, reapedReply)

            // the point of the ticket: not only is each of the three Failure(UNKNOWN_ROOM), none of
            // them differs from either other — nothing on the wire tells the three apart.
            assertEquals(neverEnteredReply, refusedEntryReply)
            assertEquals(refusedEntryReply, reapedReply)
        }
    }

    /**
     * `TASK-021306`: `ADR-0044` §6's second bullet over the wire. An [OfferRematch] sent while the
     * duel is still running — the host has a decision pending, so hand 1 has not yet ended — is
     * refused with exactly one [ServerMessage.Failure]`(`[ProtocolError.REMATCH_UNAVAILABLE]`)` on
     * the offering socket, and the other socket sees nothing at all: unlike an accepted offer, which
     * [oneOfferPutsOneRematchOfferedNamingThatSeatOnBothSockets] shows reaching both seats, a refusal
     * stays private to whoever sent it.
     */
    @Test
    fun anOfferWhileTheDuelIsRunningIsRefusedAsRematchUnavailable() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val duel = client.runningDuel(deps)

            duel.host.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))

            val hostAfter = duel.host.drainServerMessages()
            val guestAfter = duel.guest.drainServerMessages()

            // the count rules out extra frames riding along with the refusal, and the equality rules
            // out both a wrong error code and the offer silently succeeding mid-duel.
            assertEquals(1, hostAfter.size, "host: $hostAfter")
            assertEquals(ServerMessage.Failure(ProtocolError.REMATCH_UNAVAILABLE), hostAfter.single())

            assertTrue(guestAfter.isEmpty(), "guest saw something while the duel was still running: $guestAfter")
        }
    }

    /**
     * `TASK-021306`: the other half of `ADR-0044` §6's second bullet — the refusal is transient, not
     * permanent. The very socket [anOfferWhileTheDuelIsRunningIsRefusedAsRematchUnavailable] shows
     * being refused sends the identical [OfferRematch] again after its own fold has ended that same
     * duel, and this time it succeeds exactly as `TASK-021302` describes: one
     * [ServerMessage.RematchOffered]`(seat = 0)` on both sockets, no [ServerMessage.Failure] on
     * either. Nothing else changes between the two attempts — same socket, same seat, same room —
     * only that the duel has since finished, which is what makes the difference in outcome a claim
     * about time rather than about which client asked.
     */
    @Test
    fun theSameOfferIsAcceptedOnceTheDuelHasFinished() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val duel = client.runningDuel(deps)

            // refused once, while the duel is still running.
            duel.host.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            val refusal = duel.host.drainServerMessages()
            duel.guest.drainServerMessages()
            assertEquals(ServerMessage.Failure(ProtocolError.REMATCH_UNAVAILABLE), refusal.single())

            // folding hand 1's only decision ends that hand, and FixedHands(1) ends the duel there —
            // the only thing this changes about the duel is that it is now finished.
            duel.host.sendAct(duel.yourTurn.handNumber, duel.yourTurn.actionSequence, PlayerAction.Fold(0))
            duel.host.drainServerMessages()
            duel.guest.drainServerMessages()

            // the same offer, resent on the same socket that was just refused.
            duel.host.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))
            val hostAfter = duel.host.drainServerMessages()
            val guestAfter = duel.guest.drainServerMessages()

            val hostOffers = hostAfter.filterIsInstance<ServerMessage.RematchOffered>()
            val guestOffers = guestAfter.filterIsInstance<ServerMessage.RematchOffered>()

            // asserted per socket, not on the pooled total, and against both counts, not only the
            // seat number: a rejection that still slipped a lone frame past the size check on one
            // socket, or that misnamed the seat, would each fail a different one of these four.
            assertEquals(1, hostOffers.size)
            assertEquals(1, guestOffers.size)
            assertEquals(0, hostOffers.single().seat)
            assertEquals(0, guestOffers.single().seat)

            // had the refusal been permanent rather than transient, this resend would answer with
            // another Failure(REMATCH_UNAVAILABLE) instead — caught here, and independently by the
            // two RematchOffered counts above coming back empty rather than one.
            assertTrue(hostAfter.none { it is ServerMessage.Failure }, "host saw a Failure once the duel had finished: $hostAfter")
            assertTrue(guestAfter.none { it is ServerMessage.Failure }, "guest saw a Failure once the duel had finished: $guestAfter")
        }
    }

    /**
     * `TASK-021308`: `ADR-0044` §2's first half again, with the offering seat flipped. Every other
     * test in this class drives the *host* into the offer, so their shared `seat == 0` expectation
     * cannot tell a seat that tracks the offering socket apart from a hard-coded `0` or a seat
     * resolved to the host's by construction. Here the **guest** sends the only [OfferRematch], and
     * both sockets' [ServerMessage.RematchOffered] must read `seat == 1` — the guest's own seat —
     * for the field to be doing anything other than one of those two wrong things.
     */
    @Test
    fun theGuestsOfferNamesSeatOneOnBothSockets() = testApplication {
        val deps = testDeps(rooms = testRoomRegistry())
        application {
            module()
            duelSocket(deps)
        }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val duel = client.finishedDuel(deps)

            duel.guest.send(Frame.Text(ProtocolCodec.encode(OfferRematch)))

            val hostAfter = duel.host.drainServerMessages()
            val guestAfter = duel.guest.drainServerMessages()

            val hostOffers = hostAfter.filterIsInstance<ServerMessage.RematchOffered>()
            val guestOffers = guestAfter.filterIsInstance<ServerMessage.RematchOffered>()

            // asserted per socket, not on the pooled total — a frame delivered twice to one
            // socket and never to the other would otherwise still sum to two.
            assertEquals(1, hostOffers.size)
            assertEquals(1, guestOffers.size)
            assertEquals(1, hostOffers.single().seat)
            assertEquals(1, guestOffers.single().seat)

            assertTrue(noDuelStarted(hostAfter), "host saw a message only a started duel would send: $hostAfter")
            assertTrue(noDuelStarted(guestAfter), "guest saw a message only a started duel would send: $guestAfter")
        }
    }
}
