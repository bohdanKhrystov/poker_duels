package duels.poker.server.protocol

import kotlinx.serialization.json.Json

/** The wire protocol this build speaks. Bumping it breaks every older client, deliberately. */
public const val PROTOCOL_VERSION: Int = 1

/**
 * The shared Json instance for encoding and decoding all protocol frames.
 *
 * - `encodeDefaults = true`: kotlinx omits a default-valued property unless this is set,
 *   and `Hello.protocolVersion` will have a default, so without it a handshake reaches
 *   the server with no version to check
 * - `classDiscriminator = "type"`: pinned rather than inherited from the library default
 *   because `STORY-0203` generates TypeScript that hard-codes this key
 * - `ignoreUnknownKeys = false`: a frame with a field we do not know is a frame we do
 *   not understand
 * - `prettyPrint = false`: frames are not for reading
 */
public val protocolJson: Json = Json {
    encodeDefaults = true
    classDiscriminator = "type"
    ignoreUnknownKeys = false
    prettyPrint = false
}
