---
schema: 2
id: TASK-030610
title: A seat's status is read off the view, never off its cards
type: task
status: backlog
parent: STORY-0306
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, duel, ui]
depends_on: [TASK-030609]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +172 passed \(172\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says a folded seat folded, whatever cards it is holding'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'prefers folded over all in and over the turn'
  - cd web-client && npm run check
---

## Goal

The story's fourth acceptance criterion as a pure function: a folded seat says so because
`hasFolded` says so, an all-in seat because `isAllIn` says so, and neither because of what the seat
is or is not holding.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/seat-status.ts` | create |
| `web-client/src/table/seat-status.test.ts` | create |
| `web-client/src/table/view-fixture.ts` | read — `aSeat` |

## Scope

- The whole module, verbatim:

  ```ts
  import type { SeatView } from "../protocol";

  /**
   * What a seat is doing, in the view's own words.
   *
   * Read in this order because the earlier answers exclude the later ones: a seat
   * that folded is out of the hand, and a seat that is all in cannot be asked to
   * act. Every branch reads a field the server sent — none of it is inferred from
   * whether the seat is holding cards.
   */
  export function seatStatus(
    seat: SeatView,
    isToAct: boolean,
    isViewer: boolean,
  ): string {
    if (seat.hasFolded) return "Folded";
    if (seat.isAllIn) return "All in";
    if (isToAct) return isViewer ? "Your turn" : "Their turn";
    return "";
  }
  ```

- The empty string, not `null`: the design reserves the status line's height in every state
  (`min-height: 1.5em`), so "nothing to say" is text with no characters in it, never an absent
  element.
- `isToAct` is a boolean the caller computes from `view.seatToAct === seat.index` — a comparison of
  two fields the server sent, which is rendering. This function is never handed the whole view, so
  it cannot reach for anything else.
- `Your turn` / `Their turn` are the design's words, from `duel-table.html` and
  `duel-table-states.html`.

## Out of scope

- The plate that shows it — `TASK-030611`, which imports this.
- Any word about *what* a seat may do. `LegalActions` lives in `state.pendingTurn` and belongs to
  `STORY-0307`.
- An "away" or "disconnected" status. `DEC-018` is unanswered and `STORY-0306` scopes it out in as
  many words.

## Tests

`web-client/src/table/seat-status.test.ts`, describe block `"a seat's status"`, built from `aSeat`.

| Test | Proves |
| --- | --- |
| `is empty when the seat is waiting` | `seatStatus(aSeat(), false, true)` is `""` |
| `says whose turn it is from the seat to act` | `(aSeat(), true, true)` is `"Your turn"` and `(aSeat(), true, false)` is `"Their turn"` |
| `says a folded seat folded, whatever cards it is holding` | `hasFolded: true` gives `"Folded"` with `holeCards` unset, with `["Ah", "Ks"]`, and with `[]` |
| `says an all-in seat is all in, whatever cards it is holding` | `isAllIn: true` gives `"All in"` in the same three card states |
| `prefers folded over all in and over the turn` | `{hasFolded: true, isAllIn: true}` with `isToAct` gives `"Folded"`; `{isAllIn: true}` with `isToAct` gives `"All in"` |

Five tests. One hundred and sixty-seven exist, so the suite reports **172**.

The three-card-states shape of tests 3 and 4 is the point: the same answer must come back whether
the seat is holding two cards, none, or the fixture default. That is what makes the derivation
below detectable.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 172 passed (172)` | the five ran and the hundred-and-sixty-seven before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Put the turn first — `if (isToAct) …` above the two field checks → `prefers folded over all in
   and over the turn` fails with `expected 'Your turn' to be 'Folded' // Object.is equality`.
   Revert.
2. Derive folding from the cards — `if (seat.holeCards.length === 0) return "Folded";` → **five**
   tests fail, among them `is empty when the seat is waiting` with `expected 'Folded' to be ''` and
   `says a folded seat folded, whatever cards it is holding` with `expected '' to be 'Folded'` (the
   case where the seat folded *and* the view still carries its cards). This is the exact bug the
   criterion exists to prevent. Revert.
3. Swap the two turn strings → `says whose turn it is from the seat to act` fails with `expected
   'Their turn' to be 'Your turn' // Object.is equality`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `a seat's status > is empty when the seat is waiting` passes
- [ ] `a seat's status > says whose turn it is from the seat to act` passes
- [ ] `a seat's status > says a folded seat folded, whatever cards it is holding` passes
- [ ] `a seat's status > says an all-in seat is all in, whatever cards it is holding` passes
- [ ] `a seat's status > prefers folded over all in and over the turn` passes
- [ ] `seat-status.ts` contains no `holeCards`
- [ ] `npm run --silent test` reports `Tests  172 passed (172)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
