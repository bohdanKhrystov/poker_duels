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
 * fact simply by naming a device.
 */
@Serializable
@SerialName("Hello")
public data class Hello(
    val deviceId: String? = null,
    val protocolVersion: Int = PROTOCOL_VERSION,
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
