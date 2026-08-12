---
schema: 2
id: TASK-020612
title: Reap idle rooms on the injected clock, and never a room that is playing
type: task
status: done
parent: STORY-0206
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, rooms, lifecycle]
depends_on: [TASK-020611]
verify:
  - ./gradlew :poker-server:test --tests '*RoomReapTest'
  - grep -L 'Thread.sleep' poker-server/src/test/kotlin/duels/poker/server/room/RoomReapTest.kt
  - ./gradlew :poker-server:check
---

## Goal

A room that nobody is using is removed after its configured idle limit, measured on the injected
clock — so an abandoned room stops holding two `PlayerId`s forever, and a live duel is never
removed underneath its players.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomReapTest.kt` | create |

`RoomRegistryTest`, `RoomRegistryJoinTest` and `RoomRegistryLifecycleTest` all run against a clock
that never advances, so no room in them is ever old enough to reap. They are untouched and every
assertion in them stands.

## Scope

- One method plus one private predicate, KDoc on the public one:

  ```kotlin
  public suspend fun reap(): List<RoomCode>
  ```

- The rule, stated in the KDoc and implemented once:
  - `WAITING` — reaped when `now - lastActivityAt >= timeouts.waitingMillis`;
  - `FINISHED` and `ABANDONED` — reaped when `now - lastActivityAt >= timeouts.finishedMillis`;
  - `PLAYING` — **never** reaped, whatever its idle time. A live duel whose players are silent is
    `ADR-0013`'s grace period, and that path ends by calling `abandon`, which makes the room
    reapable through the rule above rather than by a second timer here.
- `now` comes from `clock.nowMillis()`, read **once** per `reap()` so every room in one pass is
  judged against the same instant.
- For each candidate, take its `mutex.withLock`, re-test the predicate against `holder.room` inside
  the lock, and only then `rooms.remove(code, holder)`. A room that was joined between the scan and
  the lock must survive: removing on the stale copy is how a player loses a seat they just took.
  This is the other half of the `rooms[code] === holder` re-check `TASK-020610` added.
- Returns the codes it removed, in no particular order, so a caller can log or count them.
- `reap` is called by nothing yet. It is a method, not a timer: no `launch`, no scheduler, no
  `delay` in this file — `STORY-0207` decides where the sweep is driven from.

## Out of scope

- Scheduling `reap` on a background loop, and its interval — `STORY-0207` wires it.
- Notifying anybody that their room was reaped — no protocol message exists for it (`DEC-010`).
- Reading the timeouts from configuration — `TASK-020613`.

## Tests

`RoomReapTest`, JUnit 5, package `duels.poker.server.room`, `runBlocking` for suspending calls.
Build the registry with `MutableClock` and
`RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000)`, and move time only with
`clock.advance(...)`. **No `Thread.sleep`, no `delay`, no `System.currentTimeMillis` anywhere in
the file** — a `verify` command fails the ticket if `Thread.sleep` appears.

| Test | Proves |
| --- | --- |
| `aWaitingRoomIsReapedAtItsTimeout` | after `advance(10_000)`, `reap()` returns that one code and `get(code)` is `null` |
| `aWaitingRoomOneMillisecondShortSurvives` | after `advance(9_999)`, `reap()` is empty and `size == 1` |
| `aFinishedRoomIsReapedAtTheFinishedTimeout` | a finished room survives `advance(3_999)` and is reaped on the next `advance(1)` |
| `anAbandonedRoomIsReapedAtTheFinishedTimeout` | same for a room reached through `abandon` |
| `aPlayingRoomIsNeverReaped` | after `advance(10_000_000)`, `reap()` is empty and the playing room is still found by `get` |
| `joiningResetsTheIdleClock` | `advance(9_000)`, `join`, `advance(9_000)`, `finish`: `reap()` is empty because each transition restamped `lastActivityAt` |
| `reapReturnsEveryCodeItRemoved` | three waiting rooms past the limit give a returned list of exactly their three codes and `size == 0` |

## Acceptance criteria

- [ ] `RoomReapTest.aWaitingRoomIsReapedAtItsTimeout` passes
- [ ] `RoomReapTest.aWaitingRoomOneMillisecondShortSurvives` passes
- [ ] `RoomReapTest.aFinishedRoomIsReapedAtTheFinishedTimeout` passes
- [ ] `RoomReapTest.anAbandonedRoomIsReapedAtTheFinishedTimeout` passes
- [ ] `RoomReapTest.aPlayingRoomIsNeverReaped` passes
- [ ] `RoomReapTest.joiningResetsTheIdleClock` passes
- [ ] `RoomReapTest.reapReturnsEveryCodeItRemoved` passes
- [ ] `RoomReapTest.kt` contains no `Thread.sleep`, no `delay(` and no `System.currentTimeMillis`
- [ ] `RoomRegistry.kt` contains no `launch`, no `delay` and no `CoroutineScope`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
