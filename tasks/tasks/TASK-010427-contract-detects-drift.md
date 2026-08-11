---
schema: 2
id: TASK-010427
title: Prove the contract suite catches a drifting engine
type: task
status: ready
parent: STORY-0104
module: poker-engine
estimate: XS
tier: sonnet
review: deep
files_touched: 1
labels: [engine, contract, test]
depends_on: [TASK-010426]
verify:
  - ./gradlew :poker-engine:test --tests '*ContractDetectsDriftTest'
  - ./gradlew :poker-engine:check
---

## Goal

A permanent demonstration that the fold check has teeth: an engine whose `newState` disagrees
with its events fails, and the failure is what the suite is for.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/ContractDetectsDriftTest.kt` | create |

Read `PokerEngineContract.kt` (`TASK-010426`) for `assertEventsDescribeTheTransition`, and
`GameStates.kt`.

## Scope

- One test class holding two deliberately wrong engines, declared inside the file:
  - `SilentEngine` — moves chips in `newState` (a seat commits 300) but returns **no** events.
  - `LyingEngine` — emits `PlayerBet(state.eventCount, 0, 300)` but returns `newState` unchanged.
- Each is run through `assertEventsDescribeTheTransition` and must raise an `AssertionError`,
  caught with `assertThrows<AssertionError>` (`org.junit.jupiter.api.Assertions.assertThrows`).
- A third case runs a correct result — events and state agreeing, built with
  `StateProjection.apply` — through the same function and asserts it does **not** throw, so the
  test cannot pass by the checker being broken in the other direction.
- A comment saying why this file exists: without it, a checker that silently stopped asserting
  would leave every engine test green and this project's central invariant unguarded.

## Out of scope

- Any change to `PokerEngineContract` or `StateProjection`. If the drift is not detected, the
  defect is in `TASK-010426` and belongs in a follow-up ticket, not in this file.
- Testing real engine behaviour — STORY-0105.

## Tests

`ContractDetectsDriftTest`, JUnit 5.

| Test | Proves |
| --- | --- |
| `aStateChangeWithNoEventsFails` | `SilentEngine`'s result raises `AssertionError` |
| `anEventWithNoStateChangeFails` | `LyingEngine`'s result raises `AssertionError` |
| `anAgreeingResultPasses` | a result built with `StateProjection.apply` raises nothing |

## Acceptance criteria

- [ ] `ContractDetectsDriftTest.aStateChangeWithNoEventsFails` passes
- [ ] `ContractDetectsDriftTest.anEventWithNoStateChangeFails` passes
- [ ] `ContractDetectsDriftTest.anAgreeingResultPasses` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
