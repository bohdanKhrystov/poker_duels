---
schema: 2
id: TASK-041221
title: Signing out asks first, and says what it costs before it acts
type: task
status: backlog
parent: STORY-0412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, account, ui]
depends_on: [TASK-041220]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/SignOutControl.test.tsx 2>&1 | grep -qE 'Tests +5 passed \(5\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'warns before it acts, and acts on nothing until it is confirmed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers nothing to a browser that is not signed in'
  - cd web-client && npm run check
---

## Goal

`STORY-0412`'s *signing out during a live duel warns before it acts* is kept, in the one form that is
reachable: the warning is stated on every sign-out, so it cannot be wrong about whether a duel is
live.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/SignOutControl.tsx` | create |
| `web-client/src/account/SignOutControl.test.tsx` | create |

Read, and do not edit:
[`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §6;
[`ADR-0076`](../../docs/adr/ADR-0076-a-screen-the-player-chose-has-an-address.md) §3;
`web-client/src/account/account-text.ts`; `web-client/src/account/RevokeControl.tsx` (the
confirmation shape to follow).

## Scope

- One export:

  ```ts
  export function SignOutControl(props: {
    readonly signedIn: boolean;
    readonly signOut: () => Promise<SignOutOutcome>;
  }): ReactElement | null;
  ```

- `null` unless `signedIn`. There is nothing to sign out of otherwise.
- Pressing `SIGN_OUT_LABEL` reveals **one confirmation step** stating `SIGN_OUT_WARNING`, with a
  confirming control and `CANCEL`. The call is made only by the confirming control, exactly as
  `RevokeControl` does — one shape for both confirmations on this screen.
- **The warning is unconditional**, and this is the deliberate reading of `STORY-0412`'s seventh
  criterion. The account screen cannot be on display while a frame has seated this tab: `Lobby.tsx`
  branches on `outcome`, then `view`, then `roomCode`, and only then on the address (`ADR-0076` §3).
  So a duel-conditional warning would be a branch no fixture in this client can reach, while an
  unconditional one is true in every state including the one the criterion names. Carry that
  reasoning as a comment — it is the difference between a refusal and an omission.
- `SIGN_OUT_WARNING` states the two costs `ADR-0030` §6 names: any duel room this browser is in is
  left and a duel left this way can be lost, and the browser goes back to the profile it had before.
- No second confirmation, no `beforeunload`, and no `window.confirm`.

## Out of scope

- **A warning that reads the store for a live duel.** The branch is unreachable through the address
  by `ADR-0076` §3, so it would be a fixture-only path. **A refusal, not an omission** — a criterion
  greps this component for any store import.
- **Closing the socket, clearing storage or reloading.** `sign-out.ts` owns all three.
- Signing out other devices. `ADR-0050` §5 refuses it.

## Tests

`web-client/src/account/SignOutControl.test.tsx`, describe block `"signing out"`.

| Test | Proves |
| --- | --- |
| `offers nothing to a browser that is not signed in` | `signedIn: false` renders nothing at all |
| `offers the control to a browser holding a session` | `signedIn: true` renders `SIGN_OUT_LABEL` and no warning yet |
| `warns before it acts, and acts on nothing until it is confirmed` | Pressing the label puts `SIGN_OUT_WARNING` on screen and leaves the double's call count at `0`. `STORY-0412`'s seventh criterion, in the form the address makes reachable |
| `calls once, and only from the confirming control` | Confirming calls the double exactly once; `CANCEL` leaves the count at `0` and puts the label back with the warning gone |
| `puts no browser dialog between the press and the act` | `window.confirm` is replaced by a counting double for the test: pressing and confirming leaves its count at `0`. The rule `Lobby.test.tsx` already enforces for the waiting screen's way back |

Five tests in a new file: `npm run test -- src/account/SignOutControl.test.tsx` reports **5**.

## Acceptance criteria

- [ ] `signing out > offers nothing to a browser that is not signed in` passes
- [ ] `signing out > offers the control to a browser holding a session` passes
- [ ] `signing out > warns before it acts, and acts on nothing until it is confirmed` passes,
      asserting the warning **and** a call count of `0`
- [ ] `signing out > calls once, and only from the confirming control` passes for both paths
- [ ] `signing out > puts no browser dialog between the press and the act` passes with a
      `window.confirm` count of `0`
- [ ] `grep -cE 'useDuelState|duel-provider|beforeunload' web-client/src/account/SignOutControl.tsx`
      returns `0`
- [ ] `npm run test -- src/account/SignOutControl.test.tsx` reports `Tests  5 passed (5)`
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

1. Call `signOut()` from the first press and show the warning afterwards.
   **`warns before it acts, and acts on nothing until it is confirmed` reddens** on the call count
   while the warning assertion still passes — the words appear either way, and only the count knows
   whether they came first. Revert.
2. Render the warning but wire the confirming control to nothing.
   **`calls once, and only from the confirming control` reddens** on the confirm half.
3. Replace the in-page step with `window.confirm(SIGN_OUT_WARNING)`.
   **`puts no browser dialog between the press and the act` reddens**, and `warns before it acts…`
   reddens too, because the sentence is no longer in the document. Two tests: one for the mechanism,
   one for the words being readable by a test at all.
4. Drop the `signedIn` guard.
   **`offers nothing to a browser that is not signed in` reddens alone.** A signed-out browser
   pressing sign-out reaches `not-signed-in` and does nothing, so it is harmless and still wrong —
   the screen would offer an action that cannot happen.
5. Make the warning conditional on a prop the component does not have — e.g. always render the label
   and never the warning.
   **`warns before it acts, and acts on nothing until it is confirmed` reddens on the warning.**
   Record what this shows: the unconditional warning is what this suite gates, and a duel-conditional
   one would need a fixture that `Lobby.tsx` can never produce.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
