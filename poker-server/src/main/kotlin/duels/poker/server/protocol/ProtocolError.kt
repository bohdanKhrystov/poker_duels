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

    /** The duel is paused while the other seat is inside `ADR-0013`'s grace period. */
    DUEL_PAUSED,

    /**
     * A frame refused before it was parsed, either because it was longer than
     * [duels.poker.server.config.ServerConfig.DEFAULT_MAX_FRAME_LENGTH] allows or because its
     * bracket nesting was deeper than
     * [duels.poker.server.config.ServerConfig.DEFAULT_MAX_FRAME_NESTING_DEPTH] allows.
     */
    FRAME_LIMIT_EXCEEDED,

    /** The client presented a session token that is invalid, expired or unknown. */
    INVALID_SESSION,

    /**
     * The room cannot accept a rematch offer yet. **Transient**: nothing was recorded, and
     * the same offer may be sent again.
     */
    REMATCH_UNAVAILABLE,
}
