---
id: TASK-010101
title: Gradle multi-module skeleton with version catalog
type: task
status: ready
parent: STORY-0101
module: poker-engine
estimate: S
labels: [build]
depends_on: []
---

## Goal

`./gradlew build` succeeds from a clean clone, with `poker-engine` as the single module and all
dependency versions declared in one place.

## Context

- [`docs/architecture.md`](../../docs/architecture.md) — the module list and the dependency rule.
- [`docs/adr/ADR-0003-technology-stack.md`](../../docs/adr/ADR-0003-technology-stack.md) — why
  Gradle, Kotlin JVM, and no framework.

## Scope

- Gradle wrapper, committed, on the current stable Gradle.
- Root `build.gradle.kts` and `settings.gradle.kts`, Kotlin DSL only.
- `gradle/libs.versions.toml` version catalog. Every version lives here from the first commit —
  no version literals in a build file.
- A `poker-engine` module producing a JVM library, with **no** implementation dependencies.
- JUnit 5 wired up, plus one placeholder test so `./gradlew test` reports something real.
- `.gitignore` extended for Gradle and JVM build output.

## Out of scope

- ktlint and detekt — `TASK-010102`.
- CI — `TASK-010103`.
- Any poker type. The placeholder test asserts `true`; that is the correct amount of domain
  logic for this ticket.
- Other modules. Adding `poker-server` later must be a one-line `settings.gradle.kts` change,
  and that is the design constraint this ticket has to satisfy.

## Files

| File | Action |
| --- | --- |
| `settings.gradle.kts` | create |
| `build.gradle.kts` | create |
| `gradle/libs.versions.toml` | create |
| `gradle/wrapper/*`, `gradlew`, `gradlew.bat` | create |
| `poker-engine/build.gradle.kts` | create |
| `poker-engine/src/main/kotlin/.gitkeep` | create |
| `poker-engine/src/test/kotlin/.../SmokeTest.kt` | create |
| `.gitignore` | modify |

## Acceptance criteria

- [ ] `./gradlew build` succeeds on a clean clone with no local configuration and no network
      credentials.
- [ ] `./gradlew test` runs the placeholder test and reports it.
- [ ] `poker-engine/build.gradle.kts` declares only `testImplementation` dependencies.
- [ ] No version string appears outside `libs.versions.toml`.
- [ ] The Kotlin JVM toolchain is pinned explicitly, so the build does not depend on whichever
      JDK happens to be installed.

## Tests

- `SmokeTest` — proves the test runner is wired up and reporting.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
