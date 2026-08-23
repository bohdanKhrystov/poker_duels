package duels.poker.server.protocol

/**
 * Answers the one question a handshake can decide on its own: does [hello] name a protocol
 * version this server speaks?
 *
 * Everything past the version — minting or reading a device id, resolving a player, building
 * [ServerMessage.Welcome] — is the caller's, and happens only once this answers `null`; see
 * `ADR-0027` §4 on why identity resolution has to be a step of its own rather than folded into
 * this one.
 *
 * @param hello The client's handshake message.
 * @return A [ServerMessage.Failure] with [ProtocolError.VERSION_MISMATCH] if [hello]'s protocol
 *   version does not match [PROTOCOL_VERSION], or `null` if it does. No other error is returned
 *   by this function; it is pure, with no I/O and no state.
 */
public fun versionRefusalOrNull(hello: Hello): ServerMessage.Failure? =
    if (hello.protocolVersion == PROTOCOL_VERSION) {
        null
    } else {
        ServerMessage.Failure(ProtocolError.VERSION_MISMATCH)
    }
