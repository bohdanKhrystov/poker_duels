package duels.poker.server.protocol

import kotlinx.serialization.Serializable

/**
 * A closed set of server error responses to protocol violations.
 *
 * The set is closed on purpose, because an open `error: String` cannot be branched on or
 * tested by a client. A client must be able to tell distinct failure modes apart and act
 * accordingly.
 */
@Serializable
public enum class ProtocolError {
    /** A frame with no matching message type in the current schema. */
    UNKNOWN_MESSAGE,

    /** A frame that is not valid JSON, or names a field we do not have. */
    MALFORMED_MESSAGE,

    /** The client's `protocolVersion` does not match the server's [PROTOCOL_VERSION]. */
    VERSION_MISMATCH,

    /** The client sent an action on a turn that is not theirs. */
    NOT_YOUR_TURN,

    /** The client requested a room that does not exist. */
    UNKNOWN_ROOM,

    /** The client requested to join a room that is at capacity. */
    ROOM_FULL,

    /** The client sent an action but is not participating in an active duel. */
    NOT_IN_DUEL,
}
