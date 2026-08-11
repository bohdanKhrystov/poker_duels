---
schema: 2
id: TASK-010412
title: PlayerAction hierarchy and ActionType
type: task
status: done
parent: STORY-0104
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, domain, contract]
depends_on: [TASK-010411]
verify:
  - ./gradlew :poker-engine:test --tests '*PlayerActionTest'
  - ./gradlew :poker-engine:check
---

## Goal

A closed set of the six things a player can attempt, with amounts in one unambiguous unit, so an
exhaustive `when` keeps every future rule honest.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerAction.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/PlayerActionTest.kt` | create |

Read `docs/duel-rules.md` (the "Betting" table) for the six actions and nothing else.

## Scope

- `PlayerAction.kt`, package `duels.poker.engine.game`, one enum and one sealed hierarchy:

  ```kotlin
  public enum class ActionType { FOLD, CHECK, CALL, BET, RAISE, ALL_IN }

  public sealed interface PlayerAction {
      /** The seat attempting the action: 0 or 1. Whether it is that seat's turn is the engine's business. */
      public val seat: Int
      public val type: ActionType

      public data class Fold(override val seat: Int) : PlayerAction {
          override val type: ActionType get() = ActionType.FOLD
      }
      public data class Check(override val seat: Int) : PlayerAction { ... }
      public data class Call(override val seat: Int) : PlayerAction { ... }
      public data class Bet(override val seat: Int, val to: Int) : PlayerAction {
          init { require(to > 0) { "A bet must be positive, was $to" } }
          ...
      }
      public data class Raise(override val seat: Int, val to: Int) : PlayerAction {
          init { require(to > 0) { "A raise must be positive, was $to" } }
          ...
      }
      public data class AllIn(override val seat: Int) : PlayerAction { ... }
  }
  ```

- **`to` is a street total, never an increment.** KDoc on `Bet` and `Raise` must say so with the
  worked example from the rules: facing a bet of 10 raised to 40, the next raise is
  `Raise(to = 70)` — it is not `Raise(70)` meaning "70 more". This convention is the classic
  source of ambiguity in poker APIs and it is fixed here once.
- `Call` and `AllIn` carry no amount: the amount is a function of the state, and letting a client
  name it is letting a client assert a game fact. KDoc says exactly that.
- Whether the seat index is in range, and whether the action is legal, are the engine's answer
  (`Rejection`, `TASK-010413`) — not a constructor `require`. Only the non-positive amount is
  rejected here, because no state makes it meaningful.

## Out of scope

- What is currently legal — `LegalActions`, `TASK-010414`.
- Applying an action — STORY-0105.
- Timeouts and auto-folds: the server submits a `Fold` on the player's behalf; the engine never
  knows the difference.

## Tests

`PlayerActionTest`, JUnit 5.

| Test | Proves |
| --- | --- |
| `everyActionReportsItsType` | the six actions map to `FOLD`, `CHECK`, `CALL`, `BET`, `RAISE`, `ALL_IN` |
| `everyActionCarriesItsSeat` | each of the six built for seat 1 reports `seat == 1` |
| `amountsAreStreetTotals` | `Bet(0, to = 300).to == 300` and `Raise(1, to = 700).to == 700` |
| `rejectsANonPositiveBet` | `Bet(0, 0)` and `Bet(0, -1)` each throw `IllegalArgumentException` |
| `rejectsANonPositiveRaise` | `Raise(0, 0)` and `Raise(0, -50)` each throw |
| `exhaustiveWhenCompilesWithoutElse` | a `private fun describe(action: PlayerAction): ActionType = when (action) { ... }` with a branch per subtype and **no `else`** compiles, and returns the right type for all six |
| `equalActionsAreEqual` | `Bet(0, 300) == Bet(0, 300)` and `Bet(0, 300) != Bet(1, 300)` |

## Acceptance criteria

- [ ] `PlayerActionTest.everyActionReportsItsType` passes
- [ ] `PlayerActionTest.everyActionCarriesItsSeat` passes
- [ ] `PlayerActionTest.amountsAreStreetTotals` passes
- [ ] `PlayerActionTest.rejectsANonPositiveBet` passes
- [ ] `PlayerActionTest.rejectsANonPositiveRaise` passes
- [ ] `PlayerActionTest.exhaustiveWhenCompilesWithoutElse` passes — and its `when` has no `else`
- [ ] `PlayerActionTest.equalActionsAreEqual` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
