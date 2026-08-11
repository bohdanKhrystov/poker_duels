---
schema: 2
id: TASK-010422
title: Fold betting events into a state
type: task
status: ready
parent: STORY-0104
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, contract, chips]
depends_on: [TASK-010421]
verify:
  - ./gradlew :poker-engine:test --tests '*BettingProjectionTest'
  - ./gradlew :poker-engine:check
---

## Goal

Every chip a player moves can be replayed from the log alone: one function turns a `BettingEvent`
plus the state before it into the state after it.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/BettingProjection.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/BettingProjectionTest.kt` | create |

Read `BettingEvents.kt`, `GameState.kt` and `Seat.kt`. Do not modify them.

## Scope

- `BettingProjection.kt`, package `duels.poker.engine.game`, one public function and small
  private helpers. It is exhaustive over `BettingEvent` with **no `else`**:

  ```kotlin
  public fun applyBetting(state: GameState, event: BettingEvent): GameState
  ```

- The rules, exactly:

  | Event | Effect on the state |
  | --- | --- |
  | `PlayerFolded` | that seat's `hasFolded = true` |
  | `PlayerChecked` | nothing but the actor |
  | `PlayerCalled` | commit up to `to`; `betToMatch` and `minRaiseTo` unchanged |
  | `PlayerBet` | commit up to `to`; then `betToMatch = to`, `minRaiseTo = to + (to - betToMatch)` |
  | `PlayerRaised` | identical to `PlayerBet` |
  | `PlayerAllIn` | commit up to `to` and force `isAllIn = true`; then see below |

  and **every** branch ends with `seatToAct = null` — the actor is named by the next `ActionOn`
  event, never inferred here.

- "Commit up to `to`" is `state.withSeat(event.seat) { it.commit(to - it.committedThisStreet) }`.
  `Seat.commit` already refuses to overdraw a stack, so a malformed log throws rather than
  inventing chips.
- `PlayerAllIn` when `to > betToMatch`: `betToMatch = to`, and `minRaiseTo` becomes
  `to + (to - betToMatch)` **only if `to >= minRaiseTo`** — an all-in short of a full raise does
  not raise the bar for the next raise. When `to <= betToMatch` (an all-in call for less) neither
  value moves. Cite `docs/duel-rules.md` in the KDoc.
- `minRaiseTo` is a minimum, not permission: whether a seat that faced a short all-in may raise
  at all is `TASK-010502`'s answer, not this file's. Say so in the KDoc.
- `applyBetting` is public because `StateProjection` (`TASK-010425`) dispatches to it; its KDoc
  names `StateProjection.apply` as the entry point callers should use.

## Out of scope

- Dealer events — `TASK-010423`. The dispatching entry point — `TASK-010425`.
- Deciding whether an event was legal. The projection replays what happened; STORY-0105 decides
  what may happen.

## Tests

`BettingProjectionTest`, JUnit 5, building positions with `handState()` and `seats()` from
`GameStates.kt` plus `copy`.

| Test | Proves |
| --- | --- |
| `foldMarksTheSeatAndClearsTheActor` | `PlayerFolded(1, 0)` → `seat(0).hasFolded`, `seatToAct == null`, chips untouched |
| `checkClearsOnlyTheActor` | `PlayerChecked(1, 0)` changes nothing else |
| `callMovesChipsUpToTheBar` | from `betToMatch = 300` with seat 0 committed 100, `PlayerCalled(1, 0, 300)` → stack down 200, `committedThisStreet == 300`, `betToMatch == 300` |
| `callAllInForLessLeavesTheBarWhereItIs` | seat 0 with a 150 stack, `betToMatch = 300`: `PlayerCalled(1, 0, 150)` → `isAllIn`, stack 0, `betToMatch` still 300 |
| `betSetsTheBarAndDoublesTheMinimumRaise` | fresh street, `PlayerBet(1, 0, 300)` → `betToMatch == 300`, `minRaiseTo == 600` |
| `raiseAddsItsOwnIncrementToTheMinimum` | `betToMatch = 300`, `minRaiseTo = 600`: `PlayerRaised(1, 1, 900)` → `betToMatch == 900`, `minRaiseTo == 1_500` |
| `aFullAllInRaisesTheMinimum` | `betToMatch = 300`, `minRaiseTo = 600`: `PlayerAllIn(1, 1, 900)` → `betToMatch == 900`, `minRaiseTo == 1_500`, `isAllIn` |
| `aShortAllInDoesNotRaiseTheMinimum` | `betToMatch = 300`, `minRaiseTo = 600`: `PlayerAllIn(1, 1, 450)` → `betToMatch == 450`, `minRaiseTo` still 600 |
| `chipsAreConservedByEveryBettingEvent` | `chipsInPlay` is unchanged after each of a bet, a raise, a call, an all-in and a fold |
| `rejectsAnEventThatWouldOverdrawAStack` | `PlayerBet(1, 0, 20_000)` against a 10 000 stack throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `BettingProjectionTest.foldMarksTheSeatAndClearsTheActor` passes
- [ ] `BettingProjectionTest.checkClearsOnlyTheActor` passes
- [ ] `BettingProjectionTest.callMovesChipsUpToTheBar` passes
- [ ] `BettingProjectionTest.callAllInForLessLeavesTheBarWhereItIs` passes
- [ ] `BettingProjectionTest.betSetsTheBarAndDoublesTheMinimumRaise` passes
- [ ] `BettingProjectionTest.raiseAddsItsOwnIncrementToTheMinimum` passes
- [ ] `BettingProjectionTest.aFullAllInRaisesTheMinimum` passes
- [ ] `BettingProjectionTest.aShortAllInDoesNotRaiseTheMinimum` passes
- [ ] `BettingProjectionTest.chipsAreConservedByEveryBettingEvent` passes
- [ ] `BettingProjectionTest.rejectsAnEventThatWouldOverdrawAStack` passes
- [ ] `applyBetting` contains no `else` branch
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
