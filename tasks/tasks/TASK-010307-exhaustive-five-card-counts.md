---
schema: 2
id: TASK-010307
title: Exhaustive five-card category counts
type: task
status: done
parent: STORY-0103
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [engine, test, rules]
depends_on: [TASK-010306]
verify:
  - ./gradlew :poker-engine:test --tests '*ExhaustiveFiveCardTest'
  - ./gradlew :poker-engine:check
---

## Goal

All 2 598 960 five-card hands are evaluated once, and the resulting category counts and number
of distinct ranks match the published figures exactly — a near-complete correctness proof for the
reference evaluator.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/hand/ExhaustiveFiveCardTest.kt` | create |

Read `ReferenceHandEvaluator.kt`, `HandRank.kt` and `card/Card.kt` for their API. **No
production code changes.**

## Scope

- Enumerate every five-card combination with five nested loops over the indices of `Card.all`:
  `a in 0..47`, `b in a + 1..48`, … `e in d + 1..51`. No shuffling, no randomness, no sampling.
- Evaluate each hand **once**, in a single pass held in a `companion object` so both tests share
  it. Two independent passes would double a run that is already the slowest test in the module:

  ```kotlin
  companion object {
      private val counts = IntArray(HandCategory.entries.size)
      private val distinct = HashSet<HandRank>()
      // one init block fills both
  }
  ```

- Count into the `IntArray` by `category.ordinal`; collect the ranks into the `HashSet`.
- Keep the loop body allocation-lean: build the five-card `List` once per hand and nothing else.

## Out of scope

- Any JUnit tag or Gradle wiring to exclude this from the fast local loop. If the runtime becomes
  a problem, that is a new ticket — it needs a build change, which this ticket may not make.
- Seven-card hands — `TASK-010309`.
- Testing anything but `ReferenceHandEvaluator`; the fast one is `TASK-010310`.

## Tests

`ExhaustiveFiveCardTest`

| Test | Proves |
| --- | --- |
| `everyCategoryOccursItsPublishedNumberOfTimes` | the nine counts are exactly `STRAIGHT_FLUSH` 40, `FOUR_OF_A_KIND` 624, `FULL_HOUSE` 3 744, `FLUSH` 5 108, `STRAIGHT` 10 200, `THREE_OF_A_KIND` 54 912, `TWO_PAIR` 123 552, `PAIR` 1 098 240, `HIGH_CARD` 1 302 540, and they sum to 2 598 960 |
| `theWholeSpaceCollapsesIntoExactlySevenThousandFourHundredAndSixtyTwoDistinctRanks` | `distinct.size == 7462` — the known number of distinct five-card hand values, which no count-preserving kicker bug survives |

Assert one category per line with the category named in the failure message; a single
`assertArrayEquals` would say only *"arrays differ"*.

## Acceptance criteria

- [ ] `ExhaustiveFiveCardTest.everyCategoryOccursItsPublishedNumberOfTimes` passes
- [ ] `ExhaustiveFiveCardTest.theWholeSpaceCollapsesIntoExactlySevenThousandFourHundredAndSixtyTwoDistinctRanks` passes
- [ ] The file contains exactly one enumeration of the 2 598 960 hands
- [ ] `git diff --name-only` in the PR lists no file under `src/main/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
