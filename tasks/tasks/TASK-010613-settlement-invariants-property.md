---
schema: 2
id: TASK-010613
title: Settlement invariants over a thousand random hands
type: task
status: done
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, tests, chips]
depends_on: [TASK-010612]
verify:
  - ./gradlew :poker-engine:test --tests '*SettlementInvariantTest'
  - ./gradlew :poker-engine:test --tests '*BettingInvariantTest'
  - ./gradlew :poker-engine:check
---

## Goal

Every hand the random harness can produce ends with an empty pot, a `COMPLETE` street and exactly
the chips it started with — proved over a thousand hands nobody designed.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/SettlementInvariantTest.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/RandomHandPlayer.kt` | modify |

Read `BettingInvariantTest.kt`, `Settlement.kt`, `DealerEvents.kt`. Modify none of them.

## Scope

- `RandomHandPlayer.kt`: `isHandEnded` narrows to `state.isHandOver`, and its KDoc — which
  currently says `Street.COMPLETE` never arrives because the story stops at showdown — is
  rewritten to say that every path out of a hand now settles, so a hand that stops anywhere else
  is a bug the harness should surface rather than accept. Nothing else in the file changes: no
  other function, no invariant check, no arithmetic.
- The narrowing is the point: if a settlement path ever fails to emit `HandFinished`, the loop
  runs to `maxActions` and throws naming the seed instead of quietly declaring the hand over.
- `SettlementInvariantTest` uses `playRandomHand(seed)` over `1L..1000L` with `@Timeout(30)` on
  the long-running tests, in the style of `BettingInvariantTest`. Every failure message names its
  seed.

## Out of scope

- Card secrecy — `TASK-010614` scans the same hands for a different property.
- Changing the harness's decision-making, stack spread or seeds. The hands must stay the same
  hands `BettingInvariantTest` already plays.

## Tests

`SettlementInvariantTest`

| Test | Proves |
| --- | --- |
| `everyRandomHandFinishesComplete` | for each seed: `street == Street.COMPLETE`, `seatToAct == null`, and the log holds exactly one `HandFinished`, which is its last event |
| `noRandomHandLeavesChipsInThePot` | for each seed: `pot == 0`, `potTotal == 0`, and the two stacks sum to the sum of the starting stacks in `played.opening` |
| `everySettledChipCameFromACommitment` | for each seed: the `PotAwarded` amounts plus the `UncalledBetReturned` amounts equal the sum of both seats' `committedThisHand` |
| `noHandPaysMoreThanTwoAwards` | for each seed: at most two `PotAwarded` events, at most one `UncalledBetReturned`, and the return — when there is one — precedes every award |
| `theSampleContainsBothEndings` | over the thousand seeds, more than 100 hands end with a fold and more than 100 reach a `ShowdownReached`, so none of the above is vacuous |

## Acceptance criteria

- [ ] `SettlementInvariantTest.everyRandomHandFinishesComplete` passes
- [ ] `SettlementInvariantTest.noRandomHandLeavesChipsInThePot` passes
- [ ] `SettlementInvariantTest.everySettledChipCameFromACommitment` passes
- [ ] `SettlementInvariantTest.noHandPaysMoreThanTwoAwards` passes
- [ ] `SettlementInvariantTest.theSampleContainsBothEndings` passes
- [ ] All five tests in `BettingInvariantTest` pass unchanged with the narrowed `isHandEnded`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
