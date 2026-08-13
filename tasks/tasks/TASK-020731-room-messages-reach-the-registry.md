---
schema: 2
id: TASK-020731
title: CreateRoom and JoinRoom reach the registry, and the opening hand reaches both seats
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, rooms, websocket]
depends_on: [TASK-020730]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketRoomTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketFrameLoopTest'
  - ./gradlew :poker-server:test --tests '*RunnerLeakTest'
  - ./gradlew :poker-server:check
---

## Goal

A socket can open a room and be told its code, a second socket can join it by that code, and the
moment the second seat is taken both sockets receive the opening hand — each seeing only what the
engine says it may.

After this, a session **is** associated with a room. `TASK-020715` can then route an `Act`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketRoomTest.kt` | create |

## Scope

- Give the connection one piece of mutable state — the `RoomCode` it is currently in, `null` until
  it enters one — held in a small private class local to `DuelSocket.kt` and threaded from `serve`
  through `serveUntilEvictedOrClosed` into the reply path. It is confined to one coroutine, so it
  needs no lock; say so in a comment, because a mutable field in this file otherwise reads as a
  race.
- **The code is the only thing remembered. The seat is not.** Derive the seat on every use from
  `deps.rooms.get(code)?.seatOf(session.player.id)`. A cached seat can go stale against a room the
  registry has since reaped or abandoned; a derived one cannot, and it can never be a claim the
  client made.
- `CreateRoom` → `deps.rooms.create(session.player.id)` with the default format, remember the code,
  answer `ServerMessage.RoomJoined(code.value, 0)` — the host is always seat 0 by `Room.open`.
- `JoinRoom(code)` →
  - `RoomCode.parse(code)` returns `null` → `Failure(UNKNOWN_ROOM)`. A malformed code and an unknown
    one must look identical: telling them apart hands an attacker a code-shape oracle.
  - `JoinResult.Seated` → remember the code, answer `RoomJoined(code, room.seatOf(player)!!)` to
    this socket, then `deliver(result.outbound, result.room, deps.connections)` so the opening hand
    reaches **both** seats. The host's `RoomJoined` is not resent — it already has one.
  - `RoomRefusal.ALREADY_SEATED` → remember the code and answer `RoomJoined(code, seat)`. This is the
    right answer, not a shortcut: the player *is* in that room, and a socket that opened a room out
    of band, or reconnected, is entitled to be told which seat it holds. It is also what lets a test
    pre-create a room with a chosen `DuelFormat` and then have the host enter it over the wire.
  - `RoomRefusal.UNKNOWN_ROOM` → `Failure(UNKNOWN_ROOM)`; `RoomRefusal.ROOM_FULL` →
    `Failure(ROOM_FULL)`. Map the enum with an exhaustive `when` and no `else`.
- Remove the provisional `NOT_IN_DUEL` answers `TASK-020728` left for these two messages. `Act` keeps
  answering `NOT_IN_DUEL`; `TASK-020715` owns changing that.
- No card filtering, no `PlayerView`, no `holeCards` in `DuelSocket.kt`. Frames reach writers only
  through `deliver` (`TASK-020730`); no frame is written to a socket directly.

## Out of scope

- Routing an `Act` — `TASK-020715`.
- Leaving, abandoning or rematching a room. A socket that closes leaves its room seated; reaping and
  the grace period are `ADR-0013` and `STORY-0208`.
- A rematch's opening frames: `RematchResult.Agreed` still carries none — see `TASK-020725`.

## Tests

`DuelSocketRoomTest` — a new file, two real clients through `testApplication`, each doing the full
handshake. The `RoomRegistry` is built by the test with a fixed `HandSeedSource` and handed to
`testDeps(rooms = …)`, so the opening hand is reproducible.

| Test | Proves |
| --- | --- |
| `createRoomAnswersWithACodeAndSeatZero` | the host receives a `RoomJoined` whose `seat` is 0 and whose `code` names a room the registry now holds |
| `joinRoomSeatsTheGuestInSeatOne` | the second client receives `RoomJoined` with `seat == 1` |
| `anUnknownCodeIsRefused` | `JoinRoom("ZZZZZZZZ")` answers `Failure(UNKNOWN_ROOM)` |
| `amalformedCodeIsRefusedTheSameWay` | `JoinRoom("!")` also answers `Failure(UNKNOWN_ROOM)`, not a different error |
| `athirdClientFindsTheRoomFull` | a third handshaken client joining answers `Failure(ROOM_FULL)` |
| `thehostRejoiningItsOwnRoomIsToldItsSeat` | the host sending `JoinRoom(itsOwnCode)` answers `RoomJoined(code, 0)`, not a refusal |
| `bothSeatsReceiveTheOpeningHand` | after the guest joins, each client receives a `Snapshot`, and exactly one of them receives a `YourTurn` |
| `neitherSeatSeesTheOthersHoleCards` | in each client's opening `Snapshot`, the opponent's `seats[opponent].holeCards` is empty and its own has two |

`DuelSocketFrameLoopTest` must pass **unchanged** — in `verify:`, not in the budget. Its
`anActOutsideADuelIsAnsweredWithNotInDuel` still describes a socket that has joined no room, which
this ticket does not change.

## Acceptance criteria

- [ ] All eight `DuelSocketRoomTest` cases named above pass
- [ ] `DuelSocketFrameLoopTest` passes with the file unchanged
- [ ] `RunnerLeakTest.noServerSourceFileTouchesHoleCards` and
      `RunnerLeakTest.onlyTheBroadcastFileBuildsAStateCarryingFrame` pass with the file unchanged
- [ ] `DuelSocket.kt` contains no `PlayerView`, no `holeCards`, and no construction of
      `ServerMessage.Snapshot` or `ServerMessage.Events`
- [ ] The `when` over `RoomRefusal` has no `else` branch
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
