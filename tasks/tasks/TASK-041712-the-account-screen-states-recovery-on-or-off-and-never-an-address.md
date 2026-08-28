---
schema: 2
id: TASK-041712
title: The account screen states recovery on or off, and never an address
type: task
status: backlog
parent: STORY-0417
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, account, recovery, ui]
depends_on: [TASK-041704, TASK-041711]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/AccountScreen.test.tsx 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/account/AccountScreen.test.tsx 2>&1 | grep -qE 'Tests +9 passed \(9\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says recovery is on for one profile and off for another, in one render each'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says nothing about recovery when no profile is in hand'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders no address, because it is given none and asks for none'
  - test "$(grep -oF 'recoveryLine' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 2
  - test "$(grep -oiE 'RECOVERY_ON|RECOVERY_OFF' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 0
  - test "$(grep -oF 'hasRecoveryEmail ===' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 0
  - cd web-client && npm run check
---

## Goal

The account screen states *recovery is on* or *recovery is off* from the profile's own
`hasRecoveryEmail`, in both states, and renders no address — because it is handed none and there is
no endpoint that would give it one.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/AccountScreen.tsx` | modify |
| `web-client/src/account/AccountScreen.test.tsx` | modify |

Read, and do not edit:

- `web-client/src/account/recovery-text.ts` — `recoveryLine(has)`, `RECOVERY_ON`, `RECOVERY_OFF`.
  Call the function; do not restate the branch.
- `web-client/src/account/account-text.ts` — `deviceRouteLine`, and the merged line above the one
  this ticket adds. The pattern is identical and deliberate.
- `web-client/src/profile/profile-strip.ts` — `ProfileStripState`, and the `kind === "profile"` guard
  `AccountScreen` already applies before it states any fact.
- [`ADR-0031`](../../docs/adr/ADR-0031-an-optional-verified-recovery-email.md) §6.3 — the address is
  returned by no endpoint, so a screen that wanted to show it is asking for one this epic refuses to
  build.

## Scope

- **One more derived line, beside `deviceLine`, on exactly the same guard:**

  ```tsx
  const recoveryText =
    profile !== null && profile.kind === "profile"
      ? recoveryLine(profile.profile.hasRecoveryEmail)
      : null;
  ```

  With no profile in hand — loading, no-profile, or unavailable — the screen asserts **nothing**
  about recovery, for the reason the merged comment above `deviceLine` already gives: a sentence
  built from a read the client never got back is not a fact the client was told.
- **One more `<p className="text-small">` in the same block**, rendered only when `recoveryText` is
  not `null`, placed directly after the device-route line and before `PASSWORD_ROUTE_LIVE`.
- **No new prop.** The profile is already a prop; `hasRecoveryEmail` arrived on it in `TASK-041703`.
- **A comment saying why the screen never shows the address** and naming `ADR-0031` §6.3 — comment
  *why*, never *what*.

## Out of scope

- **The attach form.** `TASK-041713` builds it and `TASK-041714` places it here. This diff renders a
  sentence and adds no control.
- **Any word.** All copy is `recovery-text.ts`'s and a `verify:` line pins both constants at zero
  occurrences in this component.
- **Branching on `hasRecoveryEmail` in the component.** `recoveryLine` is the one place that branches,
  the way `deviceRouteLine` is; a `verify:` line pins `hasRecoveryEmail ===` at zero.
- **Anything in `Lobby.tsx`.** The screen already receives its profile there, unchanged.

## Tests

`web-client/src/account/AccountScreen.test.tsx`. **6 merged tests become 9.** Use `aProfile` from
`profile-fixture.ts` and wrap it in the `ProfileStripState` shape the merged tests already build.

| Test | Proves |
| --- | --- |
| `says recovery is on for one profile and off for another, in one render each` | Two renders in one test: `aProfile({ hasRecoveryEmail: true })` shows `RECOVERY_ON` and **not** `RECOVERY_OFF`; `aProfile({ hasRecoveryEmail: false })` shows `RECOVERY_OFF` and **not** `RECOVERY_ON`. Both directions and both absences, because one render cannot tell a branch from a constant, and asserting only presence cannot catch a screen that renders both |
| `says nothing about recovery when no profile is in hand` | Three renders — `profile={null}`, `{ kind: "no-profile" }` and `{ kind: "unavailable" }` — each asserting neither sentence appears. Then the presence half in the same test: with a real profile the sentence **is** there, so the three absences are withheld sentences and not an empty component |
| `renders no address, because it is given none and asks for none` | With `hasRecoveryEmail: true`, the rendered `textContent` of the whole section contains no `@`. Asserted over the container's text rather than over a queried element, so a stray address anywhere on the screen fails. The presence half runs first: `RECOVERY_ON` is on the screen, so the absence is over a rendered screen |

**No `try` anywhere in the added code, and no `expect()` inside one.** Query the two sentences
through `screen.queryByText(RECOVERY_ON)` — **the constant, never a string literal** — so a re-worded
sentence moves `recovery-text.test.ts` and not this file.

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says recovery is on for one profile and off for another, in one render each'`
      — passes, both directions, each asserting the other sentence is absent
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says nothing about recovery when no profile is in hand'`
      — passes, over all three profile-less states plus the presence case
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders no address, because it is given none and asks for none'`
      — passes, sweeping the container's whole `textContent` for `@`
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/account/AccountScreen.test.tsx 2>&1 | grep -qE 'Tests +9 passed \(9\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly nine**: the six merged plus these
      three. The merged six are pinned by this **count**, never by their names. Both lines, because a
      collection error prints a *passing* `Tests` count with no failure line at all
