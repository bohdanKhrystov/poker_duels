---
schema: 2
id: TASK-010414
title: LegalActions descriptor
type: task
status: backlog
parent: STORY-0104
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, domain, contract]
depends_on: [TASK-010413]
verify:
  - ./gradlew :poker-engine:test --tests '*LegalActionsTest'
  - ./gradlew :poker-engine:check
---

## Goal

Everything a client needs to draw the right buttons and sliders, as one value it can render
without owning a single poker rule.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/LegalActions.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/LegalActionsTest.kt` | create |

Read `PlayerAction.kt` for `ActionType`, and the "Betting" table in `docs/duel-rules.md`.

## Scope

- `LegalActions.kt`, package `duels.poker.engine.game`:

  ```kotlin
  public data class LegalActions(
      val seat: Int,
      val allowed: Set<ActionType>,
      val callTo: Int = 0,
      val minBetTo: Int = 0,
      val minRaiseTo: Int = 0,
      val allInTo: Int = 0,
  ) {
      public fun allows(type: ActionType): Boolean = type in allowed
      public companion object {
          /** Nothing is legal — it is not this seat's turn, or the hand is over. */
          public fun none(seat: Int): LegalActions = LegalActions(seat, emptySet())
      }
  }
  ```

- Every amount is a **street total**, matching `PlayerAction`: `callTo` is the total this seat
  will have committed after calling (capped at its stack, so calling all-in for less is
  expressible), `allInTo` is its total if it puts everything in, and `minBetTo`/`minRaiseTo` are
  the smallest legal totals for those actions. KDoc says all of this on the class.
- `init` requires:
  - every amount `>= 0`,
  - `ActionType.CHECK` and `ActionType.CALL` are never both allowed — a seat either faces a bet
    or it does not,
  - `ActionType.BET` and `ActionType.RAISE` are never both allowed, for the same reason,
  - `BET in allowed` implies `minBetTo in 1..allInTo`,
  - `RAISE in allowed` implies `minRaiseTo in 1..allInTo`.
- KDoc must show the facing-an-uncoverable-all-in case: `allowed = setOf(FOLD, CALL)` with
  `callTo == allInTo` and no raise.

## Out of scope

- Computing this from a `GameState` — `TASK-010502`, STORY-0105. This ticket defines the shape
  only; it reads no state.
- Rejecting an action — `Rejection`, `TASK-010413`.

## Tests

`LegalActionsTest`, JUnit 5.

| Test | Proves |
| --- | --- |
| `describesAnUnopenedStreet` | `allowed = setOf(CHECK, BET, ALL_IN)`, `minBetTo = 100`, `allInTo = 10_000` constructs; `allows(CHECK)` is true and `allows(CALL)` is false |
| `describesFacingABet` | `allowed = setOf(FOLD, CALL, RAISE, ALL_IN)`, `callTo = 300`, `minRaiseTo = 600`, `allInTo = 10_000` constructs and reads back |
| `describesAnUncoverableAllIn` | `allowed = setOf(FOLD, CALL)`, `callTo = 400`, `allInTo = 400` constructs, and `allows(RAISE)` is false |
| `noneAllowsNothing` | `LegalActions.none(1).allowed` is empty and `allows(FOLD)` is false |
| `rejectsCheckAndCallTogether` | `setOf(CHECK, CALL)` throws `IllegalArgumentException` |
| `rejectsBetAndRaiseTogether` | `setOf(BET, RAISE)` throws |
| `rejectsANegativeAmount` | `callTo = -1` throws |
| `rejectsABetMinimumAboveTheAllInTotal` | `allowed = setOf(BET)`, `minBetTo = 500`, `allInTo = 400` throws |
| `rejectsARaiseMinimumAboveTheAllInTotal` | `allowed = setOf(RAISE)`, `minRaiseTo = 900`, `allInTo = 800` throws |

## Acceptance criteria

- [ ] `LegalActionsTest.describesAnUnopenedStreet` passes
- [ ] `LegalActionsTest.describesFacingABet` passes
- [ ] `LegalActionsTest.describesAnUncoverableAllIn` passes
- [ ] `LegalActionsTest.noneAllowsNothing` passes
- [ ] `LegalActionsTest.rejectsCheckAndCallTogether` passes
- [ ] `LegalActionsTest.rejectsBetAndRaiseTogether` passes
- [ ] `LegalActionsTest.rejectsANegativeAmount` passes
- [ ] `LegalActionsTest.rejectsABetMinimumAboveTheAllInTotal` passes
- [ ] `LegalActionsTest.rejectsARaiseMinimumAboveTheAllInTotal` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
