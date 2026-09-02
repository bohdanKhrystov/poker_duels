---
schema: 2
id: TASK-130506
title: There is no typed total before the server has named a turn
type: task
status: backlog
parent: STORY-1305
module: web-client
estimate: XS
tier: sonnet
review: deep
files_touched: 1
labels: [client, table, action-bar]
depends_on: [TASK-130505]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx 2>&1 | grep -qE '^ *Tests +7 passed \(7\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/ActionBar.test.tsx 2>&1 | grep -qE '^ *Tests +36 passed \(36\)$'
  - awk 'index($0, "YourTurn") { n++ } END { exit (n != 1) }' web-client/src/table/null-view.test.tsx
  - awk 'index($0, "the total") { n++ } END { exit (n < 3) }' web-client/src/table/null-view.test.tsx
  - awk 'index($0, "aLegalActions") { n++ } END { exit (n < 2) }' web-client/src/table/null-view.test.tsx
  - awk 'index($0, "spoken(") { n++ } END { exit (n < 12) }' web-client/src/table/null-view.test.tsx
  - sh -c 'grep -q "the total" web-client/src/table/null-view.test.tsx && ! grep -q "data-testid" web-client/src/table/null-view.test.tsx'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`null-view.test.tsx` — `EPIC-13`'s standing contract — says what the typed total shows before the
first `Snapshot`, and proves it: nothing, spoken or printed, and nothing after a `Snapshot` either.
The field appears only once the server has named a turn.

## Why this file, and why `deep`

`TASK-130206` landed the contract and `TASK-130408` amended it, both under a `deep` review, because
`ADR-0110` §3 is `ADR-0002` applied to the empty table: *"a client may never assert a game fact"*,
and every new table surface either renders nothing there or says here what it renders instead. This
is the third amendment and the same rule applies to it.

**An action bar is a game surface**, and `null-view.test.tsx`'s own docblock already refuses it by
name (`queryByLabelText("your move")` is null). A typed field appearing before the first `Snapshot`
would break that contract — so this ticket writes the answer into the file as an assertion instead
of leaving it as three reasons in `TASK-130504`'s prose.

## The answer, and why it needs a test rather than an argument

The field does not exist while `view === null`, for three independent reasons:

1. `Lobby.tsx` mounts `WaitingTable` there and never mounts `ActionBar` at all.
2. Inside the bar, the field lives in `Live`, which is not rendered while `turn === null`.
3. Inside `Live`, the field is rendered only when `amountFloor(actions) !== null`.

Reason 2 is the one worth a test of its own: **a `Snapshot` alone is not enough**. Every other
surface this contract covers appears the moment a view arrives; this one waits for a `YourTurn`. A
test that only checked the null view and then a live table would be checking the wrong boundary, so
this one checks all three states in one flow.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/null-view.test.tsx` | modify |
| `web-client/src/table/ActionBar.tsx` | read |
| `web-client/src/lobby/Lobby.tsx` | read |
| `web-client/src/table/turn-fixture.ts` | read |

## Scope

- **One test added**, taking the file to **7**, and one paragraph added to the file's docblock
  saying what the typed total shows on the null view and why — the same shape the acting mark's and
  the last-act mark's paragraphs already take.
- **The test walks three states in one render**, using the file's own `renderNullView` helper and
  a real store:
  1. `RoomJoined` only — `queryByLabelText("the total")` is null, and `spoken(container)` is still
     closed to `[]`, so the field's accessible name has not leaked into the empty table's names.
  2. after `Snapshot` with `aView()` — still null. The bar mounts here; the field does not.
  3. after `YourTurn` carrying `aLegalActions()` — **not** null. This is the guard on the guard,
     exactly as this file's third test does for `ADR-0110`'s four probes: without it,
     `queryByLabelText("the total")` could be a probe that matches nothing anywhere in this app and
     the two refusals above would pass forever for the wrong reason.
- **`aLegalActions` is imported from `./turn-fixture`**, the fixture the bar's own tests use. If the
  store needs the frames in a different order or shape to open a turn, **say so in the PR and change
  the frames** — never a test-only prop, a `data-testid` or a hand-built store field
  (`ADR-0100` §5).
- **No closed-set assertion after the `Snapshot`.** `spoken()` is asserted equal to `[]` **only** on
  the null view. Past that point this file uses `> 0` probes and nothing else, deliberately: a
  closed set there would redden the day another story adds a legitimate accessible name, which is
  the trap `STORY-1304` recorded.

## Out of scope

- **Closing `STORY-1304`'s recorded gap** — that `spoken()` is never asserted *after* the last-act
  mark mounts, so a mark that gained an `aria-label` escapes this contract. It is real, it is
  written down on `BOARD.md`, and its fix is a targeted assertion about the mark's own text, not a
  closed set. **It is not this story's**, and widening this ticket to reach it would put a
  `STORY-1304` repair inside a `STORY-1305` diff.
- **`ActionBar.tsx`, `ActionBar.test.tsx` and every other production file.** Read-only; the gate
  pins `ActionBar.test.tsx` at 36 to prove it was not reached.
- **The four `ADR-0110` probes and the two mark probes** already in this file. None of their
  assertions moves.

## Tests

`null-view.test.tsx`, `describe("what the table shows when there is no view")` — one added to the
six it has, so the file reports **7**.

| Test | Proves |
| --- | --- |
| `offers no typed total before the server has named a turn` | with `RoomJoined` alone there is no element named `the total` and `spoken()` is `[]`; after a `Snapshot` there is still none — the bar is on screen and the field is not; after a `YourTurn` there is one. The middle assertion is the whole point: this surface waits for a turn, not for a view, and without the third state the first two would be assertions about a probe that never works |

## Acceptance criteria

- [ ] `src/table/null-view.test.tsx` reports `Tests  7 passed (7)`
- [ ] `what the table shows when there is no view.offers no typed total before the server has named
      a turn` passes
- [ ] `null-view.test.tsx` applies exactly one `YourTurn`, mentions `the total` at least three
      times and `aLegalActions` at least twice, still calls `spoken(` at least 12 times, and
      contains no `data-testid`
- [ ] `src/table/ActionBar.test.tsx` still reports `Tests  36 passed (36)`
- [ ] The file's docblock names the typed total and says what it shows when `view` is `null`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
