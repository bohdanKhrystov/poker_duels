---
schema: 2
id: TASK-010103
title: Enforce that poker-engine depends on nothing
type: task
status: done
parent: STORY-0101
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [build, architecture]
depends_on: [TASK-010102]
verify:
  - ./gradlew :poker-engine:checkNoDependencies
---

## Goal

The engine's independence is checked by the build, not by anyone remembering it.

## Context

- [`docs/architecture.md`](../../docs/architecture.md) — "Everything depends on `poker-engine`.
  `poker-engine` depends on nothing." This ticket turns that sentence into a build failure.

## Files

| File | Action |
| --- | --- |
| `poker-engine/build.gradle.kts` | modify |

## Scope

A Gradle task `checkNoDependencies` that fails if the `implementation`, `api`,
`compileOnly` or `runtimeOnly` configurations resolve to anything. Test-only configurations are
exempt — the engine may depend on a test framework.

Wire it into `check` so it runs with the normal build.

A Gradle task rather than a Kotlin test on purpose: the thing being asserted is a property of the
*build graph*, and a test that greps a build file would pass happily while a dependency arrived
through a convention plugin.

## Out of scope

- Any other module. They are allowed dependencies; only the engine is not.
- CI wiring — `TASK-010106`.

## Tests

None in Kotlin. The Gradle task is the check, and `verify` runs it.

## Acceptance criteria

- [ ] `./gradlew :poker-engine:checkNoDependencies` exits 0 as the module stands today.
- [ ] Temporarily adding `implementation("com.google.guava:guava:33.0.0-jre")` makes it exit
      non-zero. Remove the line again before finishing.
- [ ] `./gradlew :poker-engine:check` runs the task.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): `verify` green, review passed, CI green, status
`done`, `BOARD.md` updated, squash-merged into `develop`. Not done until the PR is merged.
