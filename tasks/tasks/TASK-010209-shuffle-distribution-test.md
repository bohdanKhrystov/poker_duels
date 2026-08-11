---
schema: 2
id: TASK-010209
title: Assert the shuffle spreads every card over every position
type: task
status: backlog
parent: STORY-0102
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [engine, test, determinism]
depends_on: [TASK-010207]
verify:
  - ./gradlew :poker-engine:test --tests '*ShuffleDistributionTest'
  - ./gradlew :poker-engine:check
---

## Goal

Evidence that the shuffle is not merely reproducible but unbiased: over tens of thousands of
fixed seeds, no card favours any position.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/card/ShuffleDistributionTest.kt` | create |

Read `Deck.kt` and `SplitMix64Rng.kt` for the API only.

## Scope

- Every seed is fixed, so a failure is reproducible and the test cannot flake. Do not use kotest
  `Arb` here and do not derive a seed from a clock.
- Test one: shuffle `Deck.full()` with `SplitMix64Rng(seed)` for `seed in 1..20_000`, dealing all
  52 cards each time, and count how often each card lands in each position — a 52 × 52 table.
  The expected count per cell is `20_000 / 52.0` ≈ 384.6; assert **every one of the 2704 cells
  lies within 25 % of it**, i.e. in `288..481`. A cell that far out is roughly five standard
  deviations, so an unbiased shuffle passes and a modulo-biased one does not.
- Test two: shuffle with `SplitMix64Rng(seed)` for `seed in 1..52_000` and count the top card
  only. The expected count is 1000; assert every card's count is within 20 %, i.e. in
  `800..1200`.
- Failure messages must name the offending card, position and count — a bare `assertTrue` on a
  loop tells the next reader nothing.
- Keep it under ten seconds: reuse one deck value, count into an `IntArray`, and do not build
  intermediate strings inside the loops.

## Out of scope

- Chi-squared or any other statistic that needs a critical-value table. The fixed tolerances
  above are the whole criterion.
- Claiming anything about cryptographic strength. The generator is not cryptographic and does
  not need to be; the seed never leaves the server while a hand is live (`ADR-0002`).
- Benchmarks or performance budgets — the evaluator's problem, `STORY-0103`.
- Any change to production code.

## Tests

`ShuffleDistributionTest`

| Test | Proves |
| --- | --- |
| `everyCardReachesEveryPositionAtCloseToUniformFrequency` | over seeds `1..20_000`, all 2704 card/position counts fall in `288..481` |
| `theTopCardIsUniformAcrossSeeds` | over seeds `1..52_000`, every card tops the deck between 800 and 1200 times |

## Acceptance criteria

- [ ] `ShuffleDistributionTest.everyCardReachesEveryPositionAtCloseToUniformFrequency` passes
- [ ] `ShuffleDistributionTest.theTopCardIsUniformAcrossSeeds` passes
- [ ] No seed in the file is random, derived from time, or supplied by kotest
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
