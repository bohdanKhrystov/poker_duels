---
schema: 2
id: TASK-020814
title: A returning socket picks up where it left off, and another device does not
type: task
status: backlog
parent: STORY-0208
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, websocket, resilience, security]
depends_on: [TASK-020813]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketReconnectTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketRoomTest'
  - ./gradlew :poker-server:test --tests '*RunnerLeakTest'
  - grep -c '!!' poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt | grep -qx 0
---

## Goal

A `JoinRoom` from a player who already holds a seat resumes that seat: the window stops, the socket
is told which seat it has, and the state it is entitled to arrives through the projection layer. A
`JoinRoom` from any other device is refused exactly as it is today.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketReconnectTest.kt` | create |

## Scope

- One block at the top of `replyToJoinRoom`, after `RoomCode.parse` and before the existing
  `deps.rooms.join(...)`:

  ```kotlin
  val resumed = deps.rooms.resume(parsed, session.player.id)
  if (resumed != null) {
      room.code = parsed
      send(ProtocolCodec.encode(ServerMessage.RoomJoined(parsed.value, resumed.seat)))
      deliver(resumed.outbound, resumed.room, deps.connections)
      return
  }
  ```

  Everything below it — the `Seated`, `ALREADY_SEATED`, `UNKNOWN_ROOM` and `ROOM_FULL` branches —
  stays exactly as `TASK-020731` and `TASK-020734` left it.
- **Reconnection needs no new message.** `resume` answers only for a room this player is already
  seated in that carries a duel (`TASK-020811`), so a first-time joiner, a stranger and a host
  rejoining a `WAITING` room all fall straight through to the code that already handles them. That
  is why `ADR-0018`'s "returning *is* a new socket" costs nothing on the wire: the returning client
  handshakes with its device id and re-sends the `JoinRoom` it already knows how to send.
- **Identity is the whole security story.** The seat comes from `session.player.id`, which came
  from the handshake's device id (`ADR-0012`) — never from the frame. A different device resolves
  to a different `PlayerId`, `resume` gives `null`, and the ordinary `join` refuses a full
  `PLAYING` room with `ROOM_FULL`, leaving the held seat held.
- `deliver` sends the resumed frames to the writer registered for that player, which is the newly
  handshaken socket — no direct write, no encoding in this file beyond the `RoomJoined` it already
  builds.

## Out of scope

- Anything about pausing, which `TASK-020813` finished.
- Any change to `Room.join`'s ordering. A `FINISHED` room still refuses an ordinary `join` with
  `UNKNOWN_ROOM`; resumption gets in ahead of that rather than reopening the question, so
  `RoomJoinTest`'s ordering assertions stand.

## Tests

`DuelSocketReconnectTest` — a new file, built like `DuelSocketRoomTest`, with a `RoomRegistry` the
test holds over a `MutableClock`. In every case the host creates a room and the guest joins, so a
duel is live before anything is dropped. Reconnect by opening a **new** socket and completing the
handshake with the guest's original device id, then sending `JoinRoom(code)`.

| Test | Proves |
| --- | --- |
| `aReturningSocketIsToldItsSeat` | the reconnecting socket receives `ServerMessage.RoomJoined(code, 1)` |
| `aReturningSocketSeesItsOwnCardsAndNotTheOpponents` | the `Snapshot` that follows has `view.viewer.holeCards` of size 2 and `view.opponent.holeCards` empty — asserted on the frames the client actually received |
| `aReturningSocketResumesTheSameState` | that `Snapshot`'s `view` is equal to the `Snapshot` view the guest received when the hand was dealt: no action happened in between, so the resumed state is the same state and not a re-deal |
| `theDuelIsRunningAgainAfterAReconnect` | `rooms.get(code)!!.isPaused` is `false`, and an `Act` from the seat on turn is no longer answered with `Failure(DUEL_PAUSED)` |
| `anotherDeviceMayNotTakeAHeldSeat` | while seat 1 is counting down, a third device sending `JoinRoom(code)` receives `Failure(ProtocolError.ROOM_FULL)`, **and** `rooms.get(code)!!.gracePeriods` still names seat 1 with its original deadline. The second half is the point: a refusal that quietly cleared the window would pass the first half |
| `aReconnectAfterTheDuelFinishedGetsTheFinishedState` | with `EndCondition.FixedHands(1)`, play the hand out over the sockets by folding the seat that is prompted, drop the guest, then reconnect: the guest receives `RoomJoined` followed by exactly one `ServerMessage.DuelFinished`, and no `Snapshot` and no `YourTurn` |

## Acceptance criteria

- [ ] All six `DuelSocketReconnectTest` cases named above pass
- [ ] `DuelSocketRoomTest` passes with the file **unchanged**, including
      `thehostRejoiningItsOwnRoomIsToldItsSeat` — a `WAITING` room is not resumable, so that path
      still runs through `ALREADY_SEATED`
- [ ] `RunnerLeakTest` passes with the file unchanged, including `noServerSourceFileTouchesHoleCards`
- [ ] `DuelSocket.kt` contains no `!!`, no `PlayerView`, and constructs no state-carrying
      `ServerMessage`
- [ ] No test in `DuelSocketReconnectTest` calls `Thread.sleep`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
