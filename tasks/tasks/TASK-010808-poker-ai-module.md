---
schema: 2
id: TASK-010808
title: The poker-ai module, where bots and the harness live
type: task
status: backlog
parent: STORY-0108
module: poker-ai
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [build, simulation]
depends_on: [TASK-010807]
verify:
  - ./gradlew :poker-ai:test --tests '*PokerAiModuleTest'
  - ./gradlew :poker-ai:check
---

## Goal

`:poker-ai` exists, compiles, sees the engine, and runs its own tests — the home
[`docs/architecture.md`](../../docs/architecture.md) already assigns to "bots and the simulation
harness".

## Files

| File | Action |
| --- | --- |
| `settings.gradle.kts` | modify |
| `poker-ai/build.gradle.kts` | create |
| `poker-ai/src/test/kotlin/duels/poker/ai/PokerAiModuleTest.kt` | create |

Read, do not modify: `poker-engine/build.gradle.kts`, `build.gradle.kts`.

## Scope

- `settings.gradle.kts` gains `include(":poker-ai")` on the line after `include(":poker-engine")`.
  Nothing else in that file changes.
- `poker-ai/build.gradle.kts`, modelled on `poker-engine/build.gradle.kts` minus its
  `checkNoDependencies` task, which is the engine's rule and not this module's:

  ```kotlin
  plugins {
      kotlin("jvm")
  }

  dependencies {
      implementation(project(":poker-engine"))
      testImplementation(libs.bundles.junit)
  }

  tasks.withType<Test>().configureEach {
      useJUnitPlatform()
  }
  ```

- The dependency runs one way only: `poker-ai` depends on `poker-engine`, and no change in this
  ticket adds anything to the engine's build file.
- No `src/main` sources yet — `TASK-010809` adds the first ones.

## Out of scope

- The `Bot` interface and `RandomBot` — `TASK-010809`.
- The simulation runner — `TASK-010812`, blocked on `STORY-0107`.
- Any other module from the architecture's list: `poker-cli`, `poker-server`, `poker-analysis`.
- Changing `docs/architecture.md`, which already names this module.

## Tests

`PokerAiModuleTest`, JUnit 5, package `duels.poker.ai`.

| Test | Proves |
| --- | --- |
| `theEngineIsOnTheModulesClasspath` | `Card.all.size == 52`, imported from `duels.poker.engine.card` |
| `theModuleCanOpenAHand` | `startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L)).newState.seatToAct == 0` |

## Acceptance criteria

- [ ] `PokerAiModuleTest.theEngineIsOnTheModulesClasspath` passes
- [ ] `PokerAiModuleTest.theModuleCanOpenAHand` passes
- [ ] `./gradlew :poker-ai:check` exits 0, ktlint and detekt included
- [ ] `poker-engine/build.gradle.kts` is unchanged
- [ ] No file outside the three in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
