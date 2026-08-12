---
schema: 2
id: TASK-020105
title: Boot Ktor on Netty and answer GET /health
type: task
status: backlog
parent: STORY-0201
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, http, foundation]
depends_on: [TASK-020104]
verify:
  - ./gradlew :poker-server:test --tests '*HealthRouteTest'
  - ./gradlew :poker-server:check
---

## Goal

The server has an entry point that starts Ktor on Netty at the configured port, and an
application module whose `GET /health` answers 200.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/HealthRouteTest.kt` | create |

Read, do not modify: `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt`,
`poker-server/src/main/resources/application.conf`.

## Scope

- `Application.kt`, package `duels.poker.server`, holding exactly two top-level functions:

  ```kotlin
  public fun main() {
      val config = ServerConfig.load()
      embeddedServer(Netty, port = config.port) { module() }.start(wait = true)
  }

  public fun Application.module() {
      routing {
          get("/health") {
              call.respondText("OK")
          }
      }
  }
  ```

- Config is read **once**, in `main`, and the port reaches Netty from `ServerConfig`. No other
  file reads the environment.
- Imports: `duels.poker.server.config.ServerConfig`, `io.ktor.server.application.Application`,
  `io.ktor.server.engine.embeddedServer`, `io.ktor.server.netty.Netty`,
  `io.ktor.server.response.respondText`, `io.ktor.server.routing.get`,
  `io.ktor.server.routing.routing`.
- `module()` takes no parameters. Nothing inside the application needs a tunable yet, and an
  unused `config` parameter is a detekt `UnusedParameter` failure. The first story that needs a
  value inside the application adds the parameter then.
- KDoc on both functions.
- The test uses `testApplication` from `ktor-server-test-host` and installs the module with
  `application { module() }`. In-process, no port binding, no `embeddedServer` in a test.

## Out of scope

- `ContentNegotiation` and `WebSockets` — `TASK-020106`, deliberately a separate diff.
- A `/ws` route or anything that speaks to a client — `STORY-0205`.
- A readiness probe that checks dependencies, a JSON body, or a version field in the response.
  The body is the string `OK`; `EPIC-07` owns real probes.
- The Gradle `application` plugin, `mainClass` and a runnable distribution — `EPIC-07`.
- Any test that starts Netty or binds a socket.

## Tests

`HealthRouteTest`, JUnit 5, package `duels.poker.server`. Each method is
`fun … () = testApplication { application { module() }; … }`.

| Test | Proves |
| --- | --- |
| `healthAnswersOk` | `client.get("/health").status == HttpStatusCode.OK` |
| `healthBodyIsOk` | `client.get("/health").bodyAsText() == "OK"` |
| `anUnknownPathIsNotFound` | `client.get("/nope").status == HttpStatusCode.NotFound`, so 200 comes from the route and not from a catch-all |

Imports for the test: `io.ktor.client.request.get`, `io.ktor.client.statement.bodyAsText`,
`io.ktor.http.HttpStatusCode`, `io.ktor.server.testing.testApplication`,
`org.junit.jupiter.api.Assertions.assertEquals`, `org.junit.jupiter.api.Test`.

## Acceptance criteria

- [ ] `HealthRouteTest.healthAnswersOk` passes
- [ ] `HealthRouteTest.healthBodyIsOk` passes
- [ ] `HealthRouteTest.anUnknownPathIsNotFound` passes
- [ ] `Application.kt` references `Netty` and `ServerConfig.load()` exactly once each
- [ ] `./gradlew :poker-server:check` exits 0, ktlint and detekt included
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
