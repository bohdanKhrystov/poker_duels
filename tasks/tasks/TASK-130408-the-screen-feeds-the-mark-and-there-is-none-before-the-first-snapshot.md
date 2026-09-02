---
schema: 2
id: TASK-130408
title: The screen feeds the mark, and there is none before the first snapshot
type: task
status: backlog
parent: STORY-1304
module: web-client
estimate: XS
tier: sonnet
review: deep
files_touched: 2
labels: [client, table, guard]
depends_on: [TASK-130407]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx 2>&1 | grep -qE '^ *Tests +6 passed \(6\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +80 passed \(80\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/DuelTable.test.tsx 2>&1 | grep -qE '^ *Tests +27 passed \(27\)$'
  - awk 'index($0, "lastAct={state.lastAct}") { n++ } END { exit (n != 1) }' web-client/src/lobby/Lobby.tsx
  - awk 'index($0, "last-act") { n++ } END { exit (n < 2) }' web-client/src/table/null-view.test.tsx
  - awk 'index($0, "spoken(") { n++ } END { exit (n < 12) }' web-client/src/table/null-view.test.tsx
  - awk 'index($0, "digitBearing(") { n++ } END { exit (n < 4) }' web-client/src/table/null-view.test.tsx
  - awk 'index($0, "acting-mark") { n++ } END { exit (n < 3) }' web-client/src/table/null-view.test.tsx
  - sh -c 'grep -q "last-act" web-client/src/table/null-view.test.tsx && ! grep -q "data-testid" web-client/src/table/null-view.test.tsx'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The mark reaches a real screen: `Lobby.tsx` hands `state.lastAct` to `DuelTable`, so a player facing
a bet reads what their rival just did. And `null-view.test.tsx` — `EPIC-13`'s standing contract —
says what that surface shows when `view === null`: **nothing at all**, printed or spoken.

## Why this is `deep`, and why the wiring and the contract are one diff

`null-view.test.tsx` was landed by `TASK-130206` under a `deep` review because `ADR-0110` §3 is
`ADR-0002` applied: the failure it forbids is a client asserting a game fact, the one class of client
defect this project treats as correctness. Every story that adds a table surface amends it, and the
review that reads it is the one that asks whether the new probe could pass while the thing it names
is on screen, and whether anything already in the file got weaker.

The wiring belongs in the same diff because **that file is what proves it**. `null-view.test.tsx`
renders the real `Lobby` over a real store; its positive control applies an `Events` frame carrying
an act and then a `Snapshot`, and the mark can only appear if `Lobby.tsx` actually passes the field.
A one-line prop change with no test is the shape of change that ships broken; here the test is the
guard and the wiring's only proof at once.

## The two answers this ticket writes down so nobody rediscovers them

- **`view === null`: nothing.** `Lobby.tsx` renders `WaitingTable` when `view === null && roomCode
  !== null` and `DuelTable` only in the branch above it, so no plate is mounted at all — no element,
  no class, no text, no attribute. It is belt and braces, too: `state.lastAct` is `null` until an
  `Events` frame carries an act, and the opening frame of a hand carries `HandStarted`,
  `BlindPosted`, `HoleCardsDealt` and `ActionOn` and no act. And the mark **speaks nothing on any
  screen** (`TASK-130406` pins one `aria-label` and zero `title` on the plate), so neither
  `spoken()` nor the digit sweep changes shape to admit it.
- **Across a refresh: no mark until the next act.** `PlayerView` carries no last-act field, so a
  resuming client rebuilds nothing — `ADR-0109` §Consequences accepts that by name, `TASK-130404`
  pins it as reducer behaviour (`a resume rebuilds no mark`), and **repairing it is a
  `PROTOCOL_VERSION` bump, which is out of this story's scope.** Nothing here compensates for it:
  no cached mark, no localStorage, no re-derivation from the narration.

## What is already true, measured on `develop` 2026-09-02

- `null-view.test.tsx` reports **5**; `Lobby.test.tsx` **80**; `DuelTable.test.tsx` **27** after
  `TASK-130407`.
- In `null-view.test.tsx`: `spoken(` appears **12** times, `digitBearing(` **4**, `acting-mark`
  **3**, `data-testid` **0**. The gates hold those at or above their measured values so the guard
  cannot be thinned while being added to.
- `Lobby.tsx` already passes `view`, `rivalPresence`, `narration` and `revealStep` to `DuelTable` in
  one JSX block. This is one more attribute in that block.
