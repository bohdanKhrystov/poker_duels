---
schema: 2
id: TASK-020508
title: Resolve the profile on Welcome and drop the session on every close path
type: task
status: backlog
parent: STORY-0205
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, websocket, session, identity]
depends_on: [TASK-020507]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketSessionTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketHandshakeTest'
  - ./gradlew :poker-server:check
---

## Goal

A welcomed connection holds exactly one registered `Session` for the whole time it is open, and
the registry is empty again whether the client closed cleanly, vanished, or was refused.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketSessionTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/session/SessionRegistry.kt`,
`poker-server/src/main/kotlin/duels/poker/server/session/PlayerDirectory.kt`,
`poker-server/src/test/kotlin/duels/poker/server/session/SocketFixtures.kt`,
`poker-server/src/test/kotlin/duels/poker/server/DuelSocketHandshakeTest.kt` (the client shape to
copy — it is not edited by this ticket).

## Scope

- Only the `Welcome` branch of `serve` changes, to this shape:

  ```kotlin
  is ServerMessage.Welcome -> {
      val player = deps.directory.resolve(deviceId)
      val session = Session(SessionRegistry.newSessionId(), player)
      deps.sessions.register(session)
      try {
          writer.send(ProtocolCodec.encode(reply))
          incoming.consumeEach { }
      } finally {
          deps.sessions.remove(session.id)
      }
  }
  ```

- Registration happens **before** the `Welcome` reaches the client. A client that is told it is
  welcome and then acts must find its session already there.
- Removal is in a `finally` and does not suspend, so it also runs when the connection's coroutine
  is cancelled — which is what an abrupt disconnect looks like from the inside.
- A refused handshake registers nothing: `refuseHandshake` returns before this branch is reached,
  and no session is created for a client the server never welcomed.
- Nothing else in `DuelSocket.kt` moves. The writer, the pump, the refusal paths and the close
  reasons stay exactly as `TASK-020507` left them.

## Out of scope

- What happens when the **same device id** opens a second socket while the first is live. That is
  `DEC-011`, implemented by `TASK-020511`; this ticket registers each connection's own session and
  asserts nothing about a second one.
- Keeping a session alive after the socket dies — `ADR-0013`, `STORY-0208`. Here, close means gone.
- Rooms, seats, duels, or anything the session might later be joined to — `STORY-0206`,
  `STORY-0207`.
- Any change to `DuelSocketHandshakeTest`: nothing it observes changes, because the `Welcome` it
  asserts and the close reasons it asserts are untouched.

## Tests

`DuelSocketSessionTest`, JUnit 5, package `duels.poker.server`, same `testApplication` + client
`WebSockets` shape as `DuelSocketHandshakeTest`. Each test builds its own registry and directory so
it can inspect them:
`val directory = InMemoryPlayerDirectory(); val sessions = SessionRegistry(); val deps = testDeps(directory = directory, sessions = sessions)`.
Removal happens after the client's side is gone, so tests wait with a private helper
`suspend fun awaitSize(sessions: SessionRegistry, expected: Int) = withTimeout(5.seconds) { while (sessions.size != expected) delay(10) }`.

| Test | Proves |
| --- | --- |
| `aWelcomedClientHoldsExactlyOneSession` | once the `Welcome` is received, `sessions.size == 1` with no waiting, since registration precedes the reply |
| `theSessionCarriesTheResolvedPlayer` | the single session's `player.deviceId` is the id in the `Welcome`, and `directory.profileCount == 1` |
| `reconnectingWithTheSameDeviceIdReusesTheProfile` | a second connection sending `Hello(deviceId = <the id from the first Welcome>)` leaves `directory.profileCount == 1` and gets the same id back |
| `aCleanCloseRemovesExactlyOneSession` | two connections with different device ids give `size == 2`; closing one leaves `size == 1` and the survivor is the other player's session |
| `anAbruptCloseRemovesTheSession` | after `session.cancel()` on an open connection, `awaitSize(sessions, 0)` returns |
| `theRegistryEntryIsRemovedOnlyOnce` | the session id captured while the socket was open returns `null` from `sessions.remove(id)` after the socket closed |
| `aRefusedHandshakeRegistersNoSession` | a connection whose first frame is `"{"` ends with `sessions.size == 0` and `directory.profileCount == 0` |

## Acceptance criteria

- [ ] `DuelSocketSessionTest.aWelcomedClientHoldsExactlyOneSession` passes
- [ ] `DuelSocketSessionTest.theSessionCarriesTheResolvedPlayer` passes
- [ ] `DuelSocketSessionTest.reconnectingWithTheSameDeviceIdReusesTheProfile` passes
- [ ] `DuelSocketSessionTest.aCleanCloseRemovesExactlyOneSession` passes
- [ ] `DuelSocketSessionTest.anAbruptCloseRemovesTheSession` passes
- [ ] `DuelSocketSessionTest.theRegistryEntryIsRemovedOnlyOnce` passes
- [ ] `DuelSocketSessionTest.aRefusedHandshakeRegistersNoSession` passes
- [ ] `DuelSocketHandshakeTest` passes with no edit to that file
- [ ] `deps.sessions.remove` appears exactly once in `DuelSocket.kt`, inside a `finally`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
