---
schema: 2
id: TASK-050106
title: The current season, read from an injected clock and never from a system clock
type: task
status: blocked
parent: STORY-0501
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, seasons, blocked]
depends_on: [TASK-050105]
verify:
  - ./gradlew :poker-server:test --tests '*CurrentSeasonTest'
  - ./gradlew :poker-server:test --tests '*SeasonMovesNoCoinTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - "! grep -qF Instant.now() poker-server/src/main/kotlin/duels/poker/server/season/Season.kt"
  - "! grep -qF System.currentTimeMillis poker-server/src/main/kotlin/duels/poker/server/season/Season.kt"
  - test -z "$(git diff --name-only $(git merge-base HEAD origin/develop) -- poker-engine web-client poker-server/src/main/resources/db/migration)"
---

## Goal

The server can say which season it is now, from a clock a test can move, and a test proves it by
moving one rather than by waiting.

## Blocked on `DEC-062` — do not start this before that ADR is merged

`ADR-0061` §3 and `STORY-0501`'s design notes both say *which season is it* is a function of
`ServerClock.nowMillis()`. It cannot be: `SystemClock.nowMillis()` is
`System.nanoTime() / 1_000_000`, elapsed time from an arbitrary epoch, and `ServerClock`'s own
KDoc says *"Never use this clock to stamp a database row with a date."* No calendar month is
derivable from it. Meanwhile `PostgresDuelResultSink` needed a wall clock for exactly this reason
and injects `java.time.Clock`, defaulting to `Clock.systemUTC()`, with the reason written into its
KDoc.

`DEC-062` settles which instrument this function takes and which document is amended. **That is
the only open thing in this ticket** — every test, fixture and assertion below is fixed whatever
the answer. If the answer introduces a new type (a named wall-clock port rather than
`java.time.Clock`), say so and let this ticket be re-split rather than exceeding its three-file
budget; if it puts the function in a file other than `Season.kt`, the two greps in `verify:` move
with it and nothing else changes.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/season/Season.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/season/CurrentSeasonTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/season/SeasonMovesNoCoinTest.kt` | modify |

## Scope

- One function that answers *which season is it now*, taking its instant from the injected clock
  `DEC-062` names and never from an inline `Instant.now()` or `System.currentTimeMillis()`. Two
  `verify:` greps enforce that; `Instant.now(clock)` is fine and does not match either, because
  the greps are fixed-string and look for the empty argument list.
- The function is a one-line composition over `TASK-050103`'s `seasonOf(instant)`. It computes
  nothing itself: there is one place that turns an instant into a season and this is not a second
  one (`ADR-0061` §3 — *"nothing writes a season down, so nothing can disagree about one"*).
- **`SeasonMovesNoCoinTest` gains exactly one line**: this function joins the set exercised between
  its two snapshots, so its scope sentence *"every path this story added"* stays true. **No
  assertion in that file moves, is added, or is weakened** — `theFixtureItComparesIsNotEmpty` and
  the snapshot-equality assertion are untouched, and the file is in this ticket's budget only
  because the exercised set is part of what that test claims.

## Out of scope

- **Any caller.** Nothing in this story reads the current season — no route, no query, no screen.
  `STORY-0502` is the first caller and it is blocked on `DEC-056`, `DEC-058`, `DEC-059` and
  `DEC-061`. A ticket that adds a read path belongs there.
- **Changing `ServerClock` or `SystemClock`.** If `DEC-062`'s answer widens the existing port
  rather than using `java.time.Clock`, that is a change to a type six other classes depend on and
  it is its own ticket, not a line in this one.
- **Anything a boundary triggers.** Nothing happens at one (`ADR-0061` §5), which is why
  `STORY-0505` is `dropped`. This function reports; it does not fire.

## Tests

`CurrentSeasonTest`, in `duels.poker.server.season`. JUnit `@Test`, `kotlin.test` assertions.

Every fixed instant is in a month the suite is **not** running in, so an implementation that
ignores the clock and reads the system time is red on every day of every month rather than green
by luck for four weeks.

| Test | Proves | Mutation that turns it red |
| --- | --- | --- |
| `theCurrentSeasonIsTheOneContainingTheClocksInstant` | with the clock fixed at `2019-02-15T12:00:00Z` the answer is `Season(2019, 2)`, and with it fixed at `2025-12-31T23:59:59.999Z` it is `Season(2025, 12)` — two inputs, so the assertion cannot pass on a constant | returning a fixed season; reading `Instant.now()` instead of the argument, which answers the month the suite runs in and matches neither fixture |
| `movingTheClockAcrossABoundaryChangesTheCurrentSeason` | one instrument moved from `2025-12-31T23:59:59.999Z` to `2026-01-01T00:00:00Z` answers `Season(2025, 12)` and then `Season(2026, 1)` — asserted by **moving the clock**, never by sleeping, and across a year boundary so the month arithmetic cannot carry it | caching the first answer in a `val` or a lazy field, which returns December twice |

## Acceptance criteria

- [ ] `DEC-062` is answered by a **merged** ADR before this ticket starts
- [ ] `CurrentSeasonTest.theCurrentSeasonIsTheOneContainingTheClocksInstant` passes, asserting two
      different fixed instants
- [ ] `CurrentSeasonTest.movingTheClockAcrossABoundaryChangesTheCurrentSeason` passes, and the test
      contains no sleep, no `Thread.sleep` and no timeout
- [ ] Neither fixed instant in `CurrentSeasonTest` falls in the calendar month the suite runs in
- [ ] `Season.kt` contains neither `Instant.now()` nor `System.currentTimeMillis` — both `verify:`
      greps exit 0
- [ ] `SeasonMovesNoCoinTest` passes with one added call and **no changed assertion**
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
