package duels.poker.server.e2e

import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.game.ActionType
import duels.poker.engine.game.PlayerAction
import duels.poker.engine.random.Rng
import duels.poker.engine.random.SplitMix64Rng
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.CreateRoom
import duels.poker.server.protocol.JoinRoom
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import kotlin.time.Duration.Companion.seconds

internal const val HOST_DEVICE: String = "e2e-host"
internal const val GUEST_DEVICE: String = "e2e-guest"

/**
 * Seeds the single action policy [playToFinish] threads through the whole duel by default.
 *
 * `0x0B07_000000000001L` reaches `DuelFinished` in 21 `Act` frames against [HAND_SEED] and the
 * shipped default duel format — comfortably under [playToFinish]'s default `maxActions` of 20,000.
 */
internal const val POLICY_SEED: Long = 0x0B07_000000000001L

/**
 * A WebSocket client connected to the server.
 *
 * @property deviceId The device ID this client connected with.
 * @property seat The seat the server assigned to this client (0 or 1).
 * @property session The WebSocket session. This is a `var` so that `TASK-021211` can replace one
 *   client's socket without rebuilding the duel.
 */
internal class SocketClient(val deviceId: String, val seat: Int, var session: DefaultClientWebSocketSession) {
    /** All [ServerMessage]s received by this client after its [ServerMessage.Welcome]. */
    val received: MutableList<ServerMessage> = mutableListOf()
}

/**
 * A duel between two WebSocket clients seated in one room.
 *
 * @property code The room code assigned by the server.
 * @property handSeed The seed passed to [openSocketDuel]. This is recorded but not used here; the
 *   server was already given it by [installDuelServer]. A caller passing different seeds to different
 *   clients is lying to its own failure messages.
 * @property clients The two clients, in order of connection (host first, guest second).
 */
internal class SocketDuel(val code: String, val handSeed: Long, val clients: List<SocketClient>) {
    /** Return the client seated at the given seat (0 or 1). */
    fun seat(index: Int): SocketClient = clients.single { it.seat == index }
}

/**
 * Open two WebSocket connections, seat them in a room, and return the [SocketDuel].
 *
 * Opens `/ws` for `HOST_DEVICE`, completes its handshake, sends `CreateRoom`, and reads the
 * `RoomJoined` that answers it; then opens a second `/ws` for `GUEST_DEVICE`, completes
 * its handshake, sends `JoinRoom(code)`, and reads its `RoomJoined`. The seat of each client is
 * the one the server named in that frame — never assumed from the order they connected.
 *
 * Both handshakes always carry their own `HOST_DEVICE`/`GUEST_DEVICE`, token or not — a real client
 * keeps sending its device id whether or not it holds one (`ADR-0030` §8) — so [SocketClient]'s
 * `deviceId` stays a plain, non-null `String` here: a token, when given, rides beside the device id
 * rather than replacing it.
 *
 * `received` on each client records every `ServerMessage` a client got after its `Welcome`,
 * starting with its own `RoomJoined`, in arrival order. Nothing else is read off either socket here;
 * the opening hand's frames are already queued on the guest's join and stay queued for the next task.
 *
 * @param handSeed The hand seed to use. Defaults to [HAND_SEED].
 * @param hostToken A session token sent alongside `HOST_DEVICE` on the host's handshake. Defaults to
 *   `null`, so a caller that omits it sends exactly the `Hello` this function always sent.
 * @param guestToken The same, for the guest's handshake.
 * @return A [SocketDuel] with both clients seated.
 */
internal suspend fun HttpClient.openSocketDuel(
    handSeed: Long = HAND_SEED,
    hostToken: String? = null,
    guestToken: String? = null,
): SocketDuel {
    // Open the host connection
    val hostSession = webSocketSession("/ws")
    hostSession.completeHandshake(HOST_DEVICE, hostToken)

    // Create the room
    hostSession.send(Frame.Text(ProtocolCodec.encode(CreateRoom)))
    val hostMessage = hostSession.nextServerMessage()
    val hostRoomJoined = hostMessage as? ServerMessage.RoomJoined
        ?: error("Expected RoomJoined from host, got ${hostMessage::class.simpleName}")

    val roomCode = hostRoomJoined.code
    val hostSeat = hostRoomJoined.seat

    // Create the host client
    val hostClient = SocketClient(HOST_DEVICE, hostSeat, hostSession)
    hostClient.received.add(hostRoomJoined)

    // Open the guest connection
    val guestSession = webSocketSession("/ws")
    guestSession.completeHandshake(GUEST_DEVICE, guestToken)

    // Join the room
    guestSession.send(Frame.Text(ProtocolCodec.encode(JoinRoom(roomCode))))
    val guestMessage = guestSession.nextServerMessage()
    val guestRoomJoined = guestMessage as? ServerMessage.RoomJoined
        ?: error("Expected RoomJoined from guest, got ${guestMessage::class.simpleName}")

    val guestSeat = guestRoomJoined.seat

    // Create the guest client
    val guestClient = SocketClient(GUEST_DEVICE, guestSeat, guestSession)
    guestClient.received.add(guestRoomJoined)

    return SocketDuel(roomCode, handSeed, listOf(hostClient, guestClient))
}

