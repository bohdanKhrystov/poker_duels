import type { ApiFetch } from "../profile/api";
import { writeSessionToken } from "../protocol/session-token";

/** The result of one sign-in attempt. */
export type SignInOutcome =
  | { readonly kind: "signed-in" } // 200, token stored
  | { readonly kind: "refused" } // 401 — unknown handle or wrong password, indistinguishable
  | { readonly kind: "failed" }; // 400, anything else, or a fetch that rejected

/**
 * Signs in with a handle and a password.
 *
 * The request carries no authentication of its own — no device id header,
 * no bearer token — because signing in is how a client obtains one in the
 * first place (`docs/protocol.md` *Sign in*). A wrong password and an
 * unknown handle both answer `401` with the same empty body, and this
 * function maps both to the single `refused` outcome, never retrying:
 * `ADR-0027` §6 makes an over-budget answer identical to a wrong secret, so
 * a retry would spend a budget this caller cannot see. Anything that tried
 * to tell the two cases apart — from timing, or from the shape of the
 * handle — would rebuild on the client the enumeration oracle the server
 * was built to close.
 *
 * A `200` whose body carries a `sessionToken` string is stored through
 * `writeSessionToken`; a `200` with no usable token stores nothing and is
 * `failed`. The device id already in storage is never read, written or
 * overwritten here (`ADR-0030` §8): a re-minted id abandons the profile it
 * used to name.
 *
 * On success, `reload` runs once. `reload` is injected rather than reached
 * for on a global directly — a module that called the real navigation API
 * itself would be untestable under jsdom, and would reach past the seam a
 * test can bind. Identity is fixed for the life of a socket, so picking up
 * a freshly-signed-in identity means replacing the socket, and a full
 * reload is the only boundary that rebuilds `initialState()`. `ADR-0075`'s
 * Consequences name `rivalPresence`, `graceRemainingMillis` and
 * `rivalReturned` as three presence fields no store action clears;
 * reconnecting in place after a sign-in would carry them across the
 * identity change instead of leaving them behind.
 */
export async function signIn(request: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
  readonly reload: () => void;
  readonly handle: string;
  readonly password: string;
}): Promise<SignInOutcome> {
  try {
    const response = await request.fetch("/api/auth/sign-in", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        handle: request.handle,
        password: request.password,
      }),
    });

    if (response.status === 401) {
      return { kind: "refused" };
    }

    if (response.status !== 200) {
      return { kind: "failed" };
    }

    const body = await response.json();
    const token =
      typeof body === "object" && body !== null && "sessionToken" in body
        ? (body as { readonly sessionToken: unknown }).sessionToken
        : undefined;

    if (typeof token !== "string") {
      return { kind: "failed" };
    }

    writeSessionToken(request.storage, token);
    request.reload();
    return { kind: "signed-in" };
  } catch {
    // A fetch that rejects, and a malformed 200 body that fails to parse,
    // both land here: neither carries a usable token, so both are failed.
    return { kind: "failed" };
  }
}
