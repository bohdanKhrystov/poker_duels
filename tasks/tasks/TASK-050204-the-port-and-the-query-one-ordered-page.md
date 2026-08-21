---
schema: 2
id: TASK-050204
title: The port and the query — one ordered page of the season's ladder, narrowed by nothing else
type: task
status: backlog
parent: STORY-0502
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, db, leaderboard, sql]
depends_on: [TASK-050203]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresStandingsReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - "! grep -qiE 'having|dense_rank|coin_balance' poker-server/src/main/kotlin/duels/poker/server/db/PostgresStandingsReads.kt"
  - test -z "$(git diff --name-only $(git merge-base HEAD origin/develop) -- poker-engine web-client poker-server/src/main/resources/db/migration)"
---

## Goal

There is a port that answers *one page of the season's ladder as it stood at a cutoff*, and a
PostgreSQL implementation of it that sums the ledger over the window, ranks against the whole
ladder, and orders the page the one way `ADR-0066` §3 permits.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsReads.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresStandingsReads.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresStandingsReadsTest.kt` | create |

Read if needed, and **not** in the budget: `PostgresProfileReads.kt` is the worked example for the
JDBC shape, the `DUELS_AFTER_SQL` row-value idiom and the companion-object SQL constants.

## Scope

- The port, in `duels.poker.server.http`, with exactly one method for now:

  ```kotlin
  public interface StandingsReads {
      public suspend fun standingsPage(
          season: Season,
          asOf: Instant,
          limit: Int,
          after: StandingsCursor? = null,
      ): List<StandingRow>
  }
  ```

  It returns the wire type for the reason `ProfileReads`' KDoc gives: the answer's shape *is* the
  wire's shape, and a parallel domain type would be a copy nobody reads. `standingOf` — the
  requester's own aggregate — is `TASK-050208` and is not declared here.
- The implementation is `PostgresStandingsReads(dataSource)` in `duels.poker.server.db`, built like
  `PostgresProfileReads`: `withContext(Dispatchers.IO)`, `dataSource.connection.use`, SQL in a
  private companion object, no JDBC type escaping the port.
- **The SQL, from `ADR-0066` §1 and §3.** Two constants sharing one body, exactly as
  `RECENT_DUELS_SQL`/`DUELS_AFTER_SQL` do:

  ```sql
  WITH standing AS (
      SELECT dr.player_id AS player_id, SUM(dr.coin_delta)::int AS coins
      FROM duel_result dr
      JOIN duel d ON d.id = dr.duel_id
      WHERE d.finished_at >= ?::timestamptz
        AND d.finished_at <  ?::timestamptz
      GROUP BY dr.player_id
  ),
  ranked AS (
      SELECT s.player_id, s.coins, rank() OVER (ORDER BY s.coins DESC) AS rank
      FROM standing s
  )
  SELECT r.rank, r.player_id, p.display_name, r.coins
  FROM ranked r
  JOIN player p ON p.id = r.player_id
  ORDER BY r.coins DESC, r.player_id DESC
  LIMIT ?
  ```

  The `after` variant adds `WHERE (r.coins, r.player_id) < (?::int, ?::uuid)` before the
  `ORDER BY` — one row-value comparison, the idiom `DUELS_AFTER_SQL` already uses and the reason
  `ADR-0066` §3 runs both components `DESC`.
- **Two levels of CTE on purpose.** `rank()` is a window function and cannot appear in the `WHERE`
  of the select that computes it, so the page predicate filters the `ranked` CTE from outside. This
  is also what makes the rank a function of the **whole ladder** rather than of the page
  (`ADR-0064` §1, `ADR-0066` §5).
- The lower bound is `season.start`, the upper bound is **`asOf`**, and the window is half-open —
  `ADR-0066` §2: a duel finishing exactly at the cutoff belongs to the next walk. `season.endExclusive`
  is **not** the upper bound and does not appear in this file.
- **No other `WHERE` exists.** `ADR-0063` §1: no minimum duels, no minimum standing, no account, no
  display name, no profile age. A `verify:` line greps for `having`; a predicate that appears here
  is a defect and not a refinement.

## Out of scope

- **The self standing** — `TASK-050208` adds the second method and the second statement.
- **Ranks, ties and the tests that pin them** — `TASK-050205`. This ticket ships `rank()` in the SQL
  because the column is part of the shape, and asserts only that the page comes back in coin order
  within the window.
- **The route, the cutoff and the probe row** — `TASK-050209`. This port has no `Clock` and mints
  nothing; `asOf` arrives from the caller.
- **A migration or an index.** `ADR-0066` §8 names an index and does not write one; a `verify:` line
  fails if any file under `src/main/resources/db/migration` changes.
- **Touching `PostgresProfileReads` or its tests.** They are read-only here.

## Tests

`PostgresStandingsReadsTest`, in `duels.poker.server.db`, `-PrequireDocker=true`. Set up with
`PostgresTestSupport.freshDatabase()` + `Migrations.migrate`, players via `PostgresPlayerDirectory`,
duels via `PostgresDuelResultStore.record(FinishedDuel(...))` — the fixture helper
`DuelHistoryPagingDatabaseTest.finishedDuel` is the model to copy locally.

**`@BeforeEach` builds the database, the directory and the store, and records no duel.** Every test
in this class records its own, because the tickets that follow (`TASK-050205`, `TASK-050206`,
`TASK-050207`) each need a differently shaped ladder and a shared fixture would have to be rewritten
by all three.

| Test | Proves |
| --- | --- |
| `playersComeBackInCoinOrder` | over a fixture holding one positive, two zero and one negative standing, the returned `coins` are `[2, 0, 0, -2]` in that order, the first row is alice and the last is bob |
| `theWindowExcludesTheNeighbouringSeasonInBothDirections` | three duels between three disjoint pairs — `2026-07-31T23:59:59.999Z`, `2026-08-15T12:00:00Z`, `2026-09-01T00:00:00Z` — read with `Season(2026, 8)` and `asOf = season.endExclusive` return exactly the two August players and nobody else |

**The fixture is deliberately not in coin order** — this is the whole value of the first test.
Create the players in the order `bob, carol, dave, alice`, and record the duels in this order:

1. carol draws dave (`ADR-0015` writes two rows of `0`);
2. alice beats bob;
3. alice beats bob again.

Standings are then alice `+2`, carol `0`, dave `0`, bob `-2`, which is neither the creation order
nor the recording order. A fixture that arrives already sorted cannot fail a mutation to the
`ORDER BY`, which is why this one does not.

**Named mutations.** `ORDER BY r.coins ASC` reddens `playersComeBackInCoinOrder`. Dropping
`d.finished_at >= ?` reddens `theWindowExcludesTheNeighbouringSeasonInBothDirections` by admitting
the July pair; dropping `d.finished_at < ?` admits the September pair and reddens it too. Asserting
the coins **sequence** rather than the player sequence is deliberate: the two players at `0` are a
tie, and the order among equals is not this test's business (`ADR-0064` §3).

## Acceptance criteria

- [ ] `PostgresStandingsReadsTest.playersComeBackInCoinOrder` passes, asserting the coins sequence
      `[2, 0, 0, -2]` and alice first, bob last
- [ ] `PostgresStandingsReadsTest.theWindowExcludesTheNeighbouringSeasonInBothDirections` passes,
      with all three duels recorded and only the August pair returned
- [ ] `StandingsReads` declares exactly one method and no implementation of it lives outside
      `duels.poker.server.db`
- [ ] `PostgresStandingsReads.kt` contains no `HAVING`, no `dense_rank` and no `coin_balance`
- [ ] No file under `poker-server/src/main/resources/db/migration`, `poker-engine` or `web-client`
      changes in this branch
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
