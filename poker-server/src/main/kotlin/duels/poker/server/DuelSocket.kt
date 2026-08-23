package duels.poker.server

import duels.poker.server.duel.Addressed
import duels.poker.server.protocol.Act
import duels.poker.server.protocol.CreateRoom
import duels.poker.server.protocol.Decoded
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.JoinRoom
import duels.poker.server.protocol.OfferRematch
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ProtocolError
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.versionRefusalOrNull
import duels.poker.server.room.JoinResult
import duels.poker.server.room.RematchRefusal
import duels.poker.server.room.RematchResult
import duels.poker.server.room.RoomCode
import duels.poker.server.room.RoomRefusal
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
 * The route is installed in production by `Application.duelServer` against a shared set of
 * `ServerComponents` that backs all routes; a test may still install it directly with its own
 * collaborators.
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
 * Gates the connection behind [readHello], then [versionRefusalOrNull] before anything else runs.
 *
 * A non-null refusal is sent and the socket is closed with [PROTOCOL_VERSION_MISMATCH] —
 * [versionRefusalOrNull] answers only a protocol version mismatch — without ever minting or
 * reading a device id. Only once the version is accepted does this resolve the connection's
 * player and send [ServerMessage.Welcome]; the connection then waits for further frames, racing
 * them against eviction via [seats] — see [serveUntilEvictedOrClosed].
 */
private suspend fun DefaultWebSocketServerSession.serve(
    deps: SocketDependencies,
    writer: ConnectionWriter,
    pump: Job,
    seats: SeatOwnership,
) {
    val hello = readHello(writer, pump, deps.maxFrameLength, deps.maxFrameNestingDepth) ?: return
    val refusal = versionRefusalOrNull(hello)
    if (refusal != null) {
        writer.send(ProtocolCodec.encode(refusal))
        writer.close()
        pump.join()
        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, PROTOCOL_VERSION_MISMATCH))
        return
    }
    val deviceId = hello.deviceId?.let(::DeviceId) ?: deps.deviceIds.newDeviceId()
    val player = deps.directory.resolve(deviceId)
    val message = ServerMessage.Welcome(playerId = player.id.value, deviceId = deviceId.value)
    val session = Session(SessionRegistry.newSessionId(), player)
    val room = RoomMembership()
    val eviction = seats.adopt(deps.sessions, session)
    deps.connections.register(player.id, writer)
    try {
        writer.send(ProtocolCodec.encode(message))
        serveUntilEvictedOrClosed(
            deps,
            session,
            room,
            writer,
            pump,
            eviction,
            deps.maxFrameLength,
            deps.maxFrameNestingDepth,
        )
    } finally {
        deps.sessions.remove(session.id)
        seats.forget(session.id)
        if (deps.connections.forget(player.id, writer)) {
            room.code?.let { code ->
                // RoomRegistry.disconnect takes the room's mutex, so unlike every other
                // statement in this finally block it must suspend. A finally reached by
                // cancellation sees a plain suspending call throw CancellationException
                // before it does anything, so without NonCancellable the seat's grace
                // window would silently never start on the most common close path there
                // is. The forget guard above is what keeps this from firing at all for a
                // socket a newer connection for the same device has already adopted.
                withContext(NonCancellable) {
                    deps.rooms.disconnect(code, player.id)?.let {
                        deliver(it.outbound, it.room, deps.connections)
                    }
                }
            }
        }
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
 *
 * [deps], [session] and [room] are passed through unchanged into every [ConnectionWriter.replyTo]
 * call this loop makes — [room] is this connection's own mutable record of which room, if any, it
 * has entered, updated there as the connection acts on it.
 */
private suspend fun DefaultWebSocketServerSession.serveUntilEvictedOrClosed(
    deps: SocketDependencies,
    session: Session,
    room: RoomMembership,
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
                writer.replyTo(result.getOrThrow(), maxFrameLength, maxFrameNestingDepth, deps, session, room)
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
 * The room, if any, this connection currently occupies.
 *
 * One instance per connection: created in [serve] and threaded through
 * [serveUntilEvictedOrClosed] into [ConnectionWriter.replyTo]. [code] is a plain `var`, not
 * behind a lock, because every read and write of it happens inside the single coroutine
 * [serveUntilEvictedOrClosed] runs in — one connection's frames are already processed one at a
 * time, never concurrently — unlike [SeatOwnership], which is shared by every connection an
 * [Application] accepts and needs its own locking for exactly that reason.
 */
private class RoomMembership {
    /** The code of the room this connection has entered, or `null` before it has entered one. */
    var code: RoomCode? = null
}

/**
 * Answers one post-handshake [frame].
 *
 * A second [Hello] is [ProtocolError.MALFORMED_MESSAGE]: the handshake happened once and is not
 * repeatable. An [Act] reaches the duel running in the room [room] names, or is refused as
 * [ProtocolError.NOT_IN_DUEL] if there is none — see [replyToAct]. [CreateRoom] and [JoinRoom]
 * open and enter a room through [SocketDependencies.rooms] — see [replyToCreateRoom] and
 * [replyToJoinRoom]. The `when` over [duels.poker.server.protocol.ClientMessage] has no `else`,
 * so a new message type stops this function compiling instead of silently falling through it.
 *
 * [maxFrameLength] and [maxFrameNestingDepth] are the operator's configured limits, not the
 * codec's own defaults — see [SocketDependencies]. [session] identifies this connection's player;
 * [room] is this connection's own record of which room, if any, it has entered.
 */
private suspend fun ConnectionWriter.replyTo(
    frame: Frame,
    maxFrameLength: Int,
    maxFrameNestingDepth: Int,
    deps: SocketDependencies,
    session: Session,
    room: RoomMembership,
) {
    val text = (frame as? Frame.Text)?.readText() ?: run {
        send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.MALFORMED_MESSAGE)))
        return
    }
    when (val decoded = ProtocolCodec.decodeClient(text, maxFrameLength, maxFrameNestingDepth)) {
        is Decoded.Refused -> send(ProtocolCodec.encode(ServerMessage.Failure(decoded.error)))
        is Decoded.Message -> when (val message = decoded.message) {
            is Hello -> send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.MALFORMED_MESSAGE)))
            is Act -> replyToAct(message, deps, session, room)
            is CreateRoom -> replyToCreateRoom(deps, session, room)
            is JoinRoom -> replyToJoinRoom(message.code, deps, session, room)
            is OfferRematch -> replyToOfferRematch(deps, session, room)
        }
    }
}

