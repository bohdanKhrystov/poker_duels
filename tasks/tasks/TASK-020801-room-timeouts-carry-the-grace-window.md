---
schema: 2
id: TASK-020801
title: RoomTimeouts carries the disconnect grace window
type: task
status: ready
parent: STORY-0208
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, rooms, config]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*RoomTimeoutsTest'
  - ./gradlew :poker-server:test --tests '*RoomReapTest'
  - grep -c 'disconnectGraceMillis' poker-server/src/test/kotlin/duels/poker/server/room/RoomReapTest.kt | grep -qx 0
---

## Goal

`RoomTimeouts` carries a third limit — how long a dropped connection holds its seat — so
`RoomRegistry`, which already takes a `RoomTimeouts`, can read the grace window without gaining a
constructor parameter.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomTimeouts.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomTimeoutsTest.kt` | modify |

## Scope

- One field, following the two already there exactly:

  ```kotlin
  val disconnectGraceMillis: Long = DEFAULT_DISCONNECT_GRACE_MILLIS,
  ```

  with `require(disconnectGraceMillis > 0)` alongside the two existing `require`s, a
  `DEFAULT_DISCONNECT_GRACE_MILLIS: Long = 60 * 1000L` constant beside the other two, and
  `RoomTimeouts.DEFAULT` passing it.
- **The field has a default and the other two do not.** That is deliberate and belongs in a
  comment: `RoomReapTest` builds `RoomTimeouts(waitingMillis = 10_000, finishedMillis = 4_000)` and
  has no opinion on the grace window, and a reaping test should not have to state one. The default
  keeps that file out of this ticket's blast radius entirely.
- The class KDoc currently ends with:

  > Note: A `PLAYING` room is never reaped for idleness. A disconnected player in a live duel is
  > subject to ADR-0013's grace period, not these timeouts.

  That sentence is now false in its second half. Replace it: a `PLAYING` room is still never reaped
  for idleness, and `ADR-0013`'s grace period is now *one of* these timeouts — the one measured per
  seat rather than per room. Say which one.
- Document the value: 60 seconds is long enough for an app to come back from a tunnel and short
  enough that the opponent is not held hostage, and it is tunable per `ADR-0013` without a code
  change (`TASK-020802` wires the environment variable).

## Out of scope

- Reading it from `ServerConfig` — `TASK-020802`.
- Anything that *uses* the window: `Room` learns about grace windows in `TASK-020804`, and
  `RoomRegistry` reads this field in `TASK-020809`.

## Tests

`RoomTimeoutsTest` — modified by appending two tests and adding two lines to one existing test.
Nothing else in the file changes and no assertion is weakened.

| Test | Proves |
| --- | --- |
| `theDefaultsAreTheDeclaredConstants` | **existing test, extended**: gains `assertEquals(RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS, RoomTimeouts.DEFAULT.disconnectGraceMillis)` and `assert(RoomTimeouts.DEFAULT.disconnectGraceMillis > 0)`. Without this it would keep passing while saying nothing about the new field. Its two existing assertions stay exactly as they are. |
| `rejectsANonPositiveGraceWindow` | `RoomTimeouts(1, 1, 0)` and `RoomTimeouts(1, 1, -1)` both throw `IllegalArgumentException` |
| `theGraceWindowDefaultsWhenNotNamed` | `RoomTimeouts(1, 1).disconnectGraceMillis == RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS` — this is the assertion that pins the default that keeps `RoomReapTest` compiling |

## Acceptance criteria

- [ ] `RoomTimeoutsTest.theDefaultsAreTheDeclaredConstants` passes and names `disconnectGraceMillis`
- [ ] `RoomTimeoutsTest.rejectsANonPositiveGraceWindow` passes
- [ ] `RoomTimeoutsTest.theGraceWindowDefaultsWhenNotNamed` passes
- [ ] `RoomReapTest` passes with the file unchanged, and the grep in `verify` proves it never
      mentions the new field
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
