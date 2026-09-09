---
schema: 2
id: TASK-141013
title: The account screen is told which sign-out it offers
type: task
status: done
parent: STORY-1410
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, account]
depends_on: [TASK-141012]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/account/AccountScreen.test.tsx 2>&1 | grep -qF "AccountScreen.test.tsx  (24 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qF "Lobby.test.tsx  (114 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | grep -qF "App.test.tsx  (37 tests)"'
  - sh -c 'cd web-client && ! grep -q "signOutHandsANewProfile={false}" src/account/AccountScreen.tsx'
  - sh -c 'cd web-client && test $(grep -c "signOutHandsANewProfile" src/account/AccountScreen.tsx) -eq 3'
  - sh -c 'cd web-client && test $(grep -c "signOutHandsANewProfile={false}" src/lobby/Lobby.tsx) -eq 1'
  - git diff --exit-code -- web-client/src/account/SignOutControl.tsx web-client/src/account/device-standing-provider.tsx web-client/src/main.tsx
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`AccountScreen` stops passing a literal and starts forwarding a prop of its own, so the last
hard-coded `false` in the chain sits at one place — the lobby — for one more ticket.

## Files

| File | Action |
| --- | --- |
| `web-client/src/account/AccountScreen.tsx` | modify |
| `web-client/src/account/AccountScreen.test.tsx` | modify |
| `web-client/src/lobby/Lobby.tsx` | modify |

Probed at `c6a41e6d`: a required `signOutHandsANewProfile` on `AccountScreen` names exactly these
three under `tsc --noEmit` — **nine** render sites in `AccountScreen.test.tsx` and one in
`Lobby.tsx`, and nothing else in the tree. `App.test.tsx` is not among them: it wraps the component
with `vi.fn(actual.AccountScreen)` rather than rendering it in JSX.

Read, do not edit: `web-client/src/account/SignOutControl.tsx`,
`web-client/src/account/account-text.ts`.

## Scope

- `AccountScreen` gains `readonly signOutHandsANewProfile: boolean` — **required**, beside
  `signedIn`, which is also required and is the prop it must never be confused with.
- It forwards it to `SignOutControl`, replacing `TASK-141009`'s literal. The
  `! grep -q "signOutHandsANewProfile={false}"` gate on this file is what says the literal is gone,
  and `grep -c "signOutHandsANewProfile" -eq 3` pins the three sites it may appear at: the prop
  declaration, the destructuring, and the forward.
- A KDoc line saying what the prop is for and, in `ADR-0135` §7's own terms, what it is not:
  `signedIn` decides whether a sign-out exists at all and never which of the two it is, so the two
  booleans are read in two different places and neither stands in for the other.
- `Lobby.tsx` passes `signOutHandsANewProfile={false}` — a literal, with a one-line comment naming
  `TASK-141014`. The abandoning branch stays unreachable for one more merge.
- The nine merged renders in `AccountScreen.test.tsx` gain `signOutHandsANewProfile={false}`. **No
  assertion in that file moves**: eight of the nine pass no `signOut` at all, so they render no
  `SignOutControl`, and the ninth already asserts nothing about the sentence.

## Out of scope

- **Reading the provider.** `TASK-141014` is the ticket that connects it, and it is deliberately
  the last one in the story: it is the merge at which the behaviour and the copy both become true.
- **`SignOutControl.tsx`, `device-standing-provider.tsx`, `main.tsx`.** All landed; all in the
  `git diff --exit-code` gate.
- **Making the prop optional.** An optional prop with a `false` default would keep
  `AccountScreen.test.tsx` out of this ticket and would be the invisible absence `ADR-0135` §7
  refuses: a screen that can be mounted without being told which sign-out it offers is a screen
  that will one day be.

## Tests

`AccountScreen.test.tsx`, **20 today → 21**.

| Test | Proves |
| --- | --- |
| `hands the sign-out control the answer it was given` | two renders in one test, both with a `signOut` prop so the control mounts. With `signOutHandsANewProfile={true}`, pressing `SIGN_OUT_LABEL` shows `SIGN_OUT_HANDS_A_NEW_PROFILE` and not `SIGN_OUT_WARNING`; with `{false}`, the reverse. Asserted through the rendered text rather than through a spy on the child, because what matters is that the screen forwarded the value and not that it called something |

`Lobby.test.tsx` is **not edited** and is pinned at **113**, and `App.test.tsx` at **37**: both
render the real `AccountScreen` through the real `Lobby`, so a prop that failed to reach it would
show up there as a `tsc` failure rather than a silent pass.

## What would still pass if the coder were wrong

- **Forwarding `props.signedIn` instead of the new prop** — the two are adjacent booleans and this
  is the mistake the KDoc is written against — passes both merged renders that set `signedIn: true`
  and fails `hands the sign-out control the answer it was given`, whose two renders differ **only**
  in the new prop and hold `signedIn` fixed.
- **Leaving the literal in place and adding an unused prop** typechecks and passes every merged
  test. The `! grep -q "signOutHandsANewProfile={false}"` gate on `AccountScreen.tsx` is the only
  thing that sees it, and the count of **3** is what stops a coder satisfying that gate by writing
  `signOutHandsANewProfile={props.signedIn && false}`.
- **Wiring the provider here instead of in `Lobby`** would pass every test and break `ADR-0060`
  §4 — `AccountScreen` is a prop-driven presentation with no hook and no provider read, which is
  the assumption `App.test.tsx`'s spy rests on. The `grep -c` of 3 refuses the extra reference.

## Acceptance criteria

- [ ] `AccountScreen.test.tsx` reports `(24 tests)` and all pass — the ticket asked 21 on a
      baseline predating `TASK-140914`'s five; `develop` holds 23 and this ticket adds one
- [ ] `hands the sign-out control the answer it was given` passes
- [ ] `Lobby.test.tsx` reports `(114 tests)` — asked 113, measured before `TASK-140915`'s one, and
      untouched in count here — and `App.test.tsx` reports `(37 tests)`, all passing
- [ ] `! grep -q "signOutHandsANewProfile={false}"` on `AccountScreen.tsx` exits 0
- [ ] `grep -c "signOutHandsANewProfile"` is 3 in `AccountScreen.tsx` and
      `grep -c "signOutHandsANewProfile={false}"` is 1 in `Lobby.tsx`
- [ ] `git diff --exit-code` over `SignOutControl.tsx`, `device-standing-provider.tsx` and
      `main.tsx` exits 0
- [ ] `cd web-client && npm run check` and `npm run build` exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
