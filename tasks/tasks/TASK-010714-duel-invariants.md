---
schema: 2
id: TASK-010714
title: Button, blinds and chips across a whole duel
type: task
status: backlog
parent: STORY-0107
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [engine, duel, tests, chips]
depends_on: [TASK-010713]
verify:
  - ./gradlew :poker-engine:test --tests '*DuelInvariantTest'
  - ./gradlew :poker-engine:test --tests '*DuelPlaythroughTest'
  - ./gradlew :poker-engine:check
---

## Goal

Over duels nobody designed: the button alternates every hand, every hand plays its scheduled
blinds, the ladder only ever climbs, and the chip total never moves.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/DuelInvariantTest.kt` | create |

Read `poker-engine/src/test/kotlin/duels/poker/engine/duel/RandomDuelPlayer.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/duel/BlindSchedule.kt`,
`.../game/HeadsUpOrder.kt` (`otherSeat`) and
`poker-engine/src/test/kotlin/duels/poker/engine/game/SettlementInvariantTest.kt` for the style
these assertions follow. Modify none of them.

## Scope

- One new test class. No production file changes, and no change to `RandomDuelPlayer.kt` — every
  fact these tests need is already on `PlayedDuel` and `HandSummary`.
- Seeds `1L..20L` under `DuelFormat.DEFAULT`, plus a `fastLadder` format —
  `DuelFormat.DEFAULT.copy(blinds = BlindSchedule(DuelFormat.DEFAULT.blinds.levels, handsPerLevel = 1))`
  — used only by the test that has to see a level actually rise inside a short duel.
- Every failure message names its seed.

## Out of scope

- Termination and the hand ceiling — `TASK-010715`.
- Fixed-length duels — `TASK-010716`.
- Re-asserting per-hand betting or settlement invariants: `BettingInvariantTest` and
  `SettlementInvariantTest` own those.

## Tests

`DuelInvariantTest`, `@Timeout(60)` on each test

| Test | Proves |
| --- | --- |
| `theButtonAlternatesEveryHand` | for each seed: `hands.first().buttonSeat == (seed % 2).toInt()`, and for every consecutive pair `b.buttonSeat == otherSeat(a.buttonSeat)` |
| `everyHandUsesTheScheduledBlinds` | for each seed and every `HandSummary`: `blinds == format.blinds.blindsFor(handNumber)` — one level per hand, fixed for the length of that hand |
| `theBlindLevelRisesBetweenHandsAndNeverFalls` | under `fastLadder`, the first duel among seeds `1L..20L` with at least two hands has `hands[1].blinds.bigBlind > hands[0].blinds.bigBlind`; and across every duel in both formats the `bigBlind` sequence is non-decreasing |
| `chipsAreConstantFromTheFirstDealToTheLast` | for each seed and every `HandSummary`: `stacksAfter.sum() == 2 * DuelFormat.DEFAULT.startingStack`, both stacks are `>= 0`, and `outcome.finalStacks.sum()` is the same total |

## Acceptance criteria

- [ ] `DuelInvariantTest.theButtonAlternatesEveryHand` passes
- [ ] `DuelInvariantTest.everyHandUsesTheScheduledBlinds` passes
- [ ] `DuelInvariantTest.theBlindLevelRisesBetweenHandsAndNeverFalls` passes
- [ ] `DuelInvariantTest.chipsAreConstantFromTheFirstDealToTheLast` passes
- [ ] Both tests in `DuelPlaythroughTest` pass unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
