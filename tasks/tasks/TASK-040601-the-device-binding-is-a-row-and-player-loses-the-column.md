---
schema: 2
id: TASK-040601
title: The device binding becomes a row of its own, and player loses its column
type: task
status: ready
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 15
atomic:
  - the PostgreSQL schema — V7 drops player.device_id, so every statement naming that column fails at test runtime the moment the migration lands, and every one of them is in this list
  - the poker-server test task — thirteen merged test classes write INSERT INTO player (id, device_id, ...) or read a dropped column; measured by the ADR-0070 probe, not by reading imports
  - ADR-0049 §8 — the backfill must read player.device_id in the same transaction that drops it, so the table, the copy and the drop are one migration file and one commit
labels: [server, schema, migration, identity, security]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresPlayerDirectoryTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.SchemaConstraintsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.MigrationsTest' -PrequireDocker=true
  - ./gradlew check -PrequireDocker=true
---

## Goal

The device→profile edge lives in a `device_binding` table with two partial unique indexes and a
finality trigger, `player` has no device column at all, and `PostgresPlayerDirectory` reads and
writes the new table.

> **Why one commit.** `V7` backfills `device_binding` from `player.device_id` and then drops that
> column, inside the one transaction Flyway wraps a migration in. There is no intermediate state
> where the tree compiles and the schema matches: before the drop, `resolve`'s new statement names a
> table that does not exist; after it, thirteen merged test classes insert into a column that does
> not exist. `ADR-0049` §8 says so in as many words, and its *The deadline, honestly* is why the
> drop is not deferred.

## Files

Fifteen, **measured** by the `ADR-0070` probe: stub the migration and the directory, run the
commands `.github/workflows/build.yml` runs on a pull request in full, add each path the run names,
re-run to `exit 0`. The first red run named thirteen classes and was a *prefix* — `:poker-server:test`
hides `ktlintCheck`, `detekt` and the client job behind it. The loop settled at fifteen, and both
`./gradlew check -PrequireDocker=true` and `npm ci && npm run check && npm run build` then exited 0.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/resources/db/migration/V7__device_binding.sql` | create | the schema change itself; `V1`–`V6` are immutable |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresPlayerDirectory.kt` | modify | the schema — `resolve`'s upsert and `findOrNull`'s `SELECT` both name `player.device_id` |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresPlayerDirectoryTest.kt` | modify | `:poker-server:test` — two device-keyed `SELECT`s against `player` |
| `poker-server/src/test/kotlin/duels/poker/server/db/SchemaConstraintsTest.kt` | modify | `:poker-server:test` — its `insertPlayer` names the column, and one test asserts the constraint name `player_device_id_unique` |
| `poker-server/src/test/kotlin/duels/poker/server/db/AuthSessionSchemaTest.kt` | modify | `:poker-server:test` — one `INSERT INTO player (id, device_id, coin_balance)` |
| `poker-server/src/test/kotlin/duels/poker/server/db/CoinBalanceIsSignedTest.kt` | modify | `:poker-server:test` — the same insert |
| `poker-server/src/test/kotlin/duels/poker/server/db/CredentialSchemaTest.kt` | modify | `:poker-server:test` — the same insert |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresCredentialsTest.kt` | modify | `:poker-server:test` — the same insert |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresCredentialsEnumerationTest.kt` | modify | `:poker-server:test` — the same insert |
| `poker-server/src/test/kotlin/duels/poker/server/db/PlayerNameIsRegisteredTest.kt` | modify | `:poker-server:test` — the same insert, with a literal balance |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNameSchemaTest.kt` | modify | `:poker-server:test` — three such inserts |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNamePermanenceTest.kt` | modify | `:poker-server:test` — two such inserts |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNameUniquenessTest.kt` | modify | `:poker-server:test` — two such inserts |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresAuthSessionsTest.kt` | modify | `:poker-server:test` — one insert, a `SELECT … device_id …` snapshot, its `PlayerRow.deviceId` field and the one assertion on it |
| `poker-server/src/test/kotlin/duels/poker/server/season/SeasonMovesNoCoinTest.kt` | modify | `:poker-server:test` — one such insert |

Read, and do not edit:
`docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md` §§1–4 and §8,
`docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §2,
`poker-server/src/main/resources/db/migration/V1__initial_schema.sql`,
`poker-server/src/main/resources/db/migration/V5__name_registry.sql` (the trigger register to copy).

`SignUpDatabaseTest` is **not** in this list and needs no edit: its `snapshot` helper reads column
names from `ResultSetMetaData` rather than a hard-coded list, and its own KDoc says it was written
that way for this migration. The probe confirmed it.

## Scope — the migration

`V7__device_binding.sql`, in this order, exactly as `ADR-0049` §1, §2 and §8 give it:

