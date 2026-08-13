---
schema: 2
id: TASK-021014
title: The duel table records how many hands were played
type: task
status: ready
parent: STORY-0210
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, persistence, migration, schema]
depends_on: [TASK-021013]
verify:
  - ./gradlew :poker-server:test --tests '*MigrationsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultStoreTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`ADR-0019`: the `duel` table gains a `hands_played` column, and the write path stores it.

**This has a deadline, which is why it is `ready` rather than queued.** `DuelOutcome.handsPlayed`
exists only at the moment a duel finishes. A column added after the first real duel cannot be
backfilled — every duel played before it exists would show a blank forever, in the recent-duels
list the project owner asked for. The table is empty today, so the cost is one migration; tomorrow
the cost is data that no longer exists.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/resources/db/migration/V2__duel_hands_played.sql` | create |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultStore.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreTest.kt` | modify |

Read `V1__initial_schema.sql` and `FinishedDuel.kt`. **`V1` is never edited** — migrations are
immutable, which is why `ADR-0015` had to correct a wrong comment in an ADR rather than in the file.

## Scope

- `V2` adds `hands_played INTEGER NOT NULL` to `duel`. **`NOT NULL` with no default**: every
  completed duel played some number of hands, and a nullable column would push "did this duel
  record it?" onto every reader forever. There are no existing rows, so no backfill value is
  needed — if the migration fails because rows exist, stop and report rather than inventing a
  default, because that means the deadline in `ADR-0019` has already passed.
- The insert in `PostgresDuelResultStore` binds `duel.outcome.handsPlayed`.
- Nothing else changes. The transaction, the coin increments, the `ON CONFLICT` idempotency and the
  rollback are all deep-reviewed and mutation-proven — this adds one column and one bind parameter.

## Tests

| Name | Asserts |
| --- | --- |
| `bothMigrationsApplyToAnEmptyDatabase` | `flyway_schema_history` records versions `1` **and** `2` — the chain works with more than one file, which nothing has proved yet |
| `theStoredDuelRecordsItsHandCount` | a duel finishing after N hands stores `hands_played = N`, read back with a fresh `SELECT` |
| `aSecondMigrationRunIsStillANoOp` | re-running applies zero migrations, as `MigrationsTest` already asserts for `V1` alone |

The first is worth as much as the second: `V2` is the first evidence that this project's migration
chain works at all beyond a single file.

## Out of scope

- The read path and the DTO — `TASK-021114`. `handsPlayed` stays `null` on the wire until that
  lands, and `handsPlayedIsNullWhileTheColumnDoesNotExist` stays passing until then.
- Final stacks. `DuelOutcome` carries them, nobody has asked to display them, and `ADR-0019`
  deliberately left them out.

## Done

All three `verify:` commands exit 0, both migrations apply to an empty database, and a stored duel
carries its hand count.
