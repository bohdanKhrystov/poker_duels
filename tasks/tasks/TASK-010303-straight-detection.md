---
schema: 2
id: TASK-010303
title: Detect a straight, including the wheel
type: task
status: backlog
parent: STORY-0103
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, rules]
depends_on: [TASK-010302]
verify:
  - ./gradlew :poker-engine:test --tests '*StraightDetectionTest'
  - ./gradlew :poker-engine:check
---

## Goal

Five ranks are classified as a straight or not, with `A-2-3-4-5` counted as five-high and
`Q-K-A-2-3` counted as nothing — the single off-by-one that every hand evaluator gets wrong.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/hand/HandPatterns.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/hand/StraightDetectionTest.kt` | create |

Read `card/Rank.kt` for its API. Do not touch `HandRank.kt`.

## Scope

- One `internal` file-level function, no class:

  ```kotlin
  internal fun straightHighOrNull(ranks: List<Rank>): Rank?
  ```

- It takes exactly five ranks in any order and returns the **high rank of the straight**, or
  `null` if the five ranks are not a straight.
- The rules, exactly:
  - five distinct ranks whose ordinals are consecutive → the highest of them,
  - `ACE, FIVE, FOUR, THREE, TWO` (the wheel) → `Rank.FIVE`, because the ace plays low,
  - anything else, including any repeated rank and any sequence that would need to wrap past the
    ace such as `QUEEN, KING, ACE, TWO, THREE` → `null`.
- Implementation shape: sort the distinct ordinals, bail out when fewer than five remain, then
  test consecutiveness and the one wheel special case. Clarity over cleverness — this function
  is read far more often than it runs.
- KDoc naming the wheel and stating explicitly that there is no wraparound straight.
- Any literal other than `-1`, `0`, `1`, `2` goes in a `private const val` (detekt runs with
  `maxIssues: 0`).

## Out of scope

- Flushes, pairs, trips and grouping — `TASK-010304`.
- Producing a `HandRank` — `TASK-010305`.
- Seven-card input. Exactly five ranks; a different size may `require` or return `null`, but the
  tests below only pass five.

## Tests

Build inputs from `Rank` constants, or from `duels.poker.engine.card.cards("...")` mapped to
`Card.rank` — the helper is `internal` in the test source set and importable.

`StraightDetectionTest`

| Test | Proves |
| --- | --- |
| `everyStraightFromSixHighToAceHighIsFound` | for each window of five consecutive ranks starting at `TWO`, the result is the window's top rank — nine windows, `SIX` through `ACE` |
| `theWheelIsFiveHigh` | `A-5-4-3-2` returns `Rank.FIVE`, not `Rank.ACE` |
| `queenKingAceTwoThreeIsNotAStraight` | `Q-K-A-2-3` returns `null` |
| `kingAceTwoThreeFourIsNotAStraight` | `K-A-2-3-4` returns `null` |
| `aGapBreaksTheStraight` | `9-8-7-6-4` returns `null` |
| `aPairIsNeverAStraight` | `6-5-5-4-3` returns `null` |
| `inputOrderDoesNotMatter` | all 120 permutations of the wheel and all 120 of `T-J-Q-K-A` give `FIVE` and `ACE` respectively |

For the permutation test, use a four-line local helper:

```kotlin
private fun <T> permutations(items: List<T>): List<List<T>> =
    if (items.size <= 1) listOf(items)
    else items.flatMap { head -> permutations(items - head).map { listOf(head) + it } }
```

## Acceptance criteria

- [ ] `StraightDetectionTest.everyStraightFromSixHighToAceHighIsFound` passes
- [ ] `StraightDetectionTest.theWheelIsFiveHigh` passes
- [ ] `StraightDetectionTest.queenKingAceTwoThreeIsNotAStraight` passes
- [ ] `StraightDetectionTest.kingAceTwoThreeFourIsNotAStraight` passes
- [ ] `StraightDetectionTest.aGapBreaksTheStraight` passes
- [ ] `StraightDetectionTest.aPairIsNeverAStraight` passes
- [ ] `StraightDetectionTest.inputOrderDoesNotMatter` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
