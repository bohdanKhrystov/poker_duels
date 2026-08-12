---
schema: 2
id: TASK-020901
title: Put the database dependencies in the version catalog and the server build
type: task
status: done
parent: STORY-0209
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, persistence, build]
depends_on: []
verify:
  - ./gradlew :poker-server:check -PrequireDocker=true
  - ./gradlew :poker-engine:checkNoDependencies
---

## Goal

The PostgreSQL driver, HikariCP, Flyway and Testcontainers are declared once in the version
catalog and wired into `:poker-server` only, and `:poker-server:test` accepts
`-PrequireDocker=true`.

## Files

| File | Action |
| --- | --- |
| `gradle/libs.versions.toml` | modify |
| `poker-server/build.gradle.kts` | modify |

Read, do not modify: `poker-engine/build.gradle.kts` (the `checkNoDependencies` allowlist this
ticket must not disturb).

## Scope

- Add to `[versions]`, exactly these keys and values:

  ```toml
  postgresql = "42.7.4"
  hikari = "5.1.0"
  flyway = "10.20.1"
  testcontainers = "1.20.4"
  ```

- Add to `[libraries]`:

  ```toml
  postgresql = { group = "org.postgresql", name = "postgresql", version.ref = "postgresql" }
  hikaricp = { group = "com.zaxxer", name = "HikariCP", version.ref = "hikari" }
  flyway-core = { group = "org.flywaydb", name = "flyway-core", version.ref = "flyway" }
  flyway-postgresql = { group = "org.flywaydb", name = "flyway-database-postgresql", version.ref = "flyway" }
  testcontainers-postgresql = { group = "org.testcontainers", name = "postgresql", version.ref = "testcontainers" }
  ```

  `flyway-database-postgresql` is not optional: Flyway 10 moved database support out of
  `flyway-core`, and without it Flyway fails at runtime with *"No database found to handle
  jdbc:postgresql"*.

- In `poker-server/build.gradle.kts` add `implementation` for `libs.postgresql`, `libs.hikaricp`,
  `libs.flyway.core`, `libs.flyway.postgresql`, and `testImplementation` for
  `libs.testcontainers.postgresql`.
- In the same file, extend the existing `tasks.withType<Test>` block so the Gradle property
  reaches the test JVM as a system property, keeping `useJUnitPlatform()`:

  ```kotlin
  tasks.withType<Test>().configureEach {
      useJUnitPlatform()
      // -PrequireDocker=true turns a missing Docker daemon from a skipped test into a
      // failing build. TASK-020903 reads it; CI passes it.
      systemProperty("poker.requireDocker", providers.gradleProperty("requireDocker").getOrElse("false"))
  }
  ```

- If a coordinate does not resolve, keep the artifact and move to the nearest published version of
  the same major line, and say which in the PR description. The pin matters; the last digit does
  not.

## Out of scope

- Any Kotlin source. Nothing uses these libraries yet — `TASK-020903` onward do.
- A logging backend (`logback`). Testcontainers and Flyway run without an SLF4J provider; adding
  one is not ticketed.
- `org.testcontainers:junit-jupiter`. The harness in `TASK-020903` uses the singleton-container
  pattern and does not need the annotations.
- Touching `poker-engine/build.gradle.kts` or its allowlist. `ADR-0011` is explicit: the engine
  gains no database dependency, ever.
- The CI workflow — `TASK-020903` passes the flag once there is something to gate.

## Tests

No new test class. The gate is dependency resolution: `:poker-server:check` resolves both the
compile and the test runtime classpath, so a typo'd coordinate or version fails the command.

## Acceptance criteria

- [ ] `./gradlew :poker-server:check -PrequireDocker=true` exits 0, which resolves every newly
      declared coordinate
- [ ] `./gradlew :poker-engine:checkNoDependencies` exits 0 — no database artifact is declared on
      `poker-engine`
- [ ] `poker-server/build.gradle.kts` declares Testcontainers under `testImplementation`, not
      `implementation`
- [ ] The `Test` task sets the system property `poker.requireDocker`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