- [ ] `test "$(grep -oF 'recoveryLine' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 2`
      — the import and one call
- [ ] `test "$(grep -oiE 'RECOVERY_ON|RECOVERY_OFF' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 0`
      and `test "$(grep -oF 'hasRecoveryEmail ===' web-client/src/account/AccountScreen.tsx | wc -l | tr -d ' ')" = 0`
      — the component names neither sentence and compares the flag to nothing. Both read the whole
      file, comments included
- [ ] `cd web-client && npm run check` exits 0. The whole-suite total is deliberately not pinned:
      this ticket and `TASK-041713`, `TASK-041716`, `TASK-041718` have pairwise disjoint `Files`
      tables and may be dispatched in one batch
- [ ] Every merged test in `AccountScreen.test.tsx` passes unchanged. No assertion moves and none is
      weakened
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Proof

Run each step, record what you measured, and revert it. **These are experiments, not changes**, and
both files are inside this ticket's budget.

1. **Hard-code `recoveryLine(true)`.** Predict: `says recovery is on for one profile and off for
   another…` reddens on its second render. Record the count.
2. **Hard-code `recoveryLine(false)`.** Predict: the same test reddens on its first render. Steps 1
   and 2 are run as a pair, because a single-direction assertion cannot tell a constant from a read.
3. **Read `deviceRouteLive` instead of `hasRecoveryEmail`.** Predict: the first test reddens, because
   `aProfile`'s default `deviceRouteLive` is `true` and the two fields are set independently in that
   test. **If it stays green, the fixture sets them to the same value and the test is vacuous** —
   fix the fixture and say so; this is the trap that let a hard-coded seat pass eight of nine tests
   elsewhere in this epic.
4. **Drop the `kind === "profile"` guard**, rendering `recoveryLine(false)` for every state. Predict:
   `says nothing about recovery when no profile is in hand` reddens on all three of its absences.
5. **Plant an address**: render `<p>bob@example.test</p>` inside the section. Predict: `renders no
   address, because it is given none and asks for none` reddens. If it stays green, the sweep is
   querying an element rather than the container — fix the sweep.
6. **Vacuity check on step 5's absence**: render the component with `profile={null}` and re-run the
   `@` assertion. Predict: it passes over an almost-empty screen. That is why the presence half runs
   first in that test; confirm removing the presence half makes the mutation in step 5 undetectable
   for the `null` case.

> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong — measured, 0 escape bytes with it and 74 without — so every `grep -qE` needs it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**This ticket deliberately reverses a merged criterion.** `TASK-041217`'s acceptance criteria include
`grep -ci 'recovery\|hasRecoveryEmail' web-client/src/account/AccountScreen.tsx` returning `0`, and
its *Out of scope* says *"`hasRecoveryEmail`, and any sentence about recovery"* belongs elsewhere.
That was a fence around that ticket, not a standing rule, and this is the ticket it pointed at. It is
a criterion in a merged ticket, not a live gate: nothing in CI runs it.

**Why `recoveryLine` and not a ternary.** `deviceRouteLine` exists because *"a component choosing
between the two sentences inline would be a second place able to get it wrong"*, and the same holds
here: the two sentences say opposite things about whether this account can be recovered at all, and
the cost of getting it backwards is a player who believes they are safe.
