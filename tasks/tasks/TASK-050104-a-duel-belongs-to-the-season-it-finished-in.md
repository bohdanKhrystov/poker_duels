---
schema: 2
id: TASK-050104
title: A duel belongs to the season it finished in, never the one it started in
type: task
status: backlog
parent: STORY-0501
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, seasons]
depends_on: [TASK-050103]
verify:
  - ./gradlew :poker-server:test --tests '*SeasonOfDuelTest'
  - ./gradlew :poker-server:ktlintCheck
  - "! grep -qF Instant.now() poker-server/src/main/kotlin/duels/poker/server/season/Season.kt"
  - "! grep -rqi season poker-engine/src"
  - test -z "$(git diff --name-only $(git merge-base HEAD origin/develop) -- poker-engine web-client poker-server/src/main/resources/db/migration)"
---

## Goal

There is exactly one place in the server that says which season a finished duel belongs to, and it
reads `finishedAt`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/season/Season.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/season/SeasonOfDuelTest.kt` | create |

Read, and nothing else: `poker-server/src/main/kotlin/duels/poker/server/duel/FinishedDuel.kt`
for the constructor, and `poker-server/src/test/kotlin/duels/poker/server/duel/FinishedDuelTest.kt`
for how a fixture is built. The `DuelOutcome` arguments are given verbatim under *Tests*, so
there is no need to open the engine to find them.

## Scope

- `public fun seasonOf(duel: FinishedDuel): Season = seasonOf(duel.finishedAt)` — an overload of
  `TASK-050103`'s function, one expression, in `Season.kt`.
- KDoc says why `startedAt` is deliberately unread: a duel that began on 31 August and finished on
  1 September is a September duel **in full**, because the coin is paid once, at the end
  (`ADR-0061` §2, `ADR-0017`). A duel cannot pay into two seasons because it pays once.
- `duels.poker.server.season` may import `duels.poker.server.duel.FinishedDuel`. The edge points
  one way and stays that way: `FinishedDuel` learns nothing about seasons.

## Out of scope

- **Changing `FinishedDuel`.** No `season` property, no stored month, nothing (`ADR-0061` §3).
  `FinishedDuelTest` is **not** in this ticket's budget and must not be edited — this ticket
  changes nothing that test observes.
- **Reading a season out of the database, or scoping any query by one.** `STORY-0502`, which is
  blocked on `DEC-056`, `DEC-058`, `DEC-059` and `DEC-061`. Nothing in this story reads a season:
  a ticket that adds a read path belongs to that story, not this one.
- **Any migration.** `duel.finished_at` is already `TIMESTAMPTZ NOT NULL` on every row ever
  written, so attribution needs no column and no backfill — `ADR-0061` §3 and the story's own
  design notes. `TASK-050105` asserts the branch adds no `V<n>__` file.
- **The current season** — `TASK-050106`, `blocked` on `DEC-062`.

## Tests

`SeasonOfDuelTest`, in `duels.poker.server.season`. JUnit `@Test`, `kotlin.test` assertions.

Build the `FinishedDuel` fixtures inline, the way `FinishedDuelTest` does: a random `UUID`,
`format = "FREEZEOUT"`, two `PlayerId`s with distinct UUID text, and
`DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0))`. Only `startedAt` and
`finishedAt` differ between fixtures, and they are the only thing any assertion is about.

| Test | Proves | Mutation that turns it red |
| --- | --- | --- |
| `aDuelIsAttributedToTheMonthItFinishedIn` | a duel with `startedAt = 2026-08-31T22:00:00Z` and `finishedAt = 2026-09-01T00:30:00Z` is `Season(2026, 9)`. **The start would give the other answer**, which is what makes this assertion worth writing | `duel.finishedAt` → `duel.startedAt`, which answers `Season(2026, 8)` |
| `aDuelThatRanInsideOneMonthIsAttributedToThatMonth` | a duel with `startedAt = 2026-08-15T10:00:00Z` and `finishedAt = 2026-08-15T10:40:00Z` is `Season(2026, 8)` — the second input, so the pair cannot be satisfied by "always the month after the start" | any constant offset applied to the finish, e.g. `finishedAt.plus(1, ChronoUnit.MONTHS)` |
| `theSeasonOfADuelIsAFunctionOfItsFinishAndOfNothingElse` | a duel finished long ago — `startedAt = 2019-02-28T22:00:00Z`, `finishedAt = 2019-02-28T23:30:00Z` — is `Season(2019, 2)`, asked **twice** in the one test with both answers asserted. This is the story's *same answer for the same duel every time* criterion, and no wall-clock read can produce it | `seasonOf(Instant.now())`, or any implementation that consults a clock instead of the argument — both answer the month the suite happens to run in |

## Acceptance criteria

- [ ] `SeasonOfDuelTest.aDuelIsAttributedToTheMonthItFinishedIn` passes, with a fixture whose
      `startedAt` falls in a different month from its `finishedAt`
- [ ] `SeasonOfDuelTest.aDuelThatRanInsideOneMonthIsAttributedToThatMonth` passes
- [ ] `SeasonOfDuelTest.theSeasonOfADuelIsAFunctionOfItsFinishAndOfNothingElse` passes, and asserts
      the answer twice
- [ ] `Season.kt` contains no `Instant.now()` — the `verify:` grep exits 0
- [ ] `FinishedDuel.kt` and `FinishedDuelTest.kt` are unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
