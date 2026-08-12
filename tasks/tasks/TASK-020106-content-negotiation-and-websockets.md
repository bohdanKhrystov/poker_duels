---
schema: 2
id: TASK-020106
title: Install ContentNegotiation and WebSockets in the application module
type: task
status: done
parent: STORY-0201
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, http, foundation]
depends_on: [TASK-020105]
verify:
  - ./gradlew :poker-server:test --tests '*ServerPluginsTest'
  - ./gradlew build
---

## Goal

The application module installs the two Ktor plugins the rest of the epic depends on —
`ContentNegotiation` with kotlinx JSON, and `WebSockets` — so no later story has to invent them.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/ServerPluginsTest.kt` | create |

Read, do not modify: `poker-server/build.gradle.kts` (both plugins are already declared there by
`TASK-020101`).

## Scope

- `Application.module()` gains two installs, above the existing `routing { }` block:

  ```kotlin
  install(ContentNegotiation) { json() }
  install(WebSockets)
  ```

- Imports added: `io.ktor.serialization.kotlinx.json.json`,
  `io.ktor.server.application.install`,
  `io.ktor.server.plugins.contentnegotiation.ContentNegotiation`,
  `io.ktor.server.websocket.WebSockets`.
- Both plugins take their default configuration. No ping period, no timeout, no custom `Json`
  instance — none of those are tunables anyone has asked for, and inventing them would put values
  in `ServerConfig` that no story needs.
- `main`, the `/health` route and `ServerConfig` are unchanged.
- `HealthRouteTest` is **not** in this ticket's budget and must not be edited. Installing these
  plugins does not change what it observes: `call.respondText("OK")` already produces an
  `OutgoingContent`, which `ContentNegotiation` does not transform, so the status, body and
  content type of `/health` are all identical.

## Out of scope

- A `/ws` route, a socket handler, a session — `STORY-0205`.
- Any `@Serializable` type, and the `kotlin("plugin.serialization")` plugin on this module —
  `STORY-0202` adds both with the first protocol type.
- A JSON body for `/health`.
- `StatusPages`, `CallLogging`, `CORS`, or any plugin not named above.

## Tests

`ServerPluginsTest`, JUnit 5, package `duels.poker.server`. The `application { }` block runs when
the test application starts, so capture the lookup into a local and assert after
`startApplication()`:

```kotlin
@Test
fun installsContentNegotiation() = testApplication {
    var plugin: Any? = null
    application {
        module()
        plugin = pluginOrNull(ContentNegotiation)
    }
    startApplication()
    assertNotNull(plugin)
}
```

| Test | Proves |
| --- | --- |
| `installsContentNegotiation` | `pluginOrNull(ContentNegotiation)` is not null once the application has started |
| `installsWebSockets` | `pluginOrNull(WebSockets)` is not null once the application has started |

Imports for the test: `io.ktor.server.application.pluginOrNull`,
`io.ktor.server.plugins.contentnegotiation.ContentNegotiation`,
`io.ktor.server.testing.testApplication`, `io.ktor.server.websocket.WebSockets`,
`org.junit.jupiter.api.Assertions.assertNotNull`, `org.junit.jupiter.api.Test`.

## Acceptance criteria

- [ ] `ServerPluginsTest.installsContentNegotiation` passes
- [ ] `ServerPluginsTest.installsWebSockets` passes
- [ ] `HealthRouteTest` is not modified and all three of its methods still pass
- [ ] `./gradlew build` exits 0 — every module, ktlint, detekt and
      `:poker-engine:checkNoDependencies` included
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
