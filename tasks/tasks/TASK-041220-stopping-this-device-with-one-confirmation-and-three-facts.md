---
schema: 2
id: TASK-041220
title: Stopping this device signing in, offered only where it is safe, with three facts first
type: task
status: backlog
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, ui]
depends_on: [TASK-041219]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/RevokeControl.test.tsx 2>&1 | grep -qE 'Tests +7 passed \(7\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers nothing to a browser that is not signed in'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states all three facts before it acts, and acts on nothing until it is confirmed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers nothing once the device route is already gone'
  - cd web-client && npm run check
---

## Goal

`ADR-0037`'s revoke path exists on the screen it was promised on: offered only where another route
into the profile exists, stating three facts before it acts, and never using the word *revoke*.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/RevokeControl.tsx` | create |
| `web-client/src/account/RevokeControl.test.tsx` | create |

Read, and do not edit:
[`ADR-0050`](../../docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md) §2, §3;
[`ADR-0037`](../../docs/adr/ADR-0037-the-device-is-a-credential-until-revoked.md);
`web-client/src/account/account-text.ts`; `web-client/src/account/revoke-device.ts`.

## Scope

- One export:

  ```ts
  export function RevokeControl(props: {
    readonly deviceRouteLive: boolean;
    readonly signedIn: boolean;
    readonly revoke: () => Promise<RevokeOutcome>;
  }): ReactElement | null;
  ```

- **It renders `null` unless `signedIn && deviceRouteLive`.** Both conditions are `ADR-0037`'s rule:
  offered only while the device route is live, and only when a credential exists. *A credential
  exists* is `signedIn`, because sign-in is the only endpoint in `docs/protocol.md` that issues a
  session token — the same derivation `TASK-041217` records, and the reason `ADR-0050` §4 could say
  the screen needs no new server fact.
- Pressing `REVOKE_LABEL` reveals **one confirmation step** stating `REVOKE_PERMANENT`,
  `REVOKE_OTHER_SESSIONS` and `REVOKE_ONLY_WAY_BACK` — `ADR-0050` §3's three facts, all three, in one
  place — with a confirming control and `CANCEL`. The call is made **only** by the confirming
  control.
- It is an in-page step, never `window.confirm`: three facts do not fit in a browser dialog, and
  `EPIC-06` cannot style one.
- After a `revoked`, the control renders the revoked state and offers nothing (`ADR-0050` §3). A
  `no-credential` and a `failed` leave the control where it was and say so once.
- **No count and no list of other sessions** (`ADR-0050` §3): a count needs a field that does not
  exist and tells a player nothing they can act on.

## Out of scope

- **Signing the caller out.** `ADR-0050` §2: the revoking session survives, and `revoke-device.ts`
  writes to storage not at all. **A refusal, not an omission** — a criterion asserts the token
  survives.
- **A standalone *sign out everywhere*.** `ADR-0050` §5 refuses a second button, a second endpoint
  and a flag on this one.
- **Any sentence about a recovery email**, for `TASK-041211`'s reason: it needs `hasRecoveryEmail`,
  which is `STORY-0417`'s.
- Re-reading the profile after a success. The control renders its own outcome; the next boot reads
  the field.

## Tests

`web-client/src/account/RevokeControl.test.tsx`, describe block `"stopping this device signing in"`.

| Test | Proves |
| --- | --- |
| `offers nothing to a browser that is not signed in` | `signedIn: false, deviceRouteLive: true` renders nothing at all — no label, no confirmation. `ADR-0037`'s *only when a credential exists*, which is the story's own criterion |
| `offers nothing once the device route is already gone` | `signedIn: true, deviceRouteLive: false` renders nothing. The **other** half of the guard, and a separate test because one condition passing is what a single fixture proves |
| `offers the control where both routes are live` | `signedIn: true, deviceRouteLive: true` renders `REVOKE_LABEL` and no confirmation yet |
| `states all three facts before it acts, and acts on nothing until it is confirmed` | Pressing the label shows `REVOKE_PERMANENT`, `REVOKE_OTHER_SESSIONS` **and** `REVOKE_ONLY_WAY_BACK`, and the double's call count is `0`. Three presences and a count, in one test |
| `calls once, and only from the confirming control` | Confirming calls the double exactly once; pressing `CANCEL` instead leaves the count at `0` and puts the label back |
| `offers nothing more once the device route has been stopped` | After a `revoked` outcome, `REVOKE_LABEL` is gone and `DEVICE_ROUTE_REVOKED` is what is on screen |
| `says so and stays offered when the server refuses` | A `no-credential` and a `failed` each leave `REVOKE_LABEL` on screen and render **different** sentences, asserted not equal |

Seven tests in a new file: `npm run test -- src/account/RevokeControl.test.tsx` reports **7**.

## Acceptance criteria

- [ ] `stopping this device signing in > offers nothing to a browser that is not signed in` passes
- [ ] `stopping this device signing in > offers nothing once the device route is already gone` passes
- [ ] `stopping this device signing in > offers the control where both routes are live` passes
- [ ] `stopping this device signing in > states all three facts before it acts, and acts on nothing
      until it is confirmed` passes, asserting **all three** sentences and a call count of `0`
- [ ] `stopping this device signing in > calls once, and only from the confirming control` passes for
      both the confirm and the cancel path
- [ ] `stopping this device signing in > offers nothing more once the device route has been stopped`
      passes
- [ ] `stopping this device signing in > says so and stays offered when the server refuses` passes,
      including the inequality
- [ ] `grep -cEi 'window\.confirm|revoke this|\bdevices\b' web-client/src/account/RevokeControl.tsx`
      returns `0`
- [ ] `npm run test -- src/account/RevokeControl.test.tsx` reports `Tests  7 passed (7)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Guard on `deviceRouteLive` alone, dropping `signedIn`.
   **`offers nothing to a browser that is not signed in` reddens alone.** Every other test in the
   file sets `signedIn: true`, so this is the one fixture that can see it — and a `409` from the
   server is the only other thing that would, after the player has already pressed a button they
   should never have been shown. Revert.
2. Guard on `signedIn` alone, dropping `deviceRouteLive`.
   **`offers nothing once the device route is already gone` reddens alone.** Two mutations, two
   tests, one each: a single combined test would have caught both and told you neither.
3. Call `revoke()` from the first press and treat the confirmation as an acknowledgement afterwards.
   **`states all three facts before it acts, and acts on nothing until it is confirmed` reddens** on
   the call count, and `calls once, and only from the confirming control` reddens on the cancel path.
   The three sentences still render, which is why the count is in that test.
4. Render only `REVOKE_PERMANENT` in the confirmation.
   **`states all three facts…` reddens on two of three assertions.** Run it: the permanent fact is
   the one everybody remembers and the *other sessions* fact is the one `ADR-0050` was written to
   add.
5. Clear the session token after a `revoked`.
   **Nothing in this file reddens** — the control's own tests never look at storage. It reddens
   `TASK-041215`'s `keeps the session it revoked with…`, in another file, only if the call is made
   through the real module. Record it: this component's suite does not gate `ADR-0050` §2, and
   `revoke-device.ts` is where that lives.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
