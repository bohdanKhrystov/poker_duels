---
schema: 2
id: TASK-010830
title: A hundred thousand duels, off the default test task
type: task
status: backlog
parent: STORY-0108
module: poker-ai
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [simulation, ai, build]
depends_on: [TASK-010829]
verify:
  - ./gradlew :poker-ai:soakTest -PsoakDuels=200
  - ./gradlew :poker-ai:check
  - ./gradlew :poker-ai:soakTest
---

## Goal

A hundred thousand duels play with no rule violation and no crash, on demand and off the critical
path of every build.

## Files

| File | Action |
| --- | --- |
| `poker-ai/build.gradle.kts` | modify |
| `poker-ai/src/test/kotlin/duels/poker/ai/SimulationSoakTest.kt` | create |

## Scope

- `poker-ai/build.gradle.kts`:
  - the existing `test` task excludes the tag: `useJUnitPlatform { excludeTags("soak") }`;
  - a new `soakTest` task of type `Test`, using the test source set's classpath and
    `useJUnitPlatform { includeTags("soak") }`, **not** wired into `check`;
  - `soakTest` forwards the run size to the test JVM, because a `-P` property reaches Gradle, not
    the forked test process:

    ```kotlin
    systemProperty("soakDuels", providers.gradleProperty("soakDuels").getOrElse("100000"))
    ```

- `SimulationSoakTest.kt`: one JUnit 5 class tagged `@Tag("soak")` with one test,
  `@Timeout(value = 90, unit = TimeUnit.MINUTES)`, reading
  `System.getProperty("soakDuels")?.toInt() ?: 100_000` and calling
  `runSimulation(duels, listOf(RandomBot, RandomBot))`.
- Assertions: `report.duels` equals the requested count, `report.winsBySeat.sum() + report.draws`
  equals it too, and `report.hands >= report.duels`. A `SimulationFailure` fails the test by
  propagating — that is the whole point of the run.
- `DEC-002` — the hand evaluator's performance budget — is still open, and this is the first thing
  in the project that makes evaluator speed observable. **If the run is too slow to finish inside
  the timeout, do not shrink the run and do not weaken an assertion.** Stop and report it against
  `DEC-002`.

## Out of scope

- Wiring `soakTest` into `check` or into CI: `docs/workflow.md` keeps CI fast, and this is an
  on-demand run.
- Parallelism or any performance work on the engine — that is `DEC-002`'s to answer.
- Reporting or storing the results anywhere.

## Tests

`SimulationSoakTest`, JUnit 5, package `duels.poker.ai`.

| Test | Proves |
| --- | --- |
| `oneHundredThousandDuelsPlayWithNoViolation` | the requested number of duels completes, every one accounted for in the report, no `SimulationFailure` thrown |

The first `verify` command runs the same test at 200 duels, which proves the task, the tag
exclusion and the property forwarding all work in seconds. The third runs it at its real size.

## Acceptance criteria

- [ ] `./gradlew :poker-ai:soakTest -PsoakDuels=200` exits 0 and runs exactly one test
- [ ] `./gradlew :poker-ai:test` does **not** run `SimulationSoakTest` — the tag is excluded
- [ ] `./gradlew :poker-ai:check` exits 0 and does not depend on `soakTest`
- [ ] `./gradlew :poker-ai:soakTest` exits 0 at the default 100 000 duels
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
