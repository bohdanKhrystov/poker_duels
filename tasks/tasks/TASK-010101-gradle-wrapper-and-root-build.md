---
schema: 2
id: TASK-010101
title: Gradle wrapper, settings and version catalog
type: task
status: ready
parent: STORY-0101
module: build
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [build]
depends_on: []
verify:
  - ./gradlew --version
  - ./gradlew projects --quiet
---

## Goal

`./gradlew projects` succeeds from a clean clone, with no modules yet.

## Files

| File | Action |
| --- | --- |
| `settings.gradle.kts` | create |
| `build.gradle.kts` | create |
| `gradle/libs.versions.toml` | create |

The Gradle wrapper is generated, not hand-written: run `gradle wrapper --gradle-version 8.14`
and commit `gradlew`, `gradlew.bat` and `gradle/wrapper/`. Those do not count against
`files_touched`.

## Scope

- `settings.gradle.kts` naming the build `poker-duels`, with no modules included yet.
- An empty-but-valid root `build.gradle.kts`.
- `gradle/libs.versions.toml` declaring the Kotlin and JUnit versions. Every version lives here
  from the first commit — no version literal in a build file, ever.
- Pin the Kotlin JVM toolchain explicitly so the build does not depend on whichever JDK is
  installed.

## Out of scope

- The `poker-engine` module — `TASK-010102`.
- ktlint and detekt — `TASK-010104`.
- CI — `TASK-010106`.
- Any Kotlin source file.

## Tests

None. This ticket's gate is that the build evaluates at all, which the `verify` commands check
directly.

## Acceptance criteria

- [ ] `./gradlew --version` exits 0 and reports the pinned Gradle version.
- [ ] `./gradlew projects --quiet` exits 0.
- [ ] `gradle/libs.versions.toml` exists and declares the Kotlin version.
- [ ] No version string appears in `build.gradle.kts` or `settings.gradle.kts`.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): `verify` green, review passed, CI green, status
`done`, `BOARD.md` updated, squash-merged into `develop`. Not done until the PR is merged.
