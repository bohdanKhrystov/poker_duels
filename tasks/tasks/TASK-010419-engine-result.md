---
schema: 2
id: TASK-010419
title: EngineResult and the rejection invariant
type: task
status: done
parent: STORY-0104
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [engine, domain, contract]
depends_on: [TASK-010418]
verify:
  - ./gradlew :poker-engine:test --tests '*EngineResultTest'
  - ./gradlew :poker-engine:check
---

## Goal

The single value the engine returns, shaped so that "rejected" and "something happened" cannot
both be true.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/EngineResult.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/EngineResultTest.kt` | create |

Read `docs/adr/ADR-0001-event-sourced-engine-contract.md` for the declared shape, plus
`GameState.kt`, `GameEvent.kt` and `Rejection.kt` for their APIs.

## Scope

- `EngineResult.kt`, package `duels.poker.engine.game`, exactly the ADR's shape:

  ```kotlin
  public data class EngineResult(
      val newState: GameState,
      val events: List<GameEvent> = emptyList(),
      val rejection: Rejection? = null,
  ) {
      init {
          require(rejection == null || events.isEmpty()) {
              "A rejected action emits no events, got ${events.size}"
          }
      }

      public val isRejected: Boolean get() = rejection != null

      public companion object {
          /** The action was refused: the state comes back untouched and nothing happened. */
          public fun rejected(state: GameState, reason: Rejection): EngineResult =
              EngineResult(state, emptyList(), reason)

          public fun accepted(state: GameState, events: List<GameEvent>): EngineResult =
              EngineResult(state, events, null)
      }
  }
  ```

- KDoc on the class: `newState` and `events` are two descriptions of one transition, and the
  contract suite (`TASK-010426`) asserts they agree. Anyone adding a field here should read
  ADR-0001 first.

## Out of scope

- Sequence numbering of the events and the fold check — asserted by the contract suite,
  `TASK-010426`, not by this constructor.
- Any engine — `TASK-010421`.

## Tests

`EngineResultTest`, JUnit 5. Build states with `handState()` from `GameStates.kt`.

| Test | Proves |
| --- | --- |
| `acceptedCarriesItsEvents` | `accepted(state, listOf(ActionOn(0, 1)))` has `isRejected == false`, one event, `newState === state` |
| `rejectedCarriesNoEvents` | `rejected(state, Rejection.HandComplete)` has `isRejected == true`, `events.isEmpty()`, `newState == state` |
| `rejectionWithEventsIsImpossible` | `EngineResult(state, listOf(ActionOn(0, 1)), Rejection.HandComplete)` throws `IllegalArgumentException` |
| `defaultsToAnAcceptedEmptyResult` | `EngineResult(state)` has no events and no rejection |

## Acceptance criteria

- [ ] `EngineResultTest.acceptedCarriesItsEvents` passes
- [ ] `EngineResultTest.rejectedCarriesNoEvents` passes
- [ ] `EngineResultTest.rejectionWithEventsIsImpossible` passes
- [ ] `EngineResultTest.defaultsToAnAcceptedEmptyResult` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
