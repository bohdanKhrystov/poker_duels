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
import duels.poker.server.session.PlayerId
import duels.poker.server.session.Session
import duels.poker.server.session.SessionId
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Close reason sent when a connection's first frame is not a valid [Hello] — including a frame
 * that is not text, one that fails to decode, or one that decodes to some other [ClientMessage].
 */
public const val HANDSHAKE_REQUIRED: String = "handshake required"

/** Close reason sent when a [Hello] names a protocol version this server does not speak. */
public const val PROTOCOL_VERSION_MISMATCH: String = "protocol version mismatch"

/**
 * Close reason sent to a socket whose seat a newer connection for the same device just took.
 *
 * `ADR-0018` (resolving `DEC-011`): a device holds at most one live session, and the most recent
 * socket is the one that holds it — see [SeatOwnership].
 */
public const val SEAT_ADOPTED: String = "seat adopted by a newer connection"

/**
 * Installs the `/ws` route behind a mandatory handshake.
 *
 * A connection that has not first exchanged a valid [Hello] never reaches any further logic —
 * see [readHello]. Every outbound frame for the connection passes through a single
 * [ConnectionWriter], fed by exactly one writing coroutine ([pump]), so no two coroutines ever
 * write to the socket concurrently.
 *
 * [seats] is one [SeatOwnership] per route installation, shared by every `/ws` connection this
 * `Application` accepts — that sharing is what lets one connection's [SeatOwnership.adopt] evict
 * another connection's session.
 *
 * A shipping `duels.poker.server.db.PostgresPlayerDirectory` now exists, but installing the
 * route from `Application.module()` also means handing `module()` a `DataSource`, which
 * `STORY-0212` owns — so the only caller is still a test.
 *
 * @param deps The collaborators this socket needs.
 */
