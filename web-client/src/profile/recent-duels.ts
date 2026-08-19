import type { ApiFetch } from "./api";
import { readDuelPage } from "./duel-page";
import { WHOLE_RECORD } from "./duels-query";

/** The outcome of a listed duel, from the reader's side (`docs/protocol.md`). */
export type DuelOutcomeWord = "WON" | "LOST" | "DREW";

/**
 * One row of `GET /api/me/duels`.
 *
 * The wire carries `opponentPlayerId` and this type does not: the opponent's
 * name is the label, and the id is not the client's business. Per `ADR-0021`,
 * the id is the stable identity for correlation; this client correlates nothing,
 * so dropping it at the parse is what keeps `profile-no-derivation.test.tsx`'s
 * guard cheap to keep true.
 */
export interface RecentDuel {
  readonly duelId: string;
  readonly outcome: DuelOutcomeWord;
  readonly opponentDisplayName: string | null;
  readonly coinDelta: number;
  readonly handsPlayed: number;
  readonly finishedAt: string;
}

export type RecentDuelsRead =
  | { readonly kind: "duels"; readonly duels: readonly RecentDuel[] }
  | { readonly kind: "no-profile" }
  | { readonly kind: "unavailable" };

/**
 * Reads the recent duels from the API.
 *
 * Delegates to `readDuelPage` which holds the sole parse of the duel row shape,
 * answering the subset the strip needs: no pagination (a strip has no next page),
 * and no filtering (it shows the whole history).
 *
 * Uses the device ID stored in the given storage to authenticate with /api/me/duels.
 * Returns the list of recent duels if the server answers with a valid body,
 * or no-profile/unavailable otherwise.
 */
export async function readRecentDuels(deps: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
}): Promise<RecentDuelsRead> {
  const page = await readDuelPage({ ...deps, query: WHOLE_RECORD });
  return page.kind === "page" ? { kind: "duels", duels: page.duels } : page;
}
