---
schema: 2
id: TASK-040622
title: The backfill moves a row that was already there
type: task
status: backlog
parent: STORY-0406
module: poker-server
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, migration]
depends_on: [TASK-040601]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.MigrationsTest' -PrequireDocker=true
  - ./gradlew :poker-server:detekt
---

## Goal

`V7`'s backfill — `INSERT INTO device_binding (...) SELECT ... FROM player` — must be proven to move
an existing row, with its device id intact, before `player` loses the column.

## Why this exists

`TASK-040601` moved the device binding into a table of its own. The migration is correct: the
backfill is right SQL and correctly ordered before `DROP COLUMN`, inside Flyway's transaction. The
deep review checked all of that.

What it also found is that **nothing ever runs it against data.** Every database test calls
`PostgresTestSupport.freshDatabase()` and then `Migrations.migrate()`, which applies V1→V7 to an
empty schema in one shot, so the backfill always selects zero rows. `MigrationsTest` bootstraps an
empty database in both of its tests. The `SELECT` could be `WHERE false`, or name the wrong column,
and every test in the repository would still pass.

That is tolerable while there is no production data and intolerable the moment there is — and the
migration is permanent, so the day it matters is the day it runs unobserved. `TASK-040601` named this
out of scope explicitly ("the gate is the thirteen merged classes plus the one added assertion"), so
it is this ticket.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/MigrationsTest.kt` | modify |

## Scope

One test. Migrate to **V6** only, insert a `player` row carrying a known device id, migrate to
**V7**, then assert:

- `device_binding` holds exactly one row
- its `device_id` is the one that was inserted, and its `player_id` is that player's id
- the player row still exists, and `SELECT count(*) FROM player` is unchanged

Flyway can target a version — use whatever mechanism `Migrations` already exposes, or its
configuration directly. If reaching V6 alone is genuinely impossible with the current helper, say so
in the PR rather than weakening the test to migrate everything and assert nothing.

## Out of scope

- Changing `V7__device_binding.sql`. The migration is correct; this ticket makes that a fact a gate
  holds.
- Changing `PostgresTestSupport.freshDatabase()`, which the other database tests depend on.
- Backfilling anything else. Only the device binding moved.

## Tests

| Test | Proves |
| --- | --- |
| `theDeviceBindingBackfillCarriesAnExistingRow` | a `player` row written under V6 arrives in `device_binding` with its device id and player id intact, and the player row survives |

## Acceptance criteria

- [ ] The new test exists and passes.
- [ ] Both pre-existing `MigrationsTest` tests are unchanged.
- [ ] `V7__device_binding.sql` is not edited.
- [ ] Every command in `verify:` exits 0.

## Proof

Change `V7`'s backfill `SELECT` to `WHERE false` — the new test goes red on the row count while both
pre-existing `MigrationsTest` tests stay green. Revert. **Before this ticket that mutation turns
nothing red anywhere in the repository**, which is the whole reason it exists.

**Run it.** Nine `## Proof` sections in this run were wrong or incomplete when actually executed,
including one describing an edit that could not change behaviour at all.
