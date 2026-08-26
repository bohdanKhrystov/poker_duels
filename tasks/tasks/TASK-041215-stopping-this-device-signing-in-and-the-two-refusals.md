---
schema: 2
id: TASK-041215
title: Stopping this device signing in, and the two refusals that are not failures
type: task
status: ready
parent: STORY-0412
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, auth]
depends_on: [TASK-041214]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/revoke-device.test.ts 2>&1 | grep -qE 'Tests +6 passed \(6\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the session it revoked with, because that is the one the player is standing on'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'tells a profile with no password apart from a caller with no session'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'presents the session and never the device it is revoking'
  - cd web-client && npm run check
---

## Goal

`DELETE /api/me/device` has a client, it presents the session and never the device it is ending, and
it keeps the caller signed in here — which is the whole promise `ADR-0050` §2 makes to the player
standing on the screen.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/revoke-device.ts` | create |
| `web-client/src/account/revoke-device.test.ts` | create |

Read, and do not edit: `docs/protocol.md` *Revoke this device*;
[`ADR-0050`](../../docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md) §§1–2;
[`ADR-0049`](../../docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md) §5.

## Scope

- One export:

  ```ts
  export type RevokeOutcome =
    | { readonly kind: "revoked" }        // 204
    | { readonly kind: "no-session" }     // 401
    | { readonly kind: "no-credential" }  // 409
    | { readonly kind: "failed" };        // anything else, or a fetch that rejected

  export async function revokeThisDevice(request: {
    readonly fetch: ApiFetch;
    readonly storage: Storage;
  }): Promise<RevokeOutcome>;
  ```

- `DELETE`, `Authorization: Bearer <token>`, **no body**, and **no `X-Device-Id`**: presenting a
  device id alone answers `401`, and the device being ended is identified by the server from the
  session, never by anything the client sends.
- No token held ⇒ **no request**, `no-session`.
- **The token is not cleared on success.** `ADR-0050` §2: the revoking session survives by
  construction, and signing a player out of the screen they used to secure their account is the
  hostile behaviour `ADR-0037` refused. This module writes to storage **not at all**.
- **No reload.** Unlike sign-in and sign-out, identity does not change here — the same player, one
  fewer route — so the socket is still theirs. The screen re-reads what it needs; this module returns
  an outcome.
- `409` is `no-credential` and is a **refusal, not a failure**: `ADR-0037` offers revocation only
  when another route exists, so a profile with no password is refused rather than stranded.
- KDoc naming each status and the ADR that fixed it, and stating that `204` is uniform — live,
  already revoked and never bound are one answer (`ADR-0049` §5), so no client may report which.

## Out of scope

- **Deciding whether to offer the control.** `TASK-041220` reads `deviceRouteLive` and whether a
  session is held; this module is what happens when it is pressed.
- **Reporting which of the three `204` cases happened.** `ADR-0049` §5 made the answer uniform on
  purpose. **A refusal, not an omission** — the outcome union has one `revoked`.
- **Any second action.** `ADR-0050` §5 refuses a standalone *sign out everywhere*, a per-session
  list, device names and a *last used* column, each already refused twice.
- Undoing it. Revocation is permanent by `ADR-0049` §2 — not by this route, not by any other.

## Tests

`web-client/src/account/revoke-device.test.ts`, describe block `"stopping this device signing in"`.

| Test | Proves |
| --- | --- |
| `presents the session and never the device it is revoking` | With **both** a token and a device id in storage: the recorded request has method `DELETE`, path `/api/me/device`, `Authorization: "Bearer tok-5"`, **no** `X-Device-Id` key and no body |
| `keeps the session it revoked with, because that is the one the player is standing on` | After a `204`, `readSessionToken` still answers the same token and `readDeviceId` still answers the same id. `ADR-0050` §2's promise, asserted on both keys |
| `tells a profile with no password apart from a caller with no session` | `409` gives `no-credential` and `401` gives `no-session`, in one test, asserted **not equal** to each other. Two refusals the screen must say different things about |
| `treats a broken server as a failure and not as a refusal` | `500` and a rejected `fetch` are each `failed`, and `failed` is not equal to either refusal — all four compared in one test |
| `asks nothing of the server when no session is held` | Empty storage: `no-session`, and the recording double was never called |
| `reports one answer for a live binding and an already dead one` | Two `204` responses give deeply equal outcomes, and the module exports no kind that could distinguish them. `ADR-0049` §5's uniform answer, gated rather than trusted |

Six tests in a new file: `npm run test -- src/account/revoke-device.test.ts` reports **6**.

## Acceptance criteria

- [ ] `stopping this device signing in > presents the session and never the device it is revoking`
      passes, asserting method, path, header, the absent header **and** the absent body
- [ ] `stopping this device signing in > keeps the session it revoked with, because that is the one
      the player is standing on` passes, asserting both storage keys
- [ ] `stopping this device signing in > tells a profile with no password apart from a caller with no
      session` passes, including the inequality
- [ ] `stopping this device signing in > treats a broken server as a failure and not as a refusal`
      passes over all four comparisons
- [ ] `stopping this device signing in > asks nothing of the server when no session is held` passes
      with a call count of `0`
- [ ] `stopping this device signing in > reports one answer for a live binding and an already dead
      one` passes
- [ ] `grep -cE 'forgetSessionToken|writeSessionToken|writeDeviceId|reload' web-client/src/account/revoke-device.ts`
      returns `0`
- [ ] `npm run test -- src/account/revoke-device.test.ts` reports `Tests  6 passed (6)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Call `forgetSessionToken(storage)` after a `204`.
   **`keeps the session it revoked with…` reddens** on the token assertion alone. Run it: *revoking
   signs you out* is what most systems do and is what `ADR-0037` calls hostile, and it is one line.
   Revert.
2. Send `X-Device-Id` alongside the bearer.
   **`presents the session and never the device it is revoking` reddens**, and only because the
   fixture stores a device id. Empty the storage in that test and watch it pass — a fixture blind to
   the value cannot see the leak.
3. Map `409` to `failed`.
   **`tells a profile with no password apart from a caller with no session` reddens** on the `409`
   row and on the inequality, and `treats a broken server as a failure and not as a refusal` reddens
   on its inequality too. Two tests, which is what separates *the product refused me* from *the
   product broke*.
4. Return `no-session` for `409` as well.
   **`tells a profile with no password apart from a caller with no session` reddens on the
   inequality only** — both rows individually still look plausible. The inequality assertion is the
   only thing in this file that catches a collapsed mapping.
5. Use `POST` instead of `DELETE`.
   **`presents the session and never the device it is revoking` reddens** on the method. Without that
   assertion nothing here would notice, because the double answers whatever it is told to.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
