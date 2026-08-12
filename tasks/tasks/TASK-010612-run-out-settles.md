---
schema: 2
id: TASK-010612
title: A run-out settles the showdown it reaches
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [engine, rules, chips]
depends_on: [TASK-010611]
verify:
  - ./gradlew :poker-engine:test --tests '*AllInRunOutTest'
  - ./gradlew :poker-engine:test --tests '*OpeningRunOutTest'
  - ./gradlew :poker-engine:check
---

## Goal

The other way a hand reaches showdown — nobody left who can bet, so the board runs out — pays its
winner too, which makes every path out of a hand a settled one.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StreetProgression.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/AllInRunOutTest.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/OpeningRunOutTest.kt` | modify |

Read `Settlement.kt`, `Showdown.kt`, `HandSetup.kt`. Modify none of them.

## Scope

- `runOutBoard`'s tail, after `ShowdownReached` is applied, calls
  `settleHand(current, showdownWinners(current))` and appends that result's events before
  returning. `startHand` reaches this through `endBettingRound`, so an opening run-out settles
  by the same code and no second path is written.
- With both call sites settled, `endBettingRound`'s river branch and `runOutBoard` now share the
  shape "reach showdown, then settle". Extract the two lines into a private helper in this file
  if it reads better; do not move them into `Settlement.kt`.

## Out of scope

- Revealing cards (`DEC-004`, `TASK-010615`) and the fold path (already settled by
  `TASK-010607`).
- `BettingInvariantTest`: `TASK-010609` already re-pinned its terminal check on what the final
  state accepts, which is unchanged by settlement. `DefaultPokerEngineContractTest`'s all-in
  fixture got its hole cards in `TASK-010608` and its assertions — dense sequences, the fold
  check, purity — all survive an award.

## Tests

### `AllInRunOutTest` — assertions that move, and only these

| Test | What moves |
| --- | --- |
| `bothAllInPreflopRunsOutFiveCards` | `street` `SHOWDOWN` → `COMPLETE`; add `result.events.any { it is ShowdownReached }`, `pot == 0` and both stacks summing to 20_000 |
| `theRunOutDealsEachStreetInOrder` | `dealerEvents.size` 5 → the exact number seed 1 produces (7 with one winner, 8 on a split); indices 0–4 keep their assertions word for word; add that every event between `ShowdownReached` and the last is a `PotAwarded`, and that the last is `HandFinished` |
| `aShorterStackAllInStillRunsOut` | `street` `SHOWDOWN` → `COMPLETE`; `seat(1).stack > 0` and `board.size == 5` stay; add `pot == 0` and stacks summing to 14_000 |
| `aRunOutFromTheTurnDealsOnlyTheRiver` | `events.last() is ShowdownReached` → `is HandFinished`, with `events.any { it is ShowdownReached }` keeping the old claim; `street` → `COMPLETE`. This fixture's commitments are 1_000 against 10_000, so pin what that produces: `UncalledBetReturned(seat = 1, amount = 9_000)` then `PotAwarded(seat = 1, amount = 2_000)`, ending 9_000 / 11_000 — seat 1's `9d 9c` make two pair on `As Kd 7c 2h 2c` against seat 0's pair of deuces |

`theRunOutEmitsNoActionOn`, `noCardIsDealtTwiceInARunOut`, `chipsAreConservedByTheRunOut` and
`theEventsDescribeTheTransition` are untouched — an award moves chips out of the pot onto a
stack, which leaves `chipsInPlay` and `potTotal == pot` exactly as they were.

### `OpeningRunOutTest` — assertions that move, and only these

| Test | What moves |
| --- | --- |
| `aButtonAllInOnItsBlindStillReachesShowdown` | `street` `SHOWDOWN` → `COMPLETE`; add `events.any { it is ShowdownReached }`; `board.size == 5` and `seatToAct == null` stay |
| `bothBlindsAllInReachShowdown` | `street` `SHOWDOWN` → `COMPLETE`; both `isAllIn` assertions stay — `Seat.award` never clears the flag |
| `chipsAreConservedInAnOpeningRunOut` | `potTotal` 150 → 0; `chipsInPlay == 10_050` stays; add `UncalledBetReturned(seat = 1, amount = 50)` — the big blind posted 100 against a 50-chip all-in — and pin both stacks to the exact values seed 1 produces, summing to 10_050 |

`anOpeningRunOutEmitsNoActionOn`, `anOrdinaryHandStillGetsItsActionOn` and
`theEventsDescribeTheState` are untouched.

## Acceptance criteria

- [ ] `AllInRunOutTest.bothAllInPreflopRunsOutFiveCards` passes with the amended assertions
- [ ] `AllInRunOutTest.theRunOutDealsEachStreetInOrder` passes with the dealer-event count pinned exactly
- [ ] `AllInRunOutTest.aShorterStackAllInStillRunsOut` passes with the amended assertions
- [ ] `AllInRunOutTest.aRunOutFromTheTurnDealsOnlyTheRiver` passes with the uncalled bet and award pinned exactly
- [ ] The other four tests in `AllInRunOutTest` pass unchanged
- [ ] `OpeningRunOutTest.aButtonAllInOnItsBlindStillReachesShowdown` passes with the amended assertions
- [ ] `OpeningRunOutTest.bothBlindsAllInReachShowdown` passes with the amended assertions
- [ ] `OpeningRunOutTest.chipsAreConservedInAnOpeningRunOut` passes with both stacks pinned exactly
- [ ] The other three tests in `OpeningRunOutTest` pass unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
