---
schema: 2
id: TASK-020734
title: The rejoin path cannot assume the room survived its own refusal
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, rooms, robustness]
depends_on: [TASK-020715]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketRoomTest'
  - grep -c '!!' poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt | grep -qx 0
  - ./gradlew :poker-server:check
---

## Goal

`DuelSocket`'s `ALREADY_SEATED` branch answers a client whose room has disappeared, instead of
throwing a `NullPointerException` at it. No `!!` survives in `DuelSocket.kt`.

## What was found

Found in review of `TASK-020731`, outside that ticket's scope. The rejoin path looks the room up a
**second** time, after `RoomRegistry.join` has already refused:

```kotlin
deps.rooms.get(parsed)?.seatOf(session.player.id)!!
```

Two lookups, no lock between them. If the room is gone by the second one, `!!` throws — inside the
frame loop, on a connection that was doing nothing wrong.

**It is unreachable today, and that is the problem.** It is unreachable only because
`Room.isReapable` hard-codes `false` for a `PLAYING` room and nothing schedules a background reaper
yet. `RoomRegistry`'s own KDoc says scheduling one is a later story's job. That story will arm this
line without touching it, and the failure will surface as a socket dying on a rejoin — a long way
from the change that caused it.

A `WAITING` room's self-rejoin is the one case theoretically exposed even now.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketRoomTest.kt` | modify |

## Scope

- Replace the `!!` with the same typed answer the surrounding code already gives a client whose room
  is not there: `ServerMessage.Failure(ProtocolError.UNKNOWN_ROOM)`. A room that vanished between
  the refusal and the lookup is, from the client's side, exactly a room that is not there.
- The `ALREADY_SEATED` happy path — a client that really is seated in a room that really exists —
  must still answer `RoomJoined` with the seat the registry gives it, unchanged.
- No new lock, and no third lookup. This ticket makes a missing room answerable, not atomic;
  making the whole rejoin one critical section is a different and larger change.

## Out of scope

- Scheduling the reaper, or changing `Room.isReapable`.
- Any other `!!` or non-null assertion outside `DuelSocket.kt` — the `verify` grep is scoped to that
  file deliberately.
- The two-lookup shape itself. It is fine once the second lookup has an answer for `null`.

## Tests

`DuelSocketRoomTest`

| Test | Proves |
| --- | --- |
| a rejoin whose room vanished is refused, not fatal | the socket answers `Failure(UNKNOWN_ROOM)` and stays open |
| a rejoin to a room that still exists still answers `RoomJoined` | the happy path is unchanged |

The first test must actually remove the room between the two lookups — deleting it from the
registry after the client is seated and before the rejoin frame arrives. A test that never empties
the registry proves nothing here, and this story has shipped three assertions that passed because
their subject was absent.

## Acceptance criteria

- [ ] `DuelSocket.kt` contains no `!!` — the `grep` in `verify:` exits 0
- [ ] A rejoin whose room has vanished receives `Failure(UNKNOWN_ROOM)` and the socket survives
- [ ] The `ALREADY_SEATED` happy path still answers `RoomJoined` with the registry's seat
- [ ] Every existing `DuelSocketRoomTest` case still passes, unweakened
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
