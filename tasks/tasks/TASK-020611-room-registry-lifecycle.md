---
schema: 2
id: TASK-020611
title: Finish, abandon and offer a rematch through the registry
type: task
status: done
parent: STORY-0206
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, rooms, rematch]
depends_on: [TASK-020607, TASK-020610]
verify:
  - ./gradlew :poker-server:test --tests '*RoomRegistryLifecycleTest'
  - ./gradlew :poker-server:check
---

## Goal

The three remaining transitions are reachable through the registry under the same per-room lock as
`join`, so a room can end, be abandoned, and be restarted by mutual agreement without any caller
holding a stale copy.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryLifecycleTest.kt` | create |

`RoomRegistryTest` and `RoomRegistryJoinTest` cover creation and joining, neither of which changes
here. Both files are untouched and every assertion in them stands.

## Scope

- Three methods, each the same shape as `join` — look the holder up, `mutex.withLock`, re-check
  `rooms[code] === holder`, apply the `Room` method with `clock.nowMillis()`, write back:

  ```kotlin
  public suspend fun finish(code: RoomCode): Room?
  public suspend fun abandon(code: RoomCode): Room?
  public suspend fun offerRematch(code: RoomCode, player: PlayerId): RematchResult
  ```

- `finish` and `abandon` return the stored room after the transition, or `null` for a code with no
  live room. `finish` propagates `Room.finish`'s `IllegalStateException` when the room is not
  `PLAYING`: only the server calls it, and calling it out of turn is a bug that must be loud.
- `offerRematch` returns `RematchResult.Refused(RematchRefusal.UNKNOWN_ROOM)` for an unknown code
  and otherwise whatever `Room.offerRematch` decided, writing the room back on `Offered` and on
  `Agreed` and leaving it untouched on `Refused`.
- Do not copy the lock-and-write block four times: factor a private
  `private suspend fun <T> mutate(code: RoomCode, absent: () -> T, block: (Room) -> Pair<Room?, T>): T`
  or an equivalent, and route `join` through it too. The rule being enforced — read, decide and
  write inside one critical section — must exist in exactly one place.
- Moving `join`'s lock into that helper is expected and is **not** a scope widening: `TASK-020610`
  required the read-decide-write to happen inside the lock, not that the `withLock` keyword sit
  inside `join`. `RoomRegistryJoinTest` must keep passing untouched — it asserts behaviour, and no
  behaviour changes.

## Out of scope

- Reaping — `TASK-020612`.
- Deciding *when* a duel is finished, and dealing the rematch's first hand — `STORY-0207`.
- Deciding when a player counts as gone — `ADR-0013` and `STORY-0208` call `abandon`.

## Tests

`RoomRegistryLifecycleTest`, JUnit 5, package `duels.poker.server.room`, `runBlocking` for the
suspending calls. Reach a finished room with `create` → `join` → `finish`.

| Test | Proves |
| --- | --- |
| `finishUpdatesTheStoredRoom` | `get(code)!!.state == FINISHED` after `finish`, and the returned room is the stored one |
| `finishingARoomThatIsNotPlayingThrows` | `finish` on a freshly created `WAITING` room throws `IllegalStateException` |
| `abandonUpdatesTheStoredRoom` | after `abandon`, `get(code)!!.state == ABANDONED` |
| `finishAndAbandonReturnNullForAnUnknownCode` | both return `null` for `RoomCode("ZZZZZZZZ")` and `size` is unchanged |
| `oneOfferLeavesTheStoredRoomFinished` | after the host offers, the result is `Offered` and `get(code)!!.state == FINISHED` with one offer recorded |
| `bothOffersReturnTheStoredRoomToPlaying` | after both offer, the result is `Agreed` and `get(code)!!.state == PLAYING` with `openingButtonSeat == 1` and no offers left |
| `offeringInAnUnknownRoomIsRefusedUnknownRoom` | `offerRematch(RoomCode("ZZZZZZZZ"), player)` is `Refused(UNKNOWN_ROOM)` |

## Acceptance criteria

- [ ] `RoomRegistryLifecycleTest.finishUpdatesTheStoredRoom` passes
- [ ] `RoomRegistryLifecycleTest.finishingARoomThatIsNotPlayingThrows` passes
- [ ] `RoomRegistryLifecycleTest.abandonUpdatesTheStoredRoom` passes
- [ ] `RoomRegistryLifecycleTest.finishAndAbandonReturnNullForAnUnknownCode` passes
- [ ] `RoomRegistryLifecycleTest.oneOfferLeavesTheStoredRoomFinished` passes
- [ ] `RoomRegistryLifecycleTest.bothOffersReturnTheStoredRoomToPlaying` passes
- [ ] `RoomRegistryLifecycleTest.offeringInAnUnknownRoomIsRefusedUnknownRoom` passes
- [ ] `RoomRegistry.kt` contains exactly one `withLock` call site
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
