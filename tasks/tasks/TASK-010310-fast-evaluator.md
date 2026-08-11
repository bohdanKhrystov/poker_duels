---
schema: 2
id: TASK-010310
title: Bitmask five-card evaluator
type: task
status: ready
parent: STORY-0103
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, rules, performance]
depends_on: [TASK-010309]
verify:
  - ./gradlew :poker-engine:test --tests '*FastEvaluatorEquivalenceTest'
  - ./gradlew :poker-engine:check
---

## Goal

A second `HandEvaluator` that decides a hand from bitmasks and counters instead of sorting and
grouping, and that agrees with the reference on every one of the 2 598 960 five-card hands.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/hand/FastHandEvaluator.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/hand/FastEvaluatorEquivalenceTest.kt` | create |

Read `HandEvaluator.kt`, `HandRank.kt` and `ReferenceHandEvaluator.kt` for their API. **Do not
modify the reference evaluator or `HandPatterns.kt`** — they are the oracle, and an oracle that
gets edited to agree with the thing it judges is worthless.

## Scope

- `public object FastHandEvaluator : HandEvaluator`, same `evaluate` contract, same `HandRank`
  results. It inherits `bestOfSeven` unchanged.
- Decide everything from three cheap structures, with no sorting, no `groupBy`, no `List<Rank>`
  intermediates:
  - `rankMask: Int` — bit `r.ordinal` set for each rank present,
  - `counts: IntArray(RANK_COUNT)` — how many of each rank,
  - `suitCounts: IntArray(SUIT_COUNT)` — flush is any entry equal to five.
- Straights come from a precomputed `private val STRAIGHT_MASKS: List<Pair<Int, Rank>>` built
  once at class initialisation:
  - nine masks `0b11111 shl (h - 4)` paired with `Rank.entries[h]` for `h in 4..12`, giving
    six-high through ace-high,
  - plus the wheel: `(1 shl Rank.ACE.ordinal) or 0b1111` paired with `Rank.FIVE`.

  A hand is a straight when `rankMask` equals one of them. There is no wraparound mask, and
  adding one would be the bug this whole story exists to prevent.
- Read kickers off `counts` by scanning from `ACE` down to `TWO`, which yields descending order
  for free.
- Category order must match `ReferenceHandEvaluator` exactly: straight flush, quads, full house,
  flush, straight, trips, two pair, pair, high card.
- Same `require`s as the reference: exactly five distinct cards.
- KDoc stating that `ReferenceHandEvaluator` remains the oracle and that any change here must
  keep `FastEvaluatorEquivalenceTest` green.

## Out of scope

- Throughput measurement, a benchmark harness, or a documented performance budget — **`DEC-002`
  is open**; the engine has no benchmark tooling and this ticket must not add any. Correctness
  equivalence is the whole gate here.
- Packing a `HandRank` into an integer. That changes a public type and is part of `DEC-002`.
- Multithreading, memoisation of whole hands, or a 7462-entry lookup table generated at start-up.
- Deleting or "tidying" the reference evaluator. It stays forever.
- Seven-card equivalence — `TASK-010311`.

## Tests

`FastEvaluatorEquivalenceTest`

| Test | Proves |
| --- | --- |
| `agreesWithTheReferenceOnEveryFiveCardHand` | one pass over all 2 598 960 combinations — five nested index loops over `Card.all` — asserting `FastHandEvaluator.evaluate(hand) == ReferenceHandEvaluator.evaluate(hand)`; the failure message names the hand and both ranks |
| `rejectsAHandThatIsNotFiveDistinctCards` | four cards, six cards, and five cards with a duplicate each throw `IllegalArgumentException` |

Assert inside the loop and fail on the first disagreement — collecting mismatches would keep
2.6 million ranks alive for no benefit.

## Acceptance criteria

- [ ] `FastEvaluatorEquivalenceTest.agreesWithTheReferenceOnEveryFiveCardHand` passes
- [ ] `FastEvaluatorEquivalenceTest.rejectsAHandThatIsNotFiveDistinctCards` passes
- [ ] `FastHandEvaluator.kt` contains no call to `sorted`, `sortedDescending`, `groupBy` or
      `sortedBy`
- [ ] The PR changes no file other than the two listed above
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
