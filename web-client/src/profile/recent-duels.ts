import { readFromApi, type ApiFetch } from "./api";
import { readDeviceId } from "../protocol/device-id";

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
 * Uses the device ID stored in the given storage to authenticate with /api/me/duels.
 * Returns the list of recent duels if the server answers with a valid body,
 * or no-profile/unavailable otherwise.
 */
export async function readRecentDuels(deps: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
}): Promise<RecentDuelsRead> {
  const deviceId = readDeviceId(deps.storage);
  const apiRead = await readFromApi({
    fetch: deps.fetch,
    deviceId,
    path: "/api/me/duels",
  });

  switch (apiRead.kind) {
    case "no-profile":
      return { kind: "no-profile" };

    case "unavailable":
      return { kind: "unavailable" };

    case "body": {
      const body = apiRead.body;
      // Validate that body has the required fields with correct types
      if (
        typeof body === "object" &&
        body !== null &&
        Array.isArray((body as Record<string, unknown>).duels)
      ) {
        const duelsList = (body as Record<string, unknown>).duels as unknown[];
        const duels: RecentDuel[] = [];

        for (const row of duelsList) {
          const opponentDisplayName = (row as Record<string, unknown>)
            .opponentDisplayName;
          if (
            typeof row === "object" &&
            row !== null &&
            typeof (row as Record<string, unknown>).duelId === "string" &&
            typeof (row as Record<string, unknown>).outcome === "string" &&
            (typeof opponentDisplayName === "string" ||
              opponentDisplayName === null) &&
            typeof (row as Record<string, unknown>).coinDelta === "number" &&
            typeof (row as Record<string, unknown>).handsPlayed === "number" &&
            typeof (row as Record<string, unknown>).finishedAt === "string"
          ) {
            const outcome = (row as Record<string, unknown>).outcome as string;
            // Only accept valid outcome words
            if (outcome === "WON" || outcome === "LOST" || outcome === "DREW") {
              const duel: RecentDuel = {
                duelId: (row as Record<string, unknown>).duelId as string,
                outcome: outcome as DuelOutcomeWord,
                opponentDisplayName: opponentDisplayName as string | null,
                coinDelta: (row as Record<string, unknown>).coinDelta as number,
                handsPlayed: (row as Record<string, unknown>)
                  .handsPlayed as number,
                finishedAt: (row as Record<string, unknown>)
                  .finishedAt as string,
              };
              duels.push(duel);
            } else {
              // Invalid outcome word
              return { kind: "unavailable" };
            }
          } else {
            // Missing or wrong type for required field
            return { kind: "unavailable" };
          }
        }

        return { kind: "duels", duels };
      }

      return { kind: "unavailable" };
    }
  }
}
