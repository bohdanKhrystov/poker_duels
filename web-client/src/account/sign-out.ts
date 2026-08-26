import type { ApiFetch } from "../profile/api";
import {
  forgetSessionToken,
  readSessionToken,
} from "../protocol/session-token";
import { forgetRoomCode } from "../protocol/room-memory";

/** The result of one sign-out attempt. */
export type SignOutOutcome =
  { readonly kind: "signed-out" } | { readonly kind: "not-signed-in" }; // no token held: nothing was sent

/**
 * Ends the session this browser holds, locally, whatever the server answers.
 *
 * With no token in storage there is nothing to sign out of: no request is
 * sent, `reload` never runs, and the outcome is `not-signed-in`. With a
 * token, `POST /api/auth/sign-out` carries `Authorization: Bearer <token>`
 * and no body — `docs/protocol.md` *Sign out* gives this endpoint no
 * `X-Device-Id` fallback. `204` is the only documented status, answered
 * whether or not a session row existed, so the local half never branches on
 * it: a rejected `fetch` and any non-`204` status run exactly the forgetting
 * a `204` would, because a browser that kept a token the server has already
 * deleted is a defect no later screen can detect.
 *
 * Only `forgetSessionToken` and `forgetRoomCode` run — `ADR-0030` §8: sign-out
 * clears the token and only the token. The room code goes too, because
 * `ADR-0072` remembers a room only "until the player leaves it" and
 * `ADR-0030` §6 says signing out abandons the seat; a code the next boot
 * rejoined under a different identity would be refused by the server and
 * show that new player a refusal about a room they were never in. The device
 * id is never read, written or cleared here: `ADR-0030` §8 makes it
 * write-once for the life of the browser, and clearing it would abandon the
 * anonymous profile it names.
 *
 * `reload` is injected, for `sign-in.ts`'s reason: a module that reached for
 * the real navigation API itself would be untestable under jsdom, and a full
 * reload is the only boundary that rebuilds `initialState()`, leaving behind
 * the three presence fields `ADR-0075`'s Consequences name —
 * `rivalPresence`, `graceRemainingMillis` and `rivalReturned` — that no store
 * action clears. It runs once, after the local forgetting is already done,
 * so a browser that reloads mid-way never re-boots holding the old token.
 */
export async function signOut(request: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
  readonly reload: () => void;
}): Promise<SignOutOutcome> {
  const token = readSessionToken(request.storage);
  if (token === null) {
    return { kind: "not-signed-in" };
  }

  try {
    await request.fetch("/api/auth/sign-out", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
  } catch {
    // The local half below runs whatever the server answered, including a
    // fetch that rejected outright — see the function doc comment.
  }

  forgetSessionToken(request.storage);
  forgetRoomCode(request.storage);
  request.reload();

  return { kind: "signed-out" };
}
