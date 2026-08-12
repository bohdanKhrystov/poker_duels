---
schema: 2
id: TASK-010715
title: Every default duel terminates, well inside an asserted ceiling
type: task
status: done
parent: STORY-0107
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [engine, duel, tests]
depends_on: [TASK-010713]
verify:
  - ./gradlew :poker-engine:test --tests '*DuelTerminationTest'
  - ./gradlew :poker-engine:check
---

## Goal

A hundred duels nobody designed all end, none of them near the ceiling — and the ceiling is
proved to be a real assertion rather than a comment.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/DuelTerminationTest.kt` | create |

Read `poker-engine/src/test/kotlin/duels/poker/engine/duel/RandomDuelPlayer.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelFormat.kt`,
`docs/duel-rules.md` Part 2 (the sentence about why the escalating schedule guarantees
termination) and
`poker-engine/src/test/kotlin/duels/poker/engine/game/SettlementInvariantTest.kt` for the sample
style. Modify none of them.

## Scope

- One new test class. No production change, no harness change.
- The sample is seeds `1L..100L` under `DuelFormat.DEFAULT`, played with an explicit
  `maxHands = 200`, so the ceiling under test is the argument and not the harness default.
- Every failure message names its seed.

## Out of scope

- Raising the sample into the thousands — the property this proves is termination, and 100 duels
  of tens of hands each already runs thousands of hands through the engine.
- Chip and button invariants — `TASK-010714` owns those over the same harness.

## Tests

`DuelTerminationTest`, `@Timeout(120)` on each test

| Test | Proves |
| --- | --- |
| `everyDefaultDuelEndsInsideTheCeiling` | for each of seeds `1L..100L`: `playRandomDuel(seed, maxHands = 200)` returns, `outcome.winner != null` and `outcome.handsPlayed < 200` |
| `theCeilingIsAnAssertionNotAnAssumption` | `playRandomDuel(seed = 1L, maxHands = 0)` throws `AssertionError`, and its message contains the seed — so a duel that ran away would have failed the test above rather than passing it quietly |
| `theSampleIsNotAllShortDuels` | over the same 100 seeds, at least 10 duels last more than 3 hands, and the longest duel is strictly longer than the shortest — so the ceiling is not passing on trivially short duels |

## Acceptance criteria

- [ ] `DuelTerminationTest.everyDefaultDuelEndsInsideTheCeiling` passes
- [ ] `DuelTerminationTest.theCeilingIsAnAssertionNotAnAssumption` passes
- [ ] `DuelTerminationTest.theSampleIsNotAllShortDuels` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
