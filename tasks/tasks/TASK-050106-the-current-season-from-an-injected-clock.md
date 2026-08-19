---
schema: 2
id: TASK-050106
title: The current season, read from an injected clock and never from a system clock
type: task
status: backlog
parent: STORY-0501
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, seasons]
depends_on: [TASK-050105]
verify:
  - ./gradlew :poker-server:test --tests '*CurrentSeasonTest'
  - ./gradlew :poker-server:test --tests '*SeasonMovesNoCoinTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - "grep -qF 'fun currentSeason(clock: Clock): Season' poker-server/src/main/kotlin/duels/poker/server/season/Season.kt"
  - "! grep -qF Instant.now() poker-server/src/main/kotlin/duels/poker/server/season/Season.kt"
  - "! grep -qF System.currentTimeMillis poker-server/src/main/kotlin/duels/poker/server/season/Season.kt"
  - "! grep -qF ServerClock poker-server/src/main/kotlin/duels/poker/server/season/Season.kt"
  - test -z "$(git diff --name-only $(git merge-base HEAD origin/develop) -- poker-engine web-client poker-server/src/main/resources/db/migration)"
---

## Goal

The server can say which season it is now, from a clock a test can move, and a test proves it by
moving one rather than by waiting.

## The clock this takes, and why it is not the other one

[`ADR-0062`](../../docs/adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md) answers
`DEC-062` and this ticket is unblocked by it. The server has **two** clocks and only one of them
knows what day it is:

- `duels.poker.server.time.ServerClock` — `System.nanoTime()`, elapsed time from an arbitrary
  epoch, for **durations**: room idleness, disconnect grace windows, `ADR-0025`'s sweeps. **Not
  this one.** `Instant.ofEpochMilli(clock.nowMillis())` compiles and yields a season in 1970.
- **`java.time.Clock` — this one.** `Clock.systemUTC()` in production, `Clock.fixed` in a test.

`ADR-0062` amends `ADR-0061` §3, which named the wrong one; `STORY-0501`'s design notes are
corrected in the same change. If any document you are handed still says *which season is it* comes
from `ServerClock`, it predates that ADR and the ADR wins.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/season/Season.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/season/CurrentSeasonTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/season/SeasonMovesNoCoinTest.kt` | modify |

## Scope

- One top-level function in `Season.kt`, with exactly this signature — a `verify:` line greps for
  it, so do not reformat it across lines:

  ```kotlin
  public fun currentSeason(clock: Clock): Season = seasonOf(clock.instant())
  ```

- **The parameter has no default value.** `ADR-0062` §3: a defaulted pure function can be called
  with no clock at all, and that call compiles anywhere with nothing in a diff to notice. The one
  `Clock.systemUTC()` in the server belongs at the composition root, which is a different ticket and
  has no caller to serve yet.
- **It computes nothing itself.** It is a one-line composition over `TASK-050103`'s
  `seasonOf(instant)`, which already owns the UTC conversion. There is one place that turns an
  instant into a season and this is not a second one (`ADR-0061` §3). In particular this function
  names **no** zone: `ZoneOffset.UTC` is `seasonOf`'s, and `clock.getZone()` is never read, so a
  clock fixed in another zone cannot change the answer.
- Two `verify:` greps forbid `Instant.now()` and `System.currentTimeMillis` in `Season.kt`, and a
  third forbids the word `ServerClock` anywhere in it. They are fixed-string greps looking for the
  empty argument list, so `Instant.now(clock)` would not match either of the first two — but the
  signature above does not need it, because `clock.instant()` is the same value with less to read.
- **`SeasonMovesNoCoinTest` gains exactly one line**: this function joins the set exercised between
  its two snapshots, so its scope sentence *"every path this story added"* stays true. **No
  assertion in that file moves, is added, or is weakened** — `theFixtureItComparesIsNotEmpty` and
  the snapshot-equality assertion are untouched, and the file is in this ticket's budget only
  because the exercised set is part of what that test claims. Pass it any fixed clock; the point of
  the call is that it runs, not what it returns.

## Out of scope

- **Any caller.** Nothing in this story reads the current season — no route, no query, no screen.
  `STORY-0502` is the first caller and it is blocked on `DEC-056`, `DEC-058`, `DEC-059` and
  `DEC-061`. A ticket that adds a read path belongs there.
- **The composition root.** `serverComponents` gaining `wallClock: Clock = Clock.systemUTC()` is
  `ADR-0062` §7's ticket (b), due before `STORY-0502`. This ticket wires nothing and adds no
  `Clock.systemUTC()` to `src/main`.
- **Touching `ServerClock` or `SystemClock` at all**, including its KDoc — that correction is
  `ADR-0062` §7's ticket (a). This ticket must leave `duels/poker/server/time/` byte-identical.
- **A shared wall-clock test fixture.** The movable clock below stays `private` inside
  `CurrentSeasonTest.kt`. A second test that needs one is when it earns a home, and it must not be
  confused with `duels.poker.server.time.MutableClock`, which is the *elapsed* clock's fake.
- **Anything a boundary triggers.** Nothing happens at one (`ADR-0061` §5), which is why
  `STORY-0505` is `dropped`. This function reports; it does not fire.

## Tests

`CurrentSeasonTest`, in `duels.poker.server.season`. JUnit `@Test`, `kotlin.test` assertions.

Every fixed instant is in a month the suite is **not** running in, so an implementation that
ignores the clock and reads the system time is red on every day of every month rather than green
by luck for four weeks.

**How a test fixes the clock.** `Clock.fixed(Instant.parse("2019-02-15T12:00:00Z"), ZoneOffset.UTC)`
— the JDK's own fixed clock, exactly as `PostgresDuelResultSinkTest` already pins a row timestamp.

**How a test moves it.** `java.time.Clock` is an abstract class rather than a `fun interface`, so a
clock that *moves* is a small subclass rather than a lambda — the ergonomic cost `ADR-0062` named
out loud. Declare it `private` in this test file, below the test class:

```kotlin
/**
 * A wall clock a test can move. `withZone` is never called by [currentSeason]; the season's zone
 * is `seasonOf`'s, not this clock's.
 */
