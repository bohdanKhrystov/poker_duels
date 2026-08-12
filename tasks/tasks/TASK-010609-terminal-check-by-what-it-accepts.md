---
schema: 2
id: TASK-010609
title: Pin the random hand's ending by what it accepts, not by its street
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [engine, tests]
depends_on: [TASK-010608]
verify:
  - ./gradlew :poker-engine:test --tests '*BettingInvariantTest'
  - ./gradlew :poker-engine:check
---

## Goal

The thousand-hand property suite asserts the thing it actually cares about — that a hand ends in
a state which accepts nothing further — instead of naming the street it happens to end on today.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/BettingInvariantTest.kt` | modify |

Read `RandomHandPlayer.kt` and `BettingRules.kt` (for `legalActions`). Modify neither.

## Scope

- `everyRandomHandEndsAtShowdownOrWithAFold` is replaced by
  `everyRandomHandEndsWhereNoActionIsLeft`, over the same seeds `1L..1000L`, asserting for each
  played hand's `finalState`:

  1. `seatToAct == null`,
  2. `legalActions(finalState).allowed.isEmpty()`,
  3. every one of `PlayerAction.Fold(0)`, `Fold(1)`, `Check(0)`, `Check(1)`, `Call(0)`,
     `Call(1)`, `AllIn(0)`, `AllIn(1)` comes back rejected from `DefaultPokerEngine.handle`,

  each failure message naming the seed, as the rest of the file does.

- Why this is the same claim and not a weaker one: the old assertion said "the hand stopped
  somewhere terminal", using the only terminal street that existed. `TASK-010611` and
  `TASK-010612` make settled hands finish at `COMPLETE` instead of resting at `SHOWDOWN`, which
  would make the street list wrong without making the hand any less finished. The three
  assertions above hold before and after, and are what "finished" means for an engine whose only
  input is an action.
- The exact terminal shape a settled hand has — `street == COMPLETE`, `pot == 0`, every chip back
  on a stack — is pinned at full strength by `TASK-010613` once settlement actually runs.

## Out of scope

- The other four tests in the file. They are untouched.
- `RandomHandPlayer.kt`: its `isHandEnded` already accepts `isHandOver`, so the harness needs no
  change here. Its stale KDoc is `TASK-010613`'s.

## Tests

`BettingInvariantTest`

| Test | Proves |
| --- | --- |
| `everyRandomHandEndsWhereNoActionIsLeft` | over seeds 1..1000, the final state names no seat to act, offers no legal action, and rejects every action it is handed |

## Acceptance criteria

- [ ] `BettingInvariantTest.everyRandomHandEndsWhereNoActionIsLeft` passes
- [ ] `BettingInvariantTest.aThousandRandomHandsHoldEveryInvariant` passes unchanged
- [ ] `BettingInvariantTest.everyRandomHandsLogReproducesItsFinalState` passes unchanged
- [ ] `BettingInvariantTest.theSameSeedPlaysTheSameHandTwice` passes unchanged
- [ ] `BettingInvariantTest.aHandThatCannotProgressNamesItsSeed` passes unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
