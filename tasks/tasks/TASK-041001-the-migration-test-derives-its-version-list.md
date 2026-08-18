---
schema: 2
id: TASK-041001
title: The migration test derives its version list from the migrations it applies
type: task
status: done
parent: STORY-0410
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, db, migrations, test]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.MigrationsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`everyMigrationAppliesToAnEmptyDatabase` compares what the database recorded against what Flyway
found on the classpath, so the next migration file needs no edit here and a file that exists but did
not apply is caught.

## Why this is first

`STORY-0410` adds two migrations. Today the expected list is the literal `listOf("1", "2", "3", "4")`,
so every migration ticket in this story would have to edit this file as well as its own — which is
one file out of a three-file budget spent on a constant. This ticket spends it once and returns it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/MigrationsTest.kt` | modify — one test body |
| `poker-server/src/main/kotlin/duels/poker/server/db/Migrations.kt` | read — the Flyway configuration to mirror |

## Scope

- `everyMigrationAppliesToAnEmptyDatabase` keeps its name and its `freshDatabase()` +
  `Migrations.migrate(dataSource)` opening. What changes is the expectation.
- The expectation is built from Flyway's own view of the classpath, configured **identically** to
  `Migrations.migrate` — same `dataSource`, same `.locations("classpath:db/migration")`:

  ```kotlin
  val onTheClasspath = Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration")
      .load()
      .info()
      .all()
      .map { it.version.version }
  ```

- The applied list stays exactly what it is today: `SELECT version FROM flyway_schema_history WHERE
  success = true ORDER BY version`.
- Three assertions, in this order, and all three are required:
  1. `assertTrue(onTheClasspath.containsAll(listOf("1", "2", "3", "4")))` — the floor that is already
     merged. **This is the non-empty assertion**: without it, a `locations` typo makes both lists
     empty and the equality below passes vacuously.
  2. `assertEquals(onTheClasspath, applied)` — every migration on the classpath is in the history.
  3. `assertEquals(applied.sorted(), applied)` — no assertion that a version *number* is what it is;
     only that the two views agree and the history is ordered.

  Flyway's `info().all()` returns classpath migrations in version order, so 2 compares two ordered
  lists; do not sort either side before comparing, or a mis-ordered history passes.
- `import org.flywaydb.core.Flyway` goes in the third-party block. ktlint puts `java.*`, `javax.*`
  and `kotlin.*` **after** it.

## Out of scope

- `aSecondRunAppliesNothing`, `theThirdMigrationAddsANullableDisplayNameColumn`,
  `theThirdMigrationAddsTheIndexAndTheTrigger`, `theFourthMigrationAddsTheCredentialTable` and every
  other test in this file. None of them is edited.
- Any change to `Migrations.kt`. It is on the read list only.
- Any new migration. `TASK-041002` adds `V5`.

## Tests

`MigrationsTest`, `-PrequireDocker=true`. One existing test changes; nothing is added or removed.

| Test | Proves |
| --- | --- |
| `everyMigrationAppliesToAnEmptyDatabase` | Every migration Flyway can see was applied and recorded, in order, and at least `V1`–`V4` were seen. **The wrong implementations it must fail against:** (a) a test that only asserts the history is non-empty — assertion 2 catches a pending file; (b) a test whose expected list comes from the same `flyway_schema_history` query as the actual — assertion 1 pins a floor the history alone cannot fabricate; (c) a `locations` string that matches nothing — assertion 1 fails on an empty sweep |

**How to know it is falsifiable rather than tautological:** temporarily change `.locations(...)` to
`classpath:db/nowhere` and the test must fail on assertion 1, not pass. Do that check locally; do not
commit it.

## Acceptance criteria

- [ ] `MigrationsTest.everyMigrationAppliesToAnEmptyDatabase` passes
- [ ] The string `listOf("1", "2", "3", "4")` no longer appears in `MigrationsTest.kt`
- [ ] `MigrationsTest.kt` contains an assertion that the classpath list contains `"1"`, `"2"`, `"3"`
      and `"4"`, and an `assertEquals` between the classpath list and the history list
- [ ] Every other test in `MigrationsTest` passes with its assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
