---
schema: 2
id: TASK-140901
title: The trigger stops saying permanent, and the registry gains a fourth reason
type: task
status: done
parent: STORY-1409
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 5
atomic:
  - "`:poker-server:test` — `MigrationsTest.theThirdMigrationAddsTheIndexAndTheTrigger` selects `information_schema.triggers` by the literal name `player_display_name_permanent`, which this migration drops"
  - "`:poker-server:test` — `SchemaConstraintsTest.anUpdateToAnAlreadySetDisplayNameIsRejected` asserts the exception message contains `display_name is permanent once set`, which this migration replaces"
  - "`:poker-server:test` — `DisplayNamePermanenceTest.aRetiredNameStillCannotBecomeADifferentName` asserts a refusal that `ADR-0134` §1's body deliberately stops making"
labels: [server, db, migration, account]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.MigrationsTest' -PrequireDocker=true
  - grep -q 'tests="10" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.db.MigrationsTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.DisplayNamePermanenceTest' -PrequireDocker=true
  - grep -q 'tests="11" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.db.DisplayNamePermanenceTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.NameRegistryMonotonicityTest' -PrequireDocker=true
  - grep -q 'tests="8" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.db.NameRegistryMonotonicityTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.SchemaConstraintsTest' -PrequireDocker=true
  - grep -q 'tests="11" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.db.SchemaConstraintsTest.xml
  - sh -c '! grep -rqF player_display_name_is_permanent poker-server/src/main/kotlin poker-server/src/test'
  - test -f poker-server/src/main/resources/db/migration/V9__name_never_released.sql
  - grep -qF player_display_name_never_released poker-server/src/main/resources/db/migration/V9__name_never_released.sql
  - sh -c '! grep -qF "NEW.display_name IS NOT NULL OR" poker-server/src/main/resources/db/migration/V9__name_never_released.sql'
  - sh -c '! git diff --name-only origin/develop -- poker-server/src/main/resources/db/migration/V3__player_display_name.sql poker-server/src/main/resources/db/migration/V5__name_registry.sql | grep -q .'
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

After `Migrations.migrate`, the trigger on `player.display_name` is named
`player_display_name_never_released`, its body is `ADR-0051` §3's **minus the
`NEW.display_name IS NOT NULL OR` disjunct**, and `name_registry` accepts a fourth `reason`,
`REPLACED`, reachable from `TAKEN` and terminal. Nothing a shipping code path does changes.

## Files

Four, **probed not remembered** (`ADR-0069`, `ADR-0070`). The migration file was written alone and
`./gradlew check -PrequireDocker=true` — the command `.github/workflows/build.yml` runs on a pull
request — was run in full: `1794 tests completed, 3 failed`, one in each of the three test files
below. The minimal propagation was applied at each and the command was run again to `BUILD
SUCCESSFUL`. Docker was available and every database suite ran, so this enumeration is complete **as corrected**: it named four files while four other sections of this ticket required a fifth, and the row above was added at landing
rather than a prefix.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/resources/db/migration/V9__name_never_released.sql` | create | The whole change. `ADR-0134` adds **one** migration and never edits `V3` or `V5` |
| `poker-server/src/test/kotlin/duels/poker/server/db/MigrationsTest.kt` | modify | `theThirdMigrationAddsTheIndexAndTheTrigger` fails at `MigrationsTest.kt:106` — it queries `information_schema.triggers` for the literal old name |
| `poker-server/src/test/kotlin/duels/poker/server/db/SchemaConstraintsTest.kt` | modify | `anUpdateToAnAlreadySetDisplayNameIsRejected` fails at `SchemaConstraintsTest.kt:232` — it asserts the old exception message, and only the message |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNamePermanenceTest.kt` | modify | `aRetiredNameStillCannotBecomeADifferentName` fails at `DisplayNamePermanenceTest.kt:150` — its expectation **inverts** under `ADR-0134` §1 |
| `poker-server/src/test/kotlin/duels/poker/server/db/NameRegistryMonotonicityTest.kt` | modify | **Added at landing.** The Scope, the Tests table, two acceptance criteria and the `verify:` block all require `takenBecomesReplacedAndTheNameAndCreatedAtAreUntouched` here, taking the class 7 → 8; only this table omitted it, and `ADR-0070` §4's carve-out excludes adding a test, so the coder was right to refuse rather than widen scope. The omission is corrected, not the requirement |