private class MovableClock(var now: Instant) : Clock() {
    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
}
```

| Test | Proves | Mutation that turns it red |
| --- | --- | --- |
| `theCurrentSeasonIsTheOneContainingTheClocksInstant` | with a clock fixed at `2019-02-15T12:00:00Z` the answer is `Season(2019, 2)`, and with one fixed at `2025-12-31T23:59:59.999Z` it is `Season(2025, 12)` — two inputs, so the assertion cannot pass on a constant | returning a fixed season; reading `Instant.now()` instead of the clock, which answers the month the suite runs in and matches neither fixture |
| `movingTheClockAcrossABoundaryChangesTheCurrentSeason` | one `MovableClock` moved from `2025-12-31T23:59:59.999Z` to `2026-01-01T00:00:00Z` answers `Season(2025, 12)` and then `Season(2026, 1)` — asserted by **moving the clock**, never by sleeping, and across a year boundary so the month arithmetic cannot carry it | caching the first answer in a top-level `val` or a lazy field, which returns December twice |

## Acceptance criteria

- [ ] `currentSeason` takes `java.time.Clock`, has **no default value**, and reads `clock.instant()`
      — the signature `verify:` grep exits 0
- [ ] `CurrentSeasonTest.theCurrentSeasonIsTheOneContainingTheClocksInstant` passes, asserting two
      different fixed instants
- [ ] `CurrentSeasonTest.movingTheClockAcrossABoundaryChangesTheCurrentSeason` passes, and the test
      contains no sleep, no `Thread.sleep` and no timeout
- [ ] Neither fixed instant in `CurrentSeasonTest` falls in the calendar month the suite runs in
- [ ] `Season.kt` contains none of `Instant.now()`, `System.currentTimeMillis` or `ServerClock` —
      all three `verify:` greps exit 0
- [ ] Nothing under `poker-server/src/main/kotlin/duels/poker/server/time/` changes
- [ ] `SeasonMovesNoCoinTest` passes with one added call and **no changed assertion**
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
