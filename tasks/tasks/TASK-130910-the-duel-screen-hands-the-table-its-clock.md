---
schema: 2
id: TASK-130910
title: The duel screen hands the table its clock, and it visibly changes each second
type: task
status: ready
parent: STORY-1309
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, table, clock]
depends_on: [TASK-130909]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 77) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx -t "counts the acting seat down once a second" 2>&1 | awk '/^ *Tests +[0-9]+ passed/ { n = $2 } END { exit !(n >= 1) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 9) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/no-derivation.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 8) }'
  - awk '{ n += gsub(/state\.nowMillis/, "&") } END { exit (n < 1) }' web-client/src/lobby/Lobby.tsx
  - awk '{ n += gsub(/state\.turnClock/, "&") } END { exit (n < 2) }' web-client/src/lobby/Lobby.tsx
  - sh -c '! grep -qF "performance.now" web-client/src/lobby/Lobby.tsx'
  - sh -c '! grep -qF "useEffect(() => {" web-client/src/table/DuelTable.tsx'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A player at a real duel table sees the acting seat's countdown change by one every second, both
timebanks beside it, and — before the first `Snapshot` — none of it.

## What the screen actually does, and what it must not

`Lobby.tsx` already owns the branch that mounts `DuelTable`. It gains three lines: pack the store's
`turnClock` and `nowMillis` into the one `ClockReading` prop, or `null` when there is no clock. It
reads no time of its own — `performance.now` is gated absent — because the reading is the store's
and re-taking it here would undo `ADR-0113` §6's anchor-on-arrival.

**The ticking is already merged and is not re-implemented here.** `TASK-130905` put it in the store,
at `ADR-0102` §4's `schedule` seam; React redraws because it subscribes (`ADR-0032` §3). This ticket
is the first place all of it runs together, so it is the first place the human's own sentence —
*"clock shoud visibly change each second"* — is checkable, and it is checked by driving the store's
own injected schedule rather than by waiting.

**`null-view.test.tsx` is a merged contract and this surface owes it a line.** Its docstring says so
outright: *"a surface a later `EPIC-13` story adds to the table either renders nothing while `view`
is `null`, or says here what it renders instead."* The answer is *nothing*, and the reason is
`ADR-0110` §3's — no seat is on turn before the opening `Snapshot`, `Lobby` mounts `WaitingTable`
rather than `DuelTable`, and `WaitingTable` mounts no `SeatPlate` at all. The line is owed whether
or not the test would fail without it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |
| `web-client/src/table/null-view.test.tsx` | modify |
| `web-client/src/store/duel-store.ts` | read — the `now`, `schedule` and `tickMillis` options |

## Scope

- **`Lobby.tsx`, inside the `state.view !== null` branch**, one prop on `DuelTable`:

  ```tsx
  clock={
    state.turnClock === null
      ? null
      : { clock: state.turnClock, nowMillis: state.nowMillis }
  }
  ```

  `state.turnClock` is read twice — the null test and the read — and `state.nowMillis` once; both are
  gated as **lower** bounds, not equalities, so a KDoc that names either cannot invert its own gate.
  Nothing else in `Lobby.tsx` moves: not the presence notice, not the server-action line, not the
  action bar, not the column's classes.
- **No effect, no `useState`, no timer in this file or any component.** The store ticks; the screen
  subscribes. A `useEffect` here would be the shape `ADR-0032` §3 refuses.
- **The tests drive the store's injected seams, and install no timer.** Build the store as
  `createDuelStore({ tickMillis: 1000, schedule: (run) => { pending.push(run); }, now: () => stated })`
  with `stated` a mutable number the test sets, then fire the captured callback inside `act()`.
  Nothing sleeps, `vi.useFakeTimers` is not needed, and `virtual-time.test.ts` has nothing to catch.
- **The frames are a real server's**, in order: `RoomJoined`, `Snapshot`, then `TurnClock` — never a
  prop handed straight to a component.

## Out of scope

- **`WaitingTable`.** It draws no seat plate and gains nothing; the host-alone answer is *nothing*,
  asserted rather than arranged.
- **The action bar under a running clock.** `DEC-108` is open and the product owner's, and
  `ADR-0113` §Consequences records that its subject is leaving the screen. Nothing here answers it,
  and no test in this ticket asserts anything about whether a control is enabled.
- **`presence-text.ts`.** *The duel is paused.* leaves in `TASK-130911`, deliberately after this —
  so the sentence's replacement, the rival's own clock, is on screen before the sentence goes
  (`ADR-0108` §4: *"The present player's answer to how long will I wait? is the rival's clock"*).
- **The 390 × 664 fit as a gate.** `ADR-0089` §2b keeps a browser out of `verify:`; it is a PR
  statement and a pane verdict, and this ticket's PR owes the measurement in prose.

## Tests

`Lobby.test.tsx` — **3** added to the 74 it has, so the file reports **77**.

| Test | Proves |
| --- | --- |
| `counts the acting seat down once a second` | after `RoomJoined`, a `Snapshot` and a `TurnClock` of `30_000` ms anchored at a stated reading, the table shows `30`; firing the captured tick with the reading 1 000 ms on shows `29`; once more shows `28`. Three figures, so a screen that painted the anchor and stopped fails, and so does one that jumped |
| `shows both seats' banks under the table` | the same frames with `bankRemainingMillis` `[180_000, 72_000]` put `Timebank 3:00` on one plate and `Timebank 1:12` on the other — both public facts of the table (`ADR-0108` §5) |
| `states no act when the countdown reaches zero` | driving the reading past `expiresAt` with **no further frame**, the screen still says `Their turn`, shows the figure `0`, and contains no text matching `/The server (folded\|checked)/`. The client never invents the act the server has not yet taken |

`null-view.test.tsx` — **1** added to the 8 it has, so the file reports **9**, plus a docstring
paragraph in the file's own tradition.

| Test | Proves |
| --- | --- |
| `draws no countdown and no bank before the server has named a turn` | on the host-alone screen: no text matching `/Timebank/`, no element carrying a clock colour class, and — walking on to the live table the file's other tests already open — both appearing there, so the absence is a state and not a permanent silence |

`no-derivation.test.tsx` (8) is pinned unmoved.

## Acceptance criteria

- [ ] `Lobby.test.tsx` reports at least **77** passing tests and none failing
- [ ] `counts the acting seat down once a second` passes when run alone by name
- [ ] `shows both seats' banks under the table` and `states no act when the countdown reaches zero`
      each pass, by name
- [ ] `null-view.test.tsx` reports at least **9** passing tests and none failing, and
      `draws no countdown and no bank before the server has named a turn` passes
- [ ] `no-derivation.test.tsx` still reports at least **8** passing and none failing
- [ ] `Lobby.tsx` names `state.turnClock` at least twice, names `state.nowMillis`, and contains no
      `performance.now`
- [ ] `DuelTable.tsx` opens no `useEffect` — the store ticks and the screen subscribes
      (`ADR-0032` §3)
- [ ] The PR states the document's `scrollHeight` and `clientHeight` at 390 × 664 with the clock and
      both banks on screen, read and pasted as text (`ADR-0103`)
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
