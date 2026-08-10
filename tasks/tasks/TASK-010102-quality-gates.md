---
id: TASK-010102
title: Wire ktlint, detekt and the property-testing stack
type: task
status: backlog
parent: STORY-0101
module: poker-engine
estimate: S
labels: [build, quality]
depends_on: [TASK-010101]
---

## Goal

Formatting and static analysis run as part of `./gradlew check`, and kotest is available for
property-based tests.

## Context

- [`CLAUDE.md`](../../CLAUDE.md) — the code style this configuration should enforce mechanically
  so that review time goes on logic instead of layout.

## Scope

- ktlint with the official Kotlin style, failing the build on violation.
- detekt with a configuration checked into the repository, failing the build on violation.
- kotest property testing as a `testImplementation` dependency, alongside JUnit 5 as the runner.
- `./gradlew check` runs formatting, static analysis and tests together.

## Out of scope

- A detekt baseline file. With no production code yet there is nothing to baseline, and
  creating one now would only be a place for future violations to hide.
- Test coverage thresholds. Coverage as a gate rewards the wrong behaviour; the property tests
  in later stories are the real quality signal.
- CI — `TASK-010103`.

## Files

| File | Action |
| --- | --- |
| `build.gradle.kts` | modify |
| `gradle/libs.versions.toml` | modify |
| `config/detekt/detekt.yml` | create |
| `.editorconfig` | create |

## Acceptance criteria

- [ ] `./gradlew check` runs ktlint, detekt and tests.
- [ ] A deliberately misformatted file fails the build; reverting it makes the build pass.
- [ ] A deliberate detekt violation fails the build.
- [ ] A kotest property test compiles and runs under the JUnit 5 platform.
- [ ] No baseline or suppression file is introduced.

## Tests

- One kotest property test over a trivial invariant, present only to prove the stack works.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
