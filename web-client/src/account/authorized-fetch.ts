import type { ApiFetch } from "../profile/api";
import { readSessionToken } from "../protocol/session-token";

/**
 * Wraps an API fetch to add `Authorization: Bearer <token>` header when
 * this browser holds a session token, leaving all other headers untouched.
 *
 * Reads the token on every call, not once, so a wrapper built at module scope
 * outlives a sign-out without keeping a signed-out browser signed in until
 * the next reload.
 *
 * **Must never wrap `POST /api/auth/sign-in`**, which is how a client obtains
 * authentication in the first place and carries no `Authorization` header.
 * Sign-up and other endpoints that use `X-Device-Id` authentication are also
 * excluded from this wrapper.
 *
 * @param fetch The underlying fetch implementation to wrap.
 * @param storage The storage to read the session token from.
 * @returns A new fetch function that adds the Authorization header when a token is held.
 */
export function authorizedFetch(fetch: ApiFetch, storage: Storage): ApiFetch {
  return async (path, init) => {
    const token = readSessionToken(storage);

    // If no token, pass the original headers through untouched
    if (token === null) {
      return fetch(path, init);
    }

    // If token exists, add Authorization header to the caller's headers
    const headers = {
      ...init.headers,
      Authorization: `Bearer ${token}`,
    };

    return fetch(path, {
      ...init,
      headers,
    });
  };
}
