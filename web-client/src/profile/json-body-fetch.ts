import type { ApiFetch } from "./api";

/**
 * Wraps an API fetch to add `Content-Type: application/json` when, and only
 * when, the request carries a body, leaving all other headers untouched.
 *
 * Without this header `fetch` labels a body-carrying request
 * `text/plain;charset=UTF-8`, Ktor's `call.receive<T>()` refuses to decode
 * it, and the caller sees an empty-bodied `400` — every write in the
 * product failed for exactly this reason before this wrapper existed.
 *
 * A caller that already set its own `Content-Type` keeps it: this wrapper
 * adds the header, it never overrules one already present.
 *
 * @param fetch The underlying fetch implementation to wrap.
 * @returns A new fetch function that labels body-carrying requests as JSON.
 */
export function jsonBodyFetch(fetch: ApiFetch): ApiFetch {
  return async (path, init) => {
    // No body, no label: a GET carries none, and must reach the network
    // exactly as the caller built it.
    if (init.body === undefined) {
      return fetch(path, init);
    }

    // Caller's headers spread last, so a content type the caller already
    // set overrules this default rather than being overrun by it.
    const headers = {
      "Content-Type": "application/json",
      ...init.headers,
    };

    return fetch(path, {
      ...init,
      headers,
    });
  };
}
