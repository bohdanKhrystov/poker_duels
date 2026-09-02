---
schema: 2
id: TASK-130304
title: The mark is at the seat the server named, and nowhere before it names one
type: task
status: done
parent: STORY-1303
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [client, table, guard]
depends_on: [TASK-130303]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/DuelTable.test.tsx 2>&1 | grep -qE '^ *Tests +24 passed \(24\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx 2>&1 | grep -qE '^ *Tests +5 passed \(5\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/no-derivation.test.tsx 2>&1 | grep -qE '^ *Tests +7 passed \(7\)$'
  - awk 'index($0, "acting-mark") { n++ } END { exit (n < 3) }' web-client/src/table/DuelTable.test.tsx
  - awk 'index($0, "seatToAct: 1") { n++ } END { exit (n < 1) }' web-client/src/table/DuelTable.test.tsx
  - awk 'index($0, "acting-mark") { n++ } END { exit (n < 2) }' web-client/src/table/null-view.test.tsx
  - awk 'index($0, "spoken(") { n++ } END { exit (n < 10) }' web-client/src/table/null-view.test.tsx
  - awk 'index($0, "digitBearing(") { n++ } END { exit (n < 4) }' web-client/src/table/null-view.test.tsx
  - sh -c 'grep -q "acting-mark" web-client/src/table/null-view.test.tsx && ! grep -q "data-testid" web-client/src/table/null-view.test.tsx'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The mark is a statement of a game fact — *it is this seat's turn* — so it is pinned to the field the
server sent and to nothing else: it stands at `view.seatToAct` whichever seat that is, it stands
nowhere when the view names none, and it does not exist at all before the opening `Snapshot`.

## Why this is `deep`, and why it is its own diff

`null-view.test.tsx` is `EPIC-13`'s standing contract, landed by `TASK-130206` under a `deep`
review because `ADR-0110` §3 is `ADR-0002` applied — the failure it forbids is a client asserting a
game fact, the one class of client defect this project treats as correctness. This ticket is the
first story to amend that file, and the review that reads it is the one that asks whether the new
probe could pass while the thing it names is on screen, and whether anything already in the file
got weaker.

It is separate from `TASK-130303` because that ticket's three files were spent on the plate itself.
The seat the mark lands on is a different claim from the mark existing, and it is asserted in two
files that ticket never opens.

## What is already true, measured on `develop` 2026-09-02

- `DuelTable.test.tsx` reports 22 tests; `null-view.test.tsx` 4; `no-derivation.test.tsx` 7.
- `view-fixture.ts`'s `aView()` defaults to `viewerSeat: 0` **and** `seatToAct: 0` — the hero on
  turn. A test that never sets `seatToAct: 1` cannot tell the view's field from a hard-coded seat,
  which is why one gate greps for that literal.
- `Lobby.tsx` renders `WaitingTable` when `view === null && roomCode !== null`, and
  `WaitingTable.tsx` draws its own two seat rows — it never mounts `SeatPlate`. **So the mark
  renders nothing at all on the null view**: no element, no class, no attribute, no text. The
  file's `spoken()` closure and its digit sweep are untouched, and this ticket writes that down
  rather than leaving the next reader to rediscover it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/DuelTable.test.tsx` | modify |
| `web-client/src/table/null-view.test.tsx` | modify |
| `web-client/src/table/SeatPlate.tsx` | read |
| `web-client/src/table/view-fixture.ts` | read |
| `web-client/src/table/no-derivation.test.tsx` | read |

## Scope

- **Two tests in `DuelTable.test.tsx`** (22 → 24), in the file's existing idiom. No new fixture, no
  new helper: `aView({ seatToAct: … })` and `container.querySelectorAll(".acting-mark")`.
- **One test in `null-view.test.tsx`** (4 → 5), and **one paragraph** added to the file's docstring
  saying what this surface shows when `view === null` — nothing, and why: the host-alone table is
  `WaitingTable`, which mounts no seat plate, and the mark speaks no `aria-label` or `title` on any
  screen, so neither closure in this file changes shape.
- **`ADR-0100` §5 holds.** No `data-testid`, no test-only prop, no exported setter; a gate refuses
  `data-testid` in the null-view file.
- **Nothing in either file is weakened.** The four merged null-view tests keep every assertion they
  have — gates pin `spoken(` at its measured 10 occurrences and `digitBearing(` at 4, so the
  guard's two closures cannot be thinned while adding to the file.

## Out of scope

- **Production code.** Both files are tests. If a test fails because `SeatPlate` is wrong, that is a
  repair ticket against `TASK-130303`, not an edit here.
- **A count of anything on the live table.** `TASK-130206`'s third test asserts *more than zero* and
  never a number, deliberately, so a later `EPIC-13` story cannot redden this guard for a reason
  unrelated to the contract. The new test follows it: `toHaveLength(1)` for the mark, because one
  seat is on turn, and never a count of images, figures or plates.
- **`no-derivation.test.tsx`.** It guards the table that *has* a view, and a class name is neither a
  printed figure nor a spoken one. It is pinned at 7 to prove this ticket did not touch it.
- **`WaitingTable.test.tsx` and `Lobby.test.tsx`.** Neither asserts anything about a mark; neither
  needs to, and neither is opened.

## Tests

`DuelTable.test.tsx` — two added.

| Test | Proves |
| --- | --- |
| `marks the seat the view says is to act, at either seat` | **both directions in one test.** With `aView({ viewerSeat: 0, seatToAct: 0 })` there is exactly one `.acting-mark` and it is inside the plate that prints `You`; with `aView({ viewerSeat: 0, seatToAct: 1 })` there is exactly one and it is inside the plate that prints `Your rival`. A mark hard-coded to a seat index passes the first and fails the second |
| `marks no seat when the view names none` | `aView({ seatToAct: null })` renders zero `.acting-mark`. `seatToAct` is nullable on the wire (`protocol.gen.ts:290`), and a mark that appeared whenever a plate rendered would be the client inventing a turn |

`null-view.test.tsx` — one added.

| Test | Proves |
| --- | --- |
| `marks no acting seat before the server has named one` | on the null view, `.acting-mark` is empty **and** `spoken(container)` is still `[]`. Then, on the same store, after `store.apply({ type: "Snapshot", view: aView() })`, `.acting-mark` is non-empty. The positive half is the guard on the guard, exactly as the file's third test already does it for the four `ADR-0110` probes: without it, the refusal is a selector that matches nothing anywhere and passes forever for the wrong reason |

## Acceptance criteria

- [ ] `DuelTable.test.tsx` reports `Tests  24 passed (24)`
- [ ] `DuelTable.marks the seat the view says is to act, at either seat` passes, and the file
      contains the literal `seatToAct: 1`
- [ ] `DuelTable.marks no seat when the view names none` passes
- [ ] `null-view.test.tsx` reports `Tests  5 passed (5)`
- [ ] `null-view.marks no acting seat before the server has named one` passes, and the file mentions
      `acting-mark` on at least two lines — the refusal and its positive control
- [ ] `null-view.test.tsx` still calls `spoken(` at least 10 times and `digitBearing(` at least 4,
      and still contains no `data-testid`
- [ ] `no-derivation.test.tsx` still reports `Tests  7 passed (7)`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
