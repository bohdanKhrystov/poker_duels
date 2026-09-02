---
schema: 2
id: TASK-130407
title: One mark, at the seat the act names, and it moves when the other seat acts
type: task
status: backlog
parent: STORY-1304
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, table, guard]
depends_on: [TASK-130406]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/DuelTable.test.tsx 2>&1 | grep -qE '^ *Tests +27 passed \(27\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/no-derivation.test.tsx 2>&1 | grep -qE '^ *Tests +7 passed \(7\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx 2>&1 | grep -qE '^ *Tests +12 passed \(12\)$'
  - awk 'index($0, "lastAct") { n++ } END { exit (n < 4) }' web-client/src/table/DuelTable.tsx
  - awk 'index($0, "seat: 1") { n++ } END { exit (n < 1) }' web-client/src/table/DuelTable.test.tsx
  - awk 'index($0, "seat: 0") { n++ } END { exit (n < 1) }' web-client/src/table/DuelTable.test.tsx
  - awk 'index($0, "last-act") { n++ } END { exit (n < 5) }' web-client/src/table/DuelTable.test.tsx
  - sh -c 'grep -q "lastAct" web-client/src/table/DuelTable.tsx && ! grep -q "data-testid" web-client/src/table/DuelTable.test.tsx'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

On a rendered duel table there is **exactly one** last-act mark, and it stands at the seat the act
event names — whichever seat that is, the hero's own included. When the other seat acts the mark
moves rather than multiplying, and when there is no act there is no mark.

## Why this is its own diff

`TASK-130406`'s three files were spent on what a plate does with the prop. *Which* plate gets it is
a different claim — it is the one `ADR-0109` §1 is actually about (*"exactly one mark at the table,
never one per seat"*) — and it is asserted in a file that ticket never opens.

`ADR-0109` §Alternative 2 rejected a per-seat mark on the vision's own positioning sentence, and
noted that *"reversal is asymmetric in one-mark's favour"*. A per-seat implementation is therefore
not a near-miss to be tidied later; it is the retraction the ADR forecloses. The second test below
is what makes it impossible to ship one green.

## What is already true, measured on `develop` 2026-09-02

- `DuelTable.test.tsx` reports **24**; `no-derivation.test.tsx` **7**; `SeatPlate.test.tsx` **12**
  after `TASK-130406`.
- `DuelTable.test.tsx` already imports `applyServerMessage`, `advanceReveal` and `initialState`, and
  already has `plateFor(name)`, which returns the plate whose printed name is `You` or
  `Your rival`. No new helper is needed.
- **`seat: 1` and `seat: 0` both appear zero times in this file**, so both gates are real gates.
  `view-fixture.ts`'s `aView()` defaults to `viewerSeat: 0` with seats `0` and `1` — a test that
  only ever marks one seat cannot tell the event's field from a hard-coded index, which is exactly
  how a hard-coded `0` passed eight of nine tests in an earlier story.
- `no-derivation.test.tsx` renders `<DuelTable view={…} />` in all seven tests and passes no act, so
  nothing this ticket adds can reach its sweeps. It is pinned at 7 to prove that.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/DuelTable.tsx` | modify |
| `web-client/src/table/DuelTable.test.tsx` | modify |
| `web-client/src/table/SeatPlate.tsx` | read |
| `web-client/src/table/view-fixture.ts` | read |
| `docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md` | read |

## Scope

- **`DuelTable` gains one optional prop, `lastAct?: ActEvent | null`**, typed from
  `../store/duel-state` and documented in the KDoc idiom the `revealStep` prop already uses: the
  most recent act of the hand on screen, the store's own field, never worked out here.
- **Each `SeatPlate` is handed the mark only when the event names that seat**: the rival's plate
  gets it when `lastAct.seat === rival.index`, the hero's when `lastAct.seat === you.index`, and the
  other gets `null`. One expression per plate, `lastAct.seat` read and nothing else — not
  `viewerSeat`, not `seatToAct`, not the narration.
- **Nothing else on the table moves.** Not the board, not the pot strip, not `BetLine`, not the
  reveal step, not the presence. The three merged reveal tests and the award-line tests keep every
  assertion they have.
- **Run `npm run format` before `format:check`.**

## Out of scope

- **What the mark looks like or says.** `TASK-130406` merged that, and `SeatPlate.test.tsx` is
  pinned at 12 here to prove this ticket did not reach it.
- **Where the value comes from.** `Lobby.tsx` passes `state.lastAct` in `TASK-130408`; this ticket's
  tests hand the prop in directly, exactly as the merged `revealStep` tests do.
- **`null-view.test.tsx`.** `TASK-130408` amends `EPIC-13`'s contract file, under a `deep` review.
- **Widening `no-derivation.test.tsx`.** It is pinned unmoved. If a later ticket ever renders a mark
  into one of its fixtures, that ticket owes the guard a named carve-out.
- **A count of anything else on the live table.** `TASK-130206`'s third test asserts *more than
  zero* and never a number, deliberately; nothing here changes that.

## Tests

`DuelTable.test.tsx` — three added to the 24 it has, so the file reports **27**. Each queries
`container.querySelectorAll(".last-act")` and locates a mark with `within(plateFor(name))`.

| Test | Proves |
| --- | --- |
| `marks the seat the act names, at either seat` | **both directions in one test.** With `lastAct` a `PlayerBet` at `seat: 1`, there is exactly one `.last-act` on the table and it is inside `plateFor("Your rival")`; with the same event at `seat: 0`, exactly one and it is inside `plateFor("You")`. A mark wired to the rival — the ask as literally written, which `ADR-0109` §Alternative 6 refused — passes the first case and fails the second |
| `moves the mark rather than adding one when the other seat acts` | render with a `PlayerBet` at `seat: 1` reading `Bet 950`, then re-render with a `PlayerCalled` at `seat: 0` reading `Call 400`. After the re-render there is still exactly **one** `.last-act` on the whole table, it is inside `plateFor("You")`, and the string `950` appears nowhere on the table. A per-seat implementation leaves two marks and fails all three assertions |
| `marks no seat when no act has been made` | with `lastAct` omitted, and again with it `null`, the table renders zero `.last-act`. Both spellings, because the screen passes the store's `null` and the merged tests pass nothing |

The 24 merged tests read names, stacks, buttons, cards, the pot line, presence and the acting mark
— measured, none of them counts elements on the whole table or reads its full text, so no assertion
moves and none is weakened.

## Acceptance criteria

- [ ] `DuelTable.test.tsx` reports `Tests  27 passed (27)`
- [ ] `the duel table.marks the seat the act names, at either seat` passes, and the file contains
      both `seat: 1` and `seat: 0`
- [ ] `the duel table.moves the mark rather than adding one when the other seat acts` passes
- [ ] `the duel table.marks no seat when no act has been made` passes
- [ ] `no-derivation.test.tsx` still reports `Tests  7 passed (7)` and `SeatPlate.test.tsx` still
      reports `Tests  12 passed (12)`
- [ ] `DuelTable.tsx` mentions `lastAct` on at least four lines, and `DuelTable.test.tsx` mentions
      `last-act` on at least five and contains no `data-testid`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
