---
schema: 2
id: TASK-010520
title: Play one scripted hand from blinds to showdown
type: task
status: done
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [engine, test]
depends_on: [TASK-010519]
verify:
  - ./gradlew :poker-engine:test --tests '*HandWalkthroughTest'
  - ./gradlew :poker-engine:check
---

## Goal

One test plays a whole hand — limp, check, bet, call, check, bet, raise, call — and pins the
result the story promised: four streets, the right seat on turn each time, and no chip lost.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/HandWalkthroughTest.kt` | create |

Read `HandSetup.kt`, `DefaultPokerEngine.kt`, `StreetProgression.kt`, `PokerEngineContract.kt`.
Modify none of them — this ticket adds no production code. If the walkthrough fails, the finding
is a new ticket, not a fix here.

## Scope

- Open with `startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L))` and drive
  `DefaultPokerEngine.handle` through exactly this script, collecting every event:

  | # | Action | After it |
  | --- | --- | --- |
  | 1 | `Call(0)` | seat 1 to act, its option |
  | 2 | `Check(1)` | flop dealt, seat 1 to act |
  | 3 | `Check(1)` | seat 0 to act |
  | 4 | `Bet(0, 200)` | seat 1 to act |
  | 5 | `Call(1)` | turn dealt, seat 1 to act |
  | 6 | `Check(1)` | seat 0 to act |
  | 7 | `Check(0)` | river dealt, seat 1 to act |
  | 8 | `Bet(1, 400)` | seat 0 to act |
  | 9 | `Raise(0, 800)` | seat 1 to act |
  | 10 | `Call(1)` | `ShowdownReached` |

- Keep the script in one private helper that returns the events and the final state, so each
  test below asserts one thing about the same hand.

## Out of scope

- Anything after `ShowdownReached` — STORY-0106.
- Randomised play — `TASK-010521`.

## Tests

`HandWalkthroughTest`, JUnit 5.

| Test | Proves |
| --- | --- |
| `noActionInTheScriptIsRejected` | all ten results have `isRejected == false` |
| `theHandReachesShowdownWithAFullBoard` | `street == SHOWDOWN`, `board.size == 5`, `seatToAct == null` |
| `theChipsEndWhereTheArithmeticSaysTheyDo` | `pot == 2_200`, both stacks 8 900, both `committedThisHand == 1_100` |
| `theNonButtonActsFirstOnEveryStreetAfterTheFlop` | immediately after each `StreetDealt`, `seatToAct == 1` |
| `theButtonActsFirstPreflop` | the opening state has `seatToAct == 0` |
| `chipsAreConservedAfterEveryAction` | `chipsInPlay == 20_000` after each of the ten actions |
| `everyEventSequenceIsDenseAndGapFree` | the collected sequences are `0 until events.size` |
| `noCardIsDealtTwice` | four hole cards and five board cards are nine distinct cards, `deck.remaining == 43` |
| `theEventLogReproducesTheFinalState` | `StateProjection.fold(opening, allEvents)` equals the final state except `deck` and `rng` |

## Acceptance criteria

- [ ] `HandWalkthroughTest.noActionInTheScriptIsRejected` passes
- [ ] `HandWalkthroughTest.theHandReachesShowdownWithAFullBoard` passes
- [ ] `HandWalkthroughTest.theChipsEndWhereTheArithmeticSaysTheyDo` passes
- [ ] `HandWalkthroughTest.theNonButtonActsFirstOnEveryStreetAfterTheFlop` passes
- [ ] `HandWalkthroughTest.theButtonActsFirstPreflop` passes
- [ ] `HandWalkthroughTest.chipsAreConservedAfterEveryAction` passes
- [ ] `HandWalkthroughTest.everyEventSequenceIsDenseAndGapFree` passes
- [ ] `HandWalkthroughTest.noCardIsDealtTwice` passes
- [ ] `HandWalkthroughTest.theEventLogReproducesTheFinalState` passes
- [ ] No file under `src/main` is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
