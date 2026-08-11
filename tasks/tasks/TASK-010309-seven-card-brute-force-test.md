---
schema: 2
id: TASK-010309
title: Brute-force check of seven-card evaluation
type: task
status: ready
parent: STORY-0103
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [engine, test, rules]
depends_on: [TASK-010308]
verify:
  - ./gradlew :poker-engine:test --tests '*SevenCardBruteForceTest'
  - ./gradlew :poker-engine:check
---

## Goal

Over ten thousand dealt hands, `bestOfSeven` equals the maximum of the 21 subsets computed by an
independently written enumerator — so the two disagree only if both are wrong in the same way.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/hand/SevenCardBruteForceTest.kt` | create |

Read `HandEvaluator.kt`, `card/Deck.kt` and `random/SplitMix64Rng.kt` for their API. **No
production code changes.**

## Scope

- Deal deterministically: for `seed in 1..10_000`, take
  `Deck.full().shuffled(SplitMix64Rng(seed)).deck.deal(7).cards`. Every seed is fixed, so a
  failure is reproducible and the test cannot flake. Do not use `Arb`, a clock, or
  `kotlin.random.Random`.
- The oracle in this file must **not** reuse `bestOfSeven`'s loops. Enumerate subsets by bitmask
  instead — `(0 until 128).filter { Integer.bitCount(it) == 5 }` — and take the maximum rank.
  That independence is the entire point of the ticket.
- Failure messages must print the seven cards and both ranks.

## Out of scope

- Any change to `HandEvaluator`, `BestHand` or `ReferenceHandEvaluator`. A disagreement is a
  finding to report, not something to fix here.
- The fast evaluator — `TASK-010310` and `TASK-010311`.
- Timing, throughput or benchmarks — see `DEC-002`.

## Tests

`SevenCardBruteForceTest`

| Test | Proves |
| --- | --- |
| `bestOfSevenEqualsTheMaximumOfTheTwentyOneSubsets` | over seeds `1..10_000`, `ReferenceHandEvaluator.bestOfSeven(hand).rank` equals the bitmask oracle's maximum |
| `theReturnedFiveCardsAlwaysMakeTheReturnedRank` | over the same 10 000 hands, the returned `cards` are five distinct members of the seven and `evaluate(cards) == rank` |
| `noSubsetOfTheSevenBeatsTheReturnedRank` | over the same 10 000 hands, every one of the 21 subsets ranks `<=` the returned rank |

Share one deal loop across the three assertions if that keeps the file inside 120 lines; three
separate loops over 10 000 seeds are also acceptable.

## Acceptance criteria

- [ ] `SevenCardBruteForceTest.bestOfSevenEqualsTheMaximumOfTheTwentyOneSubsets` passes
- [ ] `SevenCardBruteForceTest.theReturnedFiveCardsAlwaysMakeTheReturnedRank` passes
- [ ] `SevenCardBruteForceTest.noSubsetOfTheSevenBeatsTheReturnedRank` passes
- [ ] No seed in the file is random, derived from time, or supplied by kotest
- [ ] `git diff --name-only` in the PR lists no file under `src/main/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
