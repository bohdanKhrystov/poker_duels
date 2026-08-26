import type { ApiFetch } from "../profile/api";
import { readSessionToken } from "../protocol/session-token";

/** The result of one attempt to revoke this device. */
export type RevokeOutcome =
  | { readonly kind: "revoked" } // 204: the device binding is now revoked (or was already revoked, or never existed)
  | { readonly kind: "no-session" } // 401: this browser holds no valid session token
  | { readonly kind: "no-credential" } // 409: `ADR-0037` offers revocation only when another route exists
  | { readonly kind: "failed" }; // 500, or a rejected `fetch`, or any other status

/**
 * Revokes this device as a route to the player's account, signing the player out
 * on every other device where a session exists.
 *
 * `DELETE /api/me/device` carries `Authorization: Bearer <token>` and no body —
 * `docs/protocol.md` *Revoke this device* gives this endpoint no `X-Device-Id`
 * or body. The device being ended is identified by the server from the session,
 * never by anything the client sends. With no token in storage, no request is sent
 * and the outcome is `no-session`.
 *
 * `204` is the only documented success status, answered whether or not a device
 * binding existed, whether it was live or already revoked — `ADR-0049` §5 makes
 * the answer uniform on purpose, so a client may never report which. `401` is a
 * session error, answered with no token in storage (though that is caught
 * before the request). `409` is a refusal, not a failure: `ADR-0037` offers
 * revocation only when another route exists, and a profile with no password is
 * refused rather than stranded. A rejected `fetch` and any non-documented status
 * are both treated as `failed`.
 *
 * **The token is not cleared on success.** `ADR-0050` §2: the revoking session
 * survives by construction, and signing a player out of the screen they are using
 * to secure their account is the hostile behaviour `ADR-0037` refused. This
 * module writes to storage **not at all**.
 */
export async function revokeThisDevice(request: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
}): Promise<RevokeOutcome> {
  const token = readSessionToken(request.storage);
  if (token === null) {
    return { kind: "no-session" };
  }

  try {
    const response = await request.fetch("/api/me/device", {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (response.status === 204) {
      return { kind: "revoked" };
    }

    if (response.status === 401) {
      return { kind: "no-session" };
    }

    if (response.status === 409) {
      return { kind: "no-credential" };
    }

    return { kind: "failed" };
  } catch {
    return { kind: "failed" };
  }
}