/**
 * Applies [message] to the duel running in the room [room] names, and delivers every frame it
 * produces to that duel's own seats via [deliver] (`TASK-020730`).
 *
 * Three things must hold before a frame reaches the duel — [room] remembers a code,
 * [SocketDependencies.rooms] still holds a room under it, and [session]'s player still holds a
 * seat in that room — and all three collapse to the same [ProtocolError.NOT_IN_DUEL] refusal, so
 * a socket that never entered a room learns nothing about which one failed. The seat is
 * [duels.poker.server.room.Room.seatOf], read fresh from the room this call just fetched from
 * [SocketDependencies.rooms] rather than cached anywhere: `TASK-020731`'s rule is that a seat
 * index that outlived a re-seat addresses a frame to the wrong player. That freshly derived
 * seat — never [message]'s own [duels.poker.engine.game.PlayerAction.seat] — is what
 * `TASK-020706`'s guard inside [duels.poker.server.duel.act] judges, so a frame naming the
 * opponent's seat is judged as this sender's own attempt, refused as `NOT_YOUR_TURN`, and never
 * mistaken for a move the opponent made.
 *
 * A `null` [duels.poker.server.duel.DuelStep] means the room had no move to make — it is not
 * [duels.poker.server.room.RoomState.PLAYING], or carries no runner — so nothing is sent and
 * nothing is answered: the client has already been told everything true about it.
 *
 * @param message The inbound attempt to act.
 * @param deps The collaborators this socket needs; [SocketDependencies.rooms] both applies
 *   [message] and is the sole source of the [duels.poker.server.duel.HandSeedSource] it draws
 *   from, so every hand of one duel draws from the source that opened it.
 * @param session Identifies this connection's player; never trusted for its seat.
 * @param room This connection's own record of which room, if any, it has entered.
 */
