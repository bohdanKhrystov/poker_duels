---
schema: 2
id: TASK-010421
title: PokerEngine interface and a no-op implementation
type: task
status: backlog
parent: STORY-0104
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 3
labels: [engine, contract]
depends_on: [TASK-010420]
verify:
  - ./gradlew :poker-engine:test --tests '*NoOpEngineTest'
  - ./gradlew :poker-engine:check
---

## Goal

The interface the whole project is written against exists, and something implements it — an
engine that refuses everything, so the contract suite has a subject before any rule exists.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/PokerEngine.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/NoOpEngine.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/NoOpEngineTest.kt` | create |

Read `docs/adr/ADR-0001-event-sourced-engine-contract.md`, `EngineResult.kt`, `PlayerAction.kt`
and `Rejection.kt`.

## Scope

- `PokerEngine.kt`, main sources, exactly the ADR's shape and nothing more:

  ```kotlin
  public interface PokerEngine {
      /**
       * The next state and the events that produced it. Pure: the same [state] and [action]
       * always give the same result, on any machine. An illegal action neither throws nor
       * changes anything — it comes back as [EngineResult.rejection] with no events.
       */
      public fun handle(state: GameState, action: PlayerAction): EngineResult
  }
  ```

- `NoOpEngine.kt`, **test sources**, so no placeholder ships in the public API:

  ```kotlin
  /** Refuses every action. It exists so the contract suite has something to run against. */
  internal object NoOpEngine : PokerEngine {
      override fun handle(state: GameState, action: PlayerAction): EngineResult =
          EngineResult.rejected(state, Rejection.ActionNotAllowed(action.type, emptySet()))
  }
  ```

## Out of scope

- Any rule whatsoever — STORY-0105 onward.
- The reusable contract suite — `TASK-010426`.
- The projection — `TASK-010425`.

## Tests

`NoOpEngineTest`, JUnit 5, using `handState()` from `GameStates.kt`.

| Test | Proves |
| --- | --- |
| `refusesEveryActionType` | all six `PlayerAction` types come back with `isRejected == true` |
| `returnsTheInputStateUntouched` | `newState == handState()` for each of the six |
| `emitsNothing` | `events.isEmpty()` for each of the six |
| `isPure` | calling `handle` twice with the same state and action gives equal results |

## Acceptance criteria

- [ ] `NoOpEngineTest.refusesEveryActionType` passes
- [ ] `NoOpEngineTest.returnsTheInputStateUntouched` passes
- [ ] `NoOpEngineTest.emitsNothing` passes
- [ ] `NoOpEngineTest.isPure` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
