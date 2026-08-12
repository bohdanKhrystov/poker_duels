---
schema: 2
id: TASK-020906
title: Prove the schema refuses a duplicate device id and a duplicate result row
type: task
status: blocked
parent: STORY-0209
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, persistence, schema]
depends_on: [TASK-020904]
verify:
  - ./gradlew :poker-server:test --tests '*SchemaConstraintsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---


> **Blocked: no Docker on the build machine (2026-08-13).** This ticket's `verify:` block carries
> `-PrequireDocker=true`, which is deliberate — it means a machine without Docker cannot honestly
> close a database ticket. Docker is not installed here (`docker` binary absent, no
> `/var/run/docker.sock`), so the block cannot exit 0 and the ticket is not done. The implementation
> may be complete; the verification is not. Unblock by installing Docker, or by running this ticket
> on CI.

## Goal

The uniqueness and referential rules that `STORY-0210` will rely on are asserted against the real
database: one profile per device id, one result row per `(duel, player)`, and no result row for a
duel that does not exist.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/SchemaConstraintsTest.kt` | create |

Read, do not modify: `poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt`,
`poker-server/src/main/resources/db/migration/V1__initial_schema.sql`,
`poker-server/src/main/kotlin/duels/poker/server/db/Migrations.kt`.

## Scope

- One test class, `SchemaConstraintsTest`, package `duels.poker.server.db`, JUnit 5, plain JDBC.
- `@BeforeEach`: `dataSource = PostgresTestSupport.freshDatabase()` then
  `Migrations.migrate(dataSource)`.
- Assert violations on **`SQLException.sqlState`**, not on the message text: `23505` is
  `unique_violation`, `23503` is `foreign_key_violation`. A message assertion breaks on a
  PostgreSQL upgrade; an SQLState does not. Additionally assert that the message contains the
  constraint name, which is why `TASK-020904` named the constraints.
- Same private insert helpers as `TASK-020905` — `insertPlayer(deviceId, coinBalance)` and
  `insertDuel()`. Duplicating five lines here is correct; do not reach into the other test class
  and do not add a shared fixture file.
- `assertThrows<SQLException>` around each failing insert, then assert on the caught exception.

## Out of scope

- Signed and negative values — `TASK-020905`.
- `ON CONFLICT` upserts, retries, or any recovery from a violation — `STORY-0210` decides how the
  caller reacts; this ticket only pins that the database refuses.
- Concurrency: two connections racing to insert the same device id is `STORY-0210`'s acceptance
  criterion, not this one's.
- Editing `V1__initial_schema.sql`. Merged migrations are immutable; a missing constraint is a
  finding to report.

## Tests

`SchemaConstraintsTest`

| Test | Proves |
| --- | --- |
| `theSchemaHasTheThreeTables` | `SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'` contains exactly `player`, `duel`, `duel_result` alongside Flyway's `flyway_schema_history` |
| `aSecondProfileForOneDeviceIdIsRejected` | inserting a second `player` with the same `device_id` throws `SQLException` with `sqlState == "23505"`, message naming `player_device_id_unique` |
| `aSecondResultRowForOneDuelAndPlayerIsRejected` | inserting `duel_result` twice for one `(duel_id, player_id)` throws `SQLException` with `sqlState == "23505"`, message naming `duel_result_pkey` |
| `aResultRowForAnUnknownDuelIsRejected` | a `duel_result` whose `duel_id` is a random unused `UUID` throws `SQLException` with `sqlState == "23503"` |

## Acceptance criteria

- [ ] `SchemaConstraintsTest.theSchemaHasTheThreeTables` passes
- [ ] `SchemaConstraintsTest.aSecondProfileForOneDeviceIdIsRejected` passes
- [ ] `SchemaConstraintsTest.aSecondResultRowForOneDuelAndPlayerIsRejected` passes
- [ ] `SchemaConstraintsTest.aResultRowForAnUnknownDuelIsRejected` passes
- [ ] Every violation assertion checks `sqlState`, not only that something was thrown
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
