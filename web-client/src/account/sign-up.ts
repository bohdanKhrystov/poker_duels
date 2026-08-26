import { readDeviceId } from "../protocol/device-id";
import type { ApiFetch } from "../profile/api";

/**
 * What one call to `signUp` came back with.
 *
 * One outcome per status `POST /api/auth/sign-up` can answer (`docs/protocol.md`),
 * plus `failed` for a status the client does not model and for a fetch
 * that rejected.
 */
export type SignUpOutcome =
  | { readonly kind: "signed-up" } // 201 — credential created
  | { readonly kind: "handle-refused" } // 400 — handle fails the fold (`ADR-0031` §1)
  | { readonly kind: "unavailable-handle" } // 409 — handle taken or password exists (`ADR-0031` §2)
  | { readonly kind: "password-refused" } // 422 — password outside 8–128 (`ADR-0048` §1)
  | { readonly kind: "no-profile" } // 401 — absent, blank or unknown device id (`ADR-0012`)
  | { readonly kind: "throttled" } // 429 — rate-limited by address (`ADR-0056` §1)
  | { readonly kind: "failed" }; // anything else, or a fetch that rejected

/**
 * Sends the handle and password a player typed to `POST /api/auth/sign-up`, exactly as typed.
 *
 * The server folds the handle (`ADR-0031` §1) and stores the password as-is
 * (`ADR-0048` §1); this function sends the raw strings and reports whichever of
 * the seven outcomes the response maps to. It sends at most one request: no device id
 * means no request, and no response — success or failure — is ever retried.
 */
export async function signUp(request: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
  readonly handle: string;
  readonly password: string;
}): Promise<SignUpOutcome> {
  const deviceId = readDeviceId(request.storage);
  // No device id, no request: the server would answer 401, and that answer
  // is already known without a round trip.
  if (deviceId === null) {
    return { kind: "no-profile" };
  }

  try {
    const response = await request.fetch("/api/auth/sign-up", {
      method: "POST",
      headers: {
        "X-Device-Id": deviceId,
      },
      body: JSON.stringify({
        handle: request.handle,
        password: request.password,
      }),
    });

    switch (response.status) {
      case 201:
        return { kind: "signed-up" };
      case 400:
        return { kind: "handle-refused" };
      case 409:
        return { kind: "unavailable-handle" };
      case 422:
        return { kind: "password-refused" };
      case 401:
        return { kind: "no-profile" };
      case 429:
        return { kind: "throttled" };
      default:
        return { kind: "failed" };
    }
  } catch {
    // A fetch that rejects, or a json() that rejects, becomes failed.
    // Caught, never rethrown, and never retried on the player's behalf.
    return { kind: "failed" };
  }
}
