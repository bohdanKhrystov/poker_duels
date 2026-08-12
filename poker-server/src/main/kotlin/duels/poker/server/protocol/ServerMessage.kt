package duels.poker.server.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A message sent by the server to a client over the wire.
 *
 * **Structural guarantee:** A `ServerMessage` that carries game state carries a `PlayerView`,
 * never a `GameState`. This is currently impossible by construction, not by convention —
 * `GameState`, `Deck` and `Rng` carry no `@Serializable` annotation, so a `@Serializable`
 * subtype declaring one does not compile. What is not impossible by construction is a raw
 * `Int` seed or a stray `Card`, which is why `TASK-020211` walks the descriptors.
 */
@Serializable
public sealed interface ServerMessage {
    /**
     * The handshake message sent when a client connection is accepted.
     *
     * @property deviceId The id the server issued or recognised for this device.
     * @property protocolVersion The wire protocol version. Defaults to [PROTOCOL_VERSION].
     */
    @Serializable
    @SerialName("Welcome")
    public data class Welcome(
        val deviceId: String,
        val protocolVersion: Int = PROTOCOL_VERSION,
    ) : ServerMessage

    /**
     * The handshake message sent when a client connection is refused.
     *
     * Carries no free-text detail. Adding one would reopen the closed [ProtocolError] set
     * through the back door.
     *
     * @property error The reason for the refusal, drawn from the closed [ProtocolError] set.
     */
    @Serializable
    @SerialName("Failure")
    public data class Failure(val error: ProtocolError) : ServerMessage
}
