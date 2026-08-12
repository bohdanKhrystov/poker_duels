---
schema: 2
id: TASK-020506
title: Bundle the socket's collaborators into SocketDependencies with a test fixture
type: task
status: done
parent: STORY-0205
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [server, session, test-harness]
depends_on: [TASK-020502, TASK-020503, TASK-020504]
verify:
  - ./gradlew :poker-server:test --tests '*SocketFixturesTest'
  - ./gradlew :poker-server:check
---

## Goal

The `/ws` route takes one parameter — a `SocketDependencies` — and every socket test builds one in
a line, so the four route tickets that follow never change a signature or a call site.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/session/SocketDependencies.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/session/SocketFixtures.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/session/SocketFixturesTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/session/PlayerDirectory.kt`,
`poker-server/src/main/kotlin/duels/poker/server/session/DeviceIdSource.kt`,
`poker-server/src/main/kotlin/duels/poker/server/session/SessionRegistry.kt`,
`poker-server/src/test/kotlin/duels/poker/server/session/InMemoryPlayerDirectory.kt`.

## Scope

- Main source, package `duels.poker.server.session`, KDoc saying why it exists — the route takes
  one parameter so that a story which adds a collaborator changes this type and not every call
  site:

  ```kotlin
  public data class SocketDependencies(
      val directory: PlayerDirectory,
      val deviceIds: DeviceIdSource,
      val sessions: SessionRegistry,
  )
  ```

- Test source, `SocketFixtures.kt`, two `internal` helpers and nothing else:

  ```kotlin
  internal fun testDeps(
      directory: PlayerDirectory = InMemoryPlayerDirectory(),
      deviceIds: DeviceIdSource = RandomDeviceIdSource(),
      sessions: SessionRegistry = SessionRegistry(),
  ): SocketDependencies = SocketDependencies(directory, deviceIds, sessions)

  internal fun fixedDeviceIds(vararg ids: String): DeviceIdSource {
      val queue = ArrayDeque(ids.toList())
      return DeviceIdSource {
          DeviceId(queue.removeFirstOrNull() ?: error("fixedDeviceIds ran out of ids"))
      }
  }
  ```

  `fixedDeviceIds` is what lets a handshake test assert the exact id in a `Welcome`; the throw on
  exhaustion is what makes "the server minted an id it should not have" a loud failure instead of
  a silent one.
- Every call must be able to supply its own registry, so `testDeps()` returns a fresh
  `SessionRegistry` and a fresh `InMemoryPlayerDirectory` each time. Nothing in this file is a
  singleton, an object, or a `companion`.

## Out of scope

- Anything that opens a socket. These helpers hold dependencies; each test file opens its own
  connection.
- A `ServerConfig` field on `SocketDependencies`. Nothing in this story reads config inside the
  socket, and `TASK-020213` puts the frame limits inside `ProtocolCodec` with defaults.
- Wiring `SocketDependencies` into `main()`. There is no shipping `PlayerDirectory` until
  `STORY-0210`, which is exactly why the port is declared at its consumer.

## Tests

`SocketFixturesTest`, JUnit 5, package `duels.poker.server.session`.

| Test | Proves |
| --- | --- |
| `eachCallGetsItsOwnRegistry` | registering a session through `testDeps().sessions` leaves a second `testDeps().sessions.size == 0` |
| `fixedDeviceIdsIssuesInOrder` | `fixedDeviceIds("a", "b")` returns `DeviceId("a")` then `DeviceId("b")` |
| `fixedDeviceIdsFailsWhenExhausted` | a third `newDeviceId()` on `fixedDeviceIds("a", "b")` throws `IllegalStateException` |

## Acceptance criteria

- [ ] `SocketFixturesTest.eachCallGetsItsOwnRegistry` passes
- [ ] `SocketFixturesTest.fixedDeviceIdsIssuesInOrder` passes
- [ ] `SocketFixturesTest.fixedDeviceIdsFailsWhenExhausted` passes
- [ ] `SocketDependencies` has exactly three properties and no default values
- [ ] `SocketFixtures.kt` declares only `testDeps` and `fixedDeviceIds`, both `internal`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
