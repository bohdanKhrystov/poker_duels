---
schema: 2
id: TASK-010509
title: Restrict the legal actions around an all-in
type: task
status: backlog
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, rules]
depends_on: [TASK-010508]
verify:
  - ./gradlew :poker-engine:test --tests '*BettingRulesAllInTest'
  - ./gradlew :poker-engine:check
---

## Goal

Nobody can raise chips that nobody could call: an all-in opponent leaves fold and call only, and
a stack too short to cover the bet cannot raise.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/BettingRules.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/BettingRulesAllInTest.kt` | create |

Read `docs/duel-rules.md` ("Betting") and `LegalActions.kt`. Modify neither.

## Scope

- Two subtractions from the allowed set computed in `TASK-010508`, and no other change:

  1. **The opponent must be contestable.** With
     `contestable = !other.hasFolded && !other.isAllIn && other.stack > 0`, a non-contestable
     opponent removes `BET`, `RAISE` and `ALL_IN`. What is left is `FOLD` and `CALL`, or
     `CHECK` when there is nothing to call.
  2. **A seat that cannot cover the bet cannot raise.** `RAISE` is removed when
     `allInTo <= state.betToMatch`; `CALL` remains, capped at the stack.

- This is the whole of the "a short all-in does not reopen the betting" rule in heads-up: the
  seat facing a short all-in has an all-in opponent, so it may call but not re-raise, exactly as
  `docs/duel-rules.md` requires. Say that in the KDoc and name the rule.
- `state.minRaiseTo` is untouched by this ticket — `BettingProjection` already declines to raise
  the bar for a short all-in. Here we only decide who may act, never the arithmetic.
- The amount fields keep the caps from `TASK-010508`, which is what lets a short stack shove for
  less than a full bet or a full raise.

## Out of scope

- Returning the uncalled part of an over-large bet — STORY-0106; the chips are simply never
  matched, and `committedThisHand` records who put in more.
- Rejecting an action a client sends anyway — `TASK-010510`.

## Tests

`BettingRulesAllInTest`, JUnit 5, positions from `handState()` and `seats()` with `copy`.
Blinds are 50/100 throughout.

| Test | Proves |
| --- | --- |
| `facingAnAllInOpponentLeavesOnlyFoldAndCall` | opponent `isAllIn` with `betToMatch = 450`: `allowed == setOf(FOLD, CALL)` |
| `aShortAllInDoesNotReopenTheBetting` | seat 0 committed 300, seat 1 all-in to 450, `minRaiseTo = 600`: seat 0 gets `setOf(FOLD, CALL)` with `callTo == 450` — no `RAISE` |
| `aSeatThatCannotCoverTheBetMayNotRaise` | stack 150, `betToMatch = 300`: `allowed == setOf(FOLD, CALL, ALL_IN)`, `callTo == 150` |
| `aShortStackCallsAllInForLess` | same position: `callTo == allInTo` |
| `aShortStackMayBetLessThanABigBlind` | fresh street, stack 60, live opponent: `BET` allowed with `minBetTo == 60` |
| `aShortStackMayRaiseForLessThanTheMinimum` | `betToMatch = 300`, `minRaiseTo = 600`, stack leaves `allInTo = 500`: `RAISE` allowed with `minRaiseTo == 500` |
| `anAllInOpponentCannotBeBetInto` | fresh street, `betToMatch = 0`, opponent `isAllIn`: `allowed == setOf(CHECK)` |
| `aLiveOpponentStillAllowsTheFullSet` | both seats with chips, `betToMatch = 300`: `allowed == setOf(FOLD, CALL, RAISE, ALL_IN)` |

## Acceptance criteria

- [ ] `BettingRulesAllInTest.facingAnAllInOpponentLeavesOnlyFoldAndCall` passes
- [ ] `BettingRulesAllInTest.aShortAllInDoesNotReopenTheBetting` passes
- [ ] `BettingRulesAllInTest.aSeatThatCannotCoverTheBetMayNotRaise` passes
- [ ] `BettingRulesAllInTest.aShortStackCallsAllInForLess` passes
- [ ] `BettingRulesAllInTest.aShortStackMayBetLessThanABigBlind` passes
- [ ] `BettingRulesAllInTest.aShortStackMayRaiseForLessThanTheMinimum` passes
- [ ] `BettingRulesAllInTest.anAllInOpponentCannotBeBetInto` passes
- [ ] `BettingRulesAllInTest.aLiveOpponentStillAllowsTheFullSet` passes
- [ ] `BettingRulesTest` still passes unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
