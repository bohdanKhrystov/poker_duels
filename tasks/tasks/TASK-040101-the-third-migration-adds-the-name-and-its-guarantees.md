---
schema: 2
id: TASK-040101
title: The third migration adds the name and its four guarantees
type: task
status: done
parent: STORY-0401
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, schema, migration, identity]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*MigrationsTest.theThirdMigrationAddsANullableDisplayNameColumn' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*MigrationsTest.theThirdMigrationAddsTheIndexAndTheTrigger' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*MigrationsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*SchemaConstraintsTest' -PrequireDocker=true
  - git diff --quiet develop -- poker-server/src/main/resources/db/migration/V1__initial_schema.sql poker-server/src/main/resources/db/migration/V2__duel_hands_played.sql
---

## Goal

`player` has a nullable `display_name` column, and the database — not the application — holds the
four guarantees `ADR-0029` puts on it: bounded, trimmed, NFC, unique under a case fold, and
permanent once set.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/resources/db/migration/V3__player_display_name.sql` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/MigrationsTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/SchemaConstraintsTest.kt` | modify — one attempted violation per guarantee, so the schema is proven to refuse rather than merely to declare |
| `docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md` | read — §1, §2, §4 carry the SQL verbatim |
| `poker-server/src/main/resources/db/migration/V2__duel_hands_played.sql` | read — the header comment every migration carries |

## Scope

Transcribe `ADR-0029` §1, §2 and §4 into one new file. The ADR gives every statement in full; this
ticket copies them, it does not design them.

- The column, from `ADR-0021`: `ALTER TABLE player ADD COLUMN display_name TEXT` with
  `CONSTRAINT player_display_name_length CHECK (char_length(display_name) BETWEEN 1 AND 32)`.
- `player_display_name_trimmed` — `CHECK (display_name = btrim(display_name))`.
- `player_display_name_nfc` — `CHECK (display_name IS NFC NORMALIZED)`.
- `CREATE UNIQUE INDEX player_display_name_unique ON player (lower(display_name COLLATE "und-x-icu"))`.
  The collation is **pinned, not implicit** — left to the cluster default the same schema enforces a
  different rule on the test container than in production.
- The `player_display_name_is_permanent()` function and the
  `player_display_name_permanent` trigger, `BEFORE UPDATE OF display_name ... FOR EACH ROW`, raising
  with `ERRCODE = 'restrict_violation'`.
- A header comment in the style of `V2`, naming `ADR-0029` and saying why `NULL` is allowed and why
  the collation is written down.

## Out of scope

- **The blocklist and the retired-name set** (`ADR-0038`). Uniqueness here consults exactly one
  source of truth. `STORY-0410` adds the other two, in its own migration.
- Any Kotlin that writes or reads the column — `TASK-040105` onwards.
- Editing `V1` or `V2`. A merged migration is immutable; the last `verify` command is that rule.

## Tests

`MigrationsTest` — one existing test changes, two are added.

| Test | Proves |
| --- | --- |
| `everyMigrationAppliesToAnEmptyDatabase` | the chain applies and reports `1`, `2`, `3` |
| `theThirdMigrationAddsANullableDisplayNameColumn` | `information_schema.columns` has `player.display_name`, `is_nullable = YES`, `data_type = text` |
| `theThirdMigrationAddsTheIndexAndTheTrigger` | `pg_indexes` names `player_display_name_unique` on `player`, and `pg_trigger` names `player_display_name_permanent` |

**This ticket owns `MigrationsTest`.** `bothMigrationsApplyToAnEmptyDatabase` asserts
`listOf("1", "2")` and cannot survive a third migration: rename it to
`everyMigrationAppliesToAnEmptyDatabase` and change the expected list to `listOf("1", "2", "3")`.
That is the only assertion that moves, it is not weakened, and its comment about `V2` being the
first evidence of a multi-file chain is updated rather than deleted. `aSecondRunAppliesNothing`
keeps its body and its name.

## Acceptance criteria

- [ ] `MigrationsTest.everyMigrationAppliesToAnEmptyDatabase` passes and expects exactly
      `listOf("1", "2", "3")`
- [ ] `MigrationsTest.theThirdMigrationAddsANullableDisplayNameColumn` passes
- [ ] `MigrationsTest.theThirdMigrationAddsTheIndexAndTheTrigger` passes
- [ ] `MigrationsTest.aSecondRunAppliesNothing` still passes, with its body unchanged
- [ ] Every test in `SchemaConstraintsTest` passes unchanged — the new objects break no existing one
- [ ] `V1__initial_schema.sql` and `V2__duel_hands_played.sql` are byte-identical to `develop`,
      which the last `verify` command checks
- [ ] The index expression contains `COLLATE "und-x-icu"` — an unpinned `lower()` is the defect this
      criterion exists to catch
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
