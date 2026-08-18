---
schema: 2
id: TASK-041011
title: A registered name is never released, and the only change it may take is TAKEN to RETIRED
type: task
status: done
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, test, schema, moderation]
depends_on: [TASK-041010]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.NameRegistryMonotonicityTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`name_registry_monotone` is pinned by tests: a `TAKEN` or `RETIRED` row can never be deleted, no
`name` can ever be rewritten, the only permitted update is `TAKEN → RETIRED`, and a `BLOCKED` row can
still be removed.

## Why it matters

`ADR-0051` §3 installs this trigger so that *retired forever* is not resting on nobody running a
`DELETE`. `V5` created it and `TASK-041002` only asserted it exists. This ticket is what makes the
promise checkable, and it is `deep` because every failure mode here silently returns a spent string
to the pool.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/NameRegistryMonotonicityTest.kt` | create |
| `poker-server/src/main/resources/db/migration/V5__name_registry.sql` | read — `name_registry_is_monotone` |
| `docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md` | read — §3's monotonicity block |

## Scope

- One new test class. `PostgresTestSupport.freshDatabase()` + `Migrations.migrate` per test, as every
  other schema test in this package does.
- Rows are created with plain `INSERT INTO name_registry (name, reason)` and, where a `retired_from`
  is needed, an `UPDATE` through the permitted transition — never by inserting `reason = 'RETIRED'`
  directly, which would test a state the product cannot reach.
- Every refusal asserts **`sqlState == "23001"`** (`restrict_violation`) and matches on the code,
  never on the message. `ADR-0029` §4 established that rule and `ADR-0051` §3 repeats it. The two
  messages differ between the `DELETE` branch and the `UPDATE` branch, so a message match here would
  be pinning prose.
- No test in this file writes to `player`.

## Out of scope

- `retire_display_name` — `TASK-041012`. This file exercises the trigger directly, which is what
  makes it a test of the trigger rather than of the function that happens to satisfy it.
- The permanence trigger on `player` — `TASK-041013`.
- The blocklist's effect on the write path — `TASK-041016`. That a `BLOCKED` row is *deletable* is a
  monotonicity fact and belongs here; that it *refuses a claim* is a write-path fact and does not.

## Tests

`NameRegistryMonotonicityTest`, `-PrequireDocker=true`. Seven tests. The five refusals and the two
permissions are both required: without the permissions, a trigger that raised on everything would
pass five tests and break the product.

| Test | Proves |
| --- | --- |
| `aTakenNameCannotBeDeleted` | `DELETE FROM name_registry WHERE name = 'Ann'` on a `TAKEN` row raises `23001`, and the row is still there afterwards. This is *retired forever*'s foundation: a name in use cannot be quietly freed |
| `aRetiredNameCannotBeDeleted` | The same for a row promoted to `RETIRED`. **The wrong implementation this must fail against**: a trigger whose `DELETE` branch tests `OLD.reason = 'TAKEN'` instead of `OLD.reason <> 'BLOCKED'` — it passes the test above and returns every retired string to the pool |
| `aBlockedNameCanBeDeleted` | `DELETE` of a `('Slur', 'BLOCKED')` row succeeds and the row is gone. `ADR-0051` §5: *"a curated list must be correctable"*. Without this, a trigger that refused every `DELETE` would leave the two tests above green |
| `takenBecomesRetiredAndTheNameAndCreatedAtAreUntouched` | `UPDATE name_registry SET reason = 'RETIRED', retired_from = <player> WHERE name = 'Ann'` succeeds, and the row afterwards has `reason = 'RETIRED'`, that `retired_from`, and the same `name` and `created_at`. **This is not a guarantee the schema makes.** The trigger never references `created_at` or `retired_from`; an `UPDATE` that also set `created_at` succeeds and rewrites it. The test asserts what *this* statement leaves alone |
| `retiredCannotGoBackToTaken` | `UPDATE ... SET reason = 'TAKEN'` on a `RETIRED` row raises `23001`. Un-retiring is `ADR-0051` §9's *"no un-retire, no release"* |
| `aRegisteredNameCannotBeRewritten` | `UPDATE name_registry SET name = 'Anne' WHERE name = 'Ann'` raises `23001` even when the new string is free. **What it does not prove**: this statement leaves `reason` at `'TAKEN'`, so it is also refused by a trigger that dropped the `NEW.name <> OLD.name` guard entirely — the surviving `NEW.reason <> 'RETIRED'` clause catches it. The rename guard is pinned by the next test, not this one |
| `aRenameSmuggledIntoARetirementIsRefused` | `UPDATE name_registry SET name = 'Anne', reason = 'RETIRED' WHERE name = 'Ann'` on a `TAKEN` row raises `23001`. This is the statement that separates the real trigger from one checking only `reason`: the retirement half is permitted, so **only** the `NEW.name <> OLD.name` guard can refuse it. Without this test an operator could rename a spent string into an unspent one by retiring it in the same statement |

Each refusal test asserts the row's state **after** the refusal — same `name`, same `reason` — not
only that an exception was thrown. An exception thrown after a partial write is still a defect.

## Acceptance criteria

- [ ] `NameRegistryMonotonicityTest.aTakenNameCannotBeDeleted` passes
- [ ] `NameRegistryMonotonicityTest.aRetiredNameCannotBeDeleted` passes
- [ ] `NameRegistryMonotonicityTest.aBlockedNameCanBeDeleted` passes
- [ ] `NameRegistryMonotonicityTest.takenBecomesRetiredAndTheNameAndCreatedAtAreUntouched` passes
- [ ] `NameRegistryMonotonicityTest.retiredCannotGoBackToTaken` passes
- [ ] `NameRegistryMonotonicityTest.aRegisteredNameCannotBeRewritten` passes
- [ ] `NameRegistryMonotonicityTest.aRenameSmuggledIntoARetirementIsRefused` passes, and fails against a trigger whose `UPDATE` branch drops the `NEW.name <> OLD.name` guard
- [ ] Every refusal test asserts `sqlState == "23001"` and asserts the row is unchanged afterwards
- [ ] No test in the file matches on an exception message
- [ ] No file outside this ticket is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
