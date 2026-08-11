---
schema: 2
id: TASK-010302
title: HandRank, comparable lexicographically
type: task
status: ready
parent: STORY-0103
module: poker-engine
estimate: S
tier: haiku
review: deep
files_touched: 2
labels: [engine, rules]
depends_on: [TASK-010301]
verify:
  - ./gradlew :poker-engine:test --tests '*HandRankTest'
  - ./gradlew :poker-engine:check
---

## Goal

A `HandRank` that totally orders every poker hand, so a showdown is a comparison rather than a
special case.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/hand/HandRank.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/hand/HandRankTest.kt` | create |

Read `HandCategory.kt` and `card/Rank.kt` for their API only; do not modify them.

## Scope

- Exactly this type and this comparison:

  ```kotlin
  public data class HandRank(
      val category: HandCategory,
      val tiebreaks: List<Rank>,
  ) : Comparable<HandRank> {
      init {
          require(tiebreaks.isNotEmpty()) { "A hand rank needs at least one tiebreak rank" }
          require(tiebreaks.size <= MAX_TIEBREAKS) { "At most five tiebreak ranks: $tiebreaks" }
      }

      override fun compareTo(other: HandRank): Int {
          val byCategory = category.compareTo(other.category)
          if (byCategory != 0) return byCategory
          for (i in 0 until minOf(tiebreaks.size, other.tiebreaks.size)) {
              val byRank = tiebreaks[i].compareTo(other.tiebreaks[i])
              if (byRank != 0) return byRank
          }
          return tiebreaks.size.compareTo(other.tiebreaks.size)
      }
  }
  ```

  with `private const val MAX_TIEBREAKS = 5` at file level, so detekt sees no magic number.
- KDoc on the class stating the contract callers depend on: `tiebreaks` is ordered by
  **descending significance** (the most important rank first), suits are absent by design
  because they never break ties, and equal ranks mean a split pot.
- KDoc on `compareTo`: category first, then tiebreak by tiebreak.

## Out of scope

- Building a `HandRank` from cards — `TASK-010305`.
- Which tiebreaks each category carries. That is the evaluator's contract, fixed in
  `TASK-010305`; this ticket only orders whatever it is given.
- A packed integer representation, `toString` prose, or anything performance-shaped.

## Tests

Use `Rank` constants directly; no cards are needed in this file.

`HandRankTest`

| Test | Proves |
| --- | --- |
| `categoryDecidesBeforeAnyTiebreak` | `HandRank(STRAIGHT, [FIVE]) > HandRank(THREE_OF_A_KIND, [ACE, KING, QUEEN])` |
| `tiebreaksDecideWithinACategory` | `HandRank(FULL_HOUSE, [ACE, TWO]) > HandRank(FULL_HOUSE, [KING, ACE])` |
| `laterTiebreaksOnlyMatterWhenEarlierOnesTie` | `HandRank(PAIR, [KING, ACE, THREE, TWO]) > HandRank(PAIR, [KING, QUEEN, JACK, TEN])` |
| `identicalRanksCompareEqual` | two separately constructed `HandRank(TWO_PAIR, [KING, SEVEN, ACE])` compare `0`, are `equals`, and share a `hashCode` |
| `theOrderingIsAntisymmetricAndTransitive` | an ascending table of at least nine ranks, one per category: every pair `i < j` satisfies `a < b` **and** `b > a`, and sorting the shuffled table restores the table order |
| `rejectsAnEmptyTiebreakList` | `HandRank(FLUSH, emptyList())` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `HandRankTest.categoryDecidesBeforeAnyTiebreak` passes
- [ ] `HandRankTest.tiebreaksDecideWithinACategory` passes
- [ ] `HandRankTest.laterTiebreaksOnlyMatterWhenEarlierOnesTie` passes
- [ ] `HandRankTest.identicalRanksCompareEqual` passes
- [ ] `HandRankTest.theOrderingIsAntisymmetricAndTransitive` passes
- [ ] `HandRankTest.rejectsAnEmptyTiebreakList` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
