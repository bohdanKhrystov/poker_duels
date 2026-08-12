package duels.poker.server.protocol

import duels.poker.server.config.ServerConfig
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
     * back as [Decoded.Refused], not as a thrown exception. So does a frame longer than
     * [maxFrameLength] or nested deeper than [maxNestingDepth]: both are refused before parsing,
     * because a recursive-descent parser given deeply nested input can exhaust the stack with a
     * [StackOverflowError] — an `Error`, not an `IllegalArgumentException`, and not something the
     * narrow catch below is entitled to swallow.
     *
     * @param maxFrameLength The longest frame, in UTF-16 code units, this call will parse.
     * @param maxNestingDepth The deepest object/array nesting this call will parse.
     */
    public fun decodeClient(
        text: String,
        maxFrameLength: Int = ServerConfig.DEFAULT_MAX_FRAME_LENGTH,
        maxNestingDepth: Int = ServerConfig.DEFAULT_MAX_FRAME_NESTING_DEPTH,
    ): Decoded {
        // Length is the cheapest possible check — no scan, no allocation beyond what the caller
        // already paid for the string — and it rejects the overwhelming majority of hostile
        // frames outright. It runs first for exactly that reason.
        if (text.length > maxFrameLength) {
            return Decoded.Refused(ProtocolError.FRAME_LIMIT_EXCEEDED)
        }

        // Depth must be checked before the frame reaches the parser, not found out from it: the
        // parser recurses one stack frame per nesting level, so asking it to parse first to see
        // whether the input is too deep is the exact bug this check exists to avoid. Scanning the
        // raw text is safe because it is iterative — it cannot itself recurse — no matter how
        // deep the (unparsed) brackets go.
        if (exceedsNestingDepth(text, maxNestingDepth)) {
            return Decoded.Refused(ProtocolError.FRAME_LIMIT_EXCEEDED)
        }

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

/**
 * True if [text] contains a `{`/`[` nesting deeper than [maxDepth], counting brackets outside
 * string literals only. Deliberately a single forward pass with no recursion of its own, so it
 * can be run on input that has not yet been judged safe to hand to a real parser — that is the
 * whole point of checking depth this way instead of by parsing and seeing what happens.
 */
private fun exceedsNestingDepth(text: String, maxDepth: Int): Boolean {
    var depth = 0
    var inString = false
    var escaped = false

    for (char in text) {
        if (inString) {
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '"' -> inString = false
            }
            continue
        }

        when (char) {
            '"' -> inString = true
            '{', '[' -> {
                depth++
                if (depth > maxDepth) return true
            }
            '}', ']' -> depth--
        }
    }

    return false
}
