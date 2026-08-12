---
schema: 2
id: TASK-020608
title: RoomTimeouts, the two idle limits a room is reaped against
type: task
status: backlog
parent: STORY-0206
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, rooms, config]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*RoomTimeoutsTest'
  - ./gradlew :poker-server:check
---

## Goal

The two durations that decide when a room is reaped are a named type with defaults, not literals
buried in the registry.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomTimeouts.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomTimeoutsTest.kt` | create |

## Scope

- One file, package `duels.poker.server.room`, KDoc on everything public:

  ```kotlin
  public data class RoomTimeouts(
      val waitingMillis: Long,
      val finishedMillis: Long,
  ) {
      init { require(waitingMillis > 0); require(finishedMillis > 0) }

      public companion object {
          public const val DEFAULT_WAITING_MILLIS: Long = 10 * 60 * 1000L
          public const val DEFAULT_FINISHED_MILLIS: Long = 5 * 60 * 1000L
          public val DEFAULT: RoomTimeouts = RoomTimeouts(DEFAULT_WAITING_MILLIS, DEFAULT_FINISHED_MILLIS)
      }
  }
  ```

- `waitingMillis` is how long a room with one player and an unused code survives — long enough to
  send a link and have someone open it, ten minutes.
- `finishedMillis` is how long a `FINISHED` or `ABANDONED` room lingers — long enough for the other
  player to offer a rematch, five minutes.
- Both must be `const val` so `TASK-020613` can use them as `ServerConfig` defaults without
  restating the numbers; a duplicated default is a default that drifts.
- KDoc must state what has **no** timeout here: a `PLAYING` room is never reaped for idleness, and
  a disconnected player in a live duel is `ADR-0013`'s grace period, not this.
- No clock, no Ktor, no engine import.

## Out of scope

- Reading these from configuration — `TASK-020613`.
- Applying them — `TASK-020612`.

## Tests

`RoomTimeoutsTest`, JUnit 5, package `duels.poker.server.room`.

| Test | Proves |
| --- | --- |
| `theDefaultsAreTheDeclaredConstants` | `DEFAULT.waitingMillis == DEFAULT_WAITING_MILLIS` and `DEFAULT.finishedMillis == DEFAULT_FINISHED_MILLIS`, both `> 0` |
| `rejectsANonPositiveWaitingTimeout` | `RoomTimeouts(0, 1)` and `RoomTimeouts(-1, 1)` throw `IllegalArgumentException` |
| `rejectsANonPositiveFinishedTimeout` | `RoomTimeouts(1, 0)` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `RoomTimeoutsTest.theDefaultsAreTheDeclaredConstants` passes
- [ ] `RoomTimeoutsTest.rejectsANonPositiveWaitingTimeout` passes
- [ ] `RoomTimeoutsTest.rejectsANonPositiveFinishedTimeout` passes
- [ ] `DEFAULT_WAITING_MILLIS` and `DEFAULT_FINISHED_MILLIS` are declared `const val`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
