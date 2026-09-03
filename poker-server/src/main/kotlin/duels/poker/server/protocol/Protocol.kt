package duels.poker.server.protocol

import kotlinx.serialization.json.Json

/**
 * The wire protocol this build speaks. Bumping it breaks every older client, deliberately.
 * Version 2 is the version in which `ServerMessage` gained `DuelFinished` and both hierarchies gained the room messages (ADR-0017).
 * Version 3 is the version in which `ClientMessage` gained `OfferRematch` and `ServerMessage` gained `RematchOffered` (ADR-0044).
 * Version 4 is the version in which `ServerMessage` gained `OpponentPresence` and `ActedForAbsent` (ADR-0028).
 * Version 5 is the version in which `ClientMessage.Hello` gained `sessionToken`, `ServerMessage.Welcome` gained `playerId` and its `deviceId` became nullable, and `ProtocolError` gained `INVALID_SESSION` (ADR-0027).
 * Version 6 is the version in which `ServerMessage` gained `TurnClock`, `OpponentPresence` lost its remaining-duration field, and `ProtocolError` lost its pause-refusal entry (ADR-0113).
 */
public const val PROTOCOL_VERSION: Int = 6

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
