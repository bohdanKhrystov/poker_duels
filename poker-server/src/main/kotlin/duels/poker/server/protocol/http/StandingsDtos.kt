package duels.poker.server.protocol.http

import kotlinx.serialization.Serializable

/**
 * A row in the standings leaderboard.
 *
 * @property rank The player's rank in the current season standings. 1 is the best.
 * @property playerId The unique identifier of the player.
 * @property displayName The player's chosen display name, or `null` if never set. Null means
 *   *never set*, and the server fabricates no placeholder per `ADR-0029` §6.
 * @property coins The player's coin standing for this season. The number of coin-changing duels
 *   won minus lost in this season. May be negative per `ADR-0014`.
 */
@Serializable
public data class StandingRow(
    val rank: Int,
    val playerId: String,
    val displayName: String?,
    val coins: Int,
)

/**
 * The calling player's standing in the current season, if they have one.
 *
 * @property playerId The unique identifier of the player.
 * @property rank The player's rank in the current season standings, or `null` if the player has
 *   no place this season (finished no duel per `ADR-0065` §4). Both `rank` and `coins` are `null`
 *   together, or both are present; a half-filled response is not a valid state.
 * @property coins The player's coin standing for this season, or `null` if the player has no
 *   place this season (finished no duel per `ADR-0065` §4). Both `rank` and `coins` are `null`
 *   together, or both are present; never print `0` for a player who finished no duel, which would
 *   state something false since `0` is a real standing a draw earns (`ADR-0015`).
 */
@Serializable
public data class SelfStandingResponse(
    val playerId: String,
    val rank: Int?,
    val coins: Int?,
) {
    init {
        require((rank == null) == (coins == null)) {
            "rank and coins must both be null or both be non-null"
        }
    }
}

/**
 * The standings for the current season.
 *
 * @property season The wire form of the season this standings was computed for, `"YYYY-MM"`
 *   per `ADR-0061` §1. `Season.toString()` produces this format.
 * @property rows The leaderboard rows, in rank order from 1. Paginated by `nextCursor`.
 * @property nextCursor The cursor for the next page of results, or `null` if this is the last
 *   page. Why no default: kotlinx.serialization omits a default-valued property unless
 *   `encodeDefaults = true`, and `Application.module()` installs `ContentNegotiation { json() }`,
 *   whose `Json` has `encodeDefaults = false`. These DTOs are serialised in tests by
 *   `protocolJson`, which sets `encodeDefaults = true`, but a defaulted property would be present
 *   in every test's JSON and absent from the real last page.
 * @property self The calling player's standing in this season, or `null` if the caller provided
 *   no `playerId` in the request or if they are not the authenticated user. Three answers per
 *   `ADR-0065` §4: both `rank` and `coins` non-null (they placed and finished duels), both `null`
 *   (they have a profile but finished no duel this season), or the whole object `null` (no
 *   authentication or request did not ask).
 */
@Serializable
public data class StandingsResponse(
    val season: String,
    val rows: List<StandingRow>,
    val nextCursor: String?,
    val self: SelfStandingResponse?,
)
