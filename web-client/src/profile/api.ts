/** The part of a `Response` this module reads. */
export interface ApiResponse {
  readonly status: number;
  json(): Promise<unknown>;
}

/** The part of `fetch` this module uses, injected so no test reaches the network. */
export type ApiFetch = (
  path: string,
  init: { readonly headers: Readonly<Record<string, string>> },
) => Promise<ApiResponse>;

/** What one read under `/api/me` came back with. */
export type ApiRead =
  | { readonly kind: "body"; readonly body: unknown }
  | { readonly kind: "no-profile" }
  | { readonly kind: "unavailable" };

/**
 * Reads a profile from the API endpoint `/api/me` with device id authentication.
 *
 * If no device id is available, returns no-profile without making a request.
 * Otherwise sends a GET request with the device id in the X-Device-Id header.
 *
 * Converts responses into one of three outcomes:
 * - 200: returns the parsed JSON body
 * - 401: returns no-profile (device id is absent, blank, or unknown)
 * - other status or network error: returns unavailable
 */
export async function readFromApi(request: {
  readonly fetch: ApiFetch;
  readonly deviceId: string | null;
  readonly path: string;
}): Promise<ApiRead> {
  // If no device id, return no-profile without calling fetch.
  // A browser that has never held an id is the first visit, and the server
  // would answer 401; asking is a round trip whose answer is already known.
  if (request.deviceId === null) {
    return { kind: "no-profile" };
  }

  try {
    const response = await request.fetch(request.path, {
      headers: {
        "X-Device-Id": request.deviceId,
      },
    });

    if (response.status === 200) {
      const body = await response.json();
      return { kind: "body", body };
    }

    if (response.status === 401) {
      return { kind: "no-profile" };
    }

    return { kind: "unavailable" };
  } catch {
    // A fetch that rejects, or a json() that rejects becomes unavailable.
    // Caught, never rethrown: a lobby must not fall over because a profile
    // read did.
    return { kind: "unavailable" };
  }
}
