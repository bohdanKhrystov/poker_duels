---
schema: 2
id: TASK-010311
title: Fast and reference evaluators agree on seven cards
type: task
status: done
parent: STORY-0103
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [engine, test, rules]
depends_on: [TASK-010310]
verify:
  - ./gradlew :poker-engine:test --tests '*FastEvaluatorSevenCardTest'
  - ./gradlew :poker-engine:check
---

## Goal

The two evaluators pick the same winning hand from seven cards, over a hundred thousand dealt
boards — the shape of input a real duel actually produces.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/hand/FastEvaluatorSevenCardTest.kt` | create |

Read `HandEvaluator.kt` and `FastHandEvaluator.kt` for their API. **No production code changes.**

## Scope

- Deal deterministically: for `seed in 1..100_000`, take
  `Deck.full().shuffled(SplitMix64Rng(seed)).deck.deal(7).cards`. No `Arb`, no clock, no
  `kotlin.random.Random`.
- Compare `FastHandEvaluator.bestOfSeven(hand)` against `ReferenceHandEvaluator.bestOfSeven(hand)`
  on both fields: the `rank` must be equal, and the chosen `cards` must be equal too — both
  inherit the same default `bestOfSeven`, so a difference in the chosen five means the underlying
  `evaluate` disagreed on a tie.
- Fail on the first mismatch, printing the seed, the seven cards and both results. The seed alone
  makes the failure reproducible.

## Out of scope

- Any production code change. A disagreement is a finding against `TASK-010310`, reported, not
  patched here.
- Timing or throughput — `DEC-002` is open.
- Exhaustive five-card equivalence, which `TASK-010310` already covers.

## Tests

`FastEvaluatorSevenCardTest`

| Test | Proves |
| --- | --- |
| `agreesWithTheReferenceOnOneHundredThousandDealtHands` | over seeds `1..100_000`, both evaluators return an equal `BestHand` |

## Acceptance criteria

- [ ] `FastEvaluatorSevenCardTest.agreesWithTheReferenceOnOneHundredThousandDealtHands` passes
- [ ] No seed in the file is random, derived from time, or supplied by kotest
- [ ] `git diff --name-only` in the PR lists no file under `src/main/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
