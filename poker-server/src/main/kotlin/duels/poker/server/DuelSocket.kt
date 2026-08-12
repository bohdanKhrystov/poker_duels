package duels.poker.server

import duels.poker.server.protocol.Act
import duels.poker.server.protocol.Decoded
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ProtocolError
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.handshake
import duels.poker.server.session.ConnectionWriter
import duels.poker.server.session.DeviceId
import duels.poker.server.session.Session
import duels.poker.server.session.SessionRegistry
import duels.poker.server.session.SocketDependencies
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

/**
 * Close reason sent when a connection's first frame is not a valid [Hello] — including a frame
 * that is not text, one that fails to decode, or one that decodes to some other [ClientMessage].
 */
public const val HANDSHAKE_REQUIRED: String = "handshake required"

/** Close reason sent when a [Hello] names a protocol version this server does not speak. */
public const val PROTOCOL_VERSION_MISMATCH: String = "protocol version mismatch"

/**
 * Installs the `/ws` route behind a mandatory handshake.
 *
 * A connection that has not first exchanged a valid [Hello] never reaches any further logic —
 * see [readHello]. Every outbound frame for the connection passes through a single
 * [ConnectionWriter], fed by exactly one writing coroutine ([pump]), so no two coroutines ever
 * write to the socket concurrently.
 *
 * `duelSocket` is installed by the caller rather than from `Application.module()`, because no
 * shipping [duels.poker.server.session.PlayerDirectory] exists until `STORY-0210`; until then the
 * only caller is a test.
 *
 * @param deps The collaborators this socket needs.
 */
public fun Application.duelSocket(deps: SocketDependencies) {
    routing {
        webSocket("/ws") {
            val writer = ConnectionWriter()
            val pump = launch { writer.writeAll { frame -> outgoing.send(Frame.Text(frame)) } }
            try {
                serve(deps, writer, pump)
            } finally {
                // Non-suspending by contract (see ConnectionWriter.close): a `finally` that could
                // suspend is a close path that might not run under cancellation.
                writer.close()
            }
        }
    }
}

/**
 * Gates the connection behind [readHello], then performs the handshake itself.
 *
 * A [ServerMessage.Welcome] is sent and the connection waits for further frames — this ticket's
 * socket expects nothing after the handshake; `TASK-020509` gives that wait its behaviour. A
 * [ServerMessage.Failure] is sent and the socket is closed with [PROTOCOL_VERSION_MISMATCH],
 * since [handshake] returns a failure only for a protocol version mismatch.
 */
private suspend fun DefaultWebSocketServerSession.serve(
    deps: SocketDependencies,
    writer: ConnectionWriter,
    pump: Job,
) {
    val hello = readHello(writer, pump, deps.maxFrameLength, deps.maxFrameNestingDepth) ?: return
    val deviceId = hello.deviceId?.let(::DeviceId) ?: deps.deviceIds.newDeviceId()
    when (val message = handshake(hello, deviceId.value)) {
        is ServerMessage.Welcome -> {
            val player = deps.directory.resolve(deviceId)
            val session = Session(SessionRegistry.newSessionId(), player)
            deps.sessions.register(session)
            try {
                writer.send(ProtocolCodec.encode(message))
                incoming.consumeEach { frame ->
                    writer.replyTo(frame, deps.maxFrameLength, deps.maxFrameNestingDepth)
                }
            } finally {
                deps.sessions.remove(session.id)
            }
        }

        is ServerMessage.Failure -> {
            writer.send(ProtocolCodec.encode(message))
            writer.close()
            pump.join()
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, PROTOCOL_VERSION_MISMATCH))
        }

        is ServerMessage.Snapshot,
        is ServerMessage.Events,
        is ServerMessage.YourTurn,
        is ServerMessage.Rejected,
        ->
            error("handshake() returned $message; it may only return Welcome or Failure")
    }
}

