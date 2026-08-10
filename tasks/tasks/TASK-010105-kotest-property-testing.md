---
schema: 2
id: TASK-010105
title: Add kotest property testing to the engine
type: task
status: ready
parent: STORY-0101
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [build, test]
depends_on: [TASK-010102]
verify:
  - ./gradlew :poker-engine:test --tests '*PropertySmokeTest'
---

## Goal

Property-based tests can be written, proven by one that runs.

## Context

Property testing is how the engine's real invariants get checked later — chip conservation,
determinism, evaluator agreement over generated hands. This ticket only makes it available.

## Files

| File | Action |
| --- | --- |
| `gradle/libs.versions.toml` | modify |
| `poker-engine/build.gradle.kts` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/PropertySmokeTest.kt` | create |

## Scope

- kotest property testing as a **`testImplementation`** dependency only. It must not appear on
  any production configuration — `TASK-010103` will fail the build if it does.
- JUnit 5 stays the runner; kotest is used as a library, not a competing framework.
- One property test over a trivial invariant, present purely to prove the stack works.

## Out of scope

- Real invariants. Those belong to the tickets that introduce the code they constrain.
- kotest's own spec styles or assertion DSL as a project-wide convention.

## Tests

`PropertySmokeTest`

| Test | Proves |
| --- | --- |
| `propertyStackRuns` | a generated-input property executes under the JUnit 5 platform |

## Acceptance criteria

- [ ] `PropertySmokeTest.propertyStackRuns` passes.
- [ ] `./gradlew :poker-engine:checkNoDependencies` still exits 0.
- [ ] kotest appears only on `testImplementation`.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): `verify` green, review passed, CI green, status
`done`, `BOARD.md` updated, squash-merged into `develop`. Not done until the PR is merged.
