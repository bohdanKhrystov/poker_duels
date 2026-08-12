---
schema: 2
id: TASK-010619
title: A bet, a raise or a full all-in records its seat as the last aggressor
type: task
status: done
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, rules]
depends_on: [TASK-010618]
verify:
  - ./gradlew :poker-engine:test --tests '*BettingProjectionTest'
  - ./gradlew :poker-engine:check
---

## Goal

Folding a `BettingEvent` that moves the bar sets `GameState.lastAggressor` to the seat that moved
it, so at showdown the state names the street's aggressor without replaying the log.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/BettingProjection.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/BettingProjectionTest.kt` | modify |

Read `GameState.kt` and `BettingEvents.kt`. Modify neither.

## Scope

- In `applyBetting`: `PlayerBet` and `PlayerRaised` set `lastAggressor = event.seat`.
- `PlayerAllIn` sets it **only when `event.to > state.betToMatch`** — an all-in that moves the bar
  is aggression; a short all-in call for less than the bar is not, and must leave the field alone.
  This mirrors the existing `allInTo` branch that only raises `minRaiseTo` when the bar moves.
- `PlayerFolded`, `PlayerChecked` and `PlayerCalled` leave the field untouched.
- Extend the KDoc paragraph on `PlayerAllIn` to say why a short all-in is not aggression, and
  point at [`ADR-0008`](../../docs/adr/ADR-0008-loser-mucks-at-showdown.md) for what the field is
  used for.
- Blinds are not aggression and need no thought here: `BlindPosted` is dispatched by
  `StateProjection`, never by this function.

## Out of scope

- Clearing the field on a new street — `TASK-010620` — or on a new hand — `TASK-010621`.
- Reading the field at showdown — `TASK-010622`.

## Tests

`BettingProjectionTest`

| Test | Proves |
| --- | --- |
| `aBetRecordsTheBettorAsTheLastAggressor` | `PlayerBet(1, 0, 300)` on `handState()` leaves `lastAggressor == 0` |
| `aRaiseRecordsTheRaiserAsTheLastAggressor` | `PlayerRaised(1, 1, 900)` over a 300 bar leaves `lastAggressor == 1` |
| `aFullAllInRecordsTheAggressor` | `PlayerAllIn(1, 1, 900)` over a 300 bar leaves `lastAggressor == 1` |
| `aShortAllInIsNotAggression` | `PlayerAllIn(1, 0, 150)` under a 300 bar leaves `lastAggressor` as it was — assert against a state built with `copy(lastAggressor = 1)` so the test proves the field is *preserved*, not merely null |
| `callingAndCheckingAreNotAggression` | a `PlayerCalled` and a `PlayerChecked` folded onto `copy(lastAggressor = 1)` both leave `lastAggressor == 1` |

## Acceptance criteria

- [ ] `BettingProjectionTest.aBetRecordsTheBettorAsTheLastAggressor` passes
- [ ] `BettingProjectionTest.aRaiseRecordsTheRaiserAsTheLastAggressor` passes
- [ ] `BettingProjectionTest.aFullAllInRecordsTheAggressor` passes
- [ ] `BettingProjectionTest.aShortAllInIsNotAggression` passes
- [ ] `BettingProjectionTest.callingAndCheckingAreNotAggression` passes
- [ ] Every pre-existing test in `BettingProjectionTest` passes with no change to its body —
      including `checkClearsOnlyTheActor`, which compares whole states and would fail if a check
      touched the field
- [ ] No file outside the table above is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
