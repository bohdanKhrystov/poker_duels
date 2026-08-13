---
schema: 2
id: TASK-021202
title: One composition root installs the socket and both HTTP routes, and main calls it
type: task
status: done
parent: STORY-0212
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, wiring]
depends_on: [TASK-021201]
verify:
  - ./gradlew :poker-server:test --tests '*DuelServerRoutesTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*HealthRouteTest'
  - ./gradlew :poker-server:test --tests '*ServerPluginsTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`/ws`, `GET /api/me` and `GET /api/me/duels` are installed by the same function `main()` calls, so
a socket handshake and an HTTP read in one running server share one database.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt` | create |

Read, do not modify: `ServerComponents.kt` (`TASK-021201`), `DuelSocket.kt` (`duelSocket`),
`http/ProfileRoutes.kt` (`profileRoutes`).

## Scope

- One new function in `Application.kt`:

  ```kotlin
  public fun Application.duelServer(components: ServerComponents) {
      module()
      duelSocket(components.socket)
      profileRoutes(components.reads)
  }
  ```

- `main()` becomes: load config, `startDatabase(config)`, `serverComponents(config, pool)`, then
  `embeddedServer(Netty, port = config.port) { duelServer(components) }`.
- **`module()` keeps its signature and its body.** Roughly a dozen merged tests install it
  directly; giving it a parameter would rewrite all of them. `duelServer` calls it, so the plugins
  and `/health` are installed exactly once and in the same order as today.

## Out of scope

- Any periodic sweep — `TASK-021212`, blocked on `DEC-019`. A room reaper installed here would be
  guessing an unanswered decision.
- The stale KDoc in `DuelSocket.kt` and `ProfileRoutes.kt` saying only a test installs them —
  `TASK-021203`.
- Graceful shutdown, connection draining, TLS.

## Tests

`DuelServerRoutesTest`, JUnit 5, package `duels.poker.server`. `@BeforeEach` builds a fresh
migrated database (`PostgresTestSupport.freshDatabase()` then `Migrations.migrate`); each test uses
`testApplication { application { duelServer(serverComponents(config, dataSource)) } }` and a client
with `install(WebSockets)`.

| Test | Proves |
| --- | --- |
| `theHealthRouteStillAnswers` | `GET /health` is `200` with body `OK` — `module()` still runs |
| `theProfileRouteIsInstalled` | `GET /api/me` with no `X-Device-Id` is `401`, not `404`. A route that was never installed answers `404`, which is what makes this falsifiable |
| `theSocketRouteCompletesAHandshake` | a `/ws` session sending `Hello(deviceId = "wired")` receives a `ServerMessage.Welcome` whose `deviceId` is `"wired"` |
| `aHandshakeCreatesTheProfileTheHttpRouteThenReads` | after that same handshake, `GET /api/me` with header `X-Device-Id: wired` is `200` and decodes to a `ProfileResponse` with `coinBalance == 0` — the socket and the HTTP route are backed by one database in one application |

## Acceptance criteria

- [ ] `DuelServerRoutesTest.theHealthRouteStillAnswers` passes
- [ ] `DuelServerRoutesTest.theProfileRouteIsInstalled` passes
- [ ] `DuelServerRoutesTest.theSocketRouteCompletesAHandshake` passes
- [ ] `DuelServerRoutesTest.aHandshakeCreatesTheProfileTheHttpRouteThenReads` passes
- [ ] `HealthRouteTest` and `ServerPluginsTest` pass unchanged — neither observes `duelServer`, and
      `module()`'s signature and body are untouched, so no assertion in either file moves
- [ ] `Application.kt` names no timer, ticker, `launch` or `delay`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
