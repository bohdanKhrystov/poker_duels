---
schema: 2
id: TASK-020609
title: A RoomRegistry that creates a room under a code nobody else holds
type: task
status: done
parent: STORY-0206
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, rooms, concurrency]
depends_on: [TASK-020601, TASK-020603, TASK-020604, TASK-020608]
verify:
  - ./gradlew :poker-server:test --tests '*RoomRegistryTest'
  - ./gradlew :poker-server:check
---

## Goal

The server holds live rooms in one place, each under a minted code that is unique among live
rooms, and each behind its own lock so that later tickets can mutate a room without two callers
ever mutating the same one at once.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/room/Room.kt`,
`poker-server/src/main/kotlin/duels/poker/server/room/RoomCodeSource.kt`,
`poker-server/src/main/kotlin/duels/poker/server/room/RoomTimeouts.kt`,
`poker-server/src/main/kotlin/duels/poker/server/time/ServerClock.kt`,
`poker-server/src/main/kotlin/duels/poker/server/session/SessionRegistry.kt` (the concurrent
registry this one is shaped after).

## Scope

- One file, package `duels.poker.server.room`, KDoc on everything public:

  ```kotlin
  public class RoomRegistry(
      private val codes: RoomCodeSource,
      private val clock: ServerClock,
      private val timeouts: RoomTimeouts = RoomTimeouts.DEFAULT,
  ) {
      public fun create(host: PlayerId, format: DuelFormat = DuelFormat.DEFAULT): Room
      public fun get(code: RoomCode): Room?
      public val size: Int

      public companion object { public const val MAX_CODE_ATTEMPTS: Int = 10 }
  }
  ```

  **Declare `timeouts` now even though nothing reads it until `TASK-020612`** — a constructor that
  grows a parameter later breaks every test that built a registry before it.
- Storage is `ConcurrentHashMap<RoomCode, Holder>` with a private
  `class Holder(@Volatile var room: Room) { val mutex = Mutex() }`. One `Mutex` per room is the
  single-writer rule of this story: `TASK-020610` onwards take it around read-modify-write, so no
  two frames ever mutate one room concurrently. `DEC-013` asks whether `STORY-0207` needs to
  promote this to a channel-fed actor; a per-room mutex is what that decision is measured against
  and it changes no signature here.
- `create` mints a code, builds `Room.open(code, host, format, clock.nowMillis())` and stores it
  with `putIfAbsent`. A non-null return means another room already holds that code: retry, up to
  `MAX_CODE_ATTEMPTS`, then throw `IllegalStateException` naming the attempt count. `putIfAbsent`
  is what makes the code unique — a `containsKey` check followed by a `put` is a race.
- `get` is a non-suspending snapshot read of `Holder.room`; `size` is the live room count.
- The registry knows no JSON, no `WebSocketSession`, no `ProtocolError`: nothing in this file
  imports `io.ktor` or `duels.poker.server.protocol`, and a test asserts it.

## Out of scope

- Joining, rematch, finishing, abandoning — `TASK-020610`, `TASK-020611`.
- Reaping and anything that reads `timeouts` — `TASK-020612`.
- Finding a player's room, or one player holding two rooms at once — `STORY-0208` and `DEC-011`;
  this registry is keyed by code only.

## Tests

`RoomRegistryTest`, JUnit 5, package `duels.poker.server.room`. Declare a scripted stub in the test
file: `private class ScriptedCodes(vararg codes: String) : RoomCodeSource` handing out the given
codes in order and repeating the last one forever. Use `MutableClock` from
`duels.poker.server.time`.

| Test | Proves |
| --- | --- |
| `createStoresARoomFoundByItsCode` | `get(created.code) == created`, `created.state == WAITING`, `size == 1` |
| `createStampsTheRoomWithTheClock` | with `MutableClock(4_242)`, `created.lastActivityAt == 4_242L` |
| `everyRoomGetsItsOwnCode` | two creates against a real `RandomRoomCodeSource` give different codes and `size == 2` |
| `createRetriesWhenTheCodeSourceRepeatsItself` | with `ScriptedCodes("2B7KMNPQ", "2B7KMNPQ", "3C8MNPQR")` the second `create` lands on `3C8MNPQR`, and `size == 2` |
| `createGivesUpAfterTooManyCollisions` | a source that always returns one code: the second `create` throws `IllegalStateException` |
| `getReturnsNullForAnUnknownCode` | `get(RoomCode("ZZZZZZZZ"))` on an empty registry is `null` |

## Acceptance criteria

- [ ] `RoomRegistryTest.createStoresARoomFoundByItsCode` passes
- [ ] `RoomRegistryTest.createStampsTheRoomWithTheClock` passes
- [ ] `RoomRegistryTest.everyRoomGetsItsOwnCode` passes
- [ ] `RoomRegistryTest.createRetriesWhenTheCodeSourceRepeatsItself` passes
- [ ] `RoomRegistryTest.createGivesUpAfterTooManyCollisions` passes
- [ ] `RoomRegistryTest.getReturnsNullForAnUnknownCode` passes
- [ ] `RoomRegistry.kt` contains no `import io.ktor` and no `import duels.poker.server.protocol`
- [ ] `RoomRegistry.kt` reads time only through the injected `ServerClock` — no
      `System.currentTimeMillis`, no `System.nanoTime`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
