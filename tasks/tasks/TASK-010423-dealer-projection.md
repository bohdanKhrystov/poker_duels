---
schema: 2
id: TASK-010423
title: Fold dealer events into a state
type: task
status: done
parent: STORY-0104
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, contract, chips]
depends_on: [TASK-010422]
verify:
  - ./gradlew :poker-engine:test --tests '*DealerProjectionTest'
  - ./gradlew :poker-engine:check
---

## Goal

The other half of the replay: closing a betting round, putting cards out, reaching showdown and
settling the pot all reproduce from the log.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/DealerProjection.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/DealerProjectionTest.kt` | create |

Read `DealerEvents.kt`, `GameState.kt`, `Seat.kt` and `BettingProjection.kt` (as the shape to
follow). Do not modify them.

## Scope

- `DealerProjection.kt`, package `duels.poker.engine.game`, exhaustive over `DealerEvent` with
  **no `else`**:

  ```kotlin
  public fun applyDealer(state: GameState, event: DealerEvent): GameState
  ```

- The rules, exactly:

  | Event | Effect on the state |
  | --- | --- |
  | `BettingRoundEnded` | `pot = potTotal`; every seat `collected()`; `betToMatch = 0`; `minRaiseTo = bigBlind`; `seatToAct = null` |
  | `StreetDealt` | `street = event.street`; `board = board.dealt(event.cards)` |
  | `ShowdownReached` | `street = Street.SHOWDOWN`; `seatToAct = null` |
  | `HandRevealed` | that seat's `holeCards = event.cards` |
  | `UncalledBetReturned` | that seat `award(amount)`; `pot -= amount` |
  | `PotAwarded` | that seat `award(amount)`; `pot -= amount` |
  | `HandFinished` | `street = Street.COMPLETE`; `seatToAct = null` |

- Both settlement branches move chips *between* the pot and a stack and nothing else, which is
  what keeps `chipsInPlay` constant. Taking more than the pot holds drives `pot` negative and
  `GameState`'s own `require` throws — leave that as the guard; do not add a second one.
- `applyDealer` is public for the same reason as `applyBetting`, and its KDoc names
  `StateProjection.apply` as the entry point.

## Out of scope

- Settlement tests — `TASK-010424` covers `UncalledBetReturned`, `PotAwarded` and `HandFinished`
  in depth. Implement all seven branches here; test the first four.
- Deciding *when* a round ends or *who* is paid — STORY-0105 and STORY-0106.

## Tests

`DealerProjectionTest`, JUnit 5, using `handState()`, `seats()` and `cards(...)`.

| Test | Proves |
| --- | --- |
| `endingARoundSweepsCommitmentsIntoThePot` | both seats committed 300 with `pot = 0` → `pot == 600`, both `committedThisStreet == 0`, stacks untouched |
| `endingARoundResetsTheBarAndTheMinimumRaise` | after the same event, `betToMatch == 0`, `minRaiseTo == state.bigBlind`, `seatToAct == null` |
| `chipsAreConservedWhenARoundEnds` | `chipsInPlay` is the same before and after |
| `dealingTheFlopAdvancesTheStreetAndTheBoard` | from preflop, `StreetDealt(6, FLOP, cards("As Kd 7c"))` → `street == FLOP`, `board.size == 3` |
| `dealingTheTurnAndRiverAddOneCardEach` | applying `TURN` then `RIVER` events reaches `street == RIVER` and `board.size == 5` |
| `showdownReachedSetsTheStreet` | from a river state, `street == SHOWDOWN` and `seatToAct == null` |
| `revealingAHandRecordsTheHoleCards` | `HandRevealed(9, 1, cards("As Kd"))` → `seat(1).holeCards` is those two, seat 0 untouched |

## Acceptance criteria

- [ ] `DealerProjectionTest.endingARoundSweepsCommitmentsIntoThePot` passes
- [ ] `DealerProjectionTest.endingARoundResetsTheBarAndTheMinimumRaise` passes
- [ ] `DealerProjectionTest.chipsAreConservedWhenARoundEnds` passes
- [ ] `DealerProjectionTest.dealingTheFlopAdvancesTheStreetAndTheBoard` passes
- [ ] `DealerProjectionTest.dealingTheTurnAndRiverAddOneCardEach` passes
- [ ] `DealerProjectionTest.showdownReachedSetsTheStreet` passes
- [ ] `DealerProjectionTest.revealingAHandRecordsTheHoleCards` passes
- [ ] `applyDealer` handles all seven `DealerEvent` subtypes and contains no `else` branch
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
