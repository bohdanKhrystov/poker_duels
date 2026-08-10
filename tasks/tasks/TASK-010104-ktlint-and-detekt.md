---
schema: 2
id: TASK-010104
title: Wire ktlint and detekt into check
type: task
status: ready
parent: STORY-0101
module: build
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [build, quality]
depends_on: [TASK-010102]
verify:
  - ./gradlew ktlintCheck
  - ./gradlew detekt
---

## Goal

Formatting and static analysis fail the build, so review time goes on logic rather than layout.

## Files

| File | Action |
| --- | --- |
| `build.gradle.kts` | modify |
| `gradle/libs.versions.toml` | modify |
| `config/detekt/detekt.yml` | create |

`.editorconfig` may also be added; it does not count against `files_touched`.

## Scope

- ktlint with official Kotlin style, applied to all modules, failing on violation.
- detekt with the configuration checked in at `config/detekt/detekt.yml`, failing on violation.
- Both wired into `check`.

## Out of scope

- A detekt baseline file. With almost no production code there is nothing to baseline, and one
  created now would only be a place for future violations to hide.
- Coverage thresholds. Coverage as a gate rewards the wrong behaviour.
- kotest — `TASK-010105`.

## Tests

None. The `verify` commands are the check.

## Acceptance criteria

- [ ] `./gradlew ktlintCheck` exits 0 on the tree as committed.
- [ ] `./gradlew detekt` exits 0 on the tree as committed.
- [ ] Both run as part of `./gradlew check`.
- [ ] No baseline or suppression file is introduced.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): `verify` green, review passed, CI green, status
`done`, `BOARD.md` updated, squash-merged into `develop`. Not done until the PR is merged.
