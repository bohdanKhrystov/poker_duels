---
schema: 2
id: TASK-041002
title: The fifth migration creates the name registry, its guards and the takedown function
type: task
status: ready
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, db, migrations, schema, identity, moderation]
depends_on: [TASK-041001]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.MigrationsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

One migration creates `name_registry` — the whole display-name namespace — with its fold, its
monotonicity trigger, the permanence trigger's single exception and `retire_display_name`, and every
existing test still passes because nothing yet constrains `player.display_name` to it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/resources/db/migration/V5__name_registry.sql` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/MigrationsTest.kt` | modify — two new tests |
| `poker-server/src/main/resources/db/migration/V3__player_display_name.sql` | read — the fold, the three `CHECK`s and the function this replaces |
| `docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md` | read — §1, §3, §4, §5, §8 |
| `docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md` | read — §3's index only |

## Why the foreign key is not in this file

`ADR-0051` §8 lists `ALTER TABLE player ADD CONSTRAINT player_display_name_registered` in the same
migration as everything below, and §8 also says *"the code lands in the same PR as the migration"* —
naming seven test files that write a display name directly. **Those two sentences cannot both be
obeyed under a three-file ticket budget:** the foreign key is what breaks every direct fixture, and
there are **eight** such files, not seven (`ADR-0051` §8's list omits
`poker-server/src/test/kotlin/duels/poker/server/http/DuelHistoryFilterDatabaseTest.kt`, whose
`setPlayerDisplayName` writes a name with raw SQL). Adding the key here means a ten-file PR.

So the key — **one `ALTER TABLE` line, and nothing else** — moves to `V6` in `TASK-041009`, after
`PostgresProfileWrites` and all eight fixtures register the names they land. The end schema is
identical, `ADR-0051` §8's ordering rule (*create and backfill before adding the foreign key*) is
preserved by the version order, and `V1`–`V4` stay untouched. Everything else in §8 is in this file,
in §8's order.

## Scope — the migration

`V5__name_registry.sql`. Take the next free `V<n>`: `V5` today. If another migration has landed
first, take the next free number and `TASK-041009` takes the one after it.

```sql
CREATE TABLE name_registry (
    name         TEXT        NOT NULL,
    reason       TEXT        NOT NULL,
    retired_from UUID        REFERENCES player (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT name_registry_pkey PRIMARY KEY (name),
    CONSTRAINT name_registry_reason CHECK (reason IN ('TAKEN', 'BLOCKED', 'RETIRED')),
    CONSTRAINT name_registry_retired_from CHECK (retired_from IS NULL OR reason = 'RETIRED'),
    CONSTRAINT name_registry_length CHECK (char_length(name) BETWEEN 1 AND 32),
    CONSTRAINT name_registry_trimmed CHECK (name = btrim(name)),
    CONSTRAINT name_registry_nfc CHECK (name IS NFC NORMALIZED)
);

CREATE UNIQUE INDEX name_registry_folded
    ON name_registry (lower(name COLLATE "und-x-icu"));

CREATE INDEX name_registry_retired_from_idx
    ON name_registry (retired_from) WHERE retired_from IS NOT NULL;

INSERT INTO name_registry (name, reason)
SELECT display_name, 'TAKEN' FROM player WHERE display_name IS NOT NULL;
```

Then, in this order, the three routines. Copy them from `ADR-0051` §3 and §4 **verbatim** — they are
specified line by line there and this ticket does not restate them:

- `CREATE FUNCTION name_registry_is_monotone()` and `CREATE TRIGGER name_registry_monotone
  BEFORE UPDATE OR DELETE ON name_registry FOR EACH ROW` (`ADR-0051` §3).
- `CREATE OR REPLACE FUNCTION player_display_name_is_permanent()` (`ADR-0051` §3). `V3` is **not**
  edited and the trigger definition is **not** re-created — it binds by name and picks up the new
  body.
- `CREATE FUNCTION retire_display_name(target_player UUID, expected_name TEXT) RETURNS TEXT`
  (`ADR-0051` §4).

Things that will otherwise be got wrong:

- **The two `UPDATE`s inside `retire_display_name` are ordered**: the registry row is promoted to
  `RETIRED` first, the player's column is nulled second. Reversed, the permanence trigger raises,
  because its exception looks for a `RETIRED` row that does not yet exist.
- **The exception is scoped to the transition**, never to `current_user`, a role, a GUC or
  `SECURITY DEFINER`. `name → a different name` still raises for everybody.
- **`retire_display_name` is not `SECURITY DEFINER`** and grants nothing.
- **The backfill runs against an empty `player` table in every test**, so it inserts zero rows and no
  test in this repository can exercise it. It is here for a database with rows and it must still be
  written, in this position — after the table and before `V6`'s key.
