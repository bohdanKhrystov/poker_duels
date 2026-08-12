---
schema: 2
id: TASK-010829
title: Run a thousand duels and report on them
type: task
status: backlog
parent: STORY-0108
module: poker-ai
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [simulation, ai]
depends_on: [TASK-010828]
verify:
  - ./gradlew :poker-ai:test --tests '*SimulationRunnerTest'
  - ./gradlew :poker-ai:check
---

## Goal

A headless run of many duels returns one aggregate value, and the first invariant violation stops
it with the pair that reproduces it.

## Files

| File | Action |
| --- | --- |
| `poker-ai/src/main/kotlin/duels/poker/ai/SimulationReport.kt` | create |
| `poker-ai/src/main/kotlin/duels/poker/ai/SimulationRunner.kt` | create |
| `poker-ai/src/test/kotlin/duels/poker/ai/SimulationRunnerTest.kt` | create |

## Scope

- `SimulationReport.kt`, one public data class with KDoc:

  ```kotlin
  public data class SimulationReport(
      val duels: Int,
      val hands: Int,
      val shortestDuel: Int,
      val longestDuel: Int,
      val winsBySeat: List<Int>,
      val draws: Int,
  ) {
      public val averageHandsPerDuel: Double get() = ...
  }
  ```

- `SimulationRunner.kt`, one public function with KDoc:

  ```kotlin
  public fun runSimulation(
      duels: Int,
      bots: List<Bot>,
      format: DuelFormat = DuelFormat.DEFAULT,
      seed: Long = 1L,
  ): SimulationReport
  ```

  Duel `n` (0-based) is played with seed `seed + n`, documented in the KDoc, so a report names the
  seeds it covered. Single-threaded: correctness first, and the numbers here do not need
  concurrency.
- A `SimulationFailure` from any duel propagates unchanged — the run stops at the first violation
  rather than counting how many there were. The exception already carries the reproducing pair.
- `require(duels >= 1)` and `require(bots.size == 2)`, each with a message naming the value.
- No I/O and no printing: the report is a value the caller does what it likes with.

## Out of scope

- The hundred-thousand-duel run and its Gradle task — `TASK-010830`.
- Concurrency, progress output, or writing results anywhere.
- Showdown-category frequencies: a distribution check over `HandCategory` would be worth having,
  but it needs the evaluator on the hot path and `DEC-002` is still open. Not ticketed.

## Tests

`SimulationRunnerTest`, JUnit 5, package `duels.poker.ai`. `RandomBot` for both seats. A thousand
duels is the short run that belongs in the normal suite; give it `@Timeout(300)`.

| Test | Proves |
| --- | --- |
| `aThousandDuelsRunWithNoViolation` | `runSimulation(1000, listOf(RandomBot, RandomBot))` returns without throwing and reports `duels == 1000` |
| `theReportAccountsForEveryDuel` | `winsBySeat.sum() + draws == duels`, and `hands` is at least `duels` |
| `theHandCountsBracketTheAverage` | `shortestDuel <= averageHandsPerDuel && averageHandsPerDuel <= longestDuel`, and `shortestDuel >= 1` |
| `theSameSeedGivesTheSameReport` | two runs of 50 duels with the same seed return equal reports |
| `aDifferentSeedGivesADifferentRun` | runs of 50 duels with seeds 1 and 1000 differ in at least one field, so the seed is actually used |
| `rejectsANonPositiveDuelCount` | `runSimulation(0, bots)` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `SimulationRunnerTest.aThousandDuelsRunWithNoViolation` passes
- [ ] `SimulationRunnerTest.theReportAccountsForEveryDuel` passes
- [ ] `SimulationRunnerTest.theHandCountsBracketTheAverage` passes
- [ ] `SimulationRunnerTest.theSameSeedGivesTheSameReport` passes
- [ ] `SimulationRunnerTest.aDifferentSeedGivesADifferentRun` passes
- [ ] `SimulationRunnerTest.rejectsANonPositiveDuelCount` passes
- [ ] No file outside the three in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
