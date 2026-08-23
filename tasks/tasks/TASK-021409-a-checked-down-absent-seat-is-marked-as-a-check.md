---
schema: 2
id: TASK-021409
title: A checked-down absent seat is marked as a check, where a fold is not legal
type: task
status: backlog
parent: STORY-0214
module: poker-server
estimate: XS
tier: sonnet
review: deep
files_touched: 1
labels: [server, presence, provenance]
depends_on: [TASK-021408]
verify:
  - ./gradlew :poker-server:test --tests '*AbsentSeatsTest'
---

## Goal

The quiet half of a timeout is labelled too. In a spot where `FOLD` is not in the engine's legal
set, the server checks for the absent seat, and the mark says `CHECK` — proving the mark's `action`
is read from what was actually submitted rather than fixed at `FOLD`.

A **test-only** ticket: no production file is opened.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/duel/AbsentSeatsTest.kt` | modify |

Read `docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md` §4 and
`duel/AbsentSeats.kt`. Nothing else.

## Scope

- Two tests added, each reusing a merged fixture that already reaches a spot where
  `legalActions(...).allows(ActionType.FOLD)` is `false` and already asserts it:
  the big blind's option (`anAbsentSeatAtTheBigBlindsOptionProgressesTheHand`) and the first seat on
  a checked-through street (`anAbsentSeatFirstToActOnACheckedStreetProgressesTheHand`).
- Each new test rebuilds its fixture rather than editing the merged one, and keeps that fixture's
  `assertFalse(legalActions(...).allows(ActionType.FOLD))` — without it the test proves nothing,
  because `CHECK` would be the wrong answer if `FOLD` were available.
- Virtual time and the fixed seed only. `HandSeedSource { 7L }`, no clock, no `Thread.sleep`.
- The seat is taken from `state.seatToAct` and `1 - button`, never written as a literal.

## Out of scope

- **Any production change.** If a mark comes back `FOLD` here, that is a defect in `TASK-021408`
  and a new ticket, not an edit from this branch.
- The fold case — `TASK-021408` proves it.
- Marking anything outside `foldAbsent`.

## Tests

`AbsentSeatsTest` — an existing file. Two tests are added; **no merged assertion is edited**, and
the two fixtures above keep passing as they are.

| Test | Proves |
| --- | --- |
| `anAbsentBigBlindsOptionIsMarkedAsACheck` | at the big blind's option, with `FOLD` not legal, the frames `foldAbsent` added contain an `ActedForAbsentSeat` whose `action` is `ActionType.CHECK` and whose `seat` is `bigBlind` |
| `anAbsentSeatOnACheckedStreetIsMarkedAsACheck` | first to act on the turn after a checked-through flop, with `FOLD` not legal, the mark's `action` is `CHECK` and its `seat` is `turnFirst` |
| `theTwoOutcomesAreMarkedDifferently` | in one test, a fold spot yields a mark with `action == FOLD` and a check spot a mark with `action == CHECK` — two inputs, so a constant `action` cannot pass either |

## Acceptance criteria

- [ ] `AbsentSeatsTest.anAbsentBigBlindsOptionIsMarkedAsACheck` passes
- [ ] `AbsentSeatsTest.anAbsentSeatOnACheckedStreetIsMarkedAsACheck` passes
- [ ] `AbsentSeatsTest.theTwoOutcomesAreMarkedDifferently` passes
- [ ] Each new test asserts `FOLD` is not in the legal set at the decision point it acts on
- [ ] No production file is in the diff, and every merged test in the file is byte-identical to its
      state after `TASK-021408`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
