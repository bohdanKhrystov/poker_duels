package duels.poker.server.protocol

/**
 * Accepts or refuses a handshake based on protocol version matching.
 *
 * The caller must send the returned message to the client:
 * - If it is a [ServerMessage.Welcome], the connection is accepted; the client may proceed to
 *   joining or creating a duel.
 * - If it is a [ServerMessage.Failure], send it and then close the socket. A refused handshake
 *   is terminal for this client connection.
 *
 * @param hello The client's handshake message.
 * @param deviceId The device id for this connection. If the client presented one, it is the
 *   client's claimed id; if not, the server has generated one per ADR-0012. This function does
 *   not validate or mint it—that responsibility lives in STORY-0205 and STORY-0210.
 * @return A [ServerMessage.Welcome] if the protocol versions match, or a [ServerMessage.Failure]
 *   with [ProtocolError.VERSION_MISMATCH] if they do not. No other error is returned by this
 *   function; it is pure, with no I/O and no state.
 */
public fun handshake(hello: Hello, deviceId: String): ServerMessage =
    if (hello.protocolVersion == PROTOCOL_VERSION) {
        ServerMessage.Welcome(deviceId)
    } else {
        ServerMessage.Failure(ProtocolError.VERSION_MISMATCH)
    }
