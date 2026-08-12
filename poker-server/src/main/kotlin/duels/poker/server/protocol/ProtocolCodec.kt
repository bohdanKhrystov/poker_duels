package duels.poker.server.protocol

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The result of attempting to decode a client frame: either the message it carried, or the
 * reason it was refused.
 *
 * Deliberately not `@Serializable` — this is a server-internal result of a decode attempt, not
 * a wire type. Nothing on the wire ever carries a `Decoded`.
 */
public sealed interface Decoded {
    /** The frame decoded to a valid [ClientMessage]. */
    public data class Message(val message: ClientMessage) : Decoded

    /** The frame was refused; [error] is why. */
    public data class Refused(val error: ProtocolError) : Decoded
}

/** The `type` discriminator names [ClientMessage] declares, derived from its own descriptor. */
private val clientMessageNames: Set<String> =
    ClientMessage.serializer().descriptor.getElementDescriptor(1).let { sealed ->
        (0 until sealed.elementsCount).map { sealed.getElementName(it) }.toSet()
    }

/**
 * Turns messages into frames and frames into either a message or a named reason it was not one.
 *
 * A bad frame from one client is never an exception thrown across the connection boundary: it is
 * a [Decoded.Refused] value the caller — the connection — decides what to do with. This object
 * has no side effect of its own: no logging, no metrics, nothing beyond the pure translation.
 */
public object ProtocolCodec {
    /** Encode a [ServerMessage] to its wire frame, discriminator included. */
    public fun encode(message: ServerMessage): String =
        protocolJson.encodeToString(ServerMessage.serializer(), message)

    /** Encode a [ClientMessage] to its wire frame, discriminator included. */
    public fun encode(message: ClientMessage): String =
        protocolJson.encodeToString(ClientMessage.serializer(), message)

    /**
     * Decode a client frame, returning what it carried or why it was refused.
     *
     * Never throws: a frame that is not JSON, has no `type`, names an unknown `type`, or fails
     * to decode — including a decode that fails inside a data class's own `init` guard — comes
     * back as [Decoded.Refused], not as a thrown exception.
     */
    public fun decodeClient(text: String): Decoded {
        val element = try {
            protocolJson.parseToJsonElement(text)
        } catch (_: IllegalArgumentException) {
            return Decoded.Refused(ProtocolError.MALFORMED_MESSAGE)
        }

        if (element !is JsonObject) {
            return Decoded.Refused(ProtocolError.MALFORMED_MESSAGE)
        }
        val typeElement = element["type"] ?: return Decoded.Refused(ProtocolError.MALFORMED_MESSAGE)
        val typePrimitive = typeElement as? JsonPrimitive
        if (typePrimitive == null || !typePrimitive.isString) {
            return Decoded.Refused(ProtocolError.MALFORMED_MESSAGE)
        }

        if (typePrimitive.content !in clientMessageNames) {
            return Decoded.Refused(ProtocolError.UNKNOWN_MESSAGE)
        }

        return try {
            Decoded.Message(protocolJson.decodeFromJsonElement(ClientMessage.serializer(), element))
        } catch (_: IllegalArgumentException) {
            Decoded.Refused(ProtocolError.MALFORMED_MESSAGE)
        }
    }
}
