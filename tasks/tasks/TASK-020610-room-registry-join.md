---
schema: 2
id: TASK-020610
title: Join by code under the room's lock, so a hundred racing joiners seat exactly one
type: task
status: backlog
parent: STORY-0206
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, rooms, concurrency]
depends_on: [TASK-020605, TASK-020609]
verify:
  - ./gradlew :poker-server:test --tests '*RoomRegistryJoinTest'
  - ./gradlew :poker-server:check
---

## Goal

`RoomRegistry.join(code, player)` applies `Room.join` under that room's mutex and stores the
result, so "two seats, never three" survives a hundred callers arriving at the same instant.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryJoinTest.kt` | create |

`RoomRegistryTest` covers `create`, `get` and `size`, none of which changes here. It is not
touched and every assertion in it stands.

## Scope

- One method, KDoc included:

  ```kotlin
  public suspend fun join(code: RoomCode, player: PlayerId): JoinResult
  ```

- A code with no live room → `JoinResult.Refused(RoomRefusal.UNKNOWN_ROOM)`, and nothing is created.
  This is the only place that value is produced from a missing room rather than a dead one, and the
  two are deliberately indistinguishable to the caller.
- Otherwise take `holder.mutex.withLock { ... }`, read `holder.room`, call `Room.join(player,
  clock.nowMillis())`, and **write `holder.room` back only on `Seated`**. Read, decide and write
  all inside the one critical section: doing the read outside the lock is the lost update this
  whole ticket exists to prevent.
- Re-check inside the lock that the holder is still in the map (`rooms[code] === holder`); a room
  reaped between lookup and lock must refuse with `UNKNOWN_ROOM` rather than seat a player into a
  room nobody can find. `TASK-020612` is the other half of this handshake.
- A refusal from `Room.join` is returned unchanged and the stored room is left exactly as it was.
- `join` is the first `suspend` member of the registry; `create`, `get` and `size` stay
  non-suspending.

## Out of scope

- Rematch, finish and abandon through the registry — `TASK-020611`.
- Any protocol frame that carries a join — `STORY-0207` and `DEC-010`.
- One player joining two rooms at once, or a second socket for one device — `DEC-011`,
  `STORY-0208`. Nothing here consults `SessionRegistry`.

## Tests

`RoomRegistryJoinTest`, JUnit 5, package `duels.poker.server.room`. Drive suspending calls with
`runBlocking`.

For `oneHundredConcurrentJoinersProduceExactlyOneGuest`: create one room; inside
`runBlocking(Dispatchers.Default)` build a shared `CompletableDeferred<Unit>` gate, `launch` 100
coroutines that each `gate.await()` and then `join(code, PlayerId("p$i"))`, `complete(Unit)` the
gate, and `awaitAll` the results. Gate on the deferred, **not** on a `CountDownLatch` — blocking
100 coroutines on a dispatcher with as many threads as CPUs deadlocks and would turn a race test
into a hang. Annotate `@Timeout(60)`.

| Test | Proves |
| --- | --- |
| `joiningAnUnknownCodeIsRefusedUnknownRoom` | `join(RoomCode("ZZZZZZZZ"), player)` is `Refused(UNKNOWN_ROOM)` and `size == 0` |
| `aSuccessfulJoinIsStoredInTheRegistry` | after `join`, `get(code)!!.state == PLAYING` and its `guest` is the joiner |
| `aRefusedJoinLeavesTheStoredRoomUntouched` | the host joining their own room gives `Refused(ALREADY_SEATED)` and `get(code)` is still the `WAITING` room, guest `null` |
| `aThirdJoinerIsRefusedRoomFullAndTheGuestKeepsTheSeat` | after a successful join, a third player gets `Refused(ROOM_FULL)` and `get(code)!!.guest` is unchanged |
| `oneHundredConcurrentJoinersProduceExactlyOneGuest` | exactly 1 of 100 results is `Seated`, the other 99 are `Refused(ROOM_FULL)`, and `get(code)!!.guest` is the player from that single `Seated` |

## Acceptance criteria

- [ ] `RoomRegistryJoinTest.joiningAnUnknownCodeIsRefusedUnknownRoom` passes
- [ ] `RoomRegistryJoinTest.aSuccessfulJoinIsStoredInTheRegistry` passes
- [ ] `RoomRegistryJoinTest.aRefusedJoinLeavesTheStoredRoomUntouched` passes
- [ ] `RoomRegistryJoinTest.aThirdJoinerIsRefusedRoomFullAndTheGuestKeepsTheSeat` passes
- [ ] `RoomRegistryJoinTest.oneHundredConcurrentJoinersProduceExactlyOneGuest` passes
- [ ] `RoomRegistryJoinTest` contains no `Thread.sleep` and no `CountDownLatch`
- [ ] `RoomRegistry.join` reads and writes `holder.room` only inside `holder.mutex.withLock`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
