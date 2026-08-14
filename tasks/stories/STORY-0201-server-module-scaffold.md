---
id: STORY-0201
title: Server module and build scaffold
type: story
status: done
parent: EPIC-02
module: poker-server
labels: [server, build, foundation]
depends_on: []
---

## Goal

`:poker-server` exists in the Gradle build, boots a Ktor application, answers `GET /health`, and
reads every tunable value from one typed configuration object. Nothing in it plays poker yet —
this is the floor the rest of the epic stands on.

## Why

Every later story in this epic ends with "a test starts the application and asserts something".
Until there is an application that starts, none of those acceptance criteria mean anything.

It is also the last cheap moment to get the configuration mechanism right. `ADR-0013` requires
the disconnect grace period to be configuration rather than a literal, and `ADR-0011` adds a
database URL; if those arrive before there is somewhere to put them, they arrive as constants
scattered through the code and are never collected up again.

## Design notes

- New Gradle module `poker-server`, added to `settings.gradle.kts`, with
  `implementation(project(":poker-engine"))`. The dependency runs one way only — the engine never
  learns the server exists, and `:poker-engine:checkNoDependencies` keeps guarding it.
- Ktor with the **Netty** engine. CIO is smaller and equally capable; Netty is chosen because it
  is the most densely documented option, and the reviewer's reading speed is the scarce resource
  here (the same reasoning as `ADR-0003`).
- Ktor plugins the epic will need, wired now so no later story has to invent them:
  `ContentNegotiation` with `ktor-serialization-kotlinx-json`, and `WebSockets` installed but with
  no route yet (`STORY-0205` adds `/ws`).
- **Every version goes in `gradle/libs.versions.toml`** — one place, as `STORY-0101` established.
  A Ktor version literal in a module build file is a review finding.
- Configuration: a `ServerConfig` data class read once at startup from Ktor's `application.conf`,
  with each value overridable by an environment variable. It carries the HTTP port from day one
  and grows a `gracePeriod` (`ADR-0013`) and a database URL (`ADR-0011`) in their own stories.
  Environment override matters because `EPIC-07` will run this in a container with no edited file.
- `GET /health` returns 200 with a trivial body. It is the smoke test, not a readiness probe with
  dependency checks — that belongs to `EPIC-07`.
- Tests use `ktor-server-test-host`'s `testApplication`, in-process, no port binding.
- ktlint and detekt apply automatically through the root `subprojects { }` block, and
  `.github/workflows/build.yml` already runs `./gradlew build`, so a new module is picked up
  without touching CI. Confirm rather than re-add.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| `TASK-020101` | Add the `:poker-server` module and its Ktor dependencies to the build | ready |
| `TASK-020102` | Assert the engine and Ktor are on the server module's classpath | backlog |
| `TASK-020103` | Read every tunable from one typed `ServerConfig` | backlog |
| `TASK-020104` | Ship `application.conf` and load `ServerConfig` from it | backlog |
| `TASK-020105` | Boot Ktor on Netty and answer `GET /health` | backlog |
| `TASK-020106` | Install `ContentNegotiation` and `WebSockets` in the application module | backlog |

## Acceptance criteria

- [ ] `./gradlew :poker-server:test` passes from a clean clone with no local configuration.
- [ ] A `testApplication` test gets 200 from `GET /health`.
- [ ] `ServerConfig` is read once at startup; a test asserts an environment variable overrides the
      file default, and that an absent value falls back to the default rather than throwing.
- [ ] `./gradlew :poker-engine:checkNoDependencies` still passes, and `poker-server` does not
      appear in any engine build file.
- [ ] `./gradlew build` is green, ktlint and detekt included.

## Out of scope

- The WebSocket route and anything that speaks to a client — `STORY-0205`.
- The protocol types — `STORY-0202`.
- The database, the pool and migrations — `STORY-0209`.
- Dockerfile, image publishing, hosting, TLS — `EPIC-07`.
- Logging strategy beyond Ktor's default — nobody has asked for one yet, and a logging framework
  chosen before there is anything to log is a decision made blind.
