---
schema: 2
id: TASK-041215
title: Stopping this device signing in, and the two refusals that are not failures
type: task
status: done
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

## Notes

**The first report reasoned about all five Proof steps instead of running them, and one of the five
was wrong.** Every step was submitted as *"would redden"* — a reading of the assertions, not a
measurement. Sent back and measured, mutation 3 (map `409` to `failed`) reddens **two** tests, not
one: `tells a profile with no password apart from a caller with no session` on its `no-credential`
assertion, and `treats a broken server as a failure and not as a refusal`, because a `409` mapped to
`failed` collides with `500` and trips that test's inequality.

The coder's earlier answer to *which test catches the two refusals collapsing* — *"the only test"* —
was wrong for the same reason. **Two claims corrected by five mutations.** Neither error was findable
by reading; this story has now produced four assertions that read correctly and gated nothing.

**Both tests earn their place, and the review established which property each holds.** `treats a
broken server…` is load-bearing for **failure**-distinctness — separating `500` and a rejected fetch
from the refusals — and is **not** a refusal-distinctness gate. It reddened under mutation 3 only
because `409` became `failed` and collided with `500`. A genuine collapse of the two refusals into
one non-`failed` kind is caught by `tells a profile with no password apart…` alone, via its explicit
`not.toEqual`. Neither is dominated.

**This is the one place in `STORY-0412` where two refusals must stay distinguishable.** Sign-in's
must not be (`ADR-0031`: a wrong password and an unknown handle are one answer); here a profile with
no password and a caller with no session are different facts about different things, and the ticket
says so. Worth stating because the two look like the same shape from a distance.

**A criterion with no gate behind it, and the third of this shape in the story.** The acceptance
criteria require `grep -cE 'forgetSessionToken|writeSessionToken|writeDeviceId|reload'` to return `0`
— revoking must not clear the session the player is standing on — but that grep is **not in the
`verify:` block**, so nothing enforces it. The strings are genuinely absent today. Compare
`TASK-041210`, whose criterion 5 was never in `verify:` and went unmet through two dispatches, and
`TASK-041634`, whose Scope demanded a comment nothing checked. **A criterion outside `verify:` is a
wish.**
