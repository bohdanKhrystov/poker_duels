---
schema: 2
id: TASK-040301
title: The fourth migration adds the credential and the auth session
type: task
status: ready
parent: STORY-0403
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, schema, migration, auth]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*MigrationsTest.theFourthMigrationAddsTheCredentialTable' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*MigrationsTest.theFourthMigrationAddsTheAuthSessionTableAndItsIndex' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*MigrationsTest' -PrequireDocker=true
  - git diff --quiet develop -- poker-server/src/main/resources/db/migration/V1__initial_schema.sql poker-server/src/main/resources/db/migration/V2__duel_hands_played.sql poker-server/src/main/resources/db/migration/V3__player_display_name.sql
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The database has a `credential` table and an `auth_session` table with exactly the columns,
constraints and index [`ADR-0027`](../../docs/adr/ADR-0027-the-session-outranks-the-device-id.md)
§1 and §2 write out, and the migration chain reports four applied versions.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/resources/db/migration/V4__credential_and_auth_session.sql` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/MigrationsTest.kt` | modify — this ticket owns it |
| `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` | read — §1 and §2 carry both `CREATE TABLE` statements verbatim |
| `poker-server/src/main/resources/db/migration/V3__player_display_name.sql` | read — the header-comment style every migration carries |

## Scope

Transcribe `ADR-0027` §1 and §2. The ADR gives every statement in full; this ticket copies them, it
does not design them.

- `credential (id UUID PRIMARY KEY, player_id UUID NOT NULL REFERENCES player (id), kind TEXT NOT
  NULL, identifier TEXT NOT NULL, secret_hash TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now())`
  with `CONSTRAINT credential_kind_identifier_unique UNIQUE (kind, identifier)`.
- `auth_session (token_hash BYTEA PRIMARY KEY, player_id UUID NOT NULL REFERENCES player (id),
  issued_at TIMESTAMPTZ NOT NULL, expires_at TIMESTAMPTZ NOT NULL)` and
  `CREATE INDEX auth_session_player_id_idx ON auth_session (player_id);`.
- **`secret_hash` is nullable and every other column is `NOT NULL`.** `ADR-0027` §1 says why: whether
  a credential carries a server-held secret depends on its kind.
- **No `ON DELETE` clause on either foreign key**, deliberately. `ADR-0039` says v0.1 offers no
  account deletion and `ADR-0027` §2 says the default `NO ACTION` is what forces a future deletion
  feature to state out loud what happens to credentials and sessions. Writing `ON DELETE CASCADE`
  here would answer `DEC-029` silently.
- **`kind` is not constrained to a value set.** `ADR-0041` says `"password"` is the only kind v0.1
  writes and that the column stays open and unfinished — no `CHECK`, no enum type.
- A header comment in the style of `V3`, naming `ADR-0027` and saying why there is no `ON DELETE`
  and why `secret_hash` is nullable.
- The file is `V4`. If `develop` already carries a `V4__` when this is picked up, **stop and report**
  rather than renumbering: the test names below encode the number and the story assumes it.

## Out of scope

- Every Kotlin type, port and query that uses these tables — `TASK-040304` onwards.
- The attempted-violation tests that prove the constraints refuse — `TASK-040302` and `TASK-040303`.
- The three `STORY-0416` tables (`recovery_email`, `email_verification`, `password_reset`). They are
  their own migration in their own story.
- `SchemaConstraintsTest.theSchemaHasTheThreeTables`, which asserts with `contains` and keeps passing
  with five tables present. It is **not** renamed here.
- Editing `V1`, `V2` or `V3`. A merged migration is immutable; the second `verify` command is that
  rule.

## Tests

`MigrationsTest` — one existing test changes, two are added.

| Test | Proves |
| --- | --- |
| `everyMigrationAppliesToAnEmptyDatabase` | the chain applies and reports `1`, `2`, `3`, `4` |
| `theFourthMigrationAddsTheCredentialTable` | the collected `information_schema.columns` rows for `credential` equal exactly the six expected `(column_name, data_type, is_nullable)` triples — `secret_hash` the only `YES` — and `pg_constraint` names `credential_kind_identifier_unique` |
| `theFourthMigrationAddsTheAuthSessionTableAndItsIndex` | the collected columns for `auth_session` equal exactly the four expected triples, `token_hash` is `bytea` and the primary key, and `pg_indexes` names `auth_session_player_id_idx` on `auth_session` |

**This ticket owns `MigrationsTest`.** `everyMigrationAppliesToAnEmptyDatabase` asserts
`listOf("1", "2", "3")` and cannot survive a fourth migration: the expected list becomes
`listOf("1", "2", "3", "4")`. That is the only assertion that moves, it is not weakened, and its
comment about `V2` and `V3` being the evidence of a multi-file chain is updated rather than deleted.
`aSecondRunAppliesNothing`, `theThirdMigrationAddsANullableDisplayNameColumn` and
`theThirdMigrationAddsTheIndexAndTheTrigger` keep their names and their bodies.

No other test in the repository pins the chain length or the table set: `DatabaseStartupTest`
compares its two flyway row counts to each other, its table query filters
`IN ('player', 'duel', 'duel_result')`, and `PostgresTestSupportTest.theFreshDatabaseHasNoTables`
runs before any migration. That was checked, not assumed.

## Acceptance criteria

- [ ] `MigrationsTest.everyMigrationAppliesToAnEmptyDatabase` passes and expects exactly
      `listOf("1", "2", "3", "4")`
- [ ] `MigrationsTest.theFourthMigrationAddsTheCredentialTable` passes, and asserts the **whole
      collected column list** equals the expected one — not `contains` on one column, which would
      pass with a column missing
- [ ] `MigrationsTest.theFourthMigrationAddsTheAuthSessionTableAndItsIndex` passes, and likewise
      asserts the whole collected column list
- [ ] `MigrationsTest.aSecondRunAppliesNothing`, `theThirdMigrationAddsANullableDisplayNameColumn`
      and `theThirdMigrationAddsTheIndexAndTheTrigger` still pass, with their bodies unchanged
- [ ] `V4__credential_and_auth_session.sql` contains no `ON DELETE` and no `CHECK` — the two silent
      answers this ticket exists to avoid
- [ ] `V1`, `V2` and `V3` are byte-identical to `develop`, which the second `verify` command checks
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
