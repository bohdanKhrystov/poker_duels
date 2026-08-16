import { readProfile, type PlayerProfile } from "./profile";
import { readRecentDuels, type RecentDuel } from "./recent-duels";
import type { ApiFetch } from "./api";

/** What the lobby strip has to show. */
export type ProfileStripState =
  | {
      readonly kind: "profile";
      readonly profile: PlayerProfile;
      readonly duels: readonly RecentDuel[];
    }
  | { readonly kind: "no-profile" }
  | { readonly kind: "unavailable" };

/**
 * Reads the profile and recent duels in parallel.
 *
 * Both reads start together (Promise.all) — they are independent GETs
 * and serialising them would double the wait for no benefit.
 *
 * Returns:
 * - { kind: "no-profile" } if either read says no-profile
 * - { kind: "unavailable" } if either read fails
 * - { kind: "profile", profile, duels } if both reads succeed
 *
 * All or nothing: a balance shown beside a duel list that failed to load
 * reads as "you have no duels", which is a lie about the ledger.
 */
export async function readProfileStrip(deps: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
}): Promise<ProfileStripState> {
  const [profileRead, duelsRead] = await Promise.all([
    readProfile(deps),
    readRecentDuels(deps),
  ]);

  // If either half says no-profile, return no-profile.
  if (profileRead.kind === "no-profile" || duelsRead.kind === "no-profile") {
    return { kind: "no-profile" };
  }

  // If either half is unavailable, return unavailable.
  // All or nothing: never show a balance with a failed duel list.
  if (profileRead.kind === "unavailable" || duelsRead.kind === "unavailable") {
    return { kind: "unavailable" };
  }

  // Both reads succeeded.
  return {
    kind: "profile",
    profile: profileRead.profile,
    duels: duelsRead.duels,
  };
}
