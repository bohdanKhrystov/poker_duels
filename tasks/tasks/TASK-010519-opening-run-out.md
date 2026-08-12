---
schema: 2
id: TASK-010519
title: Do not stall a hand whose blinds leave nobody able to act
type: task
status: ready
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [engine, rules]
depends_on: [TASK-010518]
verify:
  - ./gradlew :poker-engine:test --tests '*OpeningRunOutTest'
  - ./gradlew :poker-engine:check
---

## Goal

A hand in which posting the blinds already puts a seat all-in reaches the showdown instead of
waiting forever for a player who has no chips and no legal action.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StreetProgression.kt` | modify |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/HandSetup.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/OpeningRunOutTest.kt` | create |

Read `HeadsUpOrder.kt` and `DealerEvents.kt`. Modify neither.

## Scope

- Extract the round-is-over branch of `continueHand` — `BettingRoundEnded`, then either the
  next street, the run-out, or `ShowdownReached` — into a public function of its own:

  ```kotlin
  /** Closes the current betting round and takes the hand as far as it can go without a player. */
  public fun endBettingRound(state: GameState): EngineResult
  ```

  `continueHand` then calls it. Behaviour is unchanged; this is a pure extraction so a second
  caller exists.
- `startHand` emits `ActionOn` only when both seats can act (`!hasFolded && !isAllIn &&
  stack > 0`). Otherwise it emits no `ActionOn` and appends `endBettingRound`'s events instead,
  which sweeps the blinds and runs the board out to `ShowdownReached`.
- This is reachable whenever a stack is at most its own blind — a freezeout produces it
  (STORY-0107). Without it the hand deadlocks: the seat on turn has no chips, so
  `legalActions` is empty and every action is rejected.
- Nothing else about `startHand` changes: the first six events keep their order and sequences.

## Out of scope

- Awarding the pot or returning the blind nobody could call — STORY-0106.
- Refusing to start a hand at all — STORY-0107 decides when a match is over.

## Tests

`OpeningRunOutTest`, JUnit 5, calling `startHand(1, 0, stacks, 50, 100, SplitMix64Rng(1L))`.

| Test | Proves |
| --- | --- |
| `aButtonAllInOnItsBlindStillReachesShowdown` | stacks `listOf(50, 10_000)`: `street == SHOWDOWN`, `board.size == 5`, `seatToAct == null` |
| `bothBlindsAllInReachShowdown` | stacks `listOf(50, 80)`: `street == SHOWDOWN`, both seats `isAllIn` |
| `anOpeningRunOutEmitsNoActionOn` | stacks `listOf(50, 10_000)`: no `ActionOn` in the events |
| `anOrdinaryHandStillGetsItsActionOn` | stacks `listOf(10_000, 10_000)`: last event is `ActionOn(0)` and `street == PREFLOP` |
| `chipsAreConservedInAnOpeningRunOut` | stacks `listOf(50, 10_000)`: `chipsInPlay == 10_050` and `potTotal == 150` |
| `theEventsDescribeTheState` | `StateProjection.fold` over the events reproduces `newState` except `deck` and `rng` |

## Acceptance criteria

- [ ] `OpeningRunOutTest.aButtonAllInOnItsBlindStillReachesShowdown` passes
- [ ] `OpeningRunOutTest.bothBlindsAllInReachShowdown` passes
- [ ] `OpeningRunOutTest.anOpeningRunOutEmitsNoActionOn` passes
- [ ] `OpeningRunOutTest.anOrdinaryHandStillGetsItsActionOn` passes
- [ ] `OpeningRunOutTest.chipsAreConservedInAnOpeningRunOut` passes
- [ ] `OpeningRunOutTest.theEventsDescribeTheState` passes
- [ ] `HandSetupTest`, `HandDealTest`, `StreetAdvanceTest` and `AllInRunOutTest` still pass unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
