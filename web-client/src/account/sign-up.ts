import { readDeviceId } from "../protocol/device-id";
import type { ApiFetch } from "../profile/api";
import { signIn } from "./sign-in";

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
 * The follow-up sign-in below never reloads on this function's behalf. Claiming a
 * profile only needs the resulting token in storage; forcing a navigation from inside
 * this call would be a product decision `signUp` does not own.
 */
const noReload = (): void => {};

/**
 * Sends the handle and password a player typed to `POST /api/auth/sign-up`, exactly as typed.
 *
 * The server folds the handle (`ADR-0031` §1) and stores the password as-is
 * (`ADR-0048` §1); this function sends the raw strings and reports whichever of
 * the seven outcomes the response maps to. It sends at most one request: no device id
 * means no request, and no response — success or failure — is ever retried.
 *
 * A `201` issues no session of its own (`docs/protocol.md`'s sign-up row), so this
 * function signs the browser in right afterwards with the same handle and password —
 * one `POST /api/auth/sign-in`, never retried — which is how a browser that just
 * claimed a profile ends up holding the token `signedIn` reads. The credential already
 * exists once the `201` lands, so the outcome here is `signed-up` whether or not that
 * follow-up succeeds; reporting anything else would tell the player their claim failed
 * when it did not. A refused claim (`400`, `409`, `422`, `401`, `429`, or anything else)
 * never reaches the follow-up at all — there is no credential yet to sign in to, and
 * `ADR-0056` §3 forbids this client retrying anything on its own.
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
        // The credential exists now; whatever this answers, it does not undo that.
        await signIn({
          fetch: request.fetch,
          storage: request.storage,
          reload: noReload,
          handle: request.handle,
          password: request.password,
        });
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
