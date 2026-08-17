package duels.poker.server.protocol.http

import kotlinx.serialization.Serializable

/**
 * A player's profile with their current coin balance.
 *
 * @property playerId The unique identifier of the player.
 * @property coinBalance The player's current coin balance, computed as wins minus losses. May be
 *   negative per `ADR-0014`.
 * @property displayName The player's chosen display name, or `null` if never set. Null means
 *   *never set*, and the server fabricates no placeholder per `ADR-0029` §6.
 */
@Serializable
public data class ProfileResponse(val playerId: String, val coinBalance: Int, val displayName: String?)

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
 * @property opponentPlayerId The stable identity a client correlates on. This is the opponent's
 *   `player.id`, never their device ID. A device ID is the sole authentication token in v0.1
 *   (see `DeviceIdSource`), so handing one to the other player would hand over their account.
 *   Per `ADR-0021`, the id is the stable identity and `opponentDisplayName` is the label; both
 *   travel.
 * @property opponentDisplayName The opponent's chosen display name, or `null` if never set. Null
 *   means *never set*; the server fabricates no placeholder per `ADR-0029` §6. The name is read
 *   at request time per `ADR-0021`, so a name set later relabels an older duel line.
 * @property outcome The outcome of the duel from the requesting player's perspective.
 * @property coinDelta The change in coins from this duel. A signed integer, never clamped: the
 *   winner gains one, the loser loses one, a draw changes nothing.
 * @property handsPlayed The number of hands played in the duel.
 * @property finishedAt The ISO-8601 instant when the duel finished, as text in UTC. Produced by
 *   `Instant.toString()`.
 */
@Serializable
public data class DuelSummaryResponse(
    val duelId: String,
    val opponentPlayerId: String,
    val outcome: DuelOutcomeLabel,
    val coinDelta: Int,
    val handsPlayed: Int,
    val finishedAt: String,
    val opponentDisplayName: String?,
)

/**
 * A list of recent duel summaries.
 *
 * @property duels The list of duel summaries. Why no default: kotlinx.serialization omits a
 *   default-valued property unless `encodeDefaults = true`, and `Application.module()` installs
 *   `ContentNegotiation { json() }`, whose `Json` has `encodeDefaults = false`. These DTOs are
 *   serialised by `protocolJson`, which sets `encodeDefaults = true`, but a defaulted property
 *   would be present in the tests' JSON and absent on the wire.
 * @property nextCursor The cursor for the next page of results, or `null` if this is the last
 *   page. Why no default: same as `duels` above — a default `null` would hide the `encodeDefaults`
 *   trap at the last page of every real request, where a test with `protocolJson` would show the
 *   field and the wire would omit it. The field is declared last in declaration order.
 */
@Serializable
public data class RecentDuelsResponse(val duels: List<DuelSummaryResponse>, val nextCursor: String?)

/**
 * The body of `PUT /api/me/name`, carrying a display name the player has chosen.
 *
 * The server canonicalises what it receives via `canonicalDisplayNameOrNull`, which may reject
 * the name, trim it, or normalise its casing. The DTO is a shape, not a rule.
 *
 * @property name The display name the player has chosen. No default value: a missing field must
 *   be refused rather than silently becoming `null` or `""`, and an empty body should not
 *   succeed in setting the empty string.
 */
@Serializable
public data class SetNameRequest(val name: String)