/**
 * Drops [seat]'s socket with a genuine WebSocket close and rejoins on a fresh one: opens a new
 * `/ws`, completes the handshake with the same device id, and sends the same [JoinRoom] a first
 * join would (`ADR-0018` — a device holds at most one live session, and the newest one wins).
 *
 * The [ServerMessage.RoomJoined] that answers is itself the proof the server-side resume already
 * finished — `DuelSocket.replyToJoinRoom`'s resume branch sends it only after
 * `RoomRegistry.resume` has already returned — so nothing further needs to be awaited here. It is
 * checked to still name [seat], appended to that client's [SocketClient.received], and
 * [SocketClient.session] is swapped to the new connection, so every later read on this duel
 * (`nextFrame`, [playToFinish]) follows the swap without needing to know it happened.
 *
 * Whatever else the resume queued — a [ServerMessage.Snapshot] and, if [seat] is the one holding
 * the duel up, a [ServerMessage.YourTurn] (`TASK-020810`) — is left on the new socket for
 * whichever caller reads next, exactly as [openSocketDuel] leaves the opening hand's frames
 * queued for [playToFinish].
 *
 * @param http The client the fresh `/ws` connection is opened on.
 * @param seat The seat (0 or 1) to drop and reconnect.
 * @throws IllegalStateException if the resumed [ServerMessage.RoomJoined] names a seat other than
 *   [seat].
 */
internal suspend fun SocketDuel.reconnect(http: HttpClient, seat: Int) {
    val client = this.seat(seat)
    client.session.close()

    val session = http.webSocketSession("/ws")
    session.completeHandshake(client.deviceId)
    session.send(Frame.Text(ProtocolCodec.encode(JoinRoom(code))))

    val message = session.nextServerMessage()
    val roomJoined = message as? ServerMessage.RoomJoined
        ?: error("Expected RoomJoined on reconnect, got ${message::class.simpleName}")
    check(roomJoined.seat == seat) {
        "handSeed=$handSeed: reconnecting seat $seat came back seated at ${roomJoined.seat}"
    }

    client.received += roomJoined
    client.session = session
}

/** A [ServerMessage] read off one of a duel's two sockets, paired with the client it came from. */
private data class ReceivedFrame(val client: SocketClient, val message: ServerMessage)

/** Decodes this text frame as the [ServerMessage] it carries. */
private fun Frame.decodeServerMessage(): ServerMessage {
    val text = (this as Frame.Text).readText()
    return protocolJson.decodeFromString(text)
}

/**
 * Waits for the next frame from whichever of [clients]' sockets produces one first, decodes it,
 * appends it to that client's [SocketClient.received], and returns both.
 *
 * Backed by [select] racing every socket's `incoming` channel, so the call suspends until the
 * server actually sends something: no polling delay, no fixed drain window.
 */
private suspend fun nextFrame(clients: List<SocketClient>): ReceivedFrame {
    val next =
        select<ReceivedFrame> {
            for (client in clients) {
                client.session.incoming.onReceive { frame -> ReceivedFrame(client, frame.decodeServerMessage()) }
            }
        }
    next.client.received += next.message
    return next
}

