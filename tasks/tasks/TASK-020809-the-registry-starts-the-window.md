---
schema: 2
id: TASK-020809
title: The registry starts a seat's window on its own clock and configured limit
type: task
status: backlog
parent: STORY-0208
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, rooms, resilience]
depends_on: [TASK-020808]
verify:
  - ./gradlew :poker-server:test --tests '*RoomDisconnectTest'
  - ./gradlew :poker-server:test --tests '*RoomRegistryTest'
  - ./gradlew :poker-server:test --tests '*RoomReapTest'
  - grep -c 'holder.mutex.withLock {' poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt | grep -qx 2
---

## Goal

`RoomRegistry` turns "this player's connection is gone" into a deadline: the seat starts counting
down from `ServerClock` plus the configured window, under that room's own mutex.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomDisconnectTest.kt` | create |

## Scope

- One method, in the shape of `join`, `finish` and `abandon` — a single `mutate` call, no new lock:

  ```kotlin
  public suspend fun disconnect(code: RoomCode, player: PlayerId): Room?
  ```

  Inside the critical section: `room.seatOf(player)` decides the seat and answers `null` for
  anybody this room has not seated, leaving the room untouched; otherwise
  `room.disconnect(seat, clock.nowMillis() + timeouts.disconnectGraceMillis)` is written back and
  returned. `return@mutate Pair(null, null)` is the existing idiom for "change nothing" — see
  `offerRematch`.
- **The deadline is computed here and nowhere else.** `Room` reads no clock (`TASK-020805`), and
  the socket does not know the configured window. One call site for
  `clock.nowMillis() + timeouts.disconnectGraceMillis` is what makes `ADR-0013`'s "configuration,
  not a literal" hold.
- `RoomRegistry.kt` gains no new `withLock`. It has exactly two `holder.mutex.withLock {` call
  sites today — one in `reap`, one in `mutate` — and still has exactly two after this ticket; the
  `verify` grep pins the number rather than a claim about it.
- A `WAITING` room may take a disconnect too: seat 0 is always seated, so the call is legal and
  simply records that the host is gone. Nothing acts on that yet, and the existing `WAITING` idle
  timeout still reaps such a room.

## Out of scope

- Expiring the window — `TASK-020812`.
- Clearing it on return — `TASK-020811`.
- Calling this from a socket — `TASK-020813`.

## Tests

`RoomDisconnectTest` — a new file. Build the registry the way `RoomReapTest` does, with a
`MutableClock` and an explicit `RoomTimeouts`, so the window under test is a value the test chose:

```kotlin
private val TEST_TIMEOUTS = RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000, disconnectGraceMillis = 30_000)
```

Every case seats a guest first (`registry.create(host)` then `registry.join(code, guest)`), so both
seats exist and an assertion about seat 1 can fail.

| Test | Proves |
| --- | --- |
| `aDisconnectStartsTheWindowAtNowPlusTheConfiguredLimit` | with the clock at `0`, `disconnect(code, guest)` returns a room whose `gracePeriods == mapOf(1 to 30_000L)` |
| `theWindowRunsFromWhenTheDropHappened` | after `clock.advance(5_000)`, the deadline is `35_000L`, not `30_000L` |
| `theStoredRoomIsThePausedOne` | `registry.get(code)!!.isPaused` is `true` afterwards — the write-back happened, not only the returned value |
| `aSecondDropRestartsTheWindow` | disconnecting at `0` and again at `5_000` leaves `gracePeriods == mapOf(1 to 35_000L)` |
| `theHostAndTheGuestCountDownSeparately` | disconnecting both leaves `gracePeriods.keys == setOf(0, 1)` with each seat's own deadline |
| `somebodyWhoIsNotSeatedChangesNothing` | `disconnect(code, PlayerId("stranger"))` returns `null` and `registry.get(code)` is equal to the room before the call |
| `anUnknownCodeAnswersNull` | `disconnect(RoomCode("ZZZZZZZZ"), host)` is `null` |

## Acceptance criteria

- [ ] All seven `RoomDisconnectTest` cases named above pass
- [ ] `RoomRegistryTest` and `RoomReapTest` pass with those files unchanged
- [ ] `RoomRegistry.kt` contains exactly two `holder.mutex.withLock {` call sites
- [ ] No test in `RoomDisconnectTest` calls `Thread.sleep` or waits on wall-clock time; every
      instant comes from `MutableClock`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
