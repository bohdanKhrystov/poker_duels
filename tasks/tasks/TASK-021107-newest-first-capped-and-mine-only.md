---
schema: 2
id: TASK-021107
title: Prove the duel list is newest first, capped, and nobody else's
type: task
status: done
parent: STORY-0211
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, persistence, duel]
depends_on: [TASK-021106]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The recent-duels query is pinned on the three properties a list has to have: newest first, no more
rows than the caller asked for, and not one duel belonging to anybody else.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` (the query under
test),
`poker-server/src/main/kotlin/duels/poker/server/duel/FinishedDuel.kt`.

## Scope

- Add three tests to the existing `PostgresProfileReadsTest`, plus a second pair of profiles —
  `carol` (already resolved by `TASK-021106`) and a new `dave` — resolved inside the test that
  needs them, not in `@BeforeEach`.
- The fixtures pass **explicit, distinct `finishedAt` instants**, oldest to newest, and record them
  in that order. Fixed instants such as `Instant.parse("2026-08-13T10:00:00Z")` plus minutes: no
  `Instant.now()`, because an ordering test whose fixtures are microseconds apart is a test that
  fails on a fast machine for reasons nobody can reproduce.
- The isolation test records one `alice`/`bob` duel and one `carol`/`dave` duel, then asserts from
  both sides: `alice`'s list does not contain the second duel's id, and `carol`'s list contains
  only the second. Asserting one direction would pass against a query that returns everything to
  the first player it ever sees.
- **No existing test, helper or assertion in the file changes.** This ticket adds test methods and
  reuses the `finishedDuel(...)` builder as it stands.

## Out of scope

- Any change to `PostgresProfileReads.kt`: `ORDER BY … DESC` and `LIMIT ?` are already in the query
  `TASK-021106` merged. If a test here fails, the fix belongs in that file — say so in the PR, do
  not extend this ticket's file list.
- The drawn duel — `TASK-021108`.
- The default and the cap of `limit` themselves: those are `duelLimitOrNull`'s, pinned by
  `RecentDuelsLimitTest`. This ticket only proves the query honours the number it is given.

## Tests

`PostgresProfileReadsTest`, the existing class, three tests added.

| Test | Proves |
| --- | --- |
| `duelsComeBackNewestFirst` | three `alice`/`bob` duels recorded with increasing `finishedAt` come back as `[newest, middle, oldest]` by `duelId` |
| `theLimitCapsTheNumberOfDuelsReturned` | with those three recorded, `recentDuelsOf(alice.id, 2)` returns exactly the two newest, in that order |
| `anotherPlayersDuelsNeverAppear` | with an `alice`/`bob` duel and a `carol`/`dave` duel recorded, `recentDuelsOf(alice.id, 10)` names only the first and `recentDuelsOf(carol.id, 10)` only the second |

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.duelsComeBackNewestFirst` passes
- [ ] `PostgresProfileReadsTest.theLimitCapsTheNumberOfDuelsReturned` passes
- [ ] `PostgresProfileReadsTest.anotherPlayersDuelsNeverAppear` passes
- [ ] The eleven tests already in `PostgresProfileReadsTest` pass with their assertions unchanged
- [ ] No new test calls `Instant.now()`
- [ ] No file other than `PostgresProfileReadsTest.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
