---
schema: 2
id: TASK-020729
title: A live connection's writer is findable by the player behind it
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, sessions, websocket]
depends_on: [TASK-020728]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketWriterDirectoryTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketSecondSocketTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketSessionTest'
  - ./gradlew :poker-server:check
---

## Goal

While a handshaken connection is alive, `deps.connections.writerFor(itsPlayer)` returns that
connection's `ConnectionWriter`; when it closes, it does not. That is what lets one connection's
frame reach the other seat without either coroutine touching the other's socket.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketWriterDirectoryTest.kt` | create |

## Scope

- In `serve`'s `Welcome` branch, register the connection's writer in `deps.connections` under
  `player.id`, in the same place `seats.adopt` registers the session, and `forget` it in the same
  `finally` that calls `deps.sessions.remove` and `seats.forget`.
- Call `forget(player.id, writer)` — with the writer — never a remove by player alone. Under
  `ADR-0018` an older socket for one device closes *after* a newer one adopted the seat, and a
  remove by key would delete the newer connection's writer and leave the surviving player silently
  unable to receive anything. `ConnectionDirectory.forget` already has the two-argument shape for
  this; using it wrongly here is the bug it was built to prevent.
- Order in the `finally` does not matter, but the registration must happen **after** the handshake
  succeeds: a connection that never sent a valid `Hello` has no player and must appear in no
  directory.
- Nothing else in `DuelSocket.kt` changes. No frame is written anywhere by this ticket.

## Out of scope

- Sending a frame to a found writer — `TASK-020730`.
- Rooms and seats — `TASK-020731`.
- Any change to `ConnectionDirectory` itself.

## Tests

`DuelSocketWriterDirectoryTest` — a new file, driving real sockets through `testApplication` the way
`DuelSocketSecondSocketTest` does. The player id is obtained from the same `InMemoryPlayerDirectory`
the test hands to `testDeps`, so the assertion names the player the server actually resolved.

| Test | Proves |
| --- | --- |
| `aliveConnectionIsFindableByItsPlayer` | after a handshake, `connections.writerFor(player)` is non-null and `connections.size == 1` |
| `aconnectionThatNeverHandshakesRegistersNothing` | a socket refused at the handshake leaves `connections.size == 0` |
| `aclosedConnectionLeavesNoWriterBehind` | after the client closes, `connections.writerFor(player)` becomes `null` |
| `theadoptedSocketsCloseDoesNotEvictTheNewOne` | two sockets for one device: after the first is closed by `SEAT_ADOPTED`, `connections.writerFor(player)` is still non-null and `connections.size == 1` |

The last is the one that matters, and it is why this ticket is `sonnet` and not `haiku`: it is the
`ADR-0018` interaction, and it fails silently rather than loudly if `forget` is called by key alone.

## Acceptance criteria

- [ ] `DuelSocketWriterDirectoryTest.aliveConnectionIsFindableByItsPlayer` passes
- [ ] `DuelSocketWriterDirectoryTest.aconnectionThatNeverHandshakesRegistersNothing` passes
- [ ] `DuelSocketWriterDirectoryTest.aclosedConnectionLeavesNoWriterBehind` passes
- [ ] `DuelSocketWriterDirectoryTest.theadoptedSocketsCloseDoesNotEvictTheNewOne` passes
- [ ] `DuelSocketSecondSocketTest` and `DuelSocketSessionTest` pass with both files unchanged
- [ ] `DuelSocket.kt` contains no call to a one-argument writer removal
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
