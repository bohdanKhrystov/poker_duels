---
schema: 2
id: TASK-010408
title: Seat chip transitions — commit, award, collect
type: task
status: ready
parent: STORY-0104
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, domain, chips]
depends_on: [TASK-010407]
verify:
  - ./gradlew :poker-engine:test --tests '*SeatChipsTest'
  - ./gradlew :poker-engine:check
---

## Goal

The three ways a seat's chips ever move — into the pot, back out of it, and the end-of-street
sweep — exist as three total functions, so no caller ever edits a stack by hand.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Seat.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/SeatChipsTest.kt` | create |

Read `TASK-010407` for the fields these operate on. Do not change the constructor or its
`require` blocks.

## Scope

Three members on `Seat`, each returning a new `Seat`:

```kotlin
/** Moves [amount] from the stack into this seat's commitment. Going to zero means all-in. */
public fun commit(amount: Int): Seat {
    require(amount in 0..stack) { "Cannot commit $amount from a stack of $stack" }
    return copy(
        stack = stack - amount,
        committedThisStreet = committedThisStreet + amount,
        committedThisHand = committedThisHand + amount,
        isAllIn = isAllIn || (amount > 0 && amount == stack),
    )
}

/** Chips coming back: a won pot, or an uncalled bet returned. */
public fun award(amount: Int): Seat

/** End of a betting round: this street's commitment has gone to the pot. */
public fun collected(): Seat
```

- `award` requires `amount >= 0` and only increases `stack`. It does **not** clear `isAllIn` —
  a seat that was all-in stays all-in for the rest of the hand even after it is paid.
- `collected()` sets `committedThisStreet = 0` and touches nothing else. `committedThisHand`
  is gross and never decreases.
- KDoc on each, saying what it does **not** do, because the omissions above are the ones a
  reader will otherwise assume.

## Out of scope

- Deciding *how much* to commit or award — STORY-0105 and STORY-0106.
- The pot itself — it lives on `GameState` (`TASK-010409`).

## Tests

`SeatChipsTest`, JUnit 5.

| Test | Proves |
| --- | --- |
| `commitMovesChipsFromStackToCommitment` | `Seat(0, 1_000).commit(300)` → stack 700, street 300, hand 300, not all-in |
| `commitAccumulatesWithinAStreet` | `.commit(100).commit(200)` → stack 700, street 300, hand 300 |
| `commitOfTheWholeStackIsAllIn` | `Seat(0, 500).commit(500)` → stack 0, `isAllIn` true |
| `commitOfZeroFromAnEmptyStackIsNotAllIn` | `Seat(0, 0).commit(0).isAllIn` is false |
| `commitRejectsMoreThanTheStack` | `Seat(0, 500).commit(501)` throws `IllegalArgumentException` |
| `commitRejectsANegativeAmount` | `commit(-1)` throws |
| `awardAddsToTheStackOnly` | `Seat(0, 0, committedThisHand = 500, isAllIn = true).award(1_000)` → stack 1 000, `committedThisHand` still 500, `isAllIn` still true |
| `awardRejectsANegativeAmount` | `award(-1)` throws |
| `collectedClearsOnlyTheStreetCommitment` | after `commit(300).collected()` → street 0, hand 300, stack unchanged |
| `chipsAreConservedByCommitAndAward` | for `Seat(0, 1_000)`, `stack + committedThisStreet` is 1 000 after any sequence of `commit` calls, and `stack + committedThisStreet` is 1 300 after `commit(300).collected().award(300)` |

## Acceptance criteria

- [ ] `SeatChipsTest.commitMovesChipsFromStackToCommitment` passes
- [ ] `SeatChipsTest.commitAccumulatesWithinAStreet` passes
- [ ] `SeatChipsTest.commitOfTheWholeStackIsAllIn` passes
- [ ] `SeatChipsTest.commitOfZeroFromAnEmptyStackIsNotAllIn` passes
- [ ] `SeatChipsTest.commitRejectsMoreThanTheStack` passes
- [ ] `SeatChipsTest.commitRejectsANegativeAmount` passes
- [ ] `SeatChipsTest.awardAddsToTheStackOnly` passes
- [ ] `SeatChipsTest.awardRejectsANegativeAmount` passes
- [ ] `SeatChipsTest.collectedClearsOnlyTheStreetCommitment` passes
- [ ] `SeatChipsTest.chipsAreConservedByCommitAndAward` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
