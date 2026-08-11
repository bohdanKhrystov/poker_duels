---
schema: 2
id: TASK-010305
title: Reference five-card evaluator
type: task
status: done
parent: STORY-0103
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [engine, rules]
depends_on: [TASK-010304]
verify:
  - ./gradlew :poker-engine:test --tests '*ReferenceHandEvaluatorTest'
  - ./gradlew :poker-engine:check
---

## Goal

Five cards become a `HandRank`, by an implementation written to be obviously correct rather than
fast — the oracle every later optimisation is measured against.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/hand/HandEvaluator.kt` | create |
| `poker-engine/src/main/kotlin/duels/poker/engine/hand/ReferenceHandEvaluator.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/hand/ReferenceHandEvaluatorTest.kt` | create |

Read `HandPatterns.kt`, `HandRank.kt` and `card/Card.kt` for their API. Do not modify them.

## Scope

- `HandEvaluator.kt`:

  ```kotlin
  public interface HandEvaluator {
      /** Ranks exactly five distinct cards. */
      public fun evaluate(cards: List<Card>): HandRank
  }
  ```

- `ReferenceHandEvaluator.kt` — a `public object` implementing it, assembled from the helpers
  that already exist. Exactly this decision order, because a hand that is both a flush and a
  straight must be reported as a straight flush and a full house must be found before a flush is
  ruled out:

  ```kotlin
  public object ReferenceHandEvaluator : HandEvaluator {
      override fun evaluate(cards: List<Card>): HandRank {
          require(cards.size == HAND_SIZE) { "A five-card hand is required, got ${cards.size}" }
          require(cards.toSet().size == HAND_SIZE) { "Duplicate card in $cards" }

          val ranks = cards.map { it.rank }
          val descending = ranks.sortedDescending()
          val groups = rankGroups(ranks)
          val flush = cards.all { it.suit == cards.first().suit }
          val straightHigh = straightHighOrNull(ranks)

          return when {
              flush && straightHigh != null -> HandRank(STRAIGHT_FLUSH, listOf(straightHigh))
              groups[0].size == QUADS -> HandRank(FOUR_OF_A_KIND, groups.map { it.rank })
              groups[0].size == TRIPS && groups[1].size == PAIR_SIZE ->
                  HandRank(FULL_HOUSE, groups.map { it.rank })
              flush -> HandRank(FLUSH, descending)
              straightHigh != null -> HandRank(STRAIGHT, listOf(straightHigh))
              groups[0].size == TRIPS -> HandRank(THREE_OF_A_KIND, groups.map { it.rank })
              groups[0].size == PAIR_SIZE && groups[1].size == PAIR_SIZE ->
                  HandRank(TWO_PAIR, groups.map { it.rank })
              groups[0].size == PAIR_SIZE -> HandRank(PAIR, groups.map { it.rank })
              else -> HandRank(HIGH_CARD, descending)
          }
      }
  }
  ```

  `HAND_SIZE`, `QUADS`, `TRIPS`, `PAIR_SIZE` are `private const val` in the file — detekt runs
  with `maxIssues: 0` and flags bare literals.
- KDoc on the object saying it is the test oracle and must stay readable; speed is
  `TASK-010310`'s problem, and making this one clever destroys its only reason to exist.

## Out of scope

- Seven-card hands — `TASK-010308`.
- Any optimisation, packing, caching or memoisation — `TASK-010310`.
- Wheel, suit-symmetry and permutation testing beyond the single wheel case below —
  `TASK-010306`.
- Exhaustive category counts — `TASK-010307`.

## Tests

Use `duels.poker.engine.card.cards("As Kd ...")` to build hands. Assert both
`rank.category` and `rank.tiebreaks` on every case — a category-only assertion would miss a
wrong kicker, which is the bug that actually costs pots.

`ReferenceHandEvaluatorTest`

| Test | Proves |
| --- | --- |
| `straightFlushRanksByItsHighCard` | `"9h 8h 7h 6h 5h"` → `STRAIGHT_FLUSH`, `[NINE]` |
| `fourOfAKindRanksTheQuadThenTheKicker` | `"9h 9d 9c 9s Ad"` → `FOUR_OF_A_KIND`, `[NINE, ACE]` |
| `fullHouseRanksTheTripsThenThePair` | `"3h 3d 3c Kh Kd"` → `FULL_HOUSE`, `[THREE, KING]` |
| `flushRanksAllFiveDescending` | `"Ah Jh 8h 5h 2h"` → `FLUSH`, `[ACE, JACK, EIGHT, FIVE, TWO]` |
| `straightRanksByItsHighCard` | `"9h 8d 7c 6s 5h"` → `STRAIGHT`, `[NINE]` |
| `theWheelIsAFiveHighStraight` | `"Ah 5d 4c 3s 2h"` → `STRAIGHT`, `[FIVE]` |
| `threeOfAKindRanksTripsThenTwoKickers` | `"9h 9d 9c Ah 5d"` → `THREE_OF_A_KIND`, `[NINE, ACE, FIVE]` |
| `twoPairRanksHighPairLowPairThenKicker` | `"Kh Kd 7c 7s Ah"` → `TWO_PAIR`, `[KING, SEVEN, ACE]` |
| `onePairRanksThePairThenThreeKickers` | `"5h 5d Ac Kh 2d"` → `PAIR`, `[FIVE, ACE, KING, TWO]` |
| `highCardRanksAllFiveDescending` | `"Ah Jd 8c 5s 2h"` → `HIGH_CARD`, `[ACE, JACK, EIGHT, FIVE, TWO]` |
| `rejectsAHandThatIsNotFiveCards` | four cards and six cards each throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] `ReferenceHandEvaluatorTest.straightFlushRanksByItsHighCard` passes
- [ ] `ReferenceHandEvaluatorTest.fourOfAKindRanksTheQuadThenTheKicker` passes
- [ ] `ReferenceHandEvaluatorTest.fullHouseRanksTheTripsThenThePair` passes
- [ ] `ReferenceHandEvaluatorTest.flushRanksAllFiveDescending` passes
- [ ] `ReferenceHandEvaluatorTest.straightRanksByItsHighCard` passes
- [ ] `ReferenceHandEvaluatorTest.theWheelIsAFiveHighStraight` passes
- [ ] `ReferenceHandEvaluatorTest.threeOfAKindRanksTripsThenTwoKickers` passes
- [ ] `ReferenceHandEvaluatorTest.twoPairRanksHighPairLowPairThenKicker` passes
- [ ] `ReferenceHandEvaluatorTest.onePairRanksThePairThenThreeKickers` passes
- [ ] `ReferenceHandEvaluatorTest.highCardRanksAllFiveDescending` passes
- [ ] `ReferenceHandEvaluatorTest.rejectsAHandThatIsNotFiveCards` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