1. `CREATE TABLE device_binding (device_id TEXT NOT NULL, player_id UUID NOT NULL REFERENCES player (id), bound_at TIMESTAMPTZ NOT NULL DEFAULT now(), revoked_at TIMESTAMPTZ, CONSTRAINT device_binding_pkey PRIMARY KEY (device_id, player_id))`.
   **No `ON DELETE` clause and no `CHECK`**, matching `V4` and `V5`.
2. `CREATE UNIQUE INDEX device_binding_live_device ON device_binding (device_id) WHERE revoked_at IS NULL`
   and `CREATE UNIQUE INDEX device_binding_live_player ON device_binding (player_id) WHERE revoked_at IS NULL`.
3. `INSERT INTO device_binding (device_id, player_id, bound_at) SELECT device_id, id, created_at FROM player`.
4. `ALTER TABLE player DROP CONSTRAINT player_device_id_unique;` then
   `ALTER TABLE player DROP COLUMN device_id;` — **after** the backfill, or the copy has nothing to
   read.
5. `CREATE FUNCTION device_binding_revocation_is_final()` raising
   `'a revoked device binding is final (ADR-0049)'` with `ERRCODE = 'restrict_violation'` when
   `OLD.revoked_at IS NOT NULL AND NEW.revoked_at IS DISTINCT FROM OLD.revoked_at`, and
   `CREATE TRIGGER device_binding_revocation_final BEFORE UPDATE OF revoked_at ON device_binding FOR EACH ROW`.
   The `OF revoked_at` clause is not decoration — `TASK-040603` pins it.

Comments in the file say *why*, in `V5`'s register, and cite `ADR-0049` §1 and §2.

## Scope — the directory

`PostgresPlayerDirectory`, and nothing else under `src/main`:

- `findOrNull` becomes `SELECT player_id FROM device_binding WHERE device_id = ? AND revoked_at IS NULL`.
- `resolve` is that same read, and on a miss a mint **in an explicit transaction**:

  ```sql
  WITH minted AS (INSERT INTO player (id) VALUES (?) RETURNING id)
  INSERT INTO device_binding (device_id, player_id)
  SELECT ?, id FROM minted
  ON CONFLICT (device_id) WHERE revoked_at IS NULL DO NOTHING
  RETURNING player_id
  ```

- **If that statement returns no row, the transaction is rolled back and the read is re-run.** This
  is the defect `ADR-0049` §4 exists to name: `ON CONFLICT … DO NOTHING` is a *success*, so under
  autocommit the CTE's `INSERT INTO player` commits while the binding does not, leaving an orphan
  profile on every contended first contact. `autoCommit = false`, `commit()` only on a returned row,
  `rollback()` otherwise and in a `catch`, `autoCommit = true` in a `finally` — the shape
  `PostgresProfileWrites.writeName` already uses.
- The re-read is a bounded loop, not recursion and not `while (true)`: a conflict is only observed
  after the concurrent inserter has committed or aborted, so the next pass finds the winner. Give it
  a small constant ceiling and `error(...)` past it.
