---
schema: 2
id: TASK-020601
title: Declare the injectable ServerClock and a test clock that never sleeps
type: task
status: ready
parent: STORY-0206
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [server, time]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*ServerClockTest'
  - ./gradlew :poker-server:check
---

## Goal

The server has one abstraction for elapsed time, and a test clock that moves by being told to —
so every later timeout in this epic is asserted without a `Thread.sleep`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/time/ServerClock.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/time/MutableClock.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/time/ServerClockTest.kt` | create |

## Scope

- New package `duels.poker.server.time`. KDoc on everything public:

  ```kotlin
  public fun interface ServerClock {
      public fun nowMillis(): Long
  }

  public object SystemClock : ServerClock {
      override fun nowMillis(): Long = System.nanoTime() / 1_000_000
  }
  ```

- KDoc must say *why* `nanoTime`: every consumer measures a **duration** (a room's idle time,
  `ADR-0013`'s grace period), and `System.currentTimeMillis()` can step backwards when the host
  clock is corrected, which would extend or collapse a timeout. This clock is elapsed time, not a
  wall-clock date, and must not be used to stamp a database row.
- The test fixture, in the **test** source set so it cannot ship:

  ```kotlin
  class MutableClock(private var current: Long = 0L) : ServerClock {
      override fun nowMillis(): Long = current
      fun advance(millis: Long)   // require(millis >= 0)
      fun set(millis: Long)
  }
  ```

- No Ktor type, no coroutine, no `Instant`, no `java.time` anywhere in either file.

## Out of scope

- Anything that *uses* the clock: rooms reap on it in `TASK-020612`, and `STORY-0208`'s grace
  period is its bigger consumer.
- A `Duration` type or a scheduler — a `Long` of milliseconds is the whole contract.

## Tests

`ServerClockTest`, JUnit 5, package `duels.poker.server.time`.

| Test | Proves |
| --- | --- |
| `theSystemClockNeverGoesBackwards` | 1 000 successive `SystemClock.nowMillis()` reads are non-decreasing |
| `aMutableClockReportsExactlyWhatItWasSet` | `MutableClock(7_000).nowMillis() == 7_000L`, and after `set(9_000)` it reads `9_000L` |
| `advancingTheMutableClockMovesTimeForward` | from `0`, `advance(1_500)` then `advance(500)` reads `2_000L` |
| `aMutableClockRefusesToGoBackwards` | `advance(-1)` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `ServerClockTest.theSystemClockNeverGoesBackwards` passes
- [ ] `ServerClockTest.aMutableClockReportsExactlyWhatItWasSet` passes
- [ ] `ServerClockTest.advancingTheMutableClockMovesTimeForward` passes
- [ ] `ServerClockTest.aMutableClockRefusesToGoBackwards` passes
- [ ] `ServerClock.kt` imports nothing from `io.ktor`, `kotlinx.coroutines` or `java.time`
- [ ] `MutableClock` lives under `src/test`, not `src/main`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
