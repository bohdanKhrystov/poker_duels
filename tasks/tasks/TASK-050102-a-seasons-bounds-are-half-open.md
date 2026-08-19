---
schema: 2
id: TASK-050102
title: A season's bounds are half-open, and December ends in January
type: task
status: backlog
parent: STORY-0501
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, seasons]
depends_on: [TASK-050101]
verify:
  - ./gradlew :poker-server:test --tests '*SeasonBoundsTest'
  - ./gradlew :poker-server:ktlintCheck
  - "! grep -q systemDefault poker-server/src/main/kotlin/duels/poker/server/season/Season.kt"
  - "! grep -rqi season poker-engine/src"
  - test -z "$(git diff --name-only $(git merge-base HEAD origin/develop) -- poker-engine web-client poker-server/src/main/resources/db/migration)"
---

## Goal

A `Season` knows the two instants that bound it — `[first instant of the month, first instant of
the next month)` — and says which instants fall inside, so consecutive seasons neither gap nor
overlap.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/season/Season.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/season/SeasonBoundsTest.kt` | create |

## Scope

- `public val start: Instant` — the first instant of the month, **in UTC**:
  `YearMonth.of(year, month).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()`.
- `public val endExclusive: Instant` — the `start` of the following month. Use
  `YearMonth.plusMonths(1)`, which carries December into the next January; do not write month
  arithmetic by hand.
- `public fun contains(instant: Instant): Boolean` —
  `!instant.isBefore(start) && instant.isBefore(endExclusive)`. Inclusive at the start, exclusive
  at the end: that is what half-open means, and `ADR-0061` §1 is where it is settled.
- `ZoneOffset.UTC` is written literally. `ZoneId.systemDefault()` is forbidden and a `verify:`
  line greps for it; `TASK-050103` carries the test that proves the answer does not move with the
  reader's clock.
- KDoc on `start` and `endExclusive` records **why they are public with no caller in this story**:
  `STORY-0502`'s standings query uses them as the SQL window,
  `finished_at >= start AND finished_at < endExclusive`, and having exactly one place that decides
  those two instants is the whole point of `STORY-0501`.

## Out of scope

- **`seasonOf(instant)`** — `TASK-050103`. This ticket goes from a season to its bounds; that one
  goes the other way.
- **`seasonOf(duel)`** — `TASK-050104`.
- **Any clock** — `TASK-050106`, which takes an injected `java.time.Clock` per
  [`ADR-0062`](../../docs/adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md).
- **A `next()` or `previous()` season.** Nothing needs one: `ADR-0061` §5 says a boundary does
  nothing, and reaching a *past* season is `DEC-060`. No test is possible for a function that does
  not exist; this is here so the absence is deliberate rather than forgotten.
- **Reading these bounds from anywhere.** Nothing in this story consumes them — no query, no
  route, no screen. The bounds are asserted directly, which is why this ticket's tests read like
  arithmetic rather than like behaviour.

## Tests

`SeasonBoundsTest`, in `duels.poker.server.season`. JUnit `@Test`, `kotlin.test` assertions.
Expected instants are `Instant.parse("…")` literals, never derived from `Season`.

| Test | Proves | Mutation that turns it red |
| --- | --- | --- |
| `aSeasonBeginsAtMidnightUtcOnTheFirstOfItsMonth` | `Season(2026, 8).start` is `Instant.parse("2026-08-01T00:00:00Z")` **and** `Season(2026, 9).start` is `Instant.parse("2026-09-01T00:00:00Z")` — two inputs | `atStartOfDay(ZoneOffset.UTC)` → `atStartOfDay(ZoneId.systemDefault())` on any machine not on UTC; `atDay(1)` → `atEndOfMonth()` |
| `aSeasonEndsExactlyWhereTheNextOneBegins` | `Season(2026, 8).endExclusive` equals `Season(2026, 9).start`, **and** equals the literal `Instant.parse("2026-09-01T00:00:00Z")` — both, so it cannot pass on two identically wrong values | `plusMonths(1)` → `plusDays(30)`, which makes the two seasons overlap by a day |
| `decemberEndsAtTheFirstInstantOfTheNextJanuary` | `Season(2025, 12).endExclusive` is `Instant.parse("2026-01-01T00:00:00Z")` | month arithmetic that adds one to `month` without carrying the year, i.e. anything that tries to build a `2025-13` |
| `containsIsInclusiveAtItsStartAndExclusiveAtItsEnd` | for `Season(2026, 8)`: `contains(start)` is `true`, `contains(endExclusive)` is **`false`**, and `contains(endExclusive.minusMillis(1))` is `true` — the two sides of the closing endpoint, one millisecond apart | `instant.isBefore(endExclusive)` → `!instant.isAfter(endExclusive)` makes `contains(endExclusive)` true, which is the half-open bound collapsing into a closed one |
| `aSeasonDoesNotContainTheMillisecondBeforeItBegins` | for `Season(2026, 8)`: `contains(Instant.parse("2026-07-31T23:59:59.999Z"))` is `false` and `contains(Instant.parse("2026-08-01T00:00:00.001Z"))` is `true` — the two sides of the opening endpoint | dropping the `!instant.isBefore(start)` half, which makes every earlier instant a member |

## Acceptance criteria

- [ ] `SeasonBoundsTest.aSeasonBeginsAtMidnightUtcOnTheFirstOfItsMonth` passes
- [ ] `SeasonBoundsTest.aSeasonEndsExactlyWhereTheNextOneBegins` passes, asserting both the
      neighbouring season's `start` and an `Instant.parse` literal
- [ ] `SeasonBoundsTest.decemberEndsAtTheFirstInstantOfTheNextJanuary` passes
- [ ] `SeasonBoundsTest.containsIsInclusiveAtItsStartAndExclusiveAtItsEnd` passes, and asserts
      `false` for `endExclusive` itself
- [ ] `SeasonBoundsTest.aSeasonDoesNotContainTheMillisecondBeforeItBegins` passes
- [ ] `Season.kt` names no `systemDefault` — the `verify:` grep exits 0
- [ ] `SeasonTest` is untouched: this ticket adds to `Season` and changes nothing it observes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
