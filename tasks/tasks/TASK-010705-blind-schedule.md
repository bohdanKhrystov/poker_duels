---
schema: 2
id: TASK-010705
title: A blind schedule that answers which blinds a hand number plays
type: task
status: backlog
parent: STORY-0107
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, duel, rules]
depends_on: [TASK-010704]
verify:
  - ./gradlew :poker-engine:test --tests '*BlindScheduleTest'
  - ./gradlew :poker-engine:check
---

## Goal

A hand number in, the blinds for that hand out — including for hand numbers past the last level
anyone wrote down.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/BlindSchedule.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/BlindScheduleTest.kt` | create |

Read `poker-engine/src/main/kotlin/duels/poker/engine/duel/BlindLevel.kt` and `docs/duel-rules.md`
Part 2 (the "Blind levels" table and the sentence about why the ladder guarantees termination).
Modify neither.

## Scope

- `public data class BlindSchedule(val levels: List<BlindLevel>, val handsPerLevel: Int)`.
- `init` requires, each with a message naming the offending value: `levels` is not empty,
  `handsPerLevel >= 1`, and `levels` is a strictly ascending ladder — every level's `bigBlind` is
  greater than the one before it.
- `public fun levelIndexFor(handNumber: Int): Int` — the 0-based block a hand falls in,
  `(handNumber - 1) / handsPerLevel`. Requires `handNumber >= 1`. Uncapped: it is the raw block
  number, not an index into `levels`.
- `public fun blindsFor(handNumber: Int): BlindLevel` — `levels[index]` while the block index is
  inside `levels`; past the last defined level, the last level `doubled()` once per further
  block, so block `levels.size - 1 + k` is the last level doubled `k` times. Requires
  `handNumber >= 1`.
- The number of doublings saturates at a private `MAX_DOUBLINGS = 20`, so `blindsFor` cannot
  overflow `Int` for any hand number a caller can pass. Say *why* in a comment: twenty doublings
  of any real level is orders of magnitude past any stack, so a duel that reaches it is a bug in
  the caller, not an arithmetic problem for this function to solve.
- KDoc naming this as the mechanism `docs/duel-rules.md` credits with making a duel terminate.

## Out of scope

- The default five levels — they belong to `DuelFormat.DEFAULT`, `TASK-010707`. This file
  contains no blind amounts at all.
- Applying a schedule to an actual hand — `TASK-010709`.

## Tests

`BlindScheduleTest`, over the fixture
`BlindSchedule(listOf(BlindLevel(50, 100), BlindLevel(75, 150), BlindLevel(100, 200)), handsPerLevel = 10)`

| Test | Proves |
| --- | --- |
| `holdsEachLevelForItsBlockOfHands` | `blindsFor` gives 50/100 for hands 1 and 10, 75/150 for 11 and 20, 100/200 for 21 and 30 |
| `doublesTheLastLevelOncePerBlockThereafter` | `blindsFor(31) == BlindLevel(200, 400)`, `blindsFor(41) == BlindLevel(400, 800)`, `blindsFor(51) == BlindLevel(800, 1600)` |
| `saturatesInsteadOfOverflowing` | `blindsFor(100_000) == blindsFor(1_000_000)`, and that level has `smallBlind > 0` and `bigBlind > smallBlind` |
| `levelIndexCountsBlocksFromZero` | `levelIndexFor` gives 0 for 1 and 10, 1 for 11, 2 for 30, 3 for 31 |
| `rejectsAScheduleThatIsNotAnAscendingLadder` | an empty `levels`, `handsPerLevel = 0`, and levels whose `bigBlind` does not increase each throw `IllegalArgumentException` |
| `rejectsAHandNumberBelowOne` | `blindsFor(0)` and `levelIndexFor(0)` each throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] `BlindScheduleTest.holdsEachLevelForItsBlockOfHands` passes
- [ ] `BlindScheduleTest.doublesTheLastLevelOncePerBlockThereafter` passes
- [ ] `BlindScheduleTest.saturatesInsteadOfOverflowing` passes
- [ ] `BlindScheduleTest.levelIndexCountsBlocksFromZero` passes
- [ ] `BlindScheduleTest.rejectsAScheduleThatIsNotAnAscendingLadder` passes
- [ ] `BlindScheduleTest.rejectsAHandNumberBelowOne` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
