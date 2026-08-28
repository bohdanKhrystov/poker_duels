import { readDeviceId } from "../protocol/device-id";
import type { ApiFetch } from "../profile/api";

/**
 * What one call to `attachRecoveryEmail` came back with.
 *
 * One outcome per status `POST /api/auth/recovery-email` can answer (`docs/protocol.md`),
 * plus `failed` for a status the client does not model and for a fetch
 * that rejected.
 */
export type AttachRecoveryOutcome =
  | { readonly kind: "accepted" } // 202 — address recorded for verification
  | { readonly kind: "address-refused" } // 400 — decode failed or not syntactically an address
  | { readonly kind: "no-profile" } // 401 — absent, blank or unknown identity
  | { readonly kind: "password-refused" } // 403 — the current password is wrong
  | { readonly kind: "failed" }; // anything else, or a fetch that rejected

/**
 * Sends an email address and the current password to `POST /api/auth/recovery-email`.
 *
 * `ADR-0031` §3 requires the current password even inside a valid session: a session token is a
 * bearer credential in web storage, so without this, a minute at an unattended browser converts
 * into permanent account ownership. This module never reads the session token — authorization
 * is added above by `authorizedFetch` if one is held.
 *
 * `ADR-0031` §5: a `202` says nothing — the server does not distinguish *address is new*, *address
 * already belongs to this player*, or *address belongs to another player but has not been verified
 * there* — and the client must not guess. The address may or may not receive a verification email.
 *
 * This function sends at most one request: no device id means no request, and no response —
 * success or failure — is ever retried. `ADR-0079` budgets this endpoint at five a minute and an
 * over-budget attempt still counts, so a retry spends a budget this caller cannot see.
 *
 * This module writes to storage **not at all**: no session token, no device id, no proof. It
 * reports whichever of the five outcomes the response maps to, and the caller must decide what to
 * show and what to store.
 */
export async function attachRecoveryEmail(request: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
  readonly address: string;
  readonly currentPassword: string;
}): Promise<AttachRecoveryOutcome> {
  const deviceId = readDeviceId(request.storage);
  // No device id, no request: the server would answer 401, and that answer
  // is already known without a round trip.
  if (deviceId === null) {
    return { kind: "no-profile" };
  }

  try {
    const response = await request.fetch("/api/auth/recovery-email", {
      method: "POST",
      headers: {
        "X-Device-Id": deviceId,
      },
      body: JSON.stringify({
        address: request.address,
        currentPassword: request.currentPassword,
      }),
    });

    switch (response.status) {
      case 202:
        return { kind: "accepted" };
      case 400:
        return { kind: "address-refused" };
      case 401:
        return { kind: "no-profile" };
      case 403:
        return { kind: "password-refused" };
      default:
        return { kind: "failed" };
    }
  } catch {
    // A fetch that rejects, or a json() that rejects, becomes failed.
    // Caught, never rethrown, and never retried on the player's behalf.
    return { kind: "failed" };
  }
}
