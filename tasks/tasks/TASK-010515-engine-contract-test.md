---
schema: 2
id: TASK-010515
title: Run the engine contract against the real engine
type: task
status: done
parent: STORY-0105
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [engine, contract, test]
depends_on: [TASK-010514]
verify:
  - ./gradlew :poker-engine:test --tests '*DefaultPokerEngineContractTest'
  - ./gradlew :poker-engine:check
---

## Goal

`DefaultPokerEngine` is held to the same suite as any other engine, on positions that only exist
now that hands can actually be played.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/DefaultPokerEngineContractTest.kt` | create |

Read `PokerEngineContract.kt` and `NoOpEngineContractTest.kt` for the shape. Modify neither.

## Scope

- One `internal class DefaultPokerEngineContractTest : PokerEngineContract()` that overrides
  `engine()` with `DefaultPokerEngine`.
- Override `cases()` as `super.cases()` plus the six actions from the base class applied to four
  more positions:

  1. the opening state of `startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L))`,
  2. that state after `DefaultPokerEngine.handle(it, Call(0))` — the big blind's option,
  3. a flop position with `betToMatch = 300` and seat 0 to act,
  4. a position whose opponent is all-in: seat 1 `isAllIn`, `betToMatch = 450`, seat 0 to act.

- No new assertions: the four inherited tests are the point. Add nothing else to this file.

## Out of scope

- Changing `PokerEngineContract` itself. If a case here cannot be expressed, that is a finding
  for the report, not an edit — the suite is shared with `NoOpEngine`.

## Tests

`DefaultPokerEngineContractTest`, inheriting all four tests from `PokerEngineContract`.

| Test | Proves |
| --- | --- |
| `eventsAlwaysDescribeTheStateTransition` | folding the emitted events over the old state reproduces the new one, on every position above |
| `aRejectedActionChangesNothing` | rejections leave the state identical and emit no events |
| `handleIsPure` | the same state and action give the same result twice |
| `eventsContinueTheHandSequence` | sequences are dense from `state.eventCount` and `newState.eventCount` agrees |

## Acceptance criteria

- [ ] `DefaultPokerEngineContractTest.eventsAlwaysDescribeTheStateTransition` passes
- [ ] `DefaultPokerEngineContractTest.aRejectedActionChangesNothing` passes
- [ ] `DefaultPokerEngineContractTest.handleIsPure` passes
- [ ] `DefaultPokerEngineContractTest.eventsContinueTheHandSequence` passes
- [ ] `PokerEngineContract.kt` is unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
