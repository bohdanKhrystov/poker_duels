package duels.poker.server.e2e

import duels.poker.server.protocol.CreateRoom
import duels.poker.server.protocol.JoinRoom
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame

internal const val HOST_DEVICE: String = "e2e-host"
internal const val GUEST_DEVICE: String = "e2e-guest"

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
 * `received` on each client records every `ServerMessage` a client got after its `Welcome`,
 * starting with its own `RoomJoined`, in arrival order. Nothing else is read off either socket here;
 * the opening hand's frames are already queued on the guest's join and stay queued for the next task.
 *
 * @param handSeed The hand seed to use. Defaults to [HAND_SEED].
 * @return A [SocketDuel] with both clients seated.
 */
internal suspend fun HttpClient.openSocketDuel(handSeed: Long = HAND_SEED): SocketDuel {
    // Open the host connection
    val hostSession = webSocketSession("/ws")
    hostSession.completeHandshake(HOST_DEVICE)

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
    guestSession.completeHandshake(GUEST_DEVICE)

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
