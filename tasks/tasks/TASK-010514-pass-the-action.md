---
schema: 2
id: TASK-010514
title: Pass the action to the other seat while the round runs
type: task
status: done
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [engine, rules]
depends_on: [TASK-010513]
verify:
  - ./gradlew :poker-engine:test --tests '*DefaultPokerEngineTest'
  - ./gradlew :poker-engine:check
---

## Goal

Two players can trade actions inside a betting round: every accepted action that leaves the
opponent something to decide is followed by an `ActionOn` naming them.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StreetProgression.kt` | modify |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/DefaultPokerEngine.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/DefaultPokerEngineTest.kt` | modify |

Read `HandSetup.kt` and `StateProjection.kt`. Modify neither.

## Scope

- A second public function in `StreetProgression.kt` — the single place the hand's continuation
  is decided, and the one every later ticket in this story extends:

  ```kotlin
  /**
   * The events that follow [lastAction]'s own event. [state] is the state after that event has
   * been applied, so `seatToAct` is already null.
   */
  public fun continueHand(state: GameState, lastAction: PlayerAction): EngineResult
  ```

- This ticket's whole body: if `roundContinues(state, lastAction)`, emit
  `ActionOn(state.eventCount, otherSeat(lastAction.seat))` and apply it with
  `StateProjection.apply`; otherwise return `EngineResult.accepted(state, emptyList())`.
- Note in the KDoc that the round-is-over branch is still empty and name its tickets:
  `TASK-010516` (fold), `TASK-010517` (street advance), `TASK-010518` (run-out).
- `DefaultPokerEngine.handle` grows one step: apply the betting event, call `continueHand`, and
  return `EngineResult.accepted(continued.newState, listOf(event) + continued.events)`. It gains
  no rules of its own and never changes again in this story. Delete the "the hand stops after
  each action" note from `TASK-010512`.
- Sequences stay dense: every event is built from the running state's `eventCount`.

## Out of scope

- Ending the round or the hand — the three tickets named above.

## Tests

`DefaultPokerEngineTest`, JUnit 5. Open hands with
`startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L)).newState` and drive them
through `DefaultPokerEngine.handle`.

| Test | Proves |
| --- | --- |
| `theSmallBlindActsFirstPreflop` | the opening state has `seatToAct == 0` |
| `aLimpPassesTheOptionToTheBigBlind` | after `Call(0)`: last event is `ActionOn(1)`, `seatToAct == 1` |
| `aRaisePassesTheActionBack` | after `Call(0)`, `Raise(1, 300)`: `seatToAct == 0` |
| `theActionAlternatesUntilTheRoundEnds` | `Call(0)`, `Raise(1, 300)`, `Call(0)` — the seats to act are 1, 0, then `null` |
| `aCompletedRoundNamesNoNewActor` | after `Call(0)`, `Check(1)`: no `ActionOn` in the result and `seatToAct == null` |
| `theEventsDescribeTheTransition` | `assertEventsDescribeTheTransition` holds for each action above |
| `theSequencesStayDense` | across the whole sequence of actions, event sequences are `0, 1, 2, …` with no gap |

## Acceptance criteria

- [ ] `DefaultPokerEngineTest.theSmallBlindActsFirstPreflop` passes
- [ ] `DefaultPokerEngineTest.aLimpPassesTheOptionToTheBigBlind` passes
- [ ] `DefaultPokerEngineTest.aRaisePassesTheActionBack` passes
- [ ] `DefaultPokerEngineTest.theActionAlternatesUntilTheRoundEnds` passes
- [ ] `DefaultPokerEngineTest.aCompletedRoundNamesNoNewActor` passes
- [ ] `DefaultPokerEngineTest.theEventsDescribeTheTransition` passes
- [ ] `DefaultPokerEngineTest.theSequencesStayDense` passes
- [ ] `DefaultPokerEngineTest` passes with only the assertions that pinned the single-event
      result updated — `aLegalBetEmitsOnePlayerBetAndMovesTheChips` and
      `theSequenceContinuesFromEventCount` now expect the trailing `ActionOn`. Those two
      assertions described `TASK-010512`'s deliberate stopping point, which this ticket exists
      to remove. Every other assertion in that file is unchanged, and none is weakened.
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
