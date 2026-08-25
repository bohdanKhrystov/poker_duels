---
schema: 2
id: TASK-041214
title: Sign-out clears the token and only the token, leaves the room, and reloads
type: task
status: backlog
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, auth]
depends_on: [TASK-041213]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/sign-out.test.ts 2>&1 | grep -qE 'Tests +6 passed \(6\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'clears the token and leaves the device id exactly where it was'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'forgets the room this tab remembered, because it is somebody else now'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'clears the token even when the server never answers'
  - cd web-client && npm run check
---

## Goal

`POST /api/auth/sign-out` has a client that ends the session locally whatever the server says, takes
the device id with it under no circumstances, and does not leave a stale room code for the next
identity to rejoin with.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/sign-out.ts` | create |
| `web-client/src/account/sign-out.test.ts` | create |

Read, and do not edit: `docs/protocol.md` *Sign out*;
[`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §6 and §8;
[`ADR-0072`](../../docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md);
`web-client/src/protocol/room-memory.ts`; `web-client/src/account/sign-in.ts`.

## Scope

- One export:

  ```ts
  export type SignOutOutcome =
    | { readonly kind: "signed-out" }
    | { readonly kind: "not-signed-in" };  // no token held: nothing was sent

  export async function signOut(request: {
    readonly fetch: ApiFetch;
    readonly storage: Storage;
    readonly reload: () => void;
  }): Promise<SignOutOutcome>;
  ```

- No token held ⇒ **no request**, `not-signed-in`, and no reload. There is nothing to sign out of.
- With a token: `POST /api/auth/sign-out` carrying `Authorization: Bearer <token>` and **no body**.
  `docs/protocol.md` gives it no `X-Device-Id` fallback and no request body.
- **The local half runs whatever the server answered**, including a rejected `fetch`: the token is
  forgotten, the room memory is forgotten, and `reload()` is called. `204` is the only documented
  status and it is answered whether or not a row existed, so branching on it would leave a browser
  holding a dead token because the network flickered.
- **`forgetSessionToken` and `forgetRoomCode`, and nothing else.** `ADR-0030` §8: sign-out clears the
  token and only the token — the room code goes because `ADR-0072` remembers a room *"until the
  player leaves it"* and `ADR-0030` §6 says signing out abandons the seat. A remembered code the next
  boot rejoins under a different identity is refused by the server and shows the new player a refusal
  about a room they were never in.
- The stored device id is untouched. This module must not import `writeDeviceId` and must not name
  `DEVICE_ID_STORAGE_KEY`.
- `reload` is injected, for `sign-in.ts`'s reason and with the same `ADR-0075` comment.

## Out of scope

- **Closing the socket by hand.** The reload closes it, and it is the only form of *close and
  reconnect* that also rebuilds `initialState()`.
- **The confirmation the player sees before this runs.** `TASK-041221` owns it; this module acts when
  it is called.
- **Signing out on every device.** `ADR-0050` §5 refuses a standalone *sign out everywhere*: v0.2
  ships one action on this screen and revocation is the one that ends other sessions.
- **Any opinion about a live duel.** The account screen cannot be on screen while a frame has seated
  this tab (`ADR-0076` §3), so this module has no duel state to consult. `TASK-041221`'s confirmation
  states the cost.

## Tests

`web-client/src/account/sign-out.test.ts`, describe block `"signing out"`.

| Test | Proves |
| --- | --- |
| `clears the token and leaves the device id exactly where it was` | After a `204`: `readSessionToken` is `null` and `readDeviceId` answers what the storage held before. The third of `STORY-0412`'s four device-id assertions |
| `forgets the room this tab remembered, because it is somebody else now` | A storage holding a room code has none afterwards, asserted through `readRoomCode` |
| `presents the session and no device id` | The recorded headers carry `Authorization: "Bearer tok-3"` and **no** `X-Device-Id` key, with a device id in the storage so the test can see the leak. No body is sent |
| `clears the token even when the server never answers` | With a `fetch` that rejects: the outcome is `signed-out`, the token is gone, the room is gone and `reload` was called once. The same for a `500`, in the same test — **two failures asserted together** |
| `asks nothing of the server when no session is held` | Empty storage: outcome `not-signed-in`, the recording double never called, `reload` never called, and the device id still in place |
| `reloads once, after the local half is done` | `reload` is called exactly once, and `readSessionToken` already answers `null` at the moment it is called — asserted from inside the `reload` double |

Six tests in a new file: `npm run test -- src/account/sign-out.test.ts` reports **6**.

## Acceptance criteria

- [ ] `signing out > clears the token and leaves the device id exactly where it was` passes,
      asserting both keys
- [ ] `signing out > forgets the room this tab remembered, because it is somebody else now` passes
- [ ] `signing out > presents the session and no device id` passes, with a device id in the storage
      and no body on the request
- [ ] `signing out > clears the token even when the server never answers` passes for **both** the
      rejected fetch and the `500`
- [ ] `signing out > asks nothing of the server when no session is held` passes with a call count of
      `0` and the device id intact
- [ ] `signing out > reloads once, after the local half is done` passes, asserting the ordering from
      inside the double
- [ ] `grep -cE 'writeDeviceId|DEVICE_ID_STORAGE_KEY|storage\.clear' web-client/src/account/sign-out.ts`
      returns `0`
- [ ] `npm run test -- src/account/sign-out.test.ts` reports `Tests  6 passed (6)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Clear the token only on a `204`.
   **`clears the token even when the server never answers` reddens** on both halves. Run it: it is
   the natural reading of the endpoint, and it leaves a browser signed in with a token the server has
   already deleted, which no later screen can detect. Revert.
2. Call `storage.clear()` instead of the two forgets.
   **`clears the token and leaves the device id exactly where it was` reddens** on the device id.
   Nothing else in the file moves — the token and the room really are gone. That single-word
   implementation abandons the player's anonymous profile permanently, which is `ADR-0012`'s named
   harm, and it is the reason the device id is asserted in a sign-out test at all.
3. Leave the room code in place.
   **`forgets the room this tab remembered, because it is somebody else now` reddens alone.** Nothing
   about the session moves. Worth running to see that no other test in this story would have caught
   it.
4. Call `reload()` before forgetting the token.
   **`reloads once, after the local half is done` reddens**, and only because the assertion runs
   inside the double. A test that checked the call count after `await` would pass under this
   mutation, and the real browser would re-boot with the old token still in storage.
5. Send `X-Device-Id` alongside the bearer.
   **`presents the session and no device id` reddens alone**, and only because the fixture stores a
   device id. Empty that storage first and watch it pass.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
