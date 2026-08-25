---
schema: 2
id: TASK-041602
title: Two strangers may both claim one address, and nothing cascades
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, migration, security, invariant]
depends_on: [TASK-041601]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.RecoveryEmailSchemaRefusalsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The two things `V8` deliberately does **not** have — a unique index on a pending address, and any
`ON DELETE` clause — are held by tests, so that adding either later fails the build instead of
reading as a tidy-up.

## Why this exists

Both are refusals, and a refusal produces no assertion by default. `ADR-0031` §3 calls the missing
unique constraint on `email_verification.address` the thing that stops an attacker squatting the
address of a player who has not signed up yet: *"a pending row in the unique namespace would deny
the true owner recovery forever, and refusing the squat would need an oracle to detect."* Somebody
reading that table in a year sees an obviously missing index. Nothing today tells them why it is
missing, and adding it breaks no test.

The `ON DELETE` absence is `V4`'s precedent (`DEC-029` unanswered) extended to three more foreign
keys, and `ADR-0031`'s closing note makes it load-bearing: *"whoever answers `DEC-029` must say out
loud what happens rather than discover that a cascade already decided."*

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/RecoveryEmailSchemaRefusalsTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/resources/db/migration/V8__recovery_email.sql`;
`poker-server/src/test/kotlin/duels/poker/server/db/RecoveryEmailSchemaTest.kt` — the fixture shape
and insert helpers this class mirrors;
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §3 and its closing `DEC-029` note.

## Scope

- `RecoveryEmailSchemaRefusalsTest`, `internal`, on the same `freshDatabase()` +
  `Migrations.migrate` fixture as `RecoveryEmailSchemaTest`.
- The squat test is a **behavioural** assertion against the live schema — two players, one address,
  two accepted rows — not a scan of the DDL text.
- The cascade test reads `information_schema.referential_constraints`, joined to
  `table_constraints` so it can filter to the three tables by name, and asserts every row's
  `delete_rule` is `NO ACTION`. It queries the **database**, not the file: a `.sql` grep would miss
  a cascade introduced by a later migration altering the constraint, which is exactly the change
  this is meant to catch.
- The cascade test names the three tables in a `setOf(...)` and asserts the query returned **three
  rows** before asserting their rules — a filter that matched nothing would otherwise satisfy an
  all-of-empty assertion.

## Out of scope

- `V8`'s positive shape — `TASK-041601`.
- Whether a player row is ever deleted. `DEC-029` is unanswered and this ticket does not touch it;
  it asserts only that the schema still forces somebody to answer it.
- Extending the cascade assertion to `credential`, `auth_session` or `device_binding`. Their
  absences are `V4`'s and `V7`'s and are not this story's to gate; widening the filter here would
  make an unrelated migration fail a `STORY-0416` test.

## Tests

`RecoveryEmailSchemaRefusalsTest`

| Test | Proves |
| --- | --- |
| `twoPlayersMayBothHoldAPendingClaimOnOneAddress` | Two players, two `email_verification` rows with the same `address` and different `token_hash`. **Both inserts succeed.** Adding `UNIQUE (address)` to that table reddens this and nothing else |
| `oneAddressIsStillOwnedByOnlyOneVerifiedPlayer` | Directly after the two pending rows above, both players' addresses are inserted into `recovery_email`; the second fails with `23505`. The pair is the point — pending is a free-for-all, verified is exclusive — and this is the positive control that stops the test above passing because the fixture never actually collides |
| `noRecoveryTableCascadesOnDelete` | Every referential constraint on `recovery_email`, `email_verification` and `password_reset` has `delete_rule = 'NO ACTION'`, and there are exactly three of them |

## Acceptance criteria

- [ ] `RecoveryEmailSchemaRefusalsTest.twoPlayersMayBothHoldAPendingClaimOnOneAddress` passes
- [ ] `RecoveryEmailSchemaRefusalsTest.oneAddressIsStillOwnedByOnlyOneVerifiedPlayer` passes
- [ ] `RecoveryEmailSchemaRefusalsTest.noRecoveryTableCascadesOnDelete` passes
- [ ] The two pending rows use the **same** `address` string and **different** `token_hash` values
- [ ] `noRecoveryTableCascadesOnDelete` asserts the row count is `3` **before** asserting the rules
- [ ] The cascade test queries `information_schema`, and the file contains no read of any `.sql`
      migration file
- [ ] Every command in `verify:` exits 0

## Proof

Each mutation is applied to `V8__recovery_email.sql`, run, then reverted.

1. Add `CONSTRAINT email_verification_address_unique UNIQUE (address)` to `email_verification`.
   **`twoPlayersMayBothHoldAPendingClaimOnOneAddress` reddens alone**, with an unexpected
   `SQLException` on the second insert naming that constraint. `oneAddressIsStillOwnedByOnlyOne
   VerifiedPlayer` inserts its two pending rows first, so it reddens too — which is the honest
   prediction and the reason this control lives in the same file: **two tests redden, and if only
   the second does, the first's fixture is not colliding at all.**
2. Drop `recovery_email_address_unique`. **`oneAddressIsStillOwnedByOnlyOneVerifiedPlayer` reddens
   alone**, at its `assertFailsWith`, while `twoPlayersMayBothHoldAPendingClaimOnOneAddress` stays
   green. This is the mutation that proves the two tests are not the same assertion written twice.
3. Change `password_reset`'s foreign key to
   `REFERENCES player (id) ON DELETE CASCADE`. **`noRecoveryTableCascadesOnDelete` reddens alone**,
   *expected NO ACTION, got CASCADE* for `password_reset`. The other two tests never delete a
   `player` row and are unaffected.
4. The vacuity control for the third test: change its table filter to a name that does not exist,
   such as `recovery_emails`. **`noRecoveryTableCascadesOnDelete` reddens on the count assertion**,
   *expected 3, got 0*, rather than passing an all-of-empty check. Run this one; a filter typo is
   the realistic way this test rots into a tautology.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
