import type { RecentDuel } from "./recent-duels";
import { readDeviceId } from "../protocol/device-id";
import type { ApiFetch } from "./api";
import { duelsPath, type HistoryQuery } from "./duels-query";

/**
 * One page of the duel history with the cursor naming the next page.
 *
 * The response includes `nextCursor` which tells the client where to ask for the
 * next page — either a string naming the position in the paginated order, or `null`
 * on the last page. The cursor is always present on the wire; a body lacking it
 * answers `unavailable` rather than defaulting to `null`, which would stop a page
 * walk that has not ended.
 */
export type DuelPageRead =
  | {
      readonly kind: "page";
      readonly duels: readonly RecentDuel[];
      readonly nextCursor: string | null;
    }
  | { readonly kind: "no-profile" }
  | { readonly kind: "unavailable" };

/**
 * Reads one page of `GET /api/me/duels` for a given history query.
 *
 * Uses the device ID stored in the given storage to authenticate. Returns the page
 * rows and the cursor naming the next page, or no-profile/unavailable otherwise.
 * Calls fetch directly and maps its own statuses, because `TASK-041304` must
 * tell a 400 from a 500 to honour `ADR-0057` §5.
 */
export async function readDuelPage(request: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
  readonly query: HistoryQuery;
}): Promise<DuelPageRead> {
  const deviceId = readDeviceId(request.storage);

  // No device id, no request: the server would answer 401, and that answer
  // is already known without a round trip.
  if (deviceId === null) {
    return { kind: "no-profile" };
  }

  try {
    const path = duelsPath(request.query);
    const response = await request.fetch(path, {
      headers: {
        "X-Device-Id": deviceId,
      },
    });

    switch (response.status) {
      case 200: {
        const body = await response.json();
        const parsed = parseDuelPageBody(body);
        if (parsed !== null) {
          return parsed;
        }
        // A 200 whose body is not valid is a server this client cannot follow.
        return { kind: "unavailable" };
      }

      case 401:
        return { kind: "no-profile" };

      default:
        return { kind: "unavailable" };
    }
  } catch {
    // A fetch that rejects, or a json() that rejects, becomes unavailable.
    // Caught, never rethrown, and never retried on the player's behalf.
    return { kind: "unavailable" };
  }
}

/**
 * Parses a duel page response body.
 *
 * Returns the parsed page (rows and cursor) if valid, or null if the body
 * is invalid in any way (missing fields, wrong types, missing cursor, etc.).
 */
function parseDuelPageBody(body: unknown): DuelPageRead | null {
  if (typeof body !== "object" || body === null) {
    return null;
  }

  const bodyRecord = body as Record<string, unknown>;

  // Validate that nextCursor exists and is either a string or null
  if (!("nextCursor" in bodyRecord)) {
    return null;
  }

  const nextCursor = bodyRecord.nextCursor;
  if (typeof nextCursor !== "string" && nextCursor !== null) {
    return null;
  }

  // Validate that duels is an array
  if (!Array.isArray(bodyRecord.duels)) {
    return null;
  }

  const duelsList = bodyRecord.duels as unknown[];
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
          outcome: outcome as "WON" | "LOST" | "DREW",
          opponentDisplayName: opponentDisplayName as string | null,
          coinDelta: (row as Record<string, unknown>).coinDelta as number,
          handsPlayed: (row as Record<string, unknown>).handsPlayed as number,
          finishedAt: (row as Record<string, unknown>).finishedAt as string,
        };
        duels.push(duel);
      } else {
        // Invalid outcome word
        return null;
      }
    } else {
      // Missing or wrong type for required field
      return null;
    }
  }

  return { kind: "page", duels, nextCursor };
}
