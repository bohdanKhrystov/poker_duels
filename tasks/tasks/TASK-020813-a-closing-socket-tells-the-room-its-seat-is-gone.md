---
schema: 2
id: TASK-020813
title: A closing socket tells the room its seat is gone, unless a newer socket took it
type: task
status: backlog
parent: STORY-0208
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, websocket, resilience]
depends_on: [TASK-020812, TASK-020734]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketDisconnectTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketSecondSocketTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketWriterDirectoryTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketFrameLoopTest'
  - grep -c '!!' poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt | grep -qx 0
---

## Goal

When a connection closes, the room it was seated in starts that seat's grace window — and when the
close was an `ADR-0018` adoption by that same player's newer socket, it does not.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketDisconnectTest.kt` | create |

## Scope

- One addition to `serve`'s existing `finally`, after the three cleanups already there:

  ```kotlin
  } finally {
      deps.sessions.remove(session.id)
      seats.forget(session.id)
      if (deps.connections.forget(player.id, writer)) {
          room.code?.let { code ->
              withContext(NonCancellable) { deps.rooms.disconnect(code, player.id) }
          }
      }
  }
  ```

- **The `if` is the whole ticket.** `ConnectionDirectory.forget(player, writer)` returns `true`
  only when this writer was still the registered one (`TASK-020723`). Under `ADR-0018` a newer
  socket for the same device has already registered its own writer and adopted the seat, so the
  adopted-away socket's cleanup gets `false` — and must then report nothing, or one player's
  reconnect would immediately pause the duel it just resumed. Reporting unconditionally would be a
  bug that only shows up under real network churn, which is exactly why `forget` takes the writer.
- **`withContext(NonCancellable)` is not optional.** Every other statement in this `finally` is
  non-suspending by deliberate design — see `ConnectionDirectory`'s and `SessionRegistry`'s KDoc —
  and `RoomRegistry.disconnect` cannot be, because it takes the room's mutex. A suspending call in
  a `finally` reached by cancellation throws `CancellationException` before it does anything, so
  the pause would silently never happen on the most common close path there is. Wrap this call and
  only this call, and say why in a comment.
- Nothing else in `DuelSocket.kt` moves. No `!!` is introduced (`TASK-020734`), no frame is
  constructed, and the socket still never asks whether a room is paused (`TASK-020807`).

## Out of scope

- Resuming a returning socket — `TASK-020814`.
- Expiring the window — `TASK-020812` owns the sweep, and nothing schedules it (`DEC-019`).
- Any change to how sockets are adopted or closed.

## Tests

`DuelSocketDisconnectTest` — a new file, built like `DuelSocketRoomTest`: `testApplication`,
`module()`, `duelSocket(testDeps(rooms = rooms))` with a `RoomRegistry` the test holds, over a
`MutableClock` and a `RoomTimeouts` naming `disconnectGraceMillis`. Host creates a room, guest
joins, the duel starts. Wait for state changes the way `DuelSocketSecondSocketTest.awaitSize` does
— `withTimeout(5.seconds) { while (...) delay(10) }` — never with a fixed sleep.

| Test | Proves |
| --- | --- |
| `closingASocketStartsThatSeatsWindow` | after the guest's session closes, `rooms.get(code)!!.gracePeriods.keys` becomes `setOf(1)` and the room `isPaused` |
| `theWindowIsTheConfiguredOne` | that entry's deadline is `MutableClock`'s current instant plus the `disconnectGraceMillis` the test configured, proving the value travelled from the registry's timeouts rather than a literal |
| `theOpponentCannotActWhileTheDuelIsPaused` | once paused, an `Act` from the host is answered on the host's own socket with `ServerMessage.Failure(ProtocolError.DUEL_PAUSED)`, and the room's `runner` is the same value before and after |
| `aSecondSocketForTheSameDeviceDoesNotPauseTheDuel` | the guest opens a second socket with the same device id; after awaiting the **first** socket's close reason (`SEAT_ADOPTED`, as `DuelSocketSecondSocketTest` does), `rooms.get(code)!!.isPaused` is `false` and `gracePeriods` is empty. Awaiting the close first is what makes this test falsifiable rather than merely early |
| `aSocketThatEnteredNoRoomPausesNothing` | a third client handshakes, never joins, and closes; the duel room is still unpaused |

## Acceptance criteria

- [ ] All five `DuelSocketDisconnectTest` cases named above pass
- [ ] `DuelSocketSecondSocketTest`, `DuelSocketWriterDirectoryTest` and `DuelSocketFrameLoopTest`
      pass with those files unchanged
- [ ] `DuelSocket.kt` contains no `!!`
- [ ] `DuelSocket.kt` calls `deps.rooms.disconnect` exactly once, inside a `NonCancellable` block,
      guarded by the result of `deps.connections.forget`
- [ ] No test in `DuelSocketDisconnectTest` calls `Thread.sleep`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
