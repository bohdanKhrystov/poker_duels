---
schema: 2
id: TASK-020101
title: Add the :poker-server module and its Ktor dependencies to the build
type: task
status: done
parent: STORY-0201
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [server, build, foundation]
depends_on: []
verify:
  - ./gradlew :poker-server:check
  - ./gradlew :poker-engine:checkNoDependencies
---

## Goal

`:poker-server` exists in the Gradle build with every Ktor dependency the epic needs, declared
through the version catalog, and the engine's no-dependency guard still passes.

## Files

| File | Action |
| --- | --- |
| `gradle/libs.versions.toml` | modify |
| `settings.gradle.kts` | modify |
| `poker-server/build.gradle.kts` | create |

Read, do not modify: `poker-ai/build.gradle.kts` (the module shape to copy),
`poker-engine/build.gradle.kts` (the guard that must keep passing).

## Scope

- `gradle/libs.versions.toml` gains **exactly one** new version and the Ktor libraries and bundle
  below. Nothing else in the file changes.

  ```toml
  # [versions]
  ktor = "3.0.3"

  # [libraries]
  ktor-server-core = { group = "io.ktor", name = "ktor-server-core", version.ref = "ktor" }
  ktor-server-netty = { group = "io.ktor", name = "ktor-server-netty", version.ref = "ktor" }
  ktor-server-content-negotiation = { group = "io.ktor", name = "ktor-server-content-negotiation", version.ref = "ktor" }
  ktor-server-websockets = { group = "io.ktor", name = "ktor-server-websockets", version.ref = "ktor" }
  ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
  ktor-server-test-host = { group = "io.ktor", name = "ktor-server-test-host", version.ref = "ktor" }

  # [bundles]
  ktor-server = ["ktor-server-core", "ktor-server-netty", "ktor-server-content-negotiation", "ktor-server-websockets", "ktor-serialization-kotlinx-json"]
  ```

  **`3.0.3` is not a free choice.** Ktor 3.0.3 is the last release built with Kotlin 2.0.21, the
  version this project pins; 3.1.x and later are built with Kotlin 2.1.x and their metadata is
  rejected by the 2.0.21 compiler. Ktor 3.0.3 also resolves kotlinx-serialization-json 1.7.3,
  which is exactly what the catalog already pins. Do not bump it in this ticket.

- `settings.gradle.kts` gains `include(":poker-server")` on the line after `include(":poker-ai")`.
  Nothing else in that file changes.
- `poker-server/build.gradle.kts`, modelled on `poker-ai/build.gradle.kts`:

  ```kotlin
  plugins {
      kotlin("jvm")
  }

  dependencies {
      implementation(project(":poker-engine"))
      implementation(libs.bundles.ktor.server)
      testImplementation(libs.bundles.junit)
      testImplementation(libs.ktor.server.test.host)
  }

  tasks.withType<Test>().configureEach {
      useJUnitPlatform()
  }
  ```

- The dependency runs one way only: `poker-server` depends on `poker-engine`, and no file under
  `poker-engine/` is touched. `checkNoDependencies` and its allowlist are not edited.
- No Kotlin sources yet — the module has an empty source tree at the end of this ticket, exactly
  as `:poker-ai` did at `TASK-010808`.

## Out of scope

- Any `src/` file at all — `TASK-020102` adds the first one.
- `kotlin("plugin.serialization")` on this module: nothing here is `@Serializable` yet, and
  `STORY-0202` applies it with the first protocol type.
- The Gradle `application` plugin, `mainClass`, `installDist`, a distribution or a Dockerfile —
  `EPIC-07`.
- A logging dependency. Ktor resolves SLF4J with no provider and logs to a no-op logger; the
  story leaves logging deliberately unchosen.
- `.github/workflows/build.yml`. Verified already correct: `./gradlew check` at the root fans out
  to every subproject's `check` (confirmed with `./gradlew check --dry-run`), so a new module is
  picked up with no CI edit.

## Tests

None. This ticket adds no source, so it adds no test; its gate is that the module configures and
that the engine guard is untouched. `TASK-020102` is the first compile of this module and is the
ticket that proves the Ktor coordinates actually resolve.

## Acceptance criteria

- [ ] `./gradlew :poker-server:check` exits 0 — the module configures, and every `libs.` accessor
      used in `poker-server/build.gradle.kts` resolves (a mistyped catalog alias fails this at
      configuration time)
- [ ] `./gradlew :poker-engine:checkNoDependencies` exits 0
- [ ] `git diff --stat` on the merge shows exactly the three files in the Files table
- [ ] `gradle/libs.versions.toml` contains `ktor = "3.0.3"` and no other new version
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
