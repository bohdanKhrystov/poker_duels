---
schema: 2
id: TASK-010426
title: PokerEngineContract — the reusable engine test suite
type: task
status: done
parent: STORY-0104
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, contract, test]
depends_on: [TASK-010425]
verify:
  - ./gradlew :poker-engine:test --tests '*NoOpEngineContractTest'
  - ./gradlew :poker-engine:check
---

## Goal

The assertion that keeps this architecture from rotting exists and runs: on every `handle` call,
the events fold over the old state into exactly the new state.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/PokerEngineContract.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/NoOpEngineContractTest.kt` | create |

Read `docs/adr/ADR-0001-event-sourced-engine-contract.md` (the "Given up" section names the risk
this ticket closes), `StateProjection.kt`, `PokerEngine.kt` and `NoOpEngine.kt`.

## Scope

- `PokerEngineContract.kt`, test sources, two things:

  ```kotlin
  /**
   * Fails unless [result]'s events, folded over [before], reproduce its own `newState`.
   *
   * `deck` and `rng` are copied across before comparing: no event carries an undealt card or a
   * seed, by design (see StateProjection), so those two fields cannot be reproduced from a log
   * and are the one deliberate gap in this check. Every other field must match exactly.
   */
  internal fun assertEventsDescribeTheTransition(before: GameState, result: EngineResult)

  /** The suite every [PokerEngine] implementation must pass. Subclass it, supply the engine. */
  internal abstract class PokerEngineContract {
      protected abstract fun engine(): PokerEngine

      /** Positions and actions to exercise. Override to add implementation-specific ones. */
      protected open fun cases(): List<Pair<GameState, PlayerAction>> = ...

      /** Every call in the suite goes through here, so the fold check is unavoidable. */
      protected fun handle(state: GameState, action: PlayerAction): EngineResult
  }
  ```

- `handle` calls `engine().handle(state, action)`, runs `assertEventsDescribeTheTransition`, and
  returns the result. No test in the suite may call `engine().handle` directly — the check is not
  a test, it is a precondition of every test.
- The default `cases()` is a handful of `handState()` positions crossed with the six action types,
  including a `COMPLETE` state.
- The suite's own tests, all driven through `handle`:
  - `eventsAlwaysDescribeTheStateTransition` — runs every case; the fold check does the asserting.
  - `aRejectedActionChangesNothing` — where `isRejected`, `newState == state` and `events` is
    empty.
  - `handleIsPure` — the same `(state, action)` twice gives equal results.
  - `eventsContinueTheHandSequence` — an accepted result's sequences are
    `state.eventCount, state.eventCount + 1, …`, dense, and `newState.eventCount` is one past the
    last; a rejected result emits none.
- `NoOpEngineContractTest : PokerEngineContract()` supplies `NoOpEngine`, proving the suite runs
  and inherits its tests.
- The class is `internal` and lives in test sources: later implementations (`TASK-010501`) extend
  it from the same module without it ever shipping.

## Out of scope

- Proving the suite is not vacuous — `TASK-010427`.
- Any rule, and any engine other than the no-op one — STORY-0105.

## Tests

`PokerEngineContract` (inherited) run through `NoOpEngineContractTest`.

| Test | Proves |
| --- | --- |
| `eventsAlwaysDescribeTheStateTransition` | for every case, `StateProjection.fold(state, events)` equals `newState` up to `deck` and `rng` |
| `aRejectedActionChangesNothing` | a rejected result returns the input state and no events |
| `handleIsPure` | two identical calls give equal `EngineResult`s |
| `eventsContinueTheHandSequence` | sequences are dense from `state.eventCount`, and `newState.eventCount` matches |

## Acceptance criteria

- [ ] `NoOpEngineContractTest.eventsAlwaysDescribeTheStateTransition` passes
- [ ] `NoOpEngineContractTest.aRejectedActionChangesNothing` passes
- [ ] `NoOpEngineContractTest.handleIsPure` passes
- [ ] `NoOpEngineContractTest.eventsContinueTheHandSequence` passes
- [ ] No test in `PokerEngineContract` calls `engine().handle` directly; all go through `handle`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
