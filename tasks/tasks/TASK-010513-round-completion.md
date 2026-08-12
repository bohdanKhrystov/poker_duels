---
schema: 2
id: TASK-010513
title: Decide whether the betting round has anyone left to act
type: task
status: ready
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, rules]
depends_on: [TASK-010512]
verify:
  - ./gradlew :poker-engine:test --tests '*RoundCompletionTest'
  - ./gradlew :poker-engine:check
---

## Goal

After any action, one predicate says whether the opponent still has a decision on this street —
including the two cases where a seat that owes nothing still gets to act.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StreetProgression.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/RoundCompletionTest.kt` | create |

Read `HeadsUpOrder.kt`, `GameState.kt`, `docs/duel-rules.md` ("Betting"). Modify none of them.

## Scope

- `StreetProgression.kt`, package `duels.poker.engine.game`, one public function:

  ```kotlin
  /** [state] is the state *after* [lastAction]'s event has been applied. */
  public fun roundContinues(state: GameState, lastAction: PlayerAction): Boolean
  ```

- With `actor = state.seat(lastAction.seat)` and `other = state.seat(otherSeat(lastAction.seat))`,
  in this order:

  | # | Condition | Result |
  | --- | --- | --- |
  | 1 | either seat `hasFolded` | `false` |
  | 2 | `other.isAllIn` or `other.stack == 0` | `false` |
  | 3 | `other.committedThisStreet < state.betToMatch` | `true` — the opponent owes chips |
  | 4 | `actor.isAllIn` | `false` — an all-in for less leaves nothing to act against |
  | 5 | `lastAction.type == CHECK` | `lastAction.seat == firstToActOn(state.street, state.buttonSeat)` |
  | 6 | `lastAction.type == CALL` | `state.street == PREFLOP && state.betToMatch == state.bigBlind && otherSeat(lastAction.seat) == bigBlindSeat(state.buttonSeat)` |
  | 7 | otherwise | `false` |

- Rows 5 and 6 are the only two positions in heads-up where a seat that owes nothing still has a
  decision: the second player still has to answer the street's first check, and the big blind
  still has its option after a limp. Because two seats alternate strictly, both are derivable
  from the state — the engine needs no "has acted this street" flag anywhere. **Put that
  argument in the KDoc**; it is the reason this function is not obviously correct.
- No `var`, no mutation, no event emission: this is a predicate.

## Out of scope

- Emitting the resulting events — `TASK-010514` (`ActionOn`), `TASK-010516` (fold),
  `TASK-010517` (street advance).

## Tests

`RoundCompletionTest`, JUnit 5. Build each position with `handState()`/`seats()` and `copy` as
the state *after* the action, then call `roundContinues`. Blinds 50/100, button on seat 0.

| Test | Proves |
| --- | --- |
| `theBigBlindKeepsItsOptionAfterALimp` | preflop, both committed 100, `betToMatch = 100`, `Call(0)` → `true` |
| `aCalledRaiseEndsTheRound` | preflop, both committed 300, `betToMatch = 300`, `Call(0)` → `false` |
| `theBigBlindsCheckEndsTheRound` | preflop, both committed 100, `Check(1)` → `false` |
| `theFirstCheckOnAStreetLeavesTheOpponentToAct` | flop, nothing committed, `Check(1)` (the non-button acts first) → `true` |
| `theSecondCheckEndsTheRound` | flop, nothing committed, `Check(0)` → `false` |
| `anUnmatchedBetLeavesTheOpponentToAct` | flop, seat 1 committed 300, `betToMatch = 300`, `Bet(1, 300)` → `true` |
| `anAllInForLessEndsTheRound` | seat 0 all-in at 150 against `betToMatch = 300`, `Call(0)` → `false` |
| `anAllInOpponentEndsTheRound` | opponent `isAllIn` and matched, `Call(0)` → `false` |
| `aFoldEndsTheRound` | seat 0 `hasFolded`, `Fold(0)` → `false` |

## Acceptance criteria

- [ ] `RoundCompletionTest.theBigBlindKeepsItsOptionAfterALimp` passes
- [ ] `RoundCompletionTest.aCalledRaiseEndsTheRound` passes
- [ ] `RoundCompletionTest.theBigBlindsCheckEndsTheRound` passes
- [ ] `RoundCompletionTest.theFirstCheckOnAStreetLeavesTheOpponentToAct` passes
- [ ] `RoundCompletionTest.theSecondCheckEndsTheRound` passes
- [ ] `RoundCompletionTest.anUnmatchedBetLeavesTheOpponentToAct` passes
- [ ] `RoundCompletionTest.anAllInForLessEndsTheRound` passes
- [ ] `RoundCompletionTest.anAllInOpponentEndsTheRound` passes
- [ ] `RoundCompletionTest.aFoldEndsTheRound` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