- `Player`, `PlayerId`, `DeviceId` and the `PlayerDirectory` signatures are **unchanged**
  (`ADR-0049` §1's last bullet).

## Scope — the thirteen test files

Mechanical, and every one of them is propagation rather than a decision:

- `INSERT INTO player (id, device_id, …)` loses its `device_id` column and its bind, and the
  remaining parameter indices shift down by one. A helper's now-unused `deviceId` parameter goes
  with it, unless the file still needs it (see `SchemaConstraintsTest` below).
- `PostgresPlayerDirectoryTest`'s two device-keyed selects read `device_binding` instead:
  `SELECT device_id FROM device_binding WHERE player_id = ? AND revoked_at IS NULL` and
  `SELECT player_id FROM device_binding WHERE device_id = ? AND revoked_at IS NULL`.
- `SchemaConstraintsTest`'s `insertPlayer` keeps its `deviceId` parameter and gains a second
  statement writing the live `device_binding` row, so `aSecondProfileForOneDeviceIdIsRejected` still
  has something to collide with; that test's expected constraint name becomes
  `device_binding_live_device`, which `ADR-0049` §1 says *"replaces `player_device_id_unique`
  exactly"*.
- `PostgresAuthSessionsTest` loses `PlayerRow.deviceId`, the `device_id` column in its snapshot
  `SELECT`, and the one assertion that compared it. **This is the ticket owning a test its change
  invalidates**: that assertion observes a column this migration deletes, so it cannot be left
  standing. Nothing else in that file moves and no other assertion is weakened — the
  session-round-trip assertions beside it are untouched.

## Scope — one new assertion

`PostgresPlayerDirectoryTest.concurrentFirstContactFromManyConnectionsCreatesOneProfile` gains one
line: after the sixteen concurrent resolves, `SELECT count(*) FROM device_binding` reads `1`.

`ADR-0049` §4 says the current test *"asserts a single player id and would pass while littering"* —
**that is not accurate about the file on `develop`, which already asserts `playerRowCount() == 1`**,
and this ticket says so rather than repeating it. The player count is what catches the
orphan-profile defect; the binding count is what this ticket adds, and it catches the opposite
mistake — sixteen bindings under one profile.

## Out of scope

- Tests for the trigger, the two indexes and the pair primary key — `TASK-040603` and `TASK-040604`.
- What `resolve` does for a *revoked* device — `TASK-040605`. Nothing in this ticket revokes
  anything, and no `UPDATE … SET revoked_at` appears in any file it touches.
- `ProfileResponse.deviceRouteLive` — `TASK-040602`.
- `PostgresProfileReads` and `PostgresProfileWrites`. `STORY-0405` already made both player-keyed;
  the probe confirmed neither compiles or runs any differently after this migration.
- Adding `device_binding` to `SchemaConstraintsTest.theSchemaHasTheThreeTables`. That test asserts
  `contains`, not equality, so it passes unchanged, and widening it is not this ticket's diff.

## Tests

No new test class. The gate is the thirteen merged classes above going green against the new schema,
plus the one added assertion.

| Test | Proves |
| --- | --- |
| `PostgresPlayerDirectoryTest.concurrentFirstContactFromManyConnectionsCreatesOneProfile` | Sixteen concurrent `resolve` calls for one device id leave **one** `player` row (already asserted) and **one** `device_binding` row (added here) — the orphan-profile defect `ADR-0049` §4 names, in both directions |
| `PostgresPlayerDirectoryTest.findingAnUnknownDeviceCreatesNothing` | Unchanged and load-bearing: `findOrNull` is now a plain read of `device_binding` and still writes nothing, twice in a row |
| `SchemaConstraintsTest.aSecondProfileForOneDeviceIdIsRejected` | One live binding per device is still refused by the database, now by `device_binding_live_device` |
| `MigrationsTest.everyMigrationAppliesToAnEmptyDatabase` | `V7` applies to an empty database and is recorded in `flyway_schema_history` in order |
| `MigrationsTest.aSecondRunAppliesNothing` | `V7` is not re-applied on a second `migrate` |

## What red looks like here

**Every command in `verify:` already exits 0 on `develop`**, and that is structural rather than an
oversight: this ticket adds no test class, so its gate is *the merged suite still passes against a
changed schema*. The red run that matters is the intermediate one, and it was measured during the
probe — `V7` plus the rewritten directory, with none of the thirteen test files touched, gives
`./gradlew check -PrequireDocker=true` **exit 1** and thirteen failing classes, every one of them a
`PSQLException` naming `device_id`. If a coder sees fewer than thirteen, the migration did not
apply; if they see more, the *Files* table is short and `ADR-0070` §4 says to add the row, name its
gate, move `files_touched`, and quote the failure in the PR body.

## Acceptance criteria

- [ ] `./gradlew check -PrequireDocker=true` exits 0
- [ ] `grep -rln "device_id" poker-server/src/main` names exactly three files:
      `V1__initial_schema.sql` (immutable, and still describes the column it created),
      `V7__device_binding.sql`, and `PostgresPlayerDirectory.kt` — whose occurrences are all
      `device_binding`'s own `device_id` column
- [ ] `grep -rn "INSERT INTO player (id, device_id" poker-server/src` finds nothing at all
- [ ] `grep -rn "FROM player WHERE device_id" poker-server/src` finds nothing at all
- [ ] `concurrentFirstContactFromManyConnectionsCreatesOneProfile` asserts **both**
      `SELECT count(*) FROM player` is `1` and `SELECT count(*) FROM device_binding` is `1`
- [ ] `PostgresPlayerDirectory.resolve` contains `autoCommit = false`, exactly one `commit()`, and at
      least one `rollback()`
- [ ] `SchemaConstraintsTest.aSecondProfileForOneDeviceIdIsRejected` asserts `23505` and the
      substring `device_binding_live_device`
- [ ] `V1`–`V6` are byte-identical to `develop`: the only file added or changed under
      `poker-server/src/main/resources/db/migration` is `V7__device_binding.sql`
- [ ] The diff against `develop` names exactly the fifteen files in the *Files* table, plus this
      ticket and `tasks/BOARD.md`
- [ ] Every command in `verify:` exits 0

## Proof

Delete the `rollback()` on the no-row branch of `resolve`'s mint and leave the re-read, so the CTE's
`INSERT INTO player` commits while its binding conflicts away.
`concurrentFirstContactFromManyConnectionsCreatesOneProfile` reddens on
`SELECT count(*) FROM player`, which reads above `1`. The binding count stays at `1`, and every
other test in the class stays green — a single-threaded `resolve` never reaches the conflict branch.
**One test, one assertion**, and it is the merged assertion rather than the one this ticket adds:
the added binding count catches the mirror-image defect, not this one.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
