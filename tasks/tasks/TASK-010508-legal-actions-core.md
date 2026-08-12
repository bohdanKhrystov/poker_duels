---
schema: 2
id: TASK-010508
title: Compute the legal actions at an ordinary decision point
type: task
status: done
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, rules]
depends_on: [TASK-010507]
verify:
  - ./gradlew :poker-engine:test --tests '*BettingRulesTest'
  - ./gradlew :poker-engine:check
---

## Goal

Given a state, the engine can say exactly what the seat on turn may do and for how much, with
both seats still holding chips.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/BettingRules.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/BettingRulesTest.kt` | create |

Read `LegalActions.kt`, `GameState.kt`, `docs/duel-rules.md` ("Betting"). Modify none of them.

## Scope

- `BettingRules.kt`, package `duels.poker.engine.game`, one public function:

  ```kotlin
  public fun legalActions(state: GameState): LegalActions
  ```

- `LegalActions.none(state.seatToAct ?: 0)` when the hand is over, no seat is to act, the seat
  has folded, or the seat has no chips left. Nothing else returns an empty set here.
- Amounts, all street totals, with `committed = seat.committedThisStreet`:

  | Field | Value |
  | --- | --- |
  | `allInTo` | `committed + seat.stack` |
  | `callTo` | `committed + state.toCall(seat.index)` |
  | `minBetTo` | `minOf(state.bigBlind, allInTo)` — the minimum bet is one big blind |
  | `minRaiseTo` | `minOf(state.minRaiseTo, allInTo)` |

- `state.minRaiseTo` is already correct: `BettingProjection` maintains it (bet 10, raise to 40
  leaves 70). **Do not recompute the raise increment here** — read the state and cap it.
- The allowed set:

  | Condition | Adds |
  | --- | --- |
  | `callTo == committed` (nothing to call) | `CHECK` |
  | otherwise | `FOLD` and `CALL` |
  | `state.betToMatch == 0` | `BET` |
  | otherwise | `RAISE` |
  | always, while the seat has chips | `ALL_IN` |

- Folding is legal only when facing a bet: `docs/duel-rules.md` lists `check, bet` as the whole
  set with no bet outstanding. Say so in the KDoc.
- The big blind's preflop option falls out of this table and must not be special-cased: it has
  nothing to call, so it gets `CHECK`, and `betToMatch > 0`, so its aggressive action is
  `RAISE` and not `BET`.

## Out of scope

- What an all-in opponent, or a stack too short to cover the bet, does to this set —
  `TASK-010509`. Every test here has both seats holding chips and neither all-in.
- Rejecting an action — `TASK-010510`.

## Tests

`BettingRulesTest`, JUnit 5, positions built from `handState()` and `seats()` with `copy`.

| Test | Proves |
| --- | --- |
| `aFreshStreetAllowsCheckAndBet` | `betToMatch = 0`: `allowed == setOf(CHECK, BET, ALL_IN)`, `minBetTo == 100` |
| `facingABetAllowsFoldCallAndRaise` | `betToMatch = 300`, seat committed 0: `allowed == setOf(FOLD, CALL, RAISE, ALL_IN)` |
| `callToIsAStreetTotalNotAnIncrement` | seat committed 100 facing 300: `callTo == 300` |
| `theBigBlindKeepsItsOptionPreflop` | preflop, `betToMatch = 100`, seat committed 100: `CHECK` and `RAISE` allowed, `BET` and `FOLD` are not |
| `theMinimumRaiseComesFromTheState` | after bet 10 raised to 40 (`betToMatch = 40`, `minRaiseTo = 70`): `minRaiseTo == 70` |
| `theMinimumBetIsOneBigBlind` | `minBetTo == state.bigBlind` on a fresh street |
| `allInToIsTheWholeStackPlusWhatIsAlreadyIn` | seat with stack 900 committed 100: `allInTo == 1_000` |
| `noSeatToActMeansNoLegalActions` | `seatToAct = null` gives an empty `allowed` |
| `aCompleteHandHasNoLegalActions` | `street = COMPLETE` gives an empty `allowed` |
| `aFoldedSeatHasNoLegalActions` | the seat to act has `hasFolded = true`, empty `allowed` |

## Acceptance criteria

- [ ] `BettingRulesTest.aFreshStreetAllowsCheckAndBet` passes
- [ ] `BettingRulesTest.facingABetAllowsFoldCallAndRaise` passes
- [ ] `BettingRulesTest.callToIsAStreetTotalNotAnIncrement` passes
- [ ] `BettingRulesTest.theBigBlindKeepsItsOptionPreflop` passes
- [ ] `BettingRulesTest.theMinimumRaiseComesFromTheState` passes
- [ ] `BettingRulesTest.theMinimumBetIsOneBigBlind` passes
- [ ] `BettingRulesTest.allInToIsTheWholeStackPlusWhatIsAlreadyIn` passes
- [ ] `BettingRulesTest.noSeatToActMeansNoLegalActions` passes
- [ ] `BettingRulesTest.aCompleteHandHasNoLegalActions` passes
- [ ] `BettingRulesTest.aFoldedSeatHasNoLegalActions` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