/**
 * Plays this duel to completion, seeing only what a client would see: it answers every
 * `ServerMessage.YourTurn` addressed to a client on that same client's socket, and stops once
 * both clients have received a `ServerMessage.DuelFinished`.
 *
 * The policy is uniform over `turn.legalActions`, mirroring `playDuel` (`TASK-020710`) line for
 * line: one draw over the sorted legal action types, then, for `BET`/`RAISE`, a second draw over
 * 2 choosing `minBetTo`/`minRaiseTo` on 0 and `allInTo` on 1. `poker-ai`'s `RandomBot` plays the
 * same policy but cannot be called from here — its `choose` takes a `GameState`, which is exactly
 * what a client must not hold — so this mirrors it rather than importing it, as `TASK-020710`
 * already decided for the runner-level harness.
 *
 * A single [SplitMix64Rng] seeded with [policySeed] is threaded through the whole duel, and is
 * drawn from only at the moment an `Act` is built and sent — never when the `YourTurn` that
 * prompted it merely arrives — so that a decision point delivered twice still consumes exactly
 * one draw.
 *
 * @param policySeed seeds the one action policy shared by both clients. Defaults to [POLICY_SEED].
 * @param maxActions the ceiling on `Act` frames sent, guarding against a duel that never ends.
 * @param beforeAct called with the client and the `YourTurn` it just received, before any draw is
 *   made or `Act` is sent. Returning `true` says the hook already disturbed this decision point —
 *   [reconnect], say — so the loop drops the prompt it holds without drawing or sending anything,
 *   and goes back to receiving; the `YourTurn` a resume then redelivers reaches this same branch
 *   again and is answered normally. Defaults to a hook that never intervenes, so a caller that
 *   omits it sees exactly the behaviour this function had before it gained the parameter. This is
 *   what keeps an interrupted duel reproducible: the dropped prompt costs no draw, and the
 *   redelivered one costs exactly the one draw it would have cost without the interruption — true
 *   only because the draw happens here, when the `Act` is built, never when the `YourTurn` that
 *   prompted it merely arrives.
 * @return the outcome both clients' `DuelFinished` frames agreed on.
 * @throws AssertionError naming [SocketDuel.handSeed], [policySeed], a seat and a frame, if either
 *   client is ever sent a `Rejected` or a `Failure`, if [maxActions] is exceeded, or if the two
 *   clients' `DuelFinished` outcomes disagree.
 */
internal suspend fun SocketDuel.playToFinish(
    policySeed: Long = POLICY_SEED,
    maxActions: Int = 20_000,
    beforeAct: suspend (SocketClient, ServerMessage.YourTurn) -> Boolean = { _, _ -> false },
): DuelOutcome {
    fun reproducibleFailure(client: SocketClient, frame: ServerMessage, detail: String): AssertionError =
        AssertionError("handSeed=$handSeed policySeed=$policySeed seat=${client.seat}: $detail, frame=$frame")

    return withTimeout(60.seconds) {
        var policyRng: Rng = SplitMix64Rng(policySeed)
        var actions = 0
        val outcomes = mutableMapOf<SocketClient, DuelOutcome>()

        while (outcomes.size < clients.size) {
            val (client, message) = nextFrame(clients)
            when (message) {
                is ServerMessage.YourTurn -> if (!beforeAct(client, message)) {
                    if (actions >= maxActions) {
                        throw reproducibleFailure(client, message, "exceeded maxActions ($maxActions)")
                    }

                    val turn = message
                    val legal = turn.legalActions
                    val types = legal.allowed.sorted()
                    val typeDraw = policyRng.nextInt(types.size)
                    policyRng = typeDraw.next
                    val type = types[typeDraw.value]

                    val action: PlayerAction =
                        when (type) {
                            ActionType.FOLD -> PlayerAction.Fold(legal.seat)
                            ActionType.CHECK -> PlayerAction.Check(legal.seat)
                            ActionType.CALL -> PlayerAction.Call(legal.seat)
                            ActionType.ALL_IN -> PlayerAction.AllIn(legal.seat)
                            ActionType.BET -> {
                                val amountDraw = policyRng.nextInt(2)
                                policyRng = amountDraw.next
                                val to = if (amountDraw.value == 0) legal.minBetTo else legal.allInTo
                                PlayerAction.Bet(legal.seat, to)
                            }
                            ActionType.RAISE -> {
                                val amountDraw = policyRng.nextInt(2)
                                policyRng = amountDraw.next
                                val to = if (amountDraw.value == 0) legal.minRaiseTo else legal.allInTo
                                PlayerAction.Raise(legal.seat, to)
                            }
                        }

                    client.session.send(
                        Frame.Text(ProtocolCodec.encode(Act(turn.handNumber, turn.actionSequence, action))),
                    )
                    actions++
                }

                is ServerMessage.Rejected -> throw reproducibleFailure(client, message, "action rejected")
                is ServerMessage.Failure -> throw reproducibleFailure(client, message, "connection refused")
                is ServerMessage.DuelFinished -> outcomes[client] = message.outcome
                is ServerMessage.Welcome,
                is ServerMessage.Snapshot,
                is ServerMessage.RoomJoined,
                is ServerMessage.RematchOffered,
                is ServerMessage.Events,
                is ServerMessage.OpponentPresence,
                is ServerMessage.ActedForAbsent,
                -> Unit
            }
        }

        val outcomeValues = outcomes.values.toSet()
        check(outcomeValues.size == 1) {
            "handSeed=$handSeed policySeed=$policySeed: clients disagree on the outcome: $outcomeValues"
        }
        outcomeValues.single()
    }
}
