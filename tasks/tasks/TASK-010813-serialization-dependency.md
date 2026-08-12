---
schema: 2
id: TASK-010813
title: Take the kotlinx.serialization dependency behind a narrowed guard
type: task
status: ready
parent: STORY-0108
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [build, architecture, serialization]
depends_on: []
verify:
  - ./gradlew :poker-engine:checkNoDependencies
  - ./gradlew -q :poker-engine:dependencies --configuration runtimeClasspath | grep -q kotlinx-serialization-json
  - ./gradlew :poker-engine:check
---

## Goal

`poker-engine` compiles with the kotlinx.serialization plugin and runtime on its classpath, and
`checkNoDependencies` still fails the build on anything else.

## Context

[`ADR-0010`](../../docs/adr/ADR-0010-engine-takes-a-serialization-dependency.md) resolved
`DEC-006` and **is the specification for this ticket**. Read the "On the guard" paragraph in
particular: `checkNoDependencies` is *narrowed*, never deleted. Every other purity clause stands
— no networking, no I/O, no clock, no framework types, no `kotlin.random.Random`.

## Files

| File | Action |
| --- | --- |
| `gradle/libs.versions.toml` | modify |
| `build.gradle.kts` | modify |
| `poker-engine/build.gradle.kts` | modify |

## Scope

- `gradle/libs.versions.toml`: a version `kotlinxSerialization = "1.7.3"` (the release that pairs
  with Kotlin 2.0.21, already pinned as `kotlin` in the same file) and a library

  ```toml
  kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
  ```

- Root `build.gradle.kts`, in the existing `plugins { }` block, beside the Kotlin plugin:

  ```kotlin
  kotlin("plugin.serialization") version libs.versions.kotlin apply false
  ```

  The version is declared once, at the root, for the same reason `kotlin("jvm")` is.

- `poker-engine/build.gradle.kts`:
  - apply `kotlin("plugin.serialization")` in its `plugins { }` block, with no version,
  - add `implementation(libs.kotlinx.serialization.json)` to `dependencies { }`,
  - narrow the guard's allowlist from the single hard-coded `"kotlin-stdlib"` to exactly two
    entries — `kotlin-stdlib` and `kotlinx-serialization` — declared once and named, so the next
    dependency has to argue for itself in a new ADR rather than slipping in behind this one:

    ```kotlin
    // ADR-0010: the allowlist is narrowed, never removed. Adding to it needs a new ADR.
    val allowedDependencies = listOf("kotlin-stdlib", "kotlinx-serialization")
    ```

    and the filter inside `checkNoDependencies` becomes
    `.filter { dep -> allowedDependencies.none { dep.toString().contains(it) } }`.

- No Kotlin source file changes in this ticket. Nothing is annotated yet; the first `@Serializable`
  arrives in `TASK-010814`.

## Out of scope

- Annotating any domain type — `TASK-010814` onwards.
- `poker-ai`, `poker-server` or any other module's build file. Only the engine's independence was
  ever the constraint.
- Removing or relaxing any other clause of the purity rule. `ADR-0010` amends exactly one.

## Tests

None in Kotlin, for the same reason `TASK-010103` had none: the thing being asserted is a
property of the *build graph*, and a test that greps a build file would pass happily while a
dependency arrived through a convention plugin. The `verify` block is the check.

The second `verify` command is the one that proves the dependency actually landed on the engine's
runtime classpath rather than merely being declared in the catalog.

## Acceptance criteria

- [ ] `./gradlew :poker-engine:checkNoDependencies` exits 0
- [ ] `./gradlew -q :poker-engine:dependencies --configuration runtimeClasspath | grep -q kotlinx-serialization-json` exits 0
- [ ] `./gradlew :poker-engine:check` exits 0
- [ ] The guard still bites: temporarily adding `implementation("com.google.guava:guava:33.0.0-jre")`
      to `poker-engine/build.gradle.kts` makes `./gradlew :poker-engine:checkNoDependencies` exit
      non-zero. Remove the line again before finishing — exactly the check `TASK-010103` used.
- [ ] No file under `poker-engine/src/` is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