/**
 * Takes exactly one frame off the connection and demands it be a valid [Hello].
 *
 * Refuses — via [refuseHandshake] — a frame that is not [Frame.Text], one that
 * [ProtocolCodec.decodeClient] refuses, and one that decodes to a [duels.poker.server.protocol.ClientMessage]
 * other than [Hello]. A socket that has sent anything else has not shown it speaks this protocol
 * at all, so it never reaches duel logic.
 *
 * The frame is decoded with [maxFrameLength] and [maxFrameNestingDepth] rather than the codec's
 * own defaults — an unauthenticated client is exactly who the operator's configured limits exist
 * to protect against, so this pre-handshake frame gets no less scrutiny than any other.
 *
 * @return The client's [Hello], or `null` if the connection was refused and closed.
 */
private suspend fun DefaultWebSocketServerSession.readHello(
    writer: ConnectionWriter,
    pump: Job,
    maxFrameLength: Int,
    maxFrameNestingDepth: Int,
): Hello? {
    val frame = incoming.receiveCatching().getOrNull()
    if (frame !is Frame.Text) {
        // A non-text first frame has not shown it speaks this protocol at all, so it gets no
        // Failure body — only a client that at least sent JSON earns an explanation.
        return refuseHandshake(writer, pump, null)
    }
    return when (val decoded = ProtocolCodec.decodeClient(frame.readText(), maxFrameLength, maxFrameNestingDepth)) {
        is Decoded.Refused -> refuseHandshake(writer, pump, decoded.error)
        is Decoded.Message ->
            (decoded.message as? Hello) ?: refuseHandshake(writer, pump, ProtocolError.MALFORMED_MESSAGE)
    }
}

/**
 * Refuses the handshake and closes the connection with [HANDSHAKE_REQUIRED].
 *
 * Sends a [ServerMessage.Failure] first when [error] is non-null; a `null` error means the frame
 * did not even earn an explanation (see [readHello]). [pump] is joined after the writer is closed
 * so the single writing coroutine drains the buffered `Failure` before the socket closes —
 * skipping the join would let the close race the write, and the client could see the close frame
 * with no `Failure` ahead of it.
 *
 * @return Always `null`, so callers can write `readHello(...) ?: return`-style short circuits.
 */
private suspend fun DefaultWebSocketServerSession.refuseHandshake(
    writer: ConnectionWriter,
    pump: Job,
    error: ProtocolError?,
): Hello? {
    if (error != null) {
        writer.send(ProtocolCodec.encode(ServerMessage.Failure(error)))
    }
    writer.close()
    pump.join()
    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, HANDSHAKE_REQUIRED))
    return null
}

/**
 * Answers one post-handshake [frame] with a [ServerMessage.Failure] — this socket has nothing
 * else to say yet, since `STORY-0207` is the story that puts a duel behind it.
 *
 * A second [Hello] is [ProtocolError.MALFORMED_MESSAGE]: the handshake happened once and is not
 * repeatable. An [Act] is [ProtocolError.NOT_IN_DUEL] — this socket's session is in no duel, and
 * saying so is the whole answer. The `when` over [duels.poker.server.protocol.ClientMessage] has
 * no `else`, so a new message type stops this function compiling instead of silently falling
 * through it.
 *
 * [maxFrameLength] and [maxFrameNestingDepth] are the operator's configured limits, not the
 * codec's own defaults — see [SocketDependencies].
 */
private suspend fun ConnectionWriter.replyTo(frame: Frame, maxFrameLength: Int, maxFrameNestingDepth: Int) {
    val text = (frame as? Frame.Text)?.readText() ?: run {
        send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.MALFORMED_MESSAGE)))
        return
    }
    val failure = when (val decoded = ProtocolCodec.decodeClient(text, maxFrameLength, maxFrameNestingDepth)) {
        is Decoded.Refused -> ServerMessage.Failure(decoded.error)
        is Decoded.Message -> when (decoded.message) {
            is Hello -> ServerMessage.Failure(ProtocolError.MALFORMED_MESSAGE)
            is Act -> ServerMessage.Failure(ProtocolError.NOT_IN_DUEL)
        }
    }
    send(ProtocolCodec.encode(failure))
}
