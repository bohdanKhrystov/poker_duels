package duels.poker.server.protocol.http

import kotlinx.serialization.Serializable

/**
 * A player's profile with their current coin balance.
 *
 * @property playerId The unique identifier of the player.
 * @property coinBalance The player's current coin balance, computed as wins minus losses. May be
 *   negative per `ADR-0014`.
 */
@Serializable
public data class ProfileResponse(val playerId: String, val coinBalance: Int)

/**
 * The outcome of a duel from the requesting player's perspective.
 */
@Serializable
public enum class DuelOutcomeLabel {
    WON,
    LOST,
    DREW,
}

/**
 * A summary of a completed duel.
 *
 * @property duelId The unique identifier of the duel.
 * @property opponentPlayerId The player ID of the opponent. Note: this is the opponent's
 *   `player.id`, never their device ID. A device ID is the sole authentication token in v0.1
 *   (see `DeviceIdSource`), so handing one to the other player would hand over their account.
 *   `DEC-016` asks whether a human-readable name is added later; adding one would be a new field,
 *   not a change to this one.
 * @property outcome The outcome of the duel from the requesting player's perspective.
 * @property coinDelta The change in coins from this duel. A signed integer, never clamped: the
 *   winner gains one, the loser loses one, a draw changes nothing.
 * @property handsPlayed The number of hands played in the duel. Nullable and null until `DEC-014`
 *   is answered — the `duel` table has no `hands_played` column, `DEC-014` decides whether it gains
 *   one, and a nullable field is the shape that lets the answer be additive either way.
 * @property finishedAt The ISO-8601 instant when the duel finished, as text in UTC. Produced by
 *   `Instant.toString()`.
 */
@Serializable
public data class DuelSummaryResponse(
    val duelId: String,
    val opponentPlayerId: String,
    val outcome: DuelOutcomeLabel,
    val coinDelta: Int,
    val handsPlayed: Int?,
    val finishedAt: String,
)

/**
 * A list of recent duel summaries.
 *
 * @property duels The list of duel summaries. Why no default: kotlinx.serialization omits a
 *   default-valued property unless `encodeDefaults = true`, and `Application.module()` installs
 *   `ContentNegotiation { json() }`, whose `Json` has `encodeDefaults = false`. These DTOs are
 *   serialised by `protocolJson`, which sets `encodeDefaults = true`, but a defaulted property
 *   would be present in the tests' JSON and absent on the wire.
 */
@Serializable
public data class RecentDuelsResponse(val duels: List<DuelSummaryResponse>)
