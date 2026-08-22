import type { LadderPage } from "./ladder-page";
import { parseLadderPage } from "./ladder-page";
import { readDeviceId } from "../protocol/device-id";
import type { ApiFetch } from "../profile/api";

/**
 * One page of the standings with the cursor naming the next page.
 *
 * The response includes `nextCursor` which tells the client where to ask for the
 * next page — either a string naming the position in the paginated order, or `null`
 * on the last page. A body lacking it answers `unavailable` rather than defaulting
 * to `null`, which would stop a page walk that has not ended.
 */
export type LadderRead =
  | { readonly kind: "page"; readonly page: LadderPage }
  | { readonly kind: "unavailable" };

/**
 * Builds the path for `GET /api/standings`, with an optional cursor.
 *
 * `after` is passed through untouched, never parsed, never incremented, never derived
 * from a row count. No page size parameter is sent; the server's default of 10 rows applies.
 */
export function ladderPath(after: string | null): string {
  if (after === null) {
    return "/api/standings";
  }
  return `/api/standings?after=${encodeURIComponent(after)}`;
}

/**
 * Reads one page of `GET /api/standings`.
 *
 * Uses the device ID stored in the given storage to authenticate if present. The device
 * ID is optional on this endpoint — unlike `GET /api/me/duels` which answers 401 when
 * absent, `/api/standings` answers a valid page for any reader, and an absent device id
 * is not a refusal. The browser's storage is the sole source of the ID; the inert global
 * Node defines when testing makes a test that relies on the global untestable and
 * behaviorally different from the browser.
 */
export async function readLadderPage(request: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
  readonly after: string | null;
}): Promise<LadderRead> {
  const deviceId = readDeviceId(request.storage);

  try {
    const path = ladderPath(request.after);
    const headers: Record<string, string> = {};

    // Include the device header only if a device ID is present.
    if (deviceId !== null) {
      headers["X-Device-Id"] = deviceId;
    }

    const response = await request.fetch(path, { headers });

    if (response.status === 200) {
      const body = await response.json();
      const parsed = parseLadderPage(body);
      if (parsed !== null) {
        return { kind: "page", page: parsed };
      }
    }

    // Any non-200 status, or a 200 with an unparseable body, is unavailable.
    return { kind: "unavailable" };
  } catch {
    // A fetch that rejects, or a json() that rejects, becomes unavailable.
    // Caught, never rethrown, and never retried on the player's behalf.
    return { kind: "unavailable" };
  }
}
