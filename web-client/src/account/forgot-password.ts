import type { ApiFetch } from "../profile/api";

/** The result of one forgot-password request. */
export type ForgotPasswordOutcome =
  | { readonly kind: "accepted" } // 202 — the request was accepted
  | { readonly kind: "failed" }; // any other status, or a fetch that rejected

/**
 * Requests a password reset link for an address.
 *
 * The request carries no authentication — no device id header, no bearer token —
 * because this endpoint serves unauthenticated callers. The endpoint answers `202`
 * in every case — regardless of whether the address is in storage, verified,
 * over budget, or a mail sender is configured — with an identical empty body.
 * A client that modelled more would rebuild on the client the enumeration oracle
 * the server was built to close (`ADR-0031` §5). The response is written before
 * any mail work and delivery runs on a detached coroutine, so latency does not
 * vary with the address — the timing side channel is closed.
 *
 * This function maps `202` to `accepted` and anything else to `failed`. The
 * `failed` outcome means the request did not reach the server — a network error,
 * or a non-202 status. Retrying is not done: `ADR-0079` puts an attempt budget
 * of ten per minute behind an answer that never changes, and an over-budget
 * attempt still counts against that limit.
 */
export async function forgotPassword(request: {
  readonly fetch: ApiFetch;
  readonly address: string;
}): Promise<ForgotPasswordOutcome> {
  try {
    const response = await request.fetch("/api/auth/forgot-password", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        address: request.address,
      }),
    });

    if (response.status === 202) {
      return { kind: "accepted" };
    }
  } catch {
    // A fetch that rejects answers failed and the returned promise does not reject.
  }

  return { kind: "failed" };
}
