---
schema: 2
id: TASK-020104
title: Ship application.conf and load ServerConfig from it
type: task
status: done
parent: STORY-0201
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, config, foundation]
depends_on: [TASK-020103]
verify:
  - ./gradlew :poker-server:test --tests '*ServerConfigTest'
  - ./gradlew :poker-server:check
---

## Goal

The server ships an `application.conf` holding its defaults, and `ServerConfig.load()` reads it
once from the classpath with the environment still able to override it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/resources/application.conf` | create |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | modify |

Read, do not modify: `poker-server/build.gradle.kts`.

## Scope

- `poker-server/src/main/resources/application.conf`, HOCON, exactly this and nothing else:

  ```hocon
  server {
      port = 8080
  }
  ```

  **It must not contain a `ktor { }` block or `ktor.application.modules`.** Ktor's test host
  loads `application.conf` from the classpath, and a `modules` entry there would make every
  `testApplication` install the application module a second time — a duplicate-route failure in
  `TASK-020105` that looks like a bug in the route.

- `ServerConfig.kt` gains one function in the existing companion, and nothing else changes in the
  file:

  ```kotlin
  public fun load(env: (String) -> String? = { name -> System.getenv(name) }): ServerConfig =
      from(ConfigLoader.load("application.conf"), env)
  ```

  Import `io.ktor.server.config.ConfigLoader`. The path is spelled out deliberately: a missing
  resource then fails loudly at startup instead of silently serving defaults.
- KDoc on `load` saying it is called once, from `main`, and that its result is passed down rather
  than re-read.
- `ServerConfigTest` gains the two test methods below. It is an append: the four methods from
  `TASK-020103` keep their bodies and their assertions exactly as they are.

## Out of scope

- Any second configuration value. `port` is still the only one.
- A per-environment or per-profile config file, `application-test.conf`, or config in
  `src/test/resources`.
- `main`, `embeddedServer` and the Ktor application module — `TASK-020105`.
- Reading config anywhere other than through `ServerConfig`.

## Tests

`ServerConfigTest`, package `duels.poker.server.config` — two methods added to the existing class.

| Test | Proves |
| --- | --- |
| `loadsThePortFromTheShippedApplicationConf` | `ServerConfig.load { null }.port == 8080`, so the resource is found and parsed |
| `theEnvironmentVariableOverridesTheShippedFile` | `ServerConfig.load { "9001" }.port == 9001` |

## Acceptance criteria

- [ ] `ServerConfigTest.loadsThePortFromTheShippedApplicationConf` passes
- [ ] `ServerConfigTest.theEnvironmentVariableOverridesTheShippedFile` passes
- [ ] The four `ServerConfigTest` methods from `TASK-020103` are byte-identical to what merged
      there — this ticket only appends
- [ ] `application.conf` contains no `ktor` key
- [ ] `./gradlew :poker-server:check` exits 0, ktlint and detekt included
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
