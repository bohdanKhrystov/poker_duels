---
schema: 2
id: TASK-010102
title: poker-engine module with a running test
type: task
status: ready
parent: STORY-0101
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [build]
depends_on: [TASK-010101]
verify:
  - ./gradlew :poker-engine:test
---

## Goal

`poker-engine` exists as a JVM library module and its test task runs a real test.

## Files

| File | Action |
| --- | --- |
| `settings.gradle.kts` | modify |
| `poker-engine/build.gradle.kts` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/SmokeTest.kt` | create |

## Scope

- `include(":poker-engine")` in settings.
- `poker-engine/build.gradle.kts`: Kotlin JVM library, JUnit 5 on the test platform, **no
  implementation dependencies at all**.
- `SmokeTest` with one passing test, so the runner is proven wired rather than assumed.

## Out of scope

- Any domain type. `Card`, `Deck` and friends start in `STORY-0102`.
- Asserting the no-dependency rule — `TASK-010103` does that.
- ktlint, detekt, kotest — `TASK-010104`, `TASK-010105`.

## Tests

`SmokeTest`

| Test | Proves |
| --- | --- |
| `testRunnerIsWired` | the JUnit 5 platform is configured and reporting |

## Acceptance criteria

- [ ] `SmokeTest.testRunnerIsWired` passes.
- [ ] `./gradlew :poker-engine:test` exits 0 and reports one executed test.
- [ ] `poker-engine/build.gradle.kts` contains no `implementation(` or `api(` line.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): `verify` green, review passed, CI green, status
`done`, `BOARD.md` updated, squash-merged into `develop`. Not done until the PR is merged.
