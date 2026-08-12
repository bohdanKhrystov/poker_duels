---
schema: 2
id: TASK-020904
title: Create the initial schema and apply it with Flyway
type: task
status: done
parent: STORY-0209
module: poker-server
estimate: S
tier: haiku
review: deep
files_touched: 3
labels: [server, persistence, schema, migrations]
depends_on: [TASK-020903]
verify:
  - ./gradlew :poker-server:test --tests '*MigrationsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---


> **Blocked: no Docker on the build machine (2026-08-13).** This ticket's `verify:` block carries
> `-PrequireDocker=true`, which is deliberate — it means a machine without Docker cannot honestly
> close a database ticket. Docker is not installed here (`docker` binary absent, no
> `/var/run/docker.sock`), so the block cannot exit 0 and the ticket is not done. The implementation
> may be complete; the verification is not. Unblock by installing Docker, or by running this ticket
> on CI.

## Goal

`V1__initial_schema.sql` creates the profile, duel and duel-result tables, and
`Migrations.migrate(dataSource)` applies every pending migration and reports how many it ran.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/resources/db/migration/V1__initial_schema.sql` | create |
| `poker-server/src/main/kotlin/duels/poker/server/db/Migrations.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/MigrationsTest.kt` | create |

Read, do not modify: `poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt`.

## Scope

- **The migration rule, stated in the file and enforced by review: a merged migration is
  immutable.** Flyway records each file's checksum in `flyway_schema_history`; once `V1` has run
  anywhere, editing it makes that record a lie and the next startup fails a validation it is right
  to fail. Every later change — a column, an index, a table — is a new `V<n>__` file. Nothing in
  this repository ever edits a merged migration.
- `V1__initial_schema.sql`, exactly this schema. `ADR-0011` enumerates the durables — profiles,
  duel results, coin balances — and the tables serve those and nothing more:

  ```sql
  CREATE TABLE player (
      id           UUID        PRIMARY KEY,
      device_id    TEXT        NOT NULL,
      coin_balance INTEGER     NOT NULL DEFAULT 0,
      created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
      CONSTRAINT player_device_id_unique UNIQUE (device_id)
  );

  CREATE TABLE duel (
      id          UUID        PRIMARY KEY,
      format      TEXT        NOT NULL,
      started_at  TIMESTAMPTZ NOT NULL,
      finished_at TIMESTAMPTZ NOT NULL
  );

  CREATE TABLE duel_result (
      duel_id    UUID    NOT NULL REFERENCES duel (id),
      player_id  UUID    NOT NULL REFERENCES player (id),
      coin_delta INTEGER NOT NULL,
      CONSTRAINT duel_result_pkey PRIMARY KEY (duel_id, player_id)
  );
  ```

- `coin_balance` and `coin_delta` are **signed `INTEGER`s with no `CHECK` constraint**
  ([`ADR-0014`](../../docs/adr/ADR-0014-duel-coin-economy.md)): a balance is `wins − losses` and a
  new profile whose only duel was a loss reads back `−1`. A `CHECK (coin_balance >= 0)` here is
  the defect this ticket exists to prevent. Say so in a comment above each column, citing
  `ADR-0014`.
- `Migrations.kt`, package `duels.poker.server.db`, one top-level `public object Migrations`:

  ```kotlin
  public fun migrate(dataSource: DataSource): Int =
      Flyway.configure()
          .dataSource(dataSource)
          .locations("classpath:db/migration")
          .load()
          .migrate()
          .migrationsExecuted
  ```

- KDoc on `Migrations` and on `migrate` carrying the immutability rule above and the fact that
  applying an already-migrated database is a no-op that returns `0`.

## Out of scope

- A display name, a rating, a room, a session or a `MatchLog` column. `DEC-008` is open on whether
  the match log is persisted at all; leave the `duel` table free of it.
- Indexes beyond the keys declared above. Add one when a query in `STORY-0211` needs it.
- Any repository, DAO or insert/select helper — `STORY-0210` owns the write path.
- Calling `migrate` at startup — `TASK-020908`.
- Assertions about constraints and signedness — `TASK-020905` and `TASK-020906` own those, so
  keep this ticket's test to the migration mechanism.
- `Flyway.clean()`, `baselineOnMigrate` or repeatable (`R__`) migrations.

## Tests

`MigrationsTest`, JUnit 5, package `duels.poker.server.db`. Each test starts from
`PostgresTestSupport.freshDatabase()`, which gates on Docker and hands back an empty schema.

| Test | Proves |
| --- | --- |
| `appliesEveryMigrationToAnEmptyDatabase` | `Migrations.migrate(freshDatabase())` returns at least 1, and `flyway_schema_history` then holds a row for version `1` |
| `aSecondRunAppliesNothing` | after a first `migrate`, a second `migrate` on the same `DataSource` returns exactly `0` |

## Acceptance criteria

- [ ] `MigrationsTest.appliesEveryMigrationToAnEmptyDatabase` passes
- [ ] `MigrationsTest.aSecondRunAppliesNothing` passes
- [ ] `V1__initial_schema.sql` contains no `CHECK` constraint and no unsigned or `BIGINT UNSIGNED`
      style column; `coin_balance` and `coin_delta` are both `INTEGER NOT NULL`
- [ ] The migration lives at `poker-server/src/main/resources/db/migration/V1__initial_schema.sql`
      and is the only file in that directory
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
