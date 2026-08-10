---
id: STORY-0101
title: Engine module and build scaffold
type: story
status: ready
parent: EPIC-01
module: poker-engine
labels: [build, foundation]
---

## Goal

A Gradle build exists, `poker-engine` is a module in it, tests run, and CI runs them on every
pull request. Nothing in it does anything poker-related yet — this is the floor everything else
stands on.

## Why

Every later task ends with "tests pass". Until there is a build that can run tests, no task can
be finished, and no acceptance criterion means anything.

## Design notes

- Kotlin JVM, Gradle with the Kotlin DSL, a version catalog in `gradle/libs.versions.toml`.
  Versions are declared in exactly one place from the first commit.
- Multi-module from the start, with only `poker-engine` present. Adding `poker-server` later
  should be a settings change, not a restructuring.
- The dependency rule from [`architecture.md`](../../docs/architecture.md) is enforced by a
  test, not by hoping. `poker-engine` gets test dependencies only.
- JUnit 5 as the runner, kotest for property-based testing. kotest is used as a library, not as
  a competing test framework.

## Tasks

Split to schema 2 by `/plan-story`. Six micro-tickets replace the original three.

| ID | Title | Est | Tier | Status |
| --- | --- | --- | --- | --- |
| [TASK-010101](../tasks/TASK-010101-gradle-wrapper-and-root-build.md) | Gradle wrapper, settings, version catalog | S | haiku | **ready** |
| [TASK-010102](../tasks/TASK-010102-poker-engine-module.md) | poker-engine module with a running test | XS | haiku | backlog |
| [TASK-010103](../tasks/TASK-010103-engine-dependency-rule.md) | Enforce the engine depends on nothing | XS | haiku | backlog |
| [TASK-010104](../tasks/TASK-010104-ktlint-and-detekt.md) | Wire ktlint and detekt into check | S | haiku | backlog |
| [TASK-010105](../tasks/TASK-010105-kotest-property-testing.md) | Add kotest property testing | XS | haiku | backlog |
| [TASK-010106](../tasks/TASK-010106-build-ci-workflow.md) | Build and test CI workflow | XS | haiku | backlog |

## Acceptance criteria

- [ ] `./gradlew build` succeeds from a clean clone with no local configuration.
- [ ] `./gradlew test` runs and reports, including a placeholder test.
- [ ] A pull request into `develop` runs the build and reports a required status check.
- [ ] `poker-engine` has no implementation dependencies, and a test fails if one is added.

## Out of scope

- Any poker domain type — that starts in STORY-0102.
- Publishing the engine as an artifact.
- Docker, deployment, or anything server-related.
