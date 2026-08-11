---
schema: 2
id: TASK-010306
title: Pin the wheel, the non-wraparound and suit irrelevance
type: task
status: backlog
parent: STORY-0103
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [engine, test, rules]
depends_on: [TASK-010305]
verify:
  - ./gradlew :poker-engine:test --tests '*ReferenceEvaluatorRulesTest'
  - ./gradlew :poker-engine:check
---

## Goal

The four rules from `docs/duel-rules.md` that an evaluator can silently break — the wheel, the
missing wraparound, suit irrelevance and order independence — are each pinned by a named test.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/hand/ReferenceEvaluatorRulesTest.kt` | create |

Read `ReferenceHandEvaluator.kt` and `HandRank.kt` for their API. **No production code changes
in this ticket.** If a test below fails, that is the finding: report it and stop rather than
editing the evaluator here.

## Scope

- Tests only, against `ReferenceHandEvaluator`, built with
  `duels.poker.engine.card.cards("...")`.
- Every hand is a literal in poker notation, so a failure message names the hand.
- Reuse this local helper for the permutation test:

  ```kotlin
  private fun <T> permutations(items: List<T>): List<List<T>> =
      if (items.size <= 1) listOf(items)
      else items.flatMap { head -> permutations(items - head).map { listOf(head) + it } }
  ```

## Out of scope

- Any change to `ReferenceHandEvaluator`, `HandPatterns` or `HandRank`.
- Exhaustive enumeration of all five-card hands — `TASK-010307`.
- Seven-card hands — `TASK-010308` and `TASK-010309`.

## Tests

`ReferenceEvaluatorRulesTest`

| Test | Proves |
| --- | --- |
| `theWheelIsTheLowestStraight` | `"Ah 5d 4c 3s 2h"` is a `STRAIGHT` with tiebreaks `[FIVE]`, and ranks **below** `"6h 5d 4c 3s 2h"` and below every other straight |
| `theSteelWheelIsTheLowestStraightFlush` | `"Ah 5h 4h 3h 2h"` is a `STRAIGHT_FLUSH` with tiebreaks `[FIVE]`, and ranks below `"6h 5h 4h 3h 2h"` |
| `queenKingAceTwoThreeIsNotAStraight` | `"Qh Ks Ad 2c 3h"` is `HIGH_CARD`, and `"Qh Kh Ah 2h 3h"` is `FLUSH`, not a straight flush |
| `theRoyalFlushBeatsOneHandFromEveryOtherCategory` | `"As Ks Qs Js Ts"` compares greater than a table holding one named hand per other category, all eight of them |
| `suitsNeverBreakTies` | `"As Kd Qh 7c 2s"` and `"Ah Kc Qs 7d 2h"` compare `0` and are `equals`; likewise two aces-full-of-kings in different suits |
| `permutingTheSameFiveCardsNeverChangesTheRank` | all 120 permutations of each of `"Ah 5d 4c 3s 2h"`, `"Kh Kd 7c 7s Ah"`, `"Ah Jh 8h 5h 2h"` and `"9h 9d 9c 9s Ad"` evaluate to a single distinct `HandRank` |

## Acceptance criteria

- [ ] `ReferenceEvaluatorRulesTest.theWheelIsTheLowestStraight` passes
- [ ] `ReferenceEvaluatorRulesTest.theSteelWheelIsTheLowestStraightFlush` passes
- [ ] `ReferenceEvaluatorRulesTest.queenKingAceTwoThreeIsNotAStraight` passes
- [ ] `ReferenceEvaluatorRulesTest.theRoyalFlushBeatsOneHandFromEveryOtherCategory` passes
- [ ] `ReferenceEvaluatorRulesTest.suitsNeverBreakTies` passes
- [ ] `ReferenceEvaluatorRulesTest.permutingTheSameFiveCardsNeverChangesTheRank` passes
- [ ] `git diff --name-only` in the PR lists no file under `src/main/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