- **No test outside `duel-state.test.ts` and the recorded e2e script constructs an act event**
  (measured across `web-client/src`), so no merged test in `Lobby.test.tsx` or `App.test.tsx` can
  see a mark appear. `Lobby.test.tsx` is pinned at 80 to prove it, and the four end-to-end suites
  `ADR-0100` §3 forbids editing run inside `npm run check`: they drive the recorded duel through the
  real screens, so the mark **does** appear there — none of them queries by text on the table, and
  `drive-duel.tsx` finds action buttons with `queryByRole("button", …)`, which a printed mark is not.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/table/null-view.test.tsx` | modify |
| `web-client/src/table/DuelTable.tsx` | read |
| `web-client/src/table/view-fixture.ts` | read |
| `docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md` | read |

## Scope

- **`Lobby.tsx`:** the `<DuelTable …>` block gains `lastAct={state.lastAct}`, written exactly that
  way so the gate can pin it at one occurrence. Nothing else in the file moves — not the branch
  order, not `potIncludingStreet`, not the presence notice, not the server-action line, not the bar.
- **`null-view.test.tsx`:** one test added (5 → 6), and **one paragraph added to the file's
  docstring** saying what this surface shows when `view` is `null` and why — the host-alone screen
  is `WaitingTable`, which mounts no `SeatPlate`, and the mark speaks no `aria-label` and no `title`
  on any screen it does reach, so neither closure in this file changes shape.
- **`ADR-0100` §5 holds.** No `data-testid`, no test-only prop, no exported setter; a gate refuses
  `data-testid` in this file.
- **Nothing in the file is weakened.** The five merged tests keep every assertion they have, and the
  gates pin `spoken(` at ≥ 12, `digitBearing(` at ≥ 4 and `acting-mark` at ≥ 3 — their measured
  values — so neither closure nor the acting mark's own probe can be thinned while adding to the
  file.

## Out of scope

- **A `PlayerView` field for a resuming client**, and any client-side substitute for one. That is
  the accepted cost above; a ticket that wants it is asking the architect for a wire bump.
- **Reading the mark out of `state.narration`.** The store's `lastAct` is the one source
  (`TASK-130403`); a second derivation path would have a second lifetime.
- **`Lobby.test.tsx`.** No merged test there constructs an act, so none can observe the mark; it is
  pinned at 80 to prove this ticket did not disturb it.
- **A count of anything on the live table.** `TASK-130206`'s third test asserts *more than zero* and
  never a number, deliberately, so a later `EPIC-13` story cannot redden the guard for an unrelated
  reason. The new test follows it: `toHaveLength(0)` for the refusal and *greater than zero* for the
  control.

## Tests

`null-view.test.tsx` — one added, so the file reports **6**.

| Test | Proves |
| --- | --- |
| `marks no last act before the server has named one` | on the null view (`RoomJoined` and nothing else) `container.querySelectorAll(".last-act")` is empty **and** `spoken(container)` is still `[]`. Then, on the same store, `store.apply({ type: "Events", events: [{ type: "PlayerBet", sequence: 4, seat: 1, to: 950 }] })` followed by `store.apply({ type: "Snapshot", view: aView() })` — after which `.last-act` is non-empty. The positive half is the guard on the guard, exactly as this file's third test does it for the four `ADR-0110` probes: without it, `.last-act` is a selector that could match nothing anywhere in this app and the refusal would pass forever for the wrong reason. It is also the **only** proof that `Lobby.tsx` passes the field: delete that one attribute and this half goes red |

Both `store.apply` calls go inside `act(…)`, as the file's third and fifth tests already do. No
`YourTurn` is applied, so no action bar mounts and the mark's words are the only ones of their kind
on screen.

## Acceptance criteria

- [ ] `null-view.test.tsx` reports `Tests  6 passed (6)`
- [ ] `what the table shows when there is no view.marks no last act before the server has named one`
      passes, and the file mentions `last-act` on at least two lines — the refusal and its positive
      control
- [ ] `null-view.test.tsx` still calls `spoken(` at least 12 times, `digitBearing(` at least 4, still
      mentions `acting-mark` at least 3 times, and still contains no `data-testid`
- [ ] `Lobby.tsx` contains `lastAct={state.lastAct}` exactly once
- [ ] `Lobby.test.tsx` still reports `Tests  80 passed (80)` and `DuelTable.test.tsx` still reports
      `Tests  27 passed (27)`
- [ ] `cd web-client && npm run check` exits 0 — including the four end-to-end suites, which now
      paint the mark through the recorded duel
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