- Comments in the file say *why* (the ADR section each block implements), never *what*.

## Out of scope

- `ALTER TABLE player ADD CONSTRAINT player_display_name_registered` — `TASK-041009`.
- Any change to `PostgresProfileWrites` — `TASK-041003`.
- Any change to a test fixture that writes a display name — `TASK-041004` … `TASK-041008`.
- Any *behaviour* test of the monotonicity trigger (`TASK-041010`), `retire_display_name`
  (`TASK-041011`), or the permanence exception (`TASK-041012`). This ticket asserts the objects
  exist; those tickets assert what they do.
- Seeding a blocklist row. `ADR-0051` §5: v0.1 ships the table empty, and contents are never a
  migration.
- Editing `V1`–`V4`.

## Tests

`MigrationsTest`, `-PrequireDocker=true`. Two new tests; nothing existing is edited —
`TASK-041001` already made `everyMigrationAppliesToAnEmptyDatabase` pick up `V5` on its own.

| Test | Proves |
| --- | --- |
| `theFifthMigrationAddsTheRegistryAndItsGuards` | One query collects the six new schema objects and the result, sorted, equals the expected six exactly. **Equality, not `containsAll`**: a missing object shortens the list and fails, and a stray seventh fails too |
| `theRetiredFromIndexIsPartial` | `SELECT indexdef FROM pg_indexes WHERE indexname = 'name_registry_retired_from_idx'` contains `WHERE (retired_from IS NOT NULL)`. Without this, a plain non-partial index passes the test above while costing the write path an entry per registered name — `ADR-0053` §3's whole reason for the index |

The collecting query, and the trap in it:

```sql
SELECT DISTINCT 'table:' || tablename FROM pg_tables WHERE tablename = 'name_registry'
UNION SELECT 'index:' || indexname FROM pg_indexes
       WHERE indexname IN ('name_registry_folded', 'name_registry_retired_from_idx')
UNION SELECT 'trigger:' || trigger_name FROM information_schema.triggers
       WHERE trigger_name = 'name_registry_monotone'
UNION SELECT 'function:' || proname FROM pg_proc
       WHERE proname IN ('name_registry_is_monotone', 'retire_display_name')
```

`information_schema.triggers` returns **one row per event**, and `name_registry_monotone` fires on
`UPDATE OR DELETE` — so it returns two identical strings. `UNION` (not `UNION ALL`) is what makes the
count six; use it, or the equality below fails for a reason that has nothing to do with the schema.

Expected, sorted:

```kotlin
listOf(
    "function:name_registry_is_monotone",
    "function:retire_display_name",
    "index:name_registry_folded",
    "index:name_registry_retired_from_idx",
    "table:name_registry",
    "trigger:name_registry_monotone",
)
```

`player_display_name_is_permanent` is deliberately **not** in the list: it already exists from `V3`,
so its presence proves nothing about this migration. `TASK-041012` asserts its new behaviour.

## Acceptance criteria

- [ ] `MigrationsTest.theFifthMigrationAddsTheRegistryAndItsGuards` passes and asserts equality
      against exactly the six strings above
- [ ] `MigrationsTest.theRetiredFromIndexIsPartial` passes and asserts on `indexdef`
- [ ] `MigrationsTest.everyMigrationAppliesToAnEmptyDatabase` passes **unedited** — the file is not
      touched by this ticket except to add the two tests above
- [ ] `V5__name_registry.sql` contains no `ALTER TABLE player` statement
- [ ] `V5__name_registry.sql` contains no `INSERT INTO name_registry` other than the backfill
      `SELECT display_name, 'TAKEN' FROM player WHERE display_name IS NOT NULL`
- [ ] `retire_display_name` in the migration promotes the registry row **before** nulling
      `player.display_name`, and contains neither `SECURITY DEFINER` nor `current_user`
- [ ] `player_display_name_is_permanent`'s new body raises unless `NEW.display_name IS NULL` **and** a
      `name_registry` row for `OLD.display_name` has `reason = 'RETIRED'`
- [ ] `V1`–`V4` are byte-identical to `develop`
- [ ] Every command in `verify:` exits 0

## Size, honestly

About 140 changed lines — above `S`'s 120, and this ticket says so rather than pretending. Roughly
100 of them are declarative SQL with no branching, copied from `ADR-0051` §§1, 3, 4. It is not
splittable further without a third migration file: the foreign key has already been moved out for the
reason above, and splitting the remainder would put a `CREATE FUNCTION` in one immutable file and its
trigger in another.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
