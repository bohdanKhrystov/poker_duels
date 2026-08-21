---
schema: 2
id: TASK-050213
title: Over HTTP against the database — every player exactly once, and page two's ranks are the ladder's
type: task
status: backlog
parent: STORY-0502
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, http, db, leaderboard, paging, tests]
depends_on: [TASK-050212]
verify:
  - ./gradlew :poker-server:test --tests '*StandingsWalkDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

A client that walks `GET /api/standings` from the first page to the last, over a real database, is
handed every player of the season exactly once and a rank on every row that counts the **whole**
ladder rather than the rows on screen.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsWalkDatabaseTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsRoutes.kt` | read |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelHistoryPagingDatabaseTest.kt` | read |

## Scope

- A new Testcontainers-backed HTTP test class, built exactly like `DuelHistoryPagingDatabaseTest`:
  `PostgresTestSupport.freshDatabase()`, `Migrations.migrate`, players from
  `PostgresPlayerDirectory`, duels from `PostgresDuelResultStore.record`, and

  ```kotlin
  application { module(); standingsRoutes(PostgresProfileReads(dataSource), PostgresStandingsReads(dataSource), clock) }
  ```

  with `clock = Clock.fixed(Instant.parse("2026-08-20T09:00:00Z"), ZoneOffset.UTC)`.
- Copy that class's `walkAllPages` shape, including its **bounded** loop that fails with a message
  rather than spinning if `nextCursor` never becomes `null`.
- The cursor is handed back **exactly as received** — never decoded and re-encoded by the test,
  which would test the test's round trip instead of the server's.
- No production file changes.

## Out of scope

- **A duel recorded mid-walk** — `TASK-050214` and `TASK-050215`. This fixture is still while it is
  read, which is what makes it the baseline the other two are measured against.
- **The self standing** — `TASK-050216`. Requests here carry no device header.
- **`STORY-0408`'s sentence.** *Total and disjoint even when a duel finishes between two requests*
  is **not** inherited here (`ADR-0066` §4) and no criterion in this ticket claims it: what is
  asserted is exactly-once over a ladder **that does not move during the walk**.

## Tests

`StandingsWalkDatabaseTest`, `-PrequireDocker=true`, `limit = 2`.

**The fixture — seven players, five duels, recorded in an order that is not the answer's order.**
Create the profiles in the order `d, b, a, e, t2, c, t1` and record:

1. `b` **draws** `e` — two rows of `0` (`ADR-0015`);
2. `a` beats `c`;
3. `a` beats `c`;
4. `t1` beats `d`;
5. `t2` beats `d`.

Standings: `a +2`, `t1 +1`, `t2 +1`, `b 0`, `e 0`, `c -2`, `d -2` — they sum to zero because every
duel does. Ranks: `1, 2, 2, 4, 4, 6, 6`. In pages of two the walk is four pages and **all three ties
straddle a page boundary**, which is the arrangement `ADR-0064` §2 calls correct and a naive test
calls a duplicate.

| Test | Proves |
| --- | --- |
| `everyPlayerOfTheLadderComesBackExactlyOnceOverTheWalk` | the walk is four pages of sizes `[2, 2, 2, 1]`, the last carries `nextCursor = null`, and the seven player ids across all pages contain each of the seven **exactly once** — asserted by counting ids, naming any that repeat or go missing, never by `toSet().size` alone |
| `theRanksAWalkReturnsAreTheWholeLaddersAndNeverDecrease` | the concatenated rank sequence over the walk is exactly `[1, 2, 2, 4, 4, 6, 6]`; page two's first rank equals page one's last rank; and no rank returned is smaller than one returned before it |

**Named mutations.** A rank numbered from the page offset gives page two `[3, 4]` where the ladder
says `[2, 4]` and reddens the second test on page **two** — the client-side bug `ADR-0002` forbids,
reachable from the server side. `dense_rank()` gives `[1,2,2,3,3,4,4]` and reddens it too. Dropping
the `player_id` component of the order or of the page predicate reddens the first test, because
every one of the three ties then either repeats a player or skips one.

**Why the fixture is scrambled.** Creation order and recording order both differ from the returned
order; a fixture that arrives sorted cannot fail a mutation to the ordering.

## Acceptance criteria

- [ ] `StandingsWalkDatabaseTest.everyPlayerOfTheLadderComesBackExactlyOnceOverTheWalk` passes,
      asserting page sizes, the terminating `null` cursor, and each of the seven ids exactly once
- [ ] `StandingsWalkDatabaseTest.theRanksAWalkReturnsAreTheWholeLaddersAndNeverDecrease` passes,
      asserting the literal rank sequence `[1, 2, 2, 4, 4, 6, 6]`
- [ ] The test hands back the exact `nextCursor` string it was given and decodes none of them
- [ ] No test in this file asserts that a repeated **rank number** is a defect
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
