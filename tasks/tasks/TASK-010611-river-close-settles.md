---
schema: 2
id: TASK-010611
title: A closed river settles the showdown
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [engine, rules, chips]
depends_on: [TASK-010610]
verify:
  - ./gradlew :poker-engine:test --tests '*StreetAdvanceTest'
  - ./gradlew :poker-engine:test --tests '*HandWalkthroughTest'
  - ./gradlew :poker-engine:check
---

## Goal

A hand played to the end of the river no longer stops at `ShowdownReached`: the better hand is
paid and the hand reaches `COMPLETE`.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StreetProgression.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/StreetAdvanceTest.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/HandWalkthroughTest.kt` | modify |

Read `Settlement.kt`, `Showdown.kt`, `DealerEvents.kt`. Modify none of them.

## Scope

- Only `endBettingRound`'s river branch changes. After `ShowdownReached` is applied, it calls
  `settleHand(afterShowdown, showdownWinners(afterShowdown))` and returns that state with
  `listOf(endedEvent, showdownEvent) + settled.events`.
- `runOutBoard` is **not** touched in this ticket: a hand that ran out because nobody could act
  still stops at `ShowdownReached` until `TASK-010612`. Two call sites, two tickets, two small
  diffs — that is deliberate, so each one owns only the tests it invalidates.
- The state passed to `showdownWinners` is the post-sweep one, so both seats show
  `committedThisStreet == 0` and `pot` holds every chip at stake.

## Out of scope

- Revealing cards. No `HandRevealed` is emitted anywhere yet — that waits on `DEC-004`
  (`TASK-010615`), and the settlement events sit directly after `ShowdownReached` until then.
- The run-out path and its tests — `TASK-010612`.

## Tests

### `StreetAdvanceTest` — assertions that move, and only these

| Test | What moves |
| --- | --- |
| `theRiverEndsInShowdownReached` | rename to `theRiverShowdownPaysTheBetterHand`. `events.size` 3 → 5: `PlayerChecked`, `BettingRoundEnded`, `ShowdownReached`, `PotAwarded(seat = 0, amount = 600)`, `HandFinished`. `street` `SHOWDOWN` → `COMPLETE`; `seatToAct == null` and "no `StreetDealt`" both stay. Add `seat(0).stack == 10_300`, `seat(1).stack == 9_700`, `pot == 0` — seat 0 holds `Qh Jc` for `A K Q J 9` against seat 1's `A K T 9 8` |

`aFinishedPreflopDealsThreeFlopCards`, `theEventOrderIsEndedThenDealtThenActionOn`,
`theNonButtonActsFirstOnTheFlop`, `everyCommitmentIsSweptAndTheBarResets`,
`committedThisHandSurvivesTheAdvance`, `theDeckShrinksByExactlyTheCardsDealt`,
`noCardAppearsTwice`, `chipsAreConservedAcrossTheAdvance` and `theEventsDescribeTheTransition`
are untouched: none of them observes what happens after the river closes, and chip conservation
holds across an award.

### `HandWalkthroughTest` — assertions that move, and only these

The script's last action, `Call(1)` on the river, now closes the hand.

| Test | What moves |
| --- | --- |
| `theHandReachesShowdownWithAFullBoard` | `street` `SHOWDOWN` → `COMPLETE`; the showdown is now pinned in the log instead — `allEvents.any { it is ShowdownReached }` — and `allEvents.last() is HandFinished`. `board.size == 5` and `seatToAct == null` stay |
| `theChipsEndWhereTheArithmeticSaysTheyDo` | `pot` 2_200 → 0; both `committedThisHand == 1_100` stay; the two stacks are pinned to the exact values seed 1 produces, and their sum is asserted to be 20_000. With equal commitments of 1_100 the only two possible shapes are 11_100/8_900 in one order or the other, or 10_000/10_000 if the board splits — anything else means the settlement is wrong. Add a comment naming which hand won |

`noActionInTheScriptIsRejected`, `theNonButtonActsFirstOnEveryStreetAfterTheFlop`,
`theButtonActsFirstPreflop`, `chipsAreConservedAfterEveryAction`,
`everyEventSequenceIsDenseAndGapFree`, `noCardIsDealtTwice` and
`theEventLogReproducesTheFinalState` are untouched — the settlement events fold through
`StateProjection` like every other event.

## Acceptance criteria

- [ ] `StreetAdvanceTest.theRiverShowdownPaysTheBetterHand` passes
- [ ] The other nine tests in `StreetAdvanceTest` pass unchanged
- [ ] `HandWalkthroughTest.theHandReachesShowdownWithAFullBoard` passes with the amended assertions
- [ ] `HandWalkthroughTest.theChipsEndWhereTheArithmeticSaysTheyDo` passes with both stacks pinned exactly and summing to 20_000
- [ ] The other seven tests in `HandWalkthroughTest` pass unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
