---
schema: 2
id: TASK-010308
title: Seven-card best-of-five evaluation
type: task
status: ready
parent: STORY-0103
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [engine, rules]
depends_on: [TASK-010307]
verify:
  - ./gradlew :poker-engine:test --tests '*SevenCardEvaluationTest'
  - ./gradlew :poker-engine:check
---

## Goal

Two hole cards plus five board cards resolve to the best five-card hand available, together with
*which* five cards make it.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/hand/HandEvaluator.kt` | modify |
| `poker-engine/src/main/kotlin/duels/poker/engine/hand/BestHand.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/hand/SevenCardEvaluationTest.kt` | create |

Read `HandRank.kt` and `ReferenceHandEvaluator.kt` for their API; do not modify either.

## Scope

- `BestHand.kt`:

  ```kotlin
  /** The winning five of a seven-card hand, and the rank they make. */
  public data class BestHand(val rank: HandRank, val cards: List<Card>)
  ```

  KDoc: `cards` exists because the client highlights the winning five at showdown, and
  recomputing them there would duplicate this logic in a place that could disagree with it.
- Add to the `HandEvaluator` interface a **default** method, so every implementation — including
  the fast one in `TASK-010310` — inherits it and gets its speed from `evaluate`:

  ```kotlin
  public fun bestOfSeven(cards: List<Card>): BestHand
  ```

- Implementation: require exactly seven distinct cards, then walk all 21 subsets with five nested
  index loops (`a in 0..2`, `b in a + 1..3`, … `e in d + 1..6`), keeping the best. Keep the first
  subset on a tie — the rank is what matters, and a stable rule makes the choice reproducible.
  State that tie rule in the KDoc.
- Neither hole card need be used; the board may play in full. That falls out of the enumeration —
  do not special-case it.

## Out of scope

- A faster subset walk, bit tricks or caching — `TASK-010310`.
- Six-card, four-card or two-card evaluation. Hold'em never needs them.
- Deciding who wins a *pot*, splitting chips, or mucking — STORY-0106.
- Brute-force agreement over generated hands — `TASK-010309`.

## Tests

`SevenCardEvaluationTest`, against `ReferenceHandEvaluator`, hands built with
`duels.poker.engine.card.cards("...")`.

| Test | Proves |
| --- | --- |
| `picksTheBestOfTheTwentyOneSubsets` | `"As Ks 7h 7d 7c 2s 3d"` → `THREE_OF_A_KIND` with tiebreaks `[SEVEN, ACE, KING]` |
| `theBoardCanPlayInFull` | `"2c 3d Ah Kh Qh Jh Th"` → `STRAIGHT_FLUSH` `[ACE]`, and the returned cards are exactly the five hearts |
| `oneHoleCardCanPlay` | `"Ah 2c Kh Qh Jh Th 5d"` → `STRAIGHT_FLUSH` `[ACE]` using `Ah` |
| `theReturnedCardsAreFiveOfTheSevenAndMakeTheReturnedRank` | for each hand above: `cards.size == 5`, all five are in the input, and `evaluate(cards) == rank` |
| `orderOfTheSevenCardsDoesNotChangeTheRank` | each hand above, also evaluated reversed and rotated by three, gives the same `rank` |
| `rejectsAnythingOtherThanSevenDistinctCards` | six cards, eight cards and seven cards containing a duplicate each throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] `SevenCardEvaluationTest.picksTheBestOfTheTwentyOneSubsets` passes
- [ ] `SevenCardEvaluationTest.theBoardCanPlayInFull` passes
- [ ] `SevenCardEvaluationTest.oneHoleCardCanPlay` passes
- [ ] `SevenCardEvaluationTest.theReturnedCardsAreFiveOfTheSevenAndMakeTheReturnedRank` passes
- [ ] `SevenCardEvaluationTest.orderOfTheSevenCardsDoesNotChangeTheRank` passes
- [ ] `SevenCardEvaluationTest.rejectsAnythingOtherThanSevenDistinctCards` passes
- [ ] `bestOfSeven` is a default method on `HandEvaluator`, not an override in
      `ReferenceHandEvaluator`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
