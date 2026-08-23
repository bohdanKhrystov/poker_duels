package duels.poker.server.protocol

import duels.poker.engine.game.PlayerAction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A client message expresses an **intent** to attempt an action or establish a connection, never
 * an outcome or game state fact.
 *
 * Per ADR-0002 ("The server is authoritative"), the server owns all game truth. When a client
 * sends a message, it is never stating what it believes the game state to be—it is asking the
 * server to *attempt* an action in response to a decision point the client identified. Fields
 * like `handNumber` and `actionSequence` are *questions* the server will compare against its own
 * state, not values it will adopt.
 *
 * Consequently, no message field will ever carry:
 * - Cards (own or opponent's, dealt or visible)
 * - A stack size or pot amount
 * - Whose turn it is or which seat is acting
 * - The outcome of a hand, a decision, or a match
 * - Anything else that derives from the engine's state
 *
 * This design ensures that a malformed or replayed client message cannot corrupt the server's
 * view of the game, and that every client can trust the server's projection of the game state
 * without needing to run the engine themselves.
 */
@Serializable
public sealed interface ClientMessage

/**
 * A handshake from a client identifying itself and the protocol version it speaks.
 *
 * The server uses `protocolVersion` to decide whether to accept the connection. `deviceId`,
 * if provided, is a claim of identity—the server will either accept that claim or reject it
 * if the device is already in a duel, but the server never allows a client to assert a game
 * fact simply by naming a device. `sessionToken`, if provided, will let a client resume an
 * existing session — nothing reads it yet (`TASK-040518`).
 */
@Serializable
@SerialName("Hello")
public data class Hello(
    val deviceId: String? = null,
    val protocolVersion: Int = PROTOCOL_VERSION,
    val sessionToken: String? = null,
) : ClientMessage

/**
 * An attempt to perform an action on the given hand at the given decision point.
 *
 * `handNumber` and `actionSequence` are not claims about the game state; they are the client's
 * way of saying "this action responds to decision point N in hand M". The server will compare
 * them against its own record. If they do not match—because a message was replayed, reordered,
 * or arrived late—the server will reject the action without applying it.
 */
@Serializable
@SerialName("Act")
public data class Act(
    val handNumber: Int,
    val actionSequence: Int,
    val action: PlayerAction,
) : ClientMessage {
    init {
        require(handNumber >= 1) { "handNumber must be at least 1, was $handNumber" }
        require(actionSequence >= 0) { "actionSequence must be non-negative, was $actionSequence" }
    }
}

/**
 * An attempt to open a new room.
 *
 * This does not name a format, seat, stack, or any other game configuration — the server owns
 * all such decisions and will open the room with `DuelFormat.DEFAULT` (an open decision under
 * `DEC-001`). A client that chose a format would be asserting a rule of the game, which
 * `ADR-0002` forbids.
 */
@Serializable
@SerialName("CreateRoom")
public data object CreateRoom : ClientMessage

/**
 * An attempt to join an existing room by its code.
 *
 * This names a room by code; it does not claim a seat in it or attempt to reserve capacity.
 * The server will verify the code names a real room that has capacity and is not in-game, then
 * seat the client. A code that does not parse is refused by the server's room registry, not by
 * this constructor — `RoomCode.parse` is the single place a code's shape is decided.
 */
@Serializable
@SerialName("JoinRoom")
public data class JoinRoom(val code: String) : ClientMessage

/**
 * An attempt to offer a rematch after a finished duel.
 *
 * Names no room and no seat. The socket's own `RoomMembership` names the room and the
 * handshake's session names the player, so a client cannot offer a rematch in a room it
 * never entered — structurally, not by a check (`ADR-0044` §1).
 */
@Serializable
@SerialName("OfferRematch")
public data object OfferRematch : ClientMessage
