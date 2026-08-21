---
schema: 2
id: TASK-050219
title: Nothing stores a standing — no table, no column, no materialised view, and no migration
type: task
status: done
parent: STORY-0502
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [server, db, leaderboard, guard, tests]
depends_on: [TASK-050218]
verify:
  - ./gradlew :poker-server:test --tests '*NothingStoresAStandingTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultStoreTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - test -z "$(git diff --name-only $(git merge-base HEAD origin/develop) -- poker-server/src/main/resources/db/migration poker-engine web-client)"
---

## Goal

`ADR-0066` §1's *"nothing stores one"* is a command rather than a sentence: the migrated schema holds
no standing and no season anywhere, and it will fail the build on the day somebody adds one.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/NothingStoresAStandingTest.kt` | create |
| `poker-server/src/main/resources/db/migration/V1__initial_schema.sql` | read |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt` | read |

## Scope

- One Testcontainers test class that migrates a fresh database and interrogates the catalogue
  through plain SQL:
  - `information_schema.tables` in the `public` schema holds **no** table whose name contains
    `standing` or `season`, case-insensitively;
  - `information_schema.columns` in the `public` schema holds **no** column whose name contains
    `standing` or `season`;
  - `pg_matviews` is empty — there is no materialised view of any kind.
- The failure message names what it found, so the day it fires the reader is told which object
  appeared rather than that a count was wrong.
- **This test is not about this story's branch.** It is a durable guard: it runs on every CI build
  from here on, and it is the criterion a materialised ladder, a summary column or a refresh job
  fails.

## Out of scope

- **Forbidding migrations in general.** `EPIC-04` has unlanded ones and any `V<n>__` number is
  claimed at merge time; the `verify:` line here only asserts that **this** branch adds none.
- **An index for the season window.** `ADR-0066` §8 names that ticket and deliberately does not
  write it — a permanent write cost on the one transaction where a coin moves, added on imagination,
  with nothing in this product ever timed. It is **not** part of `STORY-0502` and is not a
  prerequisite for it; whoever writes it carries an `EXPLAIN`-backed measurement in its `verify:`
  block and claims its `V<n>__` number then.
- **Changing `PostgresDuelResultStore` or its tests.** They are in `verify:` to prove the
  duel-recording transaction still writes exactly what it wrote before this story: one `duel` row,
  two `duel_result` rows, two balance moves.
- A cache, a pre-render or a snapshot held across requests — `ADR-0066` §8 refuses all three by name
  and this ticket adds none.

## Tests

`NothingStoresAStandingTest`, in `duels.poker.server.db`, `-PrequireDocker=true`.

| Test | Proves |
| --- | --- |
| `theSchemaHoldsNoStandingAndNoSeason` | no table and no column in `public` names a standing or a season — the two things `ADR-0061` §3 and `ADR-0066` §1 say are derived and never written |
| `theSchemaHoldsNoMaterialisedView` | `pg_matviews` is empty |

**Named mutations.** A `season_standing` table, a `player.season_coins` column or a
`CREATE MATERIALIZED VIEW ladder` in any future migration reddens one of the two, whatever it is
called, because the assertions match on substrings rather than on a list of known names.

## Acceptance criteria

- [ ] `NothingStoresAStandingTest.theSchemaHoldsNoStandingAndNoSeason` passes and its failure message
      names the offending object
- [ ] `NothingStoresAStandingTest.theSchemaHoldsNoMaterialisedView` passes
- [ ] `PostgresDuelResultStoreTest` passes with its assertions unchanged and
      `PostgresDuelResultStore.kt` is not modified in this branch
- [ ] This branch adds no file under `poker-server/src/main/resources/db/migration`, `poker-engine`
      or `web-client`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
