---
schema: 2
id: TASK-031301
title: The seat's status line learns Away and Timed out, and where they rank
type: task
status: ready
parent: STORY-0313
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, presence]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +581 passed \(581\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says a seat whose window is still running is away'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says a seat whose window ran out timed out'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says nothing of its own for a seat that is present'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'prefers the presence over the turn'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'prefers a fact about the hand over the presence'
  - cd web-client && npm run check
---

## Goal

`seatStatus` answers `Away` and `Timed out` when it is told a seat's presence, ranked above the
turn and below anything true about the hand.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/seat-status.ts` | modify — one parameter, two branches |
| `web-client/src/table/seat-status.test.ts` | modify — five tests added, none changed |
| `web-client/src/protocol/protocol.gen.ts` | read — `SeatPresence` is `"PRESENT" \| "AWAY" \| "ABSENT"`, already generated |

## Scope

- `seatStatus` gains a **fourth, optional** parameter and two branches:

  ```ts
  export function seatStatus(
    seat: SeatView,
    isToAct: boolean,
    isViewer: boolean,
    presence: SeatPresence | null = null,
  ): string {
    if (seat.hasFolded) return "Folded";
    if (seat.isAllIn) return "All in";
    // ADR-0046 §1: presence outranks the turn, because `Their turn` on a seat nobody is
    // sitting at blames a pause on thinking — and never outranks a fact about the hand,
    // which stays true whoever is at the keyboard.
    if (presence === "AWAY") return "Away";
    if (presence === "ABSENT") return "Timed out";
    if (isToAct) return isViewer ? "Your turn" : "Their turn";
    return "";
  }
  ```

- `SeatPresence` is imported as a type from `../protocol`, beside `SeatView`.
- **Optional on purpose.** `SeatPlate.tsx` calls this with three arguments today and is not edited
  here — `TASK-031307` is what hands it a presence. A required parameter would drag two files this
  ticket has no budget for.
- `"PRESENT"` and `null` both fall through to the existing answers. There is no fourth word:
  `ADR-0046` §1 gives `PRESENT` nothing of its own.

## Out of scope

- Every string that is not one of these two words. The explaining line is `TASK-031302`.
- Any component. `SeatPlate` is `TASK-031307`, `DuelTable` is `TASK-031308`.
- The store. No reducer field exists yet; `TASK-031303` adds it.

## Tests

`web-client/src/table/seat-status.test.ts`, describe block `"a seat's status"`. Five added; the five
already there keep their names, their inputs and their expectations, because a default of `null`
makes every existing call mean exactly what it meant.

`a seat's status`

| Test | Proves |
| --- | --- |
| `says a seat whose window is still running is away` | `seatStatus(aSeat(), false, false, "AWAY")` is `Away`, **and so is** `seatStatus(aSeat(), false, true, "AWAY")` — the word is the seat's presence, not a fact about who is looking |
| `says a seat whose window ran out timed out` | `seatStatus(aSeat(), false, false, "ABSENT")` is `Timed out`. Asserted separately from `AWAY` so a branch that answered one word for both fails here |
| `says nothing of its own for a seat that is present` | `"PRESENT"` and `null` both give `""` when nothing else applies, **and both give `Their turn`** when `isToAct` is true — the ordinary status returns rather than being suppressed |
| `prefers the presence over the turn` | with `isToAct: true`, `"AWAY"` gives `Away` and `"ABSENT"` gives `Timed out`; neither gives `Their turn` or `Your turn`. Both `isViewer` values are driven, so a branch that let the viewer's own turn win fails |
| `prefers a fact about the hand over the presence` | `hasFolded` with `"AWAY"` gives `Folded`; `isAllIn` with `"ABSENT"` gives `All in`; and `hasFolded` with `"ABSENT"` gives `Folded`. Three pairs, because one pair cannot tell which of the two rules is doing the work |

Five tests. Five hundred and seventy-six exist, so the suite reports **581**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 581 passed (581)` | five ran and the five hundred and seventy-six before them still do |
| the five `--reporter=verbose` greps | each exists by name |
| `npm run check` | `SeatPresence` typechecks, and the three-argument call in `SeatPlate.tsx` still compiles |

**Name the edit that makes each assertion red:**

1. Move the two presence branches **above** `if (seat.hasFolded)` → `prefers a fact about the hand
   over the presence` fails with `Away` against `Folded`. Revert.
2. Move the two presence branches **below** `if (isToAct)` → `prefers the presence over the turn`
   fails with `Their turn` against `Away`. Revert.

Both are direction errors in the same rule and each fails a different test, which is the point of
having both: one of them alone would leave half the ordering unpinned.

## Acceptance criteria

- [ ] `a seat's status > says a seat whose window is still running is away` passes
- [ ] `a seat's status > says a seat whose window ran out timed out` passes
- [ ] `a seat's status > says nothing of its own for a seat that is present` passes
- [ ] `a seat's status > prefers the presence over the turn` passes
- [ ] `a seat's status > prefers a fact about the hand over the presence` passes
- [ ] The five tests already in the file are byte-identical to `develop`
- [ ] `seat-status.ts` returns exactly the strings `Folded`, `All in`, `Away`, `Timed out`,
      `Your turn`, `Their turn` and `""`, and no others
- [ ] `npm run --silent test` reports `Tests  581 passed (581)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
