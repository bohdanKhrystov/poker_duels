import type { ApiFetch } from "../profile/api";

/** The result of one password reset attempt. */
export type ResetPasswordOutcome =
  | { readonly kind: "reset" } // 204 — the password is changed and every session is gone
  | { readonly kind: "link-dead" } // 400 — unknown, expired or already used; indistinguishable
  | { readonly kind: "password-refused" } // 422 — under 8 or over 128 code points
  | { readonly kind: "failed" }; // anything else, or a fetch that rejected

/**
 * Resets a player's password using a token from a mailed link.
 *
 * The token and new password are passed in the request body and never
 * reach a path, query, header or log. A token in a URL leaks through
 * referrer headers, browser history, proxy records and access logs;
 * the fragment-only approach of `ADR-0081` keeps it from the server
 * entirely, and this function keeps it from storage and logs as well.
 *
 * The password is not normalized — no `.trim()` is applied — because
 * a transport that silently reshapes a credential is deciding something
 * that is not its to decide. The server validates the password policy
 * and returns `422` if it fails.
 *
 * `ADR-0080` §2 specifies that the password is judged before the token
 * is touched: a `422` is answered whether or not the token is good and
 * without the token being looked at. A `400` for a bad token is answered
 * only once the password has passed. A `422` and a `400` are answered in
 * a fixed order the caller cannot choose — password first, token second.
 *
 * A `204` response does not issue a session and returns no token;
 * the player must sign in afterwards, on this device too, and `ADR-0031`
 * §4 guarantees that every `auth_session` row for that player is deleted
 * in the same transaction, so the reset ends every other session but
 * issues none of its own.
 */
export async function resetPassword(request: {
  readonly fetch: ApiFetch;
  readonly token: string;
  readonly newPassword: string;
}): Promise<ResetPasswordOutcome> {
  try {
    const response = await request.fetch("/api/auth/reset-password", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        token: request.token,
        newPassword: request.newPassword,
      }),
    });

    if (response.status === 204) {
      return { kind: "reset" };
    }

    if (response.status === 400) {
      return { kind: "link-dead" };
    }

    if (response.status === 422) {
      return { kind: "password-refused" };
    }

    return { kind: "failed" };
  } catch {
    // A fetch that rejects is treated as a failed reset.
    return { kind: "failed" };
  }
}
