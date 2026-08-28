import type { ApiFetch } from "../profile/api";

/** The result of one email verification attempt. */
export type VerifyEmailOutcome =
  | { readonly kind: "verified" } // 204 — the address is now attached
  | { readonly kind: "link-dead" } // 400 — unknown, expired or already used; indistinguishable
  | { readonly kind: "address-taken" } // 409 — already verified to another player
  | { readonly kind: "failed" }; // anything else, or a fetch that rejected

/**
 * Verifies a recovery email address using a token from a mailed link.
 *
 * The token is passed in the request body and never reaches a path, query,
 * header or log. A token in a URL leaks through referrer headers, browser
 * history, proxy records and access logs; the fragment-only approach of
 * `ADR-0081` keeps it from the server entirely, and this function keeps it
 * from storage and logs as well.
 *
 * The `400` response covers three indistinguishable server states — unknown,
 * expired or already-consumed — because `ADR-0031` §5 makes them one answer
 * on purpose: the client cannot tell a live token from a spent one, and it
 * must not learn.
 */
export async function verifyEmail(request: {
  readonly fetch: ApiFetch;
  readonly token: string;
}): Promise<VerifyEmailOutcome> {
  try {
    const response = await request.fetch("/api/auth/verify-email", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        token: request.token,
      }),
    });

    if (response.status === 204) {
      return { kind: "verified" };
    }

    if (response.status === 400) {
      return { kind: "link-dead" };
    }

    if (response.status === 409) {
      return { kind: "address-taken" };
    }

    return { kind: "failed" };
  } catch {
    // A fetch that rejects is treated as a failed verification.
    return { kind: "failed" };
  }
}
