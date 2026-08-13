---
schema: 2
id: TASK-020726
title: The socket's dependencies carry the rooms and the connection directory
type: task
status: ready
parent: STORY-0207
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [server, sessions, wiring]
depends_on: [TASK-020725]
verify:
  - ./gradlew :poker-server:test --tests '*SocketFixturesTest'
  - ./gradlew :poker-server:test --tests '*DuelSocket*'
  - ./gradlew :poker-server:check
---

## Goal

A `/ws` connection can reach the `RoomRegistry` and the `ConnectionDirectory`. Today it can reach
neither, which is the whole reason a socket has no way to be in a room.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/session/SocketDependencies.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/session/SocketFixtures.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/session/SocketFixturesTest.kt` | modify |

Every socket test builds its dependencies through `testDeps`, so those three files are the whole
blast radius: `DuelSocketFrameLoopTest`, `DuelSocketHandshakeTest`, `DuelSocketHostileFrameTest`,
`DuelSocketSecondSocketTest` and `DuelSocketSessionTest` must all pass **unchanged**. They are in
`verify:`, not in the budget.

## Scope

- `SocketDependencies` gains two properties, both without defaults, for the reason its KDoc already
  gives: a caller that forgets one must fail to compile rather than silently inherit something.

  ```kotlin
  val rooms: RoomRegistry,
  val connections: ConnectionDirectory,
  ```

- No `HandSeedSource` field. The seed source a duel uses is `rooms.handSeeds` (`TASK-020724`) — one
  source per duel, not two.
- `testDeps` gains matching parameters, each defaulted so that no existing socket test changes:
  - `rooms: RoomRegistry = RoomRegistry(RandomRoomCodeSource(), SystemClock)`,
  - `connections: ConnectionDirectory = ConnectionDirectory()`.
- The defaults must be constructed **per call**, like `sessions` already is, so two calls to
  `testDeps()` never share a registry. `SocketFixturesTest.eachCallGetsItsOwnRegistry` is the
  existing test of exactly that property for `sessions`; extend it rather than write a second one.

## Out of scope

- Any use of either field. `DuelSocket.kt` is deliberately **not** in this ticket: nothing reads
  `rooms` or `connections` yet, and `TASK-020729`, `TASK-020731` and `TASK-020715` each pick one up.
- Installing the route from `Application.module()` — still `STORY-0212`'s.

## Tests

`SocketFixturesTest` — the existing case gains two assertions; two named cases are added.

| Test | Proves |
| --- | --- |
| `eachCallGetsItsOwnRegistry` | `testDeps()` twice yields different `sessions`, different `rooms` and different `connections` instances |
| `theDefaultRoomRegistryStartsEmpty` | `testDeps().rooms.size == 0` |
| `theDefaultConnectionDirectoryStartsEmpty` | `testDeps().connections.size == 0` |

## Acceptance criteria

- [ ] `SocketFixturesTest.eachCallGetsItsOwnRegistry` passes and asserts distinctness for all three
      of `sessions`, `rooms` and `connections`
- [ ] `SocketFixturesTest.theDefaultRoomRegistryStartsEmpty` passes
- [ ] `SocketFixturesTest.theDefaultConnectionDirectoryStartsEmpty` passes
- [ ] `SocketDependencies.kt` declares no default value for any property
- [ ] `DuelSocketFrameLoopTest`, `DuelSocketHandshakeTest`, `DuelSocketHostileFrameTest`,
      `DuelSocketSecondSocketTest` and `DuelSocketSessionTest` pass with all five files unchanged
- [ ] `DuelSocket.kt` does not appear in the diff
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
