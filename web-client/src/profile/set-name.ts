import { profileFromBody, type PlayerProfile } from "./profile";
import { readDeviceId } from "../protocol/device-id";
import type { ApiFetch } from "./api";

/**
 * What one call to `setDisplayName` came back with.
 *
 * One outcome per status `PUT /api/me/name` can answer (`docs/protocol.md`),
 * plus `unavailable` for a status the client does not model and for a fetch
 * that rejected. `named` carries the profile the server returned, never the
 * string the player typed — the server canonicalises the name (`ADR-0029` §2).
 */
export type SetNameOutcome =
  | { readonly kind: "named"; readonly profile: PlayerProfile } // 200
  | { readonly kind: "rejected" } // 400 — the rules refuse this name
  | { readonly kind: "permanent" } // 403 — this player already has one
  | { readonly kind: "conflict" } // 409 — the name is not available
  | { readonly kind: "no-profile" } // 401 — absent, blank or unknown device id
  | { readonly kind: "unavailable" }; // anything else, or a fetch that rejected

/**
 * Sends the name a player typed to `PUT /api/me/name`, exactly as typed.
 *
 * The server canonicalises and validates the name (`docs/protocol.md`); this
 * function sends the raw string and reports whichever of the six outcomes
 * the response maps to. It sends at most one request: no device id means no
 * request, and no response — success or failure — is ever retried.
 */
export async function setDisplayName(request: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
  readonly name: string;
}): Promise<SetNameOutcome> {
  const deviceId = readDeviceId(request.storage);
  // No device id, no request: the server would answer 401, and that answer
  // is already known without a round trip.
  if (deviceId === null) {
    return { kind: "no-profile" };
  }

  try {
    const response = await request.fetch("/api/me/name", {
      method: "PUT",
      headers: {
        "X-Device-Id": deviceId,
      },
      body: JSON.stringify({ name: request.name }),
    });

    switch (response.status) {
      case 200: {
        const body = await response.json();
        const profile = profileFromBody(body);
        if (profile !== null) {
          return { kind: "named", profile };
        }
        // A 200 whose body is not a profile is a server this client cannot
        // follow, not a half-built success.
        return { kind: "unavailable" };
      }
      case 400:
        return { kind: "rejected" };
      case 403:
        return { kind: "permanent" };
      case 409:
        return { kind: "conflict" };
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