Read, and do not edit:
[`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) §1, §3 and §8;
`poker-server/src/main/resources/db/migration/V5__name_registry.sql` — the two `CHECK`s, the
monotonicity function and the permanence function this migration replaces, and
`retire_display_name`, which it must leave alone.

## Scope

- **`V9__name_never_released.sql`, transcribed from `ADR-0134` §1 and §3.** In this order: drop and
  re-add `name_registry_reason` with `'REPLACED'` in its `IN` list; drop and re-add
  `name_registry_retired_from` as `retired_from IS NULL OR reason IN ('RETIRED', 'REPLACED')`;
  `CREATE OR REPLACE FUNCTION name_registry_is_monotone()` whose `UPDATE` guard becomes
  `NEW.reason NOT IN ('RETIRED', 'REPLACED')`; then `DROP TRIGGER player_display_name_permanent ON
  player`, `DROP FUNCTION player_display_name_is_permanent()`, `CREATE FUNCTION
  player_display_name_is_never_released()` and `CREATE TRIGGER player_display_name_never_released
  BEFORE UPDATE OF display_name ON player`.
- **The surviving guard is exactly one condition**: `NOT EXISTS (SELECT 1 FROM name_registry WHERE
  name = OLD.display_name AND reason IN ('RETIRED', 'REPLACED'))`, raising with
  `ERRCODE = 'restrict_violation'` and the message
  `a display name is spent before it is left (ADR-0051, ADR-0134)`.
- **`MigrationsTest` learns the two new identifiers.** The trigger query names
  `player_display_name_never_released`; a new test `theNinthMigrationRenamesTheTriggerAndWidensTheRegistry`
  asserts, against `pg_proc` and `information_schema.triggers`, that
  `player_display_name_is_permanent` is **absent**, `player_display_name_is_never_released` is
  present, and that `pg_get_constraintdef` for `name_registry_reason` contains `'REPLACED'`.
- **`DisplayNamePermanenceTest`'s inverted test is rewritten, not deleted.** It becomes
  `aSpentNameMayBecomeADifferentName`: register `robert`, retire the player's `bob`, write `robert`
  onto the column, assert the column reads `robert`. Its comment is rewritten to say what
  `ADR-0134` §1 says — the guard governs the string being **left** — and must not keep arguing for
  the rule the migration removes.
- **A new `DisplayNamePermanenceTest` case, `aReplacedNameMayBeLeftBehind`**, does the same through
  `REPLACED` rather than `RETIRED`, so the second disjunct of the new `IN` list is exercised by
  something. Ten tests become eleven.
- **A new `NameRegistryMonotonicityTest` case, `takenBecomesReplacedAndTheNameAndCreatedAtAreUntouched`**,
  mirroring the merged `takenBecomes…Retired…` case. Seven tests become eight.

## Out of scope

- **Editing `V3` or `V5`.** `ADR-0134` says so twice; a `verify` command asserts neither file
  differs from `origin/develop`.
- **`retire_display_name`.** `ADR-0134` §1: *not edited*. Its statement order already satisfies the
  new guard, and `RetireDisplayNameTest` must stay green untouched.
- **Any Kotlin under `src/main`.** `PostgresProfileWrites` still carries `AND display_name IS NULL`
  after this ticket, which is what makes the migration inert — that is `TASK-140902`.
- **A guard that the arriving name is `TAKEN`.** `ADR-0134` §8 names the hole and refuses to close
  it. Not ticketed.

## Tests

`MigrationsTest`

| Test | Proves |
| --- | --- |
| `theThirdMigrationAddsTheIndexAndTheTrigger` | *(modified)* the trigger on `player.display_name` exists under the name `player_display_name_never_released` |
| `theNinthMigrationRenamesTheTriggerAndWidensTheRegistry` | *(new)* `player_display_name_is_permanent` is gone from `pg_proc`, `player_display_name_is_never_released` is in it, and `name_registry_reason`'s definition contains `REPLACED` |

`DisplayNamePermanenceTest`

| Test | Proves |
| --- | --- |
| `aSpentNameMayBecomeADifferentName` | *(rewritten)* a player whose held string is already `RETIRED` may have the column moved to another registered name — the guard is about the string being left |
| `aReplacedNameMayBeLeftBehind` | *(new)* the same through a `REPLACED` row, so both members of the `IN` list are load-bearing |
| `aNamedProfileCannotBeRenamed` | *(unchanged, must stay green)* a held string that is **not** spent still raises `23001` |
| `aNameThatIsNotRetiredStillCannotBeGivenUp` | *(unchanged, must stay green)* `name → NULL` still raises when the string is unspent |

`NameRegistryMonotonicityTest`

| Test | Proves |
| --- | --- |
| `takenBecomesReplacedAndTheNameAndCreatedAtAreUntouched` | *(new)* `TAKEN → REPLACED` is permitted and rewrites neither `name` nor `created_at` |
| `retiredCannotGoBackToTaken` | *(unchanged, must stay green)* a terminal reason is still terminal |

`SchemaConstraintsTest`

| Test | Proves |
| --- | --- |
| `anUpdateToAnAlreadySetDisplayNameIsRejected` | *(modified)* the refusal is still `23001` and its message is now the never-released one |

## What would still pass if the coder got it wrong

- **If the new trigger body kept the deleted disjunct**, `aSpentNameMayBecomeADifferentName` and
  `aReplacedNameMayBeLeftBehind` both fail. Nothing else would notice — which is why those two
  tests are the point of the ticket and not decoration.
- **If the body dropped the guard entirely** (a trigger that never raises),
  `aNamedProfileCannotBeRenamed` and `aNameThatIsNotRetiredStillCannotBeGivenUp` fail. Those two
  merged tests are the negative half and neither may be weakened.
- **The two disjuncts disagree by construction.** `aSpentNameMayBecomeADifferentName` uses
  `RETIRED` and `aReplacedNameMayBeLeftBehind` uses `REPLACED`, so a body that named only one of
  the two values reddens exactly one of them; a single test at one value could not tell an `IN`
  list from a single-value comparison.
- **If the `CHECK` were widened but the monotonicity trigger were not**,
  `takenBecomesReplacedAndTheNameAndCreatedAtAreUntouched` fails on `23001` while every merged
  registry test stays green.
- **If `MigrationsTest`'s new test only asserted the presence of the new function**, a migration
  that created it and left `player_display_name_is_permanent` behind would pass. It asserts the
  absence too, and that is the half that catches a `CREATE OR REPLACE` shortcut.

## Acceptance criteria

- [ ] `MigrationsTest.theNinthMigrationRenamesTheTriggerAndWidensTheRegistry` passes
- [ ] `MigrationsTest.theThirdMigrationAddsTheIndexAndTheTrigger` passes against the new name
- [ ] `DisplayNamePermanenceTest.aSpentNameMayBecomeADifferentName` passes
- [ ] `DisplayNamePermanenceTest.aReplacedNameMayBeLeftBehind` passes
- [ ] `DisplayNamePermanenceTest.aNamedProfileCannotBeRenamed` and
      `aNameThatIsNotRetiredStillCannotBeGivenUp` pass with **no assertion weakened**
- [ ] `NameRegistryMonotonicityTest.takenBecomesReplacedAndTheNameAndCreatedAtAreUntouched` passes
- [ ] `SchemaConstraintsTest.anUpdateToAnAlreadySetDisplayNameIsRejected` passes and still asserts
      `23001`, changing only the message it looks for
- [ ] `MigrationsTest` reports exactly 10 tests, `DisplayNamePermanenceTest` 11,
      `NameRegistryMonotonicityTest` 8 and `SchemaConstraintsTest` 11 — the counts measured on
      `develop` at `1c3c7fd9` (9, 10, 7, 11) plus exactly the tests named above
- [ ] `V3__player_display_name.sql` and `V5__name_registry.sql` are byte-identical to `origin/develop`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
