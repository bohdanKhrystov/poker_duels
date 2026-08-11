---
schema: 2
id: TASK-010413
title: Rejection reasons for an illegal action
type: task
status: ready
parent: STORY-0104
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [engine, domain, contract]
depends_on: [TASK-010412]
verify:
  - ./gradlew :poker-engine:test --tests '*RejectionTest'
  - ./gradlew :poker-engine:check
---

## Goal

An action the engine will not take comes back as a value that says why, carrying the numbers a
client needs to explain itself to a player without knowing a single rule.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Rejection.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/RejectionTest.kt` | create |

Read `PlayerAction.kt` for `ActionType`.

## Scope

- `Rejection.kt`, package `duels.poker.engine.game`:

  ```kotlin
  public sealed interface Rejection {
      /** It is [seatToAct]'s turn, not the sender's. Null means nobody is to act. */
      public data class NotYourTurn(val seatToAct: Int?) : Rejection

      /** The action type is not among [allowed] in this position. */
      public data class ActionNotAllowed(val attempted: ActionType, val allowed: Set<ActionType>) : Rejection

      /** A bet or raise below the smallest legal total. Both values are street totals. */
      public data class AmountTooSmall(val attempted: Int, val minimum: Int) : Rejection

      /** A bet or raise above the seat's stack. Both values are street totals. */
      public data class AmountTooLarge(val attempted: Int, val maximum: Int) : Rejection

      /** The hand is over; it accepts nothing further. */
      public data object HandComplete : Rejection
  }
  ```

- KDoc on the interface: a rejection never throws and never changes state — it is returned inside
  `EngineResult` (`TASK-010418`) with an empty event list.
- KDoc must state that amounts are **street totals**, the same convention as `PlayerAction`.

## Out of scope

- Deciding *which* rejection applies — STORY-0105.
- Any message string for a user: the client renders these, the engine does not do prose.

## Tests

`RejectionTest`, JUnit 5.

| Test | Proves |
| --- | --- |
| `eachRejectionCarriesItsNumbers` | `NotYourTurn(1).seatToAct == 1`, `AmountTooSmall(150, 200).minimum == 200`, `AmountTooLarge(9_000, 4_000).maximum == 4_000`, `ActionNotAllowed(ActionType.CHECK, setOf(ActionType.FOLD, ActionType.CALL)).allowed.size == 2` |
| `notYourTurnAllowsNoActor` | `NotYourTurn(null).seatToAct == null` |
| `handCompleteIsASingleton` | `Rejection.HandComplete === Rejection.HandComplete` |
| `exhaustiveWhenCompilesWithoutElse` | a `private fun code(rejection: Rejection): String = when (rejection) { ... }` with a branch per subtype and no `else` compiles and returns a distinct value for each of the five |

## Acceptance criteria

- [ ] `RejectionTest.eachRejectionCarriesItsNumbers` passes
- [ ] `RejectionTest.notYourTurnAllowsNoActor` passes
- [ ] `RejectionTest.handCompleteIsASingleton` passes
- [ ] `RejectionTest.exhaustiveWhenCompilesWithoutElse` passes — and its `when` has no `else`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
