---
schema: 2
id: TASK-050103
title: The season an instant falls in, in UTC, whatever the reader's clock says
type: task
status: done
parent: STORY-0501
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, seasons]
depends_on: [TASK-050102]
verify:
  - ./gradlew :poker-server:test --tests '*SeasonOfInstantTest'
  - ./gradlew :poker-server:ktlintCheck
  - "! grep -q systemDefault poker-server/src/main/kotlin/duels/poker/server/season/Season.kt"
  - "! grep -rqi season poker-engine/src"
  - test -z "$(git diff --name-only $(git merge-base HEAD origin/develop) -- poker-engine web-client poker-server/src/main/resources/db/migration)"
---

## Goal

One function turns an instant into the season containing it, and the answer is the same on a
server in Kiritimati as on one in Niue.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/season/Season.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/season/SeasonOfInstantTest.kt` | create |

## Scope

- `public fun seasonOf(instant: Instant): Season`, a top-level function in `Season.kt`. One
  top-level class in a file plus top-level functions is the shape `FinishedDuel.kt` and
  `DuelCursor.kt` already have, so ktlint's filename rule stays satisfied with the file named for
  the class.
- Implemented in **UTC and nothing else** — `YearMonth.from(instant.atOffset(ZoneOffset.UTC))`, or
  the `atZone(ZoneOffset.UTC)` equivalent. `ZoneId.systemDefault()` is forbidden, a `verify:` line
  greps for it, and `theSeasonOfAnInstantDoesNotDependOnTheDefaultTimeZone` below is what catches
  it on a machine where the grep would not be enough.
- KDoc records the cost `ADR-0061` named out loud, because this function is where it lives: the
  boundary is UTC while the client renders instants *in the reader's locale*
  (`finishedAtText`, `web-client/src/profile/profile-text.ts`), so **a player far enough east can
  read a duel as finishing on 1 September and find it counted in August**. That is intended, not a
  defect: localising the boundary per player would make the standings stop being one ordering, and
  printing UTC everywhere is worse for everything else. The tests below are its reproduction.

## Out of scope

- **`seasonOf(duel)`** — `TASK-050104`. This function takes an instant; that one takes a
  `FinishedDuel` and reads exactly one field of it.
- **The current season and any clock** — `TASK-050106`, which takes an injected
  `java.time.Clock` per
  [`ADR-0062`](../../docs/adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md)
  and never `ServerClock`. This function stays the only place a UTC conversion happens.
- **Doing anything about the locale hazard.** `ADR-0061`'s *What it costs* accepts it in writing;
  this ticket pins it with a test rather than softening it. There is nothing to build, so the
  refusal takes the form of a test that *asserts the surprising answer on purpose* —
  `halfAnHourEitherSideOfABoundaryLandsInDifferentSeasons`.
- **Any change to the web client.** Nothing in `web-client/` is touched by this story. The
  `verify:` block asserts it for this branch, and every other ticket in the story carries the
  same line for its own.

## Tests

`SeasonOfInstantTest`, in `duels.poker.server.season`. JUnit `@Test`, `kotlin.test` assertions.
Every fixture is an `Instant.parse("…")` literal.

The default-zone test saves `TimeZone.getDefault()` before it changes anything and restores it in
a `finally`, so no later test in the JVM inherits the change. Nothing in this repository enables
parallel test execution, so a single-threaded save/restore is sufficient.

| Test | Proves | Mutation that turns it red |
| --- | --- | --- |
| `anInstantExactlyOnABoundaryBelongsToTheNewSeason` | `seasonOf(Instant.parse("2026-09-01T00:00:00Z"))` is `Season(2026, 9)` **and** `seasonOf(Instant.parse("2026-08-31T23:59:59.999Z"))` is `Season(2026, 8)` — the two endpoints, one millisecond apart, which is `ADR-0061` §1's half-open bound stated from the instant's side | any implementation that rounds or truncates the instant upward, or that assigns a boundary instant to the *old* season |
| `halfAnHourEitherSideOfABoundaryLandsInDifferentSeasons` | `2026-08-31T23:30:00Z` is `Season(2026, 8)` and `2026-09-01T00:30:00Z` is `Season(2026, 9)`. **This is the hazard's reproduction**: a reader at UTC+2 sees the first as *1 September, 01:30* and a reader at UTC−5 sees the second as *31 August, 19:30*, and the server disagrees with both on purpose | computing the month in any zone other than UTC — a `ZoneId.systemDefault()` implementation running anywhere east of UTC+1 or west of UTC−1 flips one of these two |
| `twoInstantsInsideOneMonthLandInTheSameSeason` | `2026-08-01T00:30:00Z` and `2026-08-31T23:30:00Z` are both `Season(2026, 8)` — the *no boundary between them* direction, so the pair above cannot be satisfied by anything that simply returns a different season each call | an implementation that derives the month from the day of the month, or that returns a fresh season per invocation |
| `theSeasonOfAnInstantDoesNotDependOnTheDefaultTimeZone` | the one instant `2026-09-01T00:30:00Z` is `Season(2026, 9)` with the JVM default zone set to `Pacific/Kiritimati` (UTC+14) **and** with it set to `Pacific/Niue` (UTC−11); the default is restored in a `finally` | `ZoneOffset.UTC` → `ZoneId.systemDefault()`. This is the one test that catches that swap on **every** machine, including a CI runner already in UTC, where all three tests above would still pass |
| `aSeasonIsNamedByTheInstantItBeginsAt` | `seasonOf(Season(2025, 12).start)` is `Season(2025, 12)` and `seasonOf(Season(2026, 1).start)` is `Season(2026, 1)` — the round trip against `TASK-050102`'s bounds, across a year boundary | `YearMonth.from(…)` replaced by anything that loses the year, which makes the December fixture answer `2026-12` |

## Acceptance criteria

- [ ] `SeasonOfInstantTest.anInstantExactlyOnABoundaryBelongsToTheNewSeason` passes, asserting both
      `00:00:00Z` and the millisecond before it
- [ ] `SeasonOfInstantTest.halfAnHourEitherSideOfABoundaryLandsInDifferentSeasons` passes
- [ ] `SeasonOfInstantTest.twoInstantsInsideOneMonthLandInTheSameSeason` passes
- [ ] `SeasonOfInstantTest.theSeasonOfAnInstantDoesNotDependOnTheDefaultTimeZone` passes, sets the
      default zone to two zones on opposite sides of UTC, and restores the original in a `finally`
- [ ] `SeasonOfInstantTest.aSeasonIsNamedByTheInstantItBeginsAt` passes
- [ ] `Season.kt` names no `systemDefault` — the `verify:` grep exits 0
- [ ] `SeasonTest` and `SeasonBoundsTest` are untouched: this ticket adds a function and changes
      nothing either of them observes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
