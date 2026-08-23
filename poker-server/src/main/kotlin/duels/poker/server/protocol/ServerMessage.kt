package duels.poker.server.protocol

import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.game.ActionType
import duels.poker.engine.game.GameEvent
import duels.poker.engine.game.LegalActions
import duels.poker.engine.game.PlayerView
import duels.poker.engine.game.Rejection
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
     * @property playerId The player this connection was resolved to.
     * @property deviceId The id the server issued or recognised for this device, or `null`
     *   when this connection names none — unreachable until `TASK-040518`.
     * @property protocolVersion The wire protocol version. Defaults to [PROTOCOL_VERSION].
     */
    @Serializable
    @SerialName("Welcome")
    public data class Welcome(
        val playerId: String,
        val deviceId: String?,
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

    /**
     * The current state of the hand the recipient is entitled to see.
     *
     * The only state-carrying message; its only field is a `PlayerView`. Redaction lives in
     * the engine's projection layer, so transport has nothing left to filter and no way to
     * widen what is sent.
     *
     * @property view The hand state from the recipient's perspective.
     */
    @Serializable
    @SerialName("Snapshot")
    public data class Snapshot(val view: PlayerView) : ServerMessage

    /**
     * The room code and seat the server assigned to you.
     *
     * This is the only place a client learns which seat it occupies; the server remembers the seat
     * from this message and does not trust any later assertion by the client about it (see
     * `ADR-0002`).
     *
     * @property code The room's code.
     * @property seat The seat the server assigned (0 or 1).
     */
    @Serializable
    @SerialName("RoomJoined")
    public data class RoomJoined(val code: String, val seat: Int) : ServerMessage {
        init { require(seat in 0..1) { "seat must be 0 or 1, was $seat" } }
    }

    /**
     * An offer from this seat to play again stands.
     *
     * Sent to **both** seats: the offering client learns its offer was recorded from the
     * server rather than assuming it, and the opponent learns of it from the same frame.
     * Each client already knows its own seat from [RoomJoined] and compares.
     *
     * @property seat The seat (0 or 1) whose offer stands.
     */
    @Serializable
    @SerialName("RematchOffered")
    public data class RematchOffered(val seat: Int) : ServerMessage {
        init { require(seat in 0..1) { "seat must be 0 or 1, was $seat" } }
    }

    /**
     * Facts that just happened in the duel.
     *
     * Carries whatever `visibleTo(events, seat)` returned for that recipient — this type
     * does no filtering of its own and its caller must ensure the result is already filtered.
     *
     * @property events The events the recipient is entitled to see.
     */
    @Serializable
    @SerialName("Events")
    public data class Events(val events: List<GameEvent>) : ServerMessage

    /**
     * The recipient's turn to act and the legal actions they may take.
     *
     * Repeats `handNumber` and `actionSequence` because they are exactly what a
     * `ClientMessage.Act` must echo back.
     *
     * @property handNumber The 1-based hand index this action pertains to.
     * @property actionSequence The event sequence number of the decision point.
     * @property legalActions The actions this seat may take.
     */
    @Serializable
    @SerialName("YourTurn")
    public data class YourTurn(
        val handNumber: Int,
        val actionSequence: Int,
        val legalActions: LegalActions,
    ) : ServerMessage

    /**
     * An action the recipient attempted was illegal.
     *
     * Wraps the engine's own `Rejection`. The protocol invents no second vocabulary for
     * "that action was illegal".
     *
     * @property rejection Why the attempted action failed.
     */
    @Serializable
    @SerialName("Rejected")
    public data class Rejected(val rejection: Rejection) : ServerMessage

    /**
     * The duel has ended.
     *
     * This is a projection, not a `MatchEvent` on the wire (`ADR-0017`). It carries the outcome
     * the recipient is entitled to see and deliberately does **not** carry `MatchFinished` or its
     * sequence number — `ADR-0009` gave match events their own sequence space and putting it on
     * the wire would blur that boundary.
     *
     * @property outcome The duel's outcome from the recipient's perspective.
     */
    @Serializable
    @SerialName("DuelFinished")
    public data class DuelFinished(val outcome: DuelOutcome) : ServerMessage

    /**
     * How present the recipient's opponent is, from the server's point of view alone.
     *
     * Addressed to exactly one seat and entirely about that seat's opponent, the same way
     * [YourTurn] is about its own recipient — a seat field would be a second thing to get wrong
     * at the one place that already decides where a frame goes (`ADR-0028` §1).
     *
     * @property presence How present the opponent is.
     * @property graceRemainingMillis How much of the grace window remained when the server built
     *   this frame. Present exactly when [presence] is [SeatPresence.AWAY], and never negative.
     */
    @Serializable
    @SerialName("OpponentPresence")
    public data class OpponentPresence(
        val presence: SeatPresence,
        val graceRemainingMillis: Long? = null,
    ) : ServerMessage {
        init {
            require((presence == SeatPresence.AWAY) == (graceRemainingMillis != null)) {
                "graceRemainingMillis is present exactly when presence is AWAY"
            }
            require(graceRemainingMillis == null || graceRemainingMillis >= 0) {
                "graceRemainingMillis must not be negative, was $graceRemainingMillis"
            }
        }
    }

    /**
     * A fact about the log both seats share: the server acted for an absent seat.
     *
     * Sent to **both** seats identically, so — unlike [OpponentPresence] — it names the seat it
     * is about (`ADR-0028` §1).
     *
     * @property seat The seat (0 or 1) the server acted for.
     * @property handNumber The 1-based hand index this action pertains to.
     * @property actionSequence The event sequence number of the decision point.
     * @property action The action the server took on this seat's behalf — always
     *   [ActionType.FOLD] or [ActionType.CHECK], per `ADR-0023`.
     */
    @Serializable
    @SerialName("ActedForAbsent")
    public data class ActedForAbsent(
        val seat: Int,
        val handNumber: Int,
        val actionSequence: Int,
        val action: ActionType,
    ) : ServerMessage {
        init {
            require(seat in 0..1) { "seat must be 0 or 1, was $seat" }
            require(action == ActionType.FOLD || action == ActionType.CHECK) {
                "the server only ever folds or checks for an absent seat, never $action"
            }
        }
    }
}
