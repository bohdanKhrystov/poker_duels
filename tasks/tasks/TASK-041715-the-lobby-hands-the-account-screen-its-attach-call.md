---
schema: 2
id: TASK-041715
title: The lobby hands the account screen its attach call
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, account, recovery, wiring]
depends_on: [TASK-041714]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Tests +57 passed \(57\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx 2>&1 | grep -qE 'Tests +37 passed \(37\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the attach form on the account screen, wired to the account seam'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers no attach form where no account provider sits above'
  - test "$(grep -oF 'attachRecoveryEmail' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oF 'RecoveryEmailForm' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

The account screen's attach form is reachable in a running client: `Lobby.tsx` supplies
`attachRecoveryEmail` from `useAccount()`, on the same line as the three calls it already supplies.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read, and do not edit:

- `web-client/src/lobby/Lobby.tsx`'s merged `screen === "account"` branch — the three
  `account !== null ? account.X : undefined` props this adds a fourth to.
- `web-client/src/lobby/Lobby.test.tsx` lines 25–60 — the merged partial `vi.mock("../main", …)`
  with `importOriginal`. This ticket adds **no** `../main` import, so that factory is untouched.
- `web-client/src/account/AccountScreen.tsx` — the optional prop `TASK-041714` added.
- `web-client/src/account/recovery-text.ts` — `ATTACH_LABEL`, for querying the form.

## Scope

- **One prop on the merged `AccountScreen` element:**

  ```tsx
  attachRecoveryEmail={
    account !== null ? account.attachRecoveryEmail : undefined
  }
  ```

  Placed with the other three, in the same shape. Nothing else in the branch changes.
- **No new import from `../main`.** The call arrives through `useAccount()`, which `Lobby.tsx`
  already imports from `../account/account-provider`.
- **No new state, no effect, no navigation.** The lobby hands a function down; everything else is the
  screen's and the form's.

## Out of scope

- **Rendering the form here.** `AccountScreen` decides; a `verify:` line pins `RecoveryEmailForm` at
  zero occurrences in `Lobby.tsx`.
- **The verify and reset branches.** `TASK-041717` and `TASK-041719` add those, in this file, after
  this one lands — they are strictly ordered because they edit the same two files.
- **Touching `App.test.tsx`.** No `../main` binding is added, so its wholesale `vi.mock` needs none —
  a `verify:` line pins that file at 37.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, appended inside the existing `describe("the lobby")`.
**55 merged tests become 57.**

| Test | Proves |
| --- | --- |
| `puts the attach form on the account screen, wired to the account seam` | Render `Lobby` under an `AccountProvider` whose `attachRecoveryEmail` is a spy, with a profile in hand and the address at `#/account`. The form is on screen; type both fields and submit; the **spy** was called once with the two typed values. End to end from the address bar to the seam, which is the only thing this ticket can be wrong about |
| `offers no attach form where no account provider sits above` | The same render with **no** `AccountProvider`. The account screen is on screen — asserted by its heading — and the attach form is not. Presence before absence: without the heading assertion this passes for a lobby that never reached the account screen at all |

**No `try` anywhere in the added code, and no `expect()` inside one.** No test sleeps on a real
clock; the submit is awaited through `findBy…`.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts the attach form on the account screen, wired to the account seam'`
      — passes, with the spy asserted called once and with the two typed values
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers no attach form where no account provider sits above'`
      — passes, asserting the account screen's heading is present first
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE 'Tests +57 passed \(57\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly fifty-seven**: the 55 merged plus
      these two. The merged 55 are pinned by this **count**, never by their names. Both lines, because
      a collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx 2>&1 | grep -qE 'Tests +37 passed \(37\)'`
      — unmoved, because this diff adds no `../main` import. That file's `vi.mock("./main", …)` takes
      no `importOriginal` and has already forced three tickets in this epic; if it reddens, stop and
      report rather than editing it
- [ ] `test "$(grep -oF 'attachRecoveryEmail' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 2`
      — the one prop expression, and exactly two mentions: the JSX attribute name, fixed by
      `AccountScreen`'s prop, and the `account.attachRecoveryEmail` read. **Two, not one** —
      measured against the `## Scope` block above, and against the merged
      `signUp={account !== null ? account.signUp : undefined}` line it copies, which scores two for
      `signUp`. One is unreachable while the prop is supplied at all — every way of reading the
      function off the seam names it. A **third** is the thing to refuse: a lambda around the call,
      which this ticket does not write
- [ ] `test "$(grep -oF 'RecoveryEmailForm' web-client/src/lobby/Lobby.tsx | wc -l | tr -d ' ')" = 0`
      — the lobby does not render the form itself
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every merged test in `Lobby.test.tsx` passes unchanged. No assertion moves and none is weakened
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget.

1. **Pass `undefined` unconditionally.** Predict: `puts the attach form on the account screen, wired
   to the account seam` reddens on the form being absent.
2. **Pass a fresh arrow that swallows the call** — `attachRecoveryEmail={async () => ({ kind:
   "accepted" })}`. Predict: the form renders and the **spy assertion** reddens while the presence
   assertion stays green. That is the mutation a presence-only test cannot see, and it is why the
   first test drives the form rather than merely finding it.
3. **Swap the two typed values** in the seam call. Predict: the same test reddens on the arguments.
   Use two different fixture literals or this mutation is invisible.
4. **Drop the `account !== null` guard**, with a non-null assertion. Predict: `offers no attach form
   where no account provider sits above` reddens — record whether as an absence failure or as a thrown
   error, because a crash and a withheld form fail differently.

> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why this is a ticket of its own rather than a fourth file on `TASK-041714`.** The screen's optional
prop is green with nothing supplying it, and the lobby's prop is green with the screen already
merged. No gate holds the two together — a set of files no gate holds together is a split, not an
`atomic:` (`ADR-0068`, `ADR-0070`). `TASK-041231` was cut the same way for the same reason.

**Three tickets in this story edit `Lobby.tsx`, and they are strictly ordered for that reason alone.**
This one, then `TASK-041717`, then `TASK-041719`. Their subjects are independent; their diffs are not.
