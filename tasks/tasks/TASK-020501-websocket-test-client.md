---
schema: 2
id: TASK-020501
title: Put the WebSocket test client and coroutines on the poker-server classpath
type: task
status: done
parent: STORY-0205
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [server, websocket, build]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*WebSocketTestClientTest'
  - ./gradlew :poker-server:check
---

## Goal

A `testApplication` test can open a WebSocket against a route and exchange text frames, and
`poker-server` declares the coroutines dependency its connection code is about to use instead of
inheriting it transitively from Ktor.

## Files

| File | Action |
| --- | --- |
| `gradle/libs.versions.toml` | modify |
| `poker-server/build.gradle.kts` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/WebSocketTestClientTest.kt` | create |

Read, do not modify: `poker-server/src/test/kotlin/duels/poker/server/ServerPluginsTest.kt`
(the `testApplication` shape this project uses),
`poker-server/src/main/kotlin/duels/poker/server/Application.kt`.

## Scope

- `gradle/libs.versions.toml`, `[libraries]`, one new entry beside the other Ktor ones:

  ```toml
  ktor-client-websockets = { group = "io.ktor", name = "ktor-client-websockets", version.ref = "ktor" }
  ```

  Do **not** add it to the `ktor-server` bundle: it is a test-only client library, and the bundle
  is what ships.
- `poker-server/build.gradle.kts`, two lines in `dependencies`:
  `implementation(libs.kotlinx.coroutines.core)` and
  `testImplementation(libs.ktor.client.websockets)`. The `ktor` version stays **3.0.3** — it is
  pinned for a reason and no line in this ticket touches `[versions]`.
- The test proves the plumbing, not a product route: it declares a throwaway `/echo` route inside
  the test itself. `/ws` does not exist yet and is not created here.
- The client plugin is installed on a client built with `createClient { install(WebSockets) }` —
  the imported `WebSockets` in this file is `io.ktor.client.plugins.websocket.WebSockets`, not the
  server plugin of the same name.

## Out of scope

- The `/ws` route — `TASK-020507`.
- Any session, identity or protocol type — `TASK-020502` … `TASK-020505`.
- A shared test helper for opening sockets. Each test file opens its own; the shared fixture that
  does exist is `TASK-020506`'s and it holds dependencies, not connections.
- Binding a real port, `embeddedServer` in a test, or a ping/pong configuration.

## Tests

`WebSocketTestClientTest`, JUnit 5, package `duels.poker.server`. Both methods are
`fun … () = testApplication { … }`, and both install the echo route with:

```kotlin
application {
    module()
    routing {
        webSocket("/echo") {
            for (frame in incoming) {
                if (frame is Frame.Text) send(Frame.Text(frame.readText()))
            }
        }
    }
}
```

| Test | Proves |
| --- | --- |
| `theTestClientCanOpenAWebSocket` | a client created with `createClient { install(WebSockets) }` connects to `/echo`, sends `Frame.Text("ping")` and receives `"ping"` back |
| `twoSocketsAreIndependent` | two sequential sessions on the same client each get their own text back — `"one"` then `"two"` — so a test may open more than one connection |

Imports for the test: `io.ktor.client.plugins.websocket.WebSockets`,
`io.ktor.client.plugins.websocket.webSocket`, `io.ktor.server.routing.routing`,
`io.ktor.server.websocket.webSocket`, `io.ktor.server.testing.testApplication`,
`io.ktor.websocket.Frame`, `io.ktor.websocket.readText`,
`org.junit.jupiter.api.Assertions.assertEquals`, `org.junit.jupiter.api.Test`.

## Acceptance criteria

- [ ] `WebSocketTestClientTest.theTestClientCanOpenAWebSocket` passes
- [ ] `WebSocketTestClientTest.twoSocketsAreIndependent` passes
- [ ] `gradle/libs.versions.toml` gains exactly one `[libraries]` entry and no `[versions]` change,
      and `ktor` still reads `3.0.3`
- [ ] `poker-server/build.gradle.kts` gains exactly two dependency lines
- [ ] `HealthRouteTest` and `ServerPluginsTest` pass unchanged — this ticket edits no existing
      Kotlin file
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