public fun Application.duelSocket(deps: SocketDependencies) {
    val seats = SeatOwnership()
    routing {
        webSocket("/ws") {
            val writer = ConnectionWriter()
            val pump = launch { writer.writeAll { frame -> outgoing.send(Frame.Text(frame)) } }
            try {
                serve(deps, writer, pump, seats)
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
 * A [ServerMessage.Welcome] is sent and the connection waits for further frames, racing them
 * against eviction via [seats] — see [serveUntilEvictedOrClosed]. A [ServerMessage.Failure] is
 * sent and the socket is closed with [PROTOCOL_VERSION_MISMATCH], since [handshake] returns a
 * failure only for a protocol version mismatch.
 */
private suspend fun DefaultWebSocketServerSession.serve(
    deps: SocketDependencies,
    writer: ConnectionWriter,
    pump: Job,
    seats: SeatOwnership,
) {
    val hello = readHello(writer, pump, deps.maxFrameLength, deps.maxFrameNestingDepth) ?: return
    val deviceId = hello.deviceId?.let(::DeviceId) ?: deps.deviceIds.newDeviceId()
    when (val message = handshake(hello, deviceId.value)) {
        is ServerMessage.Welcome -> {
            val player = deps.directory.resolve(deviceId)
            val session = Session(SessionRegistry.newSessionId(), player)
            val eviction = seats.adopt(deps.sessions, session)
            try {
                writer.send(ProtocolCodec.encode(message))
                serveUntilEvictedOrClosed(writer, pump, eviction, deps.maxFrameLength, deps.maxFrameNestingDepth)
            } finally {
                deps.sessions.remove(session.id)
                seats.forget(session.id)
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
 * Processes frames until the connection closes on its own or [eviction] fires because a newer
 * socket has [SeatOwnership.adopt]ed this session's seat.
 *
 * [select] races [DefaultWebSocketServerSession.incoming] against [eviction] — without it, a
 * connection sitting idle with nothing to read would never notice it had been evicted. On
 * eviction the connection closes itself with [SEAT_ADOPTED], in the same
 * close-writer-then-join-then-close-frame order as [refuseHandshake], so the buffered close is
 * not raced by the socket tearing down first.
 */
private suspend fun DefaultWebSocketServerSession.serveUntilEvictedOrClosed(
    writer: ConnectionWriter,
    pump: Job,
    eviction: Deferred<Unit>,
    maxFrameLength: Int,
    maxFrameNestingDepth: Int,
) {
    while (true) {
        val event =
            select<LoopEvent> {
                incoming.onReceiveCatching { LoopEvent.Received(it) }
                eviction.onAwait { LoopEvent.Evicted }
            }
        when (event) {
            is LoopEvent.Received -> {
                val result = event.result
                if (result.isClosed) {
                    // A clean close reports no cause; consumeEach's iterator swallows that same
                    // case, so this mirrors what the loop did before it had to race eviction.
                    result.exceptionOrNull()?.let { throw it }
                    return
                }
                writer.replyTo(result.getOrThrow(), maxFrameLength, maxFrameNestingDepth)
            }

            LoopEvent.Evicted -> {
                writer.close()
                pump.join()
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, SEAT_ADOPTED))
                return
            }
        }
    }
}

/** The two events [serveUntilEvictedOrClosed] races via [select]. */
private sealed interface LoopEvent {
    data class Received(val result: ChannelResult<Frame>) : LoopEvent

    data object Evicted : LoopEvent
}

/** Number of [SeatOwnership] lock stripes — arbitrary but bounded; see [SeatOwnership.lockFor]. */
private const val STRIPE_COUNT = 64

/**
 * Enforces `ADR-0018` (resolving `DEC-011`): a device holds at most one live session, and a
 * newer socket for the same device closes the older one rather than letting both survive.
 *
 * [SessionRegistry.sessionsOf] is deliberately just a query — it enforces nothing about how many
 * sessions a player may hold. [adopt] is where that gets decided, and it is the *only* place: one
 * [SeatOwnership] instance is shared by every connection a route accepts (see [duelSocket]), so
 * every adoption for this application goes through the same [stripes].
 *
 * Adoption is made atomic against a concurrent connect by a [Mutex] per [lockFor] stripe: listing
 * a player's current sessions, evicting them, and registering the new one all happen inside a
 * single [withLock] block. Two sockets racing to adopt one player's seat serialise on that lock —
 * whichever acquires it first evicts nothing (or evicts whatever was already there) and registers;
 * the other then sees the first as "already there" and evicts it. Either order is fine, but there
 * is no interleaving in which both register, or both get evicted.
 */
private class SeatOwnership {
    // Adoption is serialised per player, but the locks are striped rather than kept per player
    // id: a map keyed by PlayerId would grow forever, since ADR-0012 device ids are trivially
    // minted, and pruning it races — removing a lock another coroutine is about to acquire hands
    // out a second lock for the same player, reopening the race this class exists to close. A
    // fixed array is allocated once and nothing is ever removed; two unrelated players sharing a
    // stripe briefly serialise against each other, which is harmless.
    private val stripes = Array(STRIPE_COUNT) { Mutex() }
    private val evictions = ConcurrentHashMap<SessionId, CompletableDeferred<Unit>>()

    private fun lockFor(player: PlayerId): Mutex = stripes[(player.hashCode() and Int.MAX_VALUE) % STRIPE_COUNT]

    /**
     * Registers [session] in [sessions], evicting every session already held by
     * [Session.player] first.
     *
     * Completing an evicted session's deferred is what wakes its connection out of the
     * [select] in [serveUntilEvictedOrClosed] so it can close itself — this method never touches
     * another connection's socket directly.
     *
     * @return A [Deferred] that completes when a later call to [adopt] evicts this session.
     */
    suspend fun adopt(sessions: SessionRegistry, session: Session): Deferred<Unit> =
        lockFor(session.player.id).withLock {
            sessions.sessionsOf(session.player.id).forEach { existing ->
                evictions.remove(existing.id)?.complete(Unit)
            }
            val eviction = CompletableDeferred<Unit>()
            evictions[session.id] = eviction
            sessions.register(session)
            eviction
        }

    /**
     * Forgets [id]'s eviction signal once its connection has actually closed, so a session that
     * closed on its own — rather than being evicted — leaves nothing behind for [adopt] to find.
     */
    fun forget(id: SessionId) {
        evictions.remove(id)
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
