---
schema: 2
id: TASK-020613
title: Read the room idle limits from ServerConfig instead of a literal
type: task
status: done
parent: STORY-0206
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, rooms, config]
depends_on: [TASK-020608]
verify:
  - ./gradlew :poker-server:test --tests '*ServerConfigTest'
  - ./gradlew :poker-server:check
---

## Goal

The two room idle limits are configuration — file, environment variable, default — like every
other value the server reads, and `ServerConfig` hands them over as a ready `RoomTimeouts`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | modify |

`ServerConfigTest` is modified by **appending** the tests below. Every existing test in it stays
exactly as it is and no assertion is weakened: they all build a config through `ServerConfig.from`
or `ServerConfig.load` and assert single fields, so adding two fields to the data class changes
nothing they observe. Do not reorder, rename or delete anything already in the file.

## Scope

- Two fields on `ServerConfig`, following the existing pattern exactly — a `DEFAULT_*` constant, a
  `*_KEY`, a `*_ENV`, resolution through the private `resolve`, and a `requireNotNull(...toLongOrNull())`
  with a message naming the bad value:

  ```kotlin
  val roomWaitingTimeoutMillis: Long,
  val roomFinishedTimeoutMillis: Long,
  ```

  Keys `room.waitingTimeoutMillis` and `room.finishedTimeoutMillis`; environment variables
  `ROOM_WAITING_TIMEOUT_MILLIS` and `ROOM_FINISHED_TIMEOUT_MILLIS`.
- The defaults are `RoomTimeouts.DEFAULT_WAITING_MILLIS` and `RoomTimeouts.DEFAULT_FINISHED_MILLIS`,
  referenced — never retyped. One number in one place is the point of `TASK-020608`.
- One accessor so callers do not reassemble it:

  ```kotlin
  public fun roomTimeouts(): RoomTimeouts = RoomTimeouts(roomWaitingTimeoutMillis, roomFinishedTimeoutMillis)
  ```

  A non-positive value therefore fails at startup, inside `RoomTimeouts`' own `require`, which is
  where that rule already lives.
- Add the two new fields at the **end** of the constructor parameter list, so no positional call
  site can silently rebind.
- `application.conf` is not touched: both keys resolve to their defaults, and the shipped file
  states only what deployment overrides.

## Out of scope

- Constructing the `RoomRegistry` from config, and everything else about wiring — `STORY-0207`;
  `Application.kt` still installs nothing.
- Any other new setting — `ADR-0013`'s grace period arrives with `STORY-0208`.

## Tests

Appended to `ServerConfigTest`, package `duels.poker.server.config`, in the style already there
(`MapApplicationConfig`, `ServerConfig.from(config) { null }`).

| Test | Proves |
| --- | --- |
| `readsTheRoomWaitingTimeoutFromTheConfig` | `"room.waitingTimeoutMillis" to "1234"` gives `roomWaitingTimeoutMillis == 1234L` |
| `theEnvironmentVariableOverridesTheRoomFinishedTimeout` | the env lookup wins over the file value for the finished timeout |
| `fallsBackToTheRoomTimeoutDefaults` | an empty config gives `RoomTimeouts.DEFAULT_WAITING_MILLIS` and `RoomTimeouts.DEFAULT_FINISHED_MILLIS` |
| `rejectsARoomTimeoutThatIsNotANumber` | `"room.waitingTimeoutMillis" to "ten minutes"` throws `IllegalArgumentException` |
| `rejectsANonPositiveRoomTimeout` | `"room.finishedTimeoutMillis" to "0"` makes `roomTimeouts()` throw `IllegalArgumentException` |
| `roomTimeoutsBundlesBothValues` | `roomTimeouts()` equals `RoomTimeouts(roomWaitingTimeoutMillis, roomFinishedTimeoutMillis)` |

## Acceptance criteria

- [ ] `ServerConfigTest.readsTheRoomWaitingTimeoutFromTheConfig` passes
- [ ] `ServerConfigTest.theEnvironmentVariableOverridesTheRoomFinishedTimeout` passes
- [ ] `ServerConfigTest.fallsBackToTheRoomTimeoutDefaults` passes
- [ ] `ServerConfigTest.rejectsARoomTimeoutThatIsNotANumber` passes
- [ ] `ServerConfigTest.rejectsANonPositiveRoomTimeout` passes
- [ ] `ServerConfigTest.roomTimeoutsBundlesBothValues` passes
- [ ] Every test that was already in `ServerConfigTest` still passes, unedited
- [ ] `ServerConfig.kt` contains neither timeout figure as a literal — both defaults read from
      `RoomTimeouts`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
