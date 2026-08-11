---
schema: 2
id: TASK-010510
title: Turn an illegal action into the reason it is illegal
type: task
status: backlog
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, rules]
depends_on: [TASK-010509]
verify:
  - ./gradlew :poker-engine:test --tests '*ActionValidationTest'
  - ./gradlew :poker-engine:check
---

## Goal

One function answers "may this seat do this, right now, for this much?" with either `null` or the
`Rejection` a client can explain to a player.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/ActionValidation.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/ActionValidationTest.kt` | create |

Read `Rejection.kt`, `PlayerAction.kt`, `BettingRules.kt`. Modify none of them.

## Scope

- `ActionValidation.kt`, package `duels.poker.engine.game`, one public function:

  ```kotlin
  public fun rejectionFor(state: GameState, action: PlayerAction): Rejection?
  ```

- The checks, in exactly this order — the order is part of the contract, because a complete hand
  has no seat to act and would otherwise report `NotYourTurn`:

  | # | Condition | Rejection |
  | --- | --- | --- |
  | 1 | `state.isHandOver` | `HandComplete` |
  | 2 | `action.seat != state.seatToAct` | `NotYourTurn(state.seatToAct)` |
  | 3 | `!legalActions(state).allows(action.type)` | `ActionNotAllowed(action.type, legal.allowed)` |
  | 4 | `Bet` with `to < legal.minBetTo`, `Raise` with `to < legal.minRaiseTo` | `AmountTooSmall(to, minimum)` |
  | 5 | `Bet` or `Raise` with `to > legal.allInTo` | `AmountTooLarge(to, legal.allInTo)` |
  | 6 | otherwise | `null` |

- `Fold`, `Check`, `Call` and `AllIn` carry no amount, so checks 4 and 5 never apply to them —
  their amounts are functions of the state, not of the client.
- The function throws nothing and touches nothing: it reads the state and returns a value.

## Out of scope

- Emitting the event for an accepted action — `TASK-010511`.
- Wiring this into `PokerEngine.handle` — `TASK-010512`.

## Tests

`ActionValidationTest`, JUnit 5, positions from `handState()` with `copy`. Blinds 50/100,
stacks 10 000.

| Test | Proves |
| --- | --- |
| `aCompleteHandRejectsEveryAction` | `street = COMPLETE`: `Fold(0)` gives `HandComplete`, not `NotYourTurn` |
| `actingOutOfTurnIsRejected` | `seatToAct = 0`, `Check(1)` gives `NotYourTurn(0)` |
| `checkingWhileFacingABetIsNotAllowed` | `betToMatch = 300`: `Check(0)` gives `ActionNotAllowed(CHECK, ...)` naming the allowed set |
| `foldingWithNothingToCallIsNotAllowed` | fresh street: `Fold(0)` gives `ActionNotAllowed` |
| `aBetBelowOneBigBlindNamesTheMinimum` | fresh street: `Bet(0, 50)` gives `AmountTooSmall(50, 100)` |
| `aRaiseBelowTheMinimumNamesTheMinimum` | `betToMatch = 300`, `minRaiseTo = 600`: `Raise(0, 400)` gives `AmountTooSmall(400, 600)` |
| `aRaiseAboveTheStackNamesTheMaximum` | stack 10 000: `Raise(0, 20_000)` gives `AmountTooLarge(20_000, 10_000)` |
| `aLegalActionIsNotRejected` | fresh street: `Check(0)`, `Bet(0, 100)` and `AllIn(0)` each give `null` |
| `theMinimumItselfIsLegal` | `betToMatch = 300`, `minRaiseTo = 600`: `Raise(0, 600)` gives `null` |

## Acceptance criteria

- [ ] `ActionValidationTest.aCompleteHandRejectsEveryAction` passes
- [ ] `ActionValidationTest.actingOutOfTurnIsRejected` passes
- [ ] `ActionValidationTest.checkingWhileFacingABetIsNotAllowed` passes
- [ ] `ActionValidationTest.foldingWithNothingToCallIsNotAllowed` passes
- [ ] `ActionValidationTest.aBetBelowOneBigBlindNamesTheMinimum` passes
- [ ] `ActionValidationTest.aRaiseBelowTheMinimumNamesTheMinimum` passes
- [ ] `ActionValidationTest.aRaiseAboveTheStackNamesTheMaximum` passes
- [ ] `ActionValidationTest.aLegalActionIsNotRejected` passes
- [ ] `ActionValidationTest.theMinimumItselfIsLegal` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
