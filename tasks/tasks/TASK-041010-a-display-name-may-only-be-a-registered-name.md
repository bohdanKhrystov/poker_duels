---
schema: 2
id: TASK-041010
title: The sixth migration makes a display name a registered name or nothing
type: task
status: backlog
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, db, migrations, schema, identity]
depends_on: [TASK-041009]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PlayerNameIsRegisteredTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`player.display_name` references `name_registry (name)`, so *"every name ever held is in the
registry"* is a constraint rather than a promise made by one Kotlin file.

## Why this is a second migration and why it is last of the two

`ADR-0051` §1: *"Without it, **every name ever held is in the registry** is a promise made by one
Kotlin file; with it, a `player` row cannot hold a name that was never registered, from any write
path, including `psql` and including a test fixture."* That last clause is exactly why it lands here
rather than in `V5`: the key breaks every direct fixture at once, and `TASK-041004`–`TASK-041009`
converted the eight files that have one. `TASK-041002` records the full reasoning; this ticket is the
one line that was held back.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/resources/db/migration/V6__player_display_name_registered.sql` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PlayerNameIsRegisteredTest.kt` | create |
| `poker-server/src/main/resources/db/migration/V5__name_registry.sql` | read — the primary key this references |
| `docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md` | read — §1's last four bullets and §8 |

## Scope — the migration

The next free `V<n>`, `V6` today, containing exactly one statement:

```sql
ALTER TABLE player ADD CONSTRAINT player_display_name_registered
    FOREIGN KEY (display_name) REFERENCES name_registry (name);
```

- **No `ON DELETE` clause**, matching `V4` and `ADR-0051` §1: `NO ACTION` forces a future deletion
  feature to say out loud what happens to the names a profile has spent (`ADR-0039`).
- **Not `DEFERRABLE`.** `PostgresProfileWrites` registers the string in the statement before it
  writes the column, in the same transaction, so an immediate check is satisfied. Deferring would
  only hide an ordering mistake until commit.
- Nothing else is in the file: no index, no trigger, no function, no backfill. `V5` already ran the
  backfill, in `ADR-0051` §8's order.

## Out of scope

- Editing `V5` or any earlier migration.
- Any further fixture conversion. If a test outside `TASK-041004`–`TASK-041009`'s eight files turns
  red here, **stop and file a ticket** rather than widening this one: that would mean a ninth direct
  writer nobody has counted, which is worth a diff of its own.
- `retire_display_name`, the monotonicity trigger and the permanence exception — all already in `V5`,
  with their behaviour tests in `TASK-041011`–`TASK-041013`.

## Tests

`PlayerNameIsRegisteredTest`, a new file, `-PrequireDocker=true`. Its single top-level declaration is
the class, so the filename matches it.

Fixture: `PostgresTestSupport.freshDatabase()` + `Migrations.migrate`, then
`INSERT INTO player (id, device_id, coin_balance) VALUES (?, ?, 0)`.

| Test | Proves |
| --- | --- |
| `anUnregisteredNameCannotBeHeld` | `UPDATE player SET display_name = 'Ghost'` with no registry row raises `23503` and the message contains `player_display_name_registered`. This is the guarantee |
| `aRegisteredNameCanBeHeld` | The same `UPDATE`, after `INSERT INTO name_registry ('Ghost', 'TAKEN')`, sets the name and it reads back as `"Ghost"`. **Without this, the test above passes against a database in which every write fails** — and that is the one wrong implementation a foreign-key test cannot otherwise tell apart |
| `aBlockedNameIsNotHoldableEither` | A row registered as `('Slur', 'BLOCKED')` satisfies the key by string, so this test asserts the **opposite** of a guess: the `UPDATE` **succeeds** at the schema level, because the key constrains the string and not the reason. Screening is the write path's (`ADR-0051` §2, `TASK-041016`), and this test exists so nobody later "fixes" the key into something that also encodes policy |
| `theKeyTakesNoActionOnDelete` | `SELECT confdeltype FROM pg_constraint WHERE conname = 'player_display_name_registered'` is `'a'`. `'c'` (cascade), `'n'` (set null) and `'d'` (set default) each silently rewrite a player's name from a `DELETE` somebody runs in `psql`; `ADR-0039` says a deletion feature must state what it does, and `'a'` is what forces that |

## Acceptance criteria

- [ ] `PlayerNameIsRegisteredTest.anUnregisteredNameCannotBeHeld` passes and asserts both `23503` and
      the constraint name
- [ ] `PlayerNameIsRegisteredTest.aRegisteredNameCanBeHeld` passes and reads the stored name back
- [ ] `PlayerNameIsRegisteredTest.aBlockedNameIsNotHoldableEither` passes, asserting that the schema
      permits it
- [ ] `PlayerNameIsRegisteredTest.theKeyTakesNoActionOnDelete` passes, asserting `confdeltype = 'a'`
- [ ] `V6__player_display_name_registered.sql` contains exactly one statement and no `ON DELETE`,
      `ON UPDATE` or `DEFERRABLE` clause
- [ ] `MigrationsTest.everyMigrationAppliesToAnEmptyDatabase` passes with `MigrationsTest.kt`
      unedited
- [ ] `./gradlew :poker-server:check -PrequireDocker=true` is green with no test file outside this
      ticket edited
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
