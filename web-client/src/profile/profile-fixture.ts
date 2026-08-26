import type { PlayerProfile } from "./profile";
import type { RecentDuel } from "./recent-duels";

/**
 * A `PlayerProfile` carrying every field the type declares, for a test to bend.
 *
 * Its values are mutually independent: the player identifier and coin balance
 * do not depend on each other, so a test that overrides one does not accidentally
 * override the other.
 */
export function aProfile(
  overrides: Partial<PlayerProfile> = {},
): PlayerProfile {
  return {
    playerId: "p-fixture",
    coinBalance: 41,
    displayName: null,
    displayNameRemoved: false,
    deviceRouteLive: true,
    ...overrides,
  };
}

/**
 * A `RecentDuel` carrying every field the type declares, for a test to bend.
 *
 * Its values are mutually independent: no two of them add, subtract, double or
 * halve into a third. A figure a test worked out for itself therefore lands
 * outside the set instead of colliding with a legitimate one.
 */
export function aDuelLine(overrides: Partial<RecentDuel> = {}): RecentDuel {
  return {
    duelId: "duel-fixture",
    outcome: "WON",
    opponentDisplayName: null,
    coinDelta: 1,
    handsPlayed: 23,
    finishedAt: "2026-02-03T04:05:06Z",
    ...overrides,
  };
}

/**
 * A wire body for `GET /api/me`, for a test to bend.
 *
 * Returns a record with unknown values so a test can pass wrong-typed fields
 * like `meBody({ coinBalance: "x" })` and still get a body.
 *
 * Carries exactly the fields `GET /api/me` documents: `playerId`, `coinBalance`,
 * `displayName`, `displayNameRemoved`, and `deviceRouteLive`.
 */
export function meBody(
  overrides: Record<string, unknown> = {},
): Record<string, unknown> {
  return {
    playerId: "p-fixture",
    coinBalance: 41,
    displayName: null,
    displayNameRemoved: false,
    deviceRouteLive: true,
    ...overrides,
  };
}

/**
 * A wire body for a row in `GET /api/me/duels`, for a test to bend.
 *
 * Returns a record with unknown values so a test can pass wrong-typed fields
 * and still get a body.
 *
 * Carries `opponentPlayerId` even though the parser drops it: `profile-no-derivation.test.tsx`
 * exists to catch it reaching a screen, and a body builder that omitted it would quietly
 * disarm that guard. The six parsed fields come from `RecentDuel`; the wire adds the seventh.
 */
export function duelRowBody(
  overrides: Record<string, unknown> = {},
): Record<string, unknown> {
  return {
    duelId: "duel-fixture",
    outcome: "WON",
    opponentDisplayName: null,
    coinDelta: 1,
    handsPlayed: 23,
    finishedAt: "2026-02-03T04:05:06Z",
    opponentPlayerId: "player-fixture",
    ...overrides,
  };
}