private suspend fun ConnectionWriter.replyToAct(
    message: Act,
    deps: SocketDependencies,
    session: Session,
    room: RoomMembership,
) {
    val code = room.code ?: return notInDuel()
    val liveRoom = deps.rooms.get(code) ?: return notInDuel()
    val seat = liveRoom.seatOf(session.player.id) ?: return notInDuel()
    val step = deps.rooms.act(code) { it.act(seat, message, deps.rooms.handSeeds) } ?: return
    deliver(step.outbound, liveRoom, deps.connections)
}

/** Answers with [ProtocolError.NOT_IN_DUEL] — see the three checks in [replyToAct]. */
private suspend fun ConnectionWriter.notInDuel() {
    send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.NOT_IN_DUEL)))
}

/**
 * Opens a fresh room for [session]'s player and answers with the seat the host always holds.
 *
 * [Room.open] always seats the host in seat 0 — see [duels.poker.server.room.Room] — so this
 * never needs to ask the freshly created room what seat it assigned.
 */
private suspend fun ConnectionWriter.replyToCreateRoom(
    deps: SocketDependencies,
    session: Session,
    room: RoomMembership,
) {
    val created = deps.rooms.create(session.player.id)
    room.code = created.code
    send(ProtocolCodec.encode(ServerMessage.RoomJoined(created.code.value, 0)))
}

/**
 * Enters, for [session]'s player, the room named by [code].
 *
 * A [code] that fails to parse is refused exactly as a well-shaped one naming no live room:
 * [RoomCode.parse] returning `null` and [RoomRefusal.UNKNOWN_ROOM] answer identically, or the
 * difference would hand an attacker a code-shape oracle.
 *
 * A player who already holds a seat in a duel this code names is resumption, not a fresh join:
 * [SocketDependencies.rooms]' `resume` answers only for a room this player is already seated in
 * that carries a duel, so it never intercepts a first-time joiner, a stranger, or a host rejoining
 * a still-`WAITING` room — those fall straight through to the ordinary
 * [join][duels.poker.server.room.RoomRegistry.join] below. The seat comes from [session]'s player
 * id, which came from the handshake's device id — never from [code] — so a different device
 * resolves to a different player, `resume` answers `null`, and the ordinary join below refuses a
 * full `PLAYING` room with [RoomRefusal.ROOM_FULL], leaving the held seat held.
 *
 * A player already seated in the room they asked to join is not an error:
 * [RoomRefusal.ALREADY_SEATED] answers exactly as a fresh seating would, with the seat they
 * already hold, so a socket that reconnects or that opened the room out of band still learns
 * where it sits. [duels.poker.server.room.JoinResult.Refused] carries no room, so that seat is
 * derived fresh from [SocketDependencies.rooms] rather than assumed — never a stale claim about a
 * room the registry has since reaped.
 *
 * A resumed seat is also handed anything the room is still holding open for it, `ADR-0044` §5:
 * after this join's own [ServerMessage.RoomJoined] and after the resumption's own frames land,
 * this socket is sent one [ServerMessage.RematchOffered] per player already recorded in
 * [duels.poker.server.room.Room.rematchOffers], addressed via
 * [duels.poker.server.room.Room.seatOf] — never before either. That order is load-bearing:
 * [ServerMessage.DuelFinished] is where a client enters its result screen, so an offer restated
 * ahead of it would be discarded by a reducer that treats that frame as where the screen begins,
 * not as a moment inside a duel still running.
 */
