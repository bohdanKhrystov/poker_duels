package duels.poker.server.protocol.http

/**
 * Builders for `ProfileResponse` and `DuelSummaryResponse` test fixtures.
 *
 * These DTOs take no default values (see `ADR-0021`), so adding a new field to either breaks
 * every constructor call at once. By routing all test-side construction through these builders
 * first, the next field either DTO gains becomes a one-line change: add a default parameter
 * here and in the production type, then all tests pass without editing. See `STORY-0402`.
 */

internal fun profileResponse(
    playerId: String,
    coinBalance: Int,
    displayName: String? = null,
): ProfileResponse = ProfileResponse(playerId, coinBalance, displayName)

internal fun duelSummaryResponse(
    duelId: String,
    opponentPlayerId: String,
    outcome: DuelOutcomeLabel,
    coinDelta: Int,
    handsPlayed: Int,
    finishedAt: String,
    opponentDisplayName: String? = null,
): DuelSummaryResponse = DuelSummaryResponse(
    duelId = duelId,
    opponentPlayerId = opponentPlayerId,
    outcome = outcome,
    coinDelta = coinDelta,
    handsPlayed = handsPlayed,
    finishedAt = finishedAt,
    opponentDisplayName = opponentDisplayName,
)
