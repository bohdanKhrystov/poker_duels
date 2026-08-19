---
schema: 2
id: TASK-050101
title: A season is a year and a month, and its identifier is 2026-08
type: task
status: ready
parent: STORY-0501
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, seasons]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*SeasonTest'
  - ./gradlew :poker-server:ktlintCheck
  - "! grep -qw var poker-server/src/main/kotlin/duels/poker/server/season/Season.kt"
  - "! grep -rqi season poker-engine/src"
  - test -z "$(git diff --name-only $(git merge-base HEAD origin/develop) -- poker-engine web-client poker-server/src/main/resources/db/migration)"
---

## Goal

The server has one type for a season, and one spelling of the identifier
[`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md) §1
gives it: `2026-08`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/season/Season.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/season/SeasonTest.kt` | create |

## Scope

- A new package `duels.poker.server.season`, a sibling of `auth`, `db`, `duel`, `http`, `room`,
  `session` and `time`. A season is a server fact about a *record of duels*, not a fact about a
  game (`ADR-0061` §3), so it lives in `poker-server` and nothing about it crosses into
  `poker-engine`.
- `public data class Season(val year: Int, val month: Int)` — `val` only, no `var`, per the style
  rules in [`CLAUDE.md`](../../CLAUDE.md). One `verify:` line greps for the word `var` in this
  file, so do not use it in prose in the KDoc either.
- `init { require(month in 1..12) { … } }`. A month outside `1..12` is not a season and cannot be
  constructed.
- `override fun toString(): String` renders `"%04d-%02d"` — `Season(2026, 8)` is `2026-08`. Its
  precedent is `Card.toString()` in the engine, which renders a card's one canonical text form.
- KDoc says, in one sentence each: this identifier is the **wire form** of `ADR-0061` §1 and the
  only identifier a season has; what a *player* reads is `August 2026`, which is `STORY-0503`'s
  string and not this type's.

## Out of scope

- **Bounds** — `start`, `endExclusive`, `contains`: `TASK-050102`. This ticket adds no `Instant`.
- **`seasonOf(...)`** in any form: `TASK-050103` (an instant) and `TASK-050104` (a duel).
- **The current season and any clock**: `TASK-050106`, which takes an injected
  `java.time.Clock` per
  [`ADR-0062`](../../docs/adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md).
- **Parsing `2026-08` back into a `Season`.** A client naming a season is a *request*, never an
  assertion (`ADR-0002`), and the route that receives one is `STORY-0502`'s. There is **no test
  for this refusal because there is no function to refuse anything** — a parser with no caller
  would be the speculative code, not the missing one. Recorded here rather than dropped.
- **The English label `August 2026`** — `STORY-0503`, `ADR-0061` §6.
- **Any migration, table or column.** `ADR-0061` §3 makes a season *derived*: a ticket that adds a
  `V<n>__` file has misread the ADR. Every ticket in this story carries the `verify:` line that
  asserts its own branch touches no migration, no `poker-engine` file and no `web-client` file.

## Tests

`SeasonTest`, in `duels.poker.server.season`. JUnit's `@Test` with `kotlin.test` assertions
(`assertEquals`, `assertFailsWith`) — the pattern `CoinBalanceIsSignedTest` already uses.

Expected identifiers are **string literals** in the test. Never build the expected value by
applying a format to the same `year` and `month` the code formats: that test passes whatever the
format string says.

| Test | Proves | Mutation that turns it red |
| --- | --- | --- |
| `theIdentifierIsTheMonthZeroPaddedToTwoDigits` | `Season(2026, 8).toString()` is `"2026-08"` **and** `Season(2026, 12).toString()` is `"2026-12"` — two inputs, one month that needs padding and one that does not, so the assertion cannot pass on a constant | dropping the `0` from `%02d` renders `2026-8` |
| `aDecemberAndAJanuaryEachCarryTheirOwnYear` | `Season(2025, 12).toString()` is `"2025-12"` and `Season(2026, 1).toString()` is `"2026-01"` | swapping year and month, or hard-coding either, gives both fixtures the same text |
| `aMonthOutsideOneToTwelveIsNotASeason` | `assertFailsWith<IllegalArgumentException>` for **both** `Season(2026, 0)` and `Season(2026, 13)` | deleting the `require`, or writing it as `month in 1..13`, leaves one of the two constructing normally |

## Acceptance criteria

- [ ] `SeasonTest.theIdentifierIsTheMonthZeroPaddedToTwoDigits` passes, and both expected values in
      it are string literals
- [ ] `SeasonTest.aDecemberAndAJanuaryEachCarryTheirOwnYear` passes
- [ ] `SeasonTest.aMonthOutsideOneToTwelveIsNotASeason` passes, asserting both `0` and `13`
- [ ] `Season.kt` contains no `var` — the `verify:` grep exits 0
- [ ] `poker-engine` mentions no season — the `verify:` grep exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