private suspend fun ConnectionWriter.replyToJoinRoom(
    code: String,
    deps: SocketDependencies,
    session: Session,
    room: RoomMembership,
) {
    val parsed = RoomCode.parse(code) ?: run {
        send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.UNKNOWN_ROOM)))
        return
    }
    val resumed = deps.rooms.resume(parsed, session.player.id)
    if (resumed != null) {
        room.code = parsed
        send(ProtocolCodec.encode(ServerMessage.RoomJoined(parsed.value, resumed.seat)))
        deliver(resumed.outbound, resumed.room, deps.connections)
        // ADR-0044 §5: restated only now, after everything above — DuelFinished is where a
        // client enters its result screen, and an offer stated ahead of that frame would be
        // discarded by a reducer that treats it as where the screen begins.
        for (player in resumed.room.rematchOffers) {
            val seat = resumed.room.seatOf(player) ?: continue
            send(ProtocolCodec.encode(ServerMessage.RematchOffered(seat)))
        }
        return
    }
    when (val result = deps.rooms.join(parsed, session.player.id)) {
        is JoinResult.Seated -> {
            room.code = parsed
            val seat = result.room.seatOf(session.player.id) ?: run {
                send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.UNKNOWN_ROOM)))
                return
            }
            send(ProtocolCodec.encode(ServerMessage.RoomJoined(parsed.value, seat)))
            deliver(result.outbound, result.room, deps.connections)
        }

        is JoinResult.Refused -> when (result.reason) {
            RoomRefusal.ALREADY_SEATED -> {
                room.code = parsed
                val seat = deps.rooms.get(parsed)?.seatOf(session.player.id) ?: run {
                    send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.UNKNOWN_ROOM)))
                    return
                }
                send(ProtocolCodec.encode(ServerMessage.RoomJoined(parsed.value, seat)))
            }

            RoomRefusal.UNKNOWN_ROOM -> send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.UNKNOWN_ROOM)))
            RoomRefusal.ROOM_FULL -> send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.ROOM_FULL)))
        }
    }
}

/**
 * Answers an attempt to offer a rematch for the finished duel in the room [room] names — the
 * whole of `ADR-0044` §§3, 4 and 6.
 *
 * A first offer is told to **both** seats through [deliver], built as
 * [duels.poker.server.protocol.ServerMessage.RematchOffered] beside where
 * [duels.poker.server.protocol.ServerMessage.RoomJoined] is built, never through the projection
 * layer (`ADR-0044` §2) — the offering client learns its offer was recorded from the server
 * rather than assuming it, and the opponent learns of it from the same frame. A repeat offer is
 * not an error: it answers **only the socket that sent it**, with the same
 * [duels.poker.server.protocol.ServerMessage.RematchOffered] already sent, the same precedent
 * [replyToJoinRoom]'s `ALREADY_SEATED` branch sets for a seat a player already holds.
 *
 * The seat addressed is never taken from the request — `OfferRematch` carries none — and never
 * cached; it comes off the [duels.poker.server.room.Room] the registry just returned, for
 * `TASK-020731`'s reason.
 *
 * @param deps The collaborators this socket needs.
 * @param session Identifies this connection's player; never trusted for its seat.
 * @param room This connection's own record of which room, if any, it has entered.
 */
private suspend fun ConnectionWriter.replyToOfferRematch(
    deps: SocketDependencies,
    session: Session,
    room: RoomMembership,
) {
    val code = room.code ?: run {
        send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.UNKNOWN_ROOM)))
        return
    }
    when (val result = deps.rooms.offerRematch(code, session.player.id)) {
        is RematchResult.Offered -> {
            val seat = result.room.seatOf(session.player.id) ?: run {
                send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.UNKNOWN_ROOM)))
                return
            }
            val offered = ServerMessage.RematchOffered(seat)
            deliver(listOf(Addressed(0, offered), Addressed(1, offered)), result.room, deps.connections)
        }

        is RematchResult.Agreed -> deliver(result.outbound, result.room, deps.connections)

        is RematchResult.Refused -> when (result.reason) {
            RematchRefusal.UNKNOWN_ROOM, RematchRefusal.NOT_A_PLAYER ->
                send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.UNKNOWN_ROOM)))

            RematchRefusal.NOT_FINISHED ->
                send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.REMATCH_UNAVAILABLE)))

            RematchRefusal.ALREADY_OFFERED -> {
                val seat = deps.rooms.get(code)?.seatOf(session.player.id) ?: run {
                    send(ProtocolCodec.encode(ServerMessage.Failure(ProtocolError.UNKNOWN_ROOM)))
                    return
                }
                send(ProtocolCodec.encode(ServerMessage.RematchOffered(seat)))
            }
        }
    }
}
