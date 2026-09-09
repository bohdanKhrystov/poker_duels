import type { ApiFetch } from "../profile/api";
import { readDeviceId } from "../protocol/device-id";
import { readSessionToken } from "../protocol/session-token";

/**
 * Reads whether this browser's sign-out would hand its account a new, empty
 * profile rather than returning it to the one it already owns.
 *
 * With no session token in storage, no request is sent and the answer is
 * `false`. The route is session-required and answers `401` to a caller with
 * no session (`ADR-0135` §6), and `401` is a keep, so the short circuit and
 * the round trip agree. This is `revoke-device.ts`'s shipped shape, line for
 * line.
 *
 * With a token, this sends `Authorization: Bearer <token>` always, and
 * `X-Device-Id` only when this browser holds one — never an empty string,
 * and never a skipped request. `200` with a body whose
 * `signOutHandsANewProfile` is a `boolean` answers that boolean.
 * **Everything else answers `false`**: a `401`, any other status, a missing
 * field, a field that is not a `boolean`, a `json()` that rejects, and a
 * `fetch` that rejects all mean the same thing (`ADR-0135` §6): *"An absent
 * field, a `401`, an unavailable read, a rejected `fetch`, a read that has
 * not returned yet, and a client too old to ask all mean the same thing."*
 * `false` is the safe direction here — it is the branch that leaves the
 * browser owning the profile it already owns.
 *
 * **This must not be built on top of the shared `/api/me` reader in
 * `profile/api.ts`.** That helper answers "no profile" without making a
 * request at all when this browser holds no device id — which is exactly
 * the browser this route must answer `true` for. This module talks to
 * `fetch` directly instead.
 *
 * This module writes to storage **not at all**, exactly as
 * `revoke-device.ts` says of itself.
 */
export async function readDeviceStanding(request: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
}): Promise<boolean> {
  try {
    const token = readSessionToken(request.storage);
    if (token === null) {
      return false;
    }

    const deviceId = readDeviceId(request.storage);
    const headers: Record<string, string> = {
      Authorization: `Bearer ${token}`,
    };
    if (deviceId !== null) {
      headers["X-Device-Id"] = deviceId;
    }

    const response = await request.fetch("/api/me/device", { headers });

    if (response.status !== 200) {
      return false;
    }

    const body = await response.json();
    if (
      body !== null &&
      typeof body === "object" &&
      typeof (body as Record<string, unknown>).signOutHandsANewProfile ===
        "boolean"
    ) {
      return (body as { signOutHandsANewProfile: boolean })
        .signOutHandsANewProfile;
    }

    return false;
  } catch {
    return false;
  }
}
