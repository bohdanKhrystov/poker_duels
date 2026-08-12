---
schema: 2
id: TASK-010620
title: A dealt street clears the last aggressor, a closed round does not
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [engine, rules]
depends_on: [TASK-010618]
verify:
  - ./gradlew :poker-engine:test --tests '*DealerProjectionTest'
  - ./gradlew :poker-engine:check
---

## Goal

`lastAggressor` means *this street's* aggressor: `StreetDealt` clears it, and — this is the part
that is easy to get wrong — `BettingRoundEnded` and `ShowdownReached` do not.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/DealerProjection.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/DealerProjectionTest.kt` | modify |

Read `GameState.kt` and `StreetProgression.kt`. Modify neither.

## Scope

- In `applyDealer`, the `StreetDealt` branch also sets `lastAggressor = null`: a new street starts
  with nobody having bet on it, which is exactly ADR-0008's "if the final street was checked
  through, the player first to act shows first".
- **Leave the `BettingRoundEnded` branch alone.** A showdown is reached *after* the river's round
  has ended (see `endBettingRound` in `StreetProgression.kt`), so clearing the field there would
  erase the aggressor the showdown is about to read. Say so in a comment — it is the one
  non-obvious thing in this diff.
- Leave every other branch alone, `ShowdownReached` included.

## Out of scope

- A new **hand** starting clean — that is `HandStarted`, dispatched by `StateProjection`:
  `TASK-010621`.
- Reading the field at showdown — `TASK-010622`.
- Any end-to-end assertion that a bet on the flop does not order a checked-down river's reveals.
  The projection-level test below is the mechanism's test; the showdown wiring is `TASK-010623`.

## Tests

`DealerProjectionTest`

| Test | Proves |
| --- | --- |
| `dealingANewStreetClearsTheLastAggressor` | `applyDealer(handState().copy(lastAggressor = 0), StreetDealt(6, Street.FLOP, cards("As Kd 7c")))` leaves `lastAggressor == null` |
| `endingABettingRoundKeepsTheLastAggressor` | `applyDealer(state.copy(lastAggressor = 1), BettingRoundEnded(1, Street.RIVER))` leaves `lastAggressor == 1` |
| `reachingShowdownKeepsTheLastAggressor` | `applyDealer(riverState.copy(lastAggressor = 1), ShowdownReached(15))` leaves `lastAggressor == 1` |

## Acceptance criteria

- [ ] `DealerProjectionTest.dealingANewStreetClearsTheLastAggressor` passes
- [ ] `DealerProjectionTest.endingABettingRoundKeepsTheLastAggressor` passes
- [ ] `DealerProjectionTest.reachingShowdownKeepsTheLastAggressor` passes
- [ ] Every pre-existing test in `DealerProjectionTest` passes with no change to its body
- [ ] No file outside the table above is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
