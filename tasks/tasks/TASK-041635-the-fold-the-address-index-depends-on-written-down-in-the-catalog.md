---
schema: 2
id: TASK-041635
title: The fold the address index depends on, written down in the catalog
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, migration, invariant, security]
depends_on: [TASK-041602]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.RecoveryEmailSchemaTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`V8`'s `COLLATE "und-x-icu"` — which `TASK-041601` proved no behaviour in that ticket could
reach — fails a build when it is removed.

## Why this exists

`TASK-041601`'s `## Proof` step 1 predicted that dropping the clause **reddens nothing**, and was
right: ASCII addresses fold identically under both collations, so `oneAddressBelongsToOnePlayer
WhateverItsCase` cannot see the difference. Its landing Notes parked the follow-up on a condition:

> `DEC-071` — which strings count as an address — is still open, so whether a non-ASCII address ever
> reaches this collation is undecided. Revisit when `DEC-071` merges; if the answer admits
> non-ASCII, this becomes a ticket.

[`ADR-0078`](../../docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md) §1 merged and
**admits non-ASCII**: an `@` with something on both sides, no ASCII control character, at most 254
code points, and nothing else examined. Its Consequences say so directly — *"A non-ASCII local part
or domain is accepted as typed… §2's index collation is pinned and already handles the fold"* — and
name this follow-up as owed. So the clause is now load-bearing rather than decorative, and the thing
defending it is still only `ADR-0029` §1's argument that the test container and `EPIC-07`'s
deployment must fold alike.

`V7` set the precedent: `device_binding`'s `BEFORE UPDATE OF revoked_at` is held by a
`pg_trigger`/`tgattr` assertion in `DeviceBindingFinalityTest.theTriggerIsScopedToTheRevokedAt
Column`, because behaviour cannot reach the column list either. This is the same move, on the
surface that can actually see this clause.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/RecoveryEmailSchemaTest.kt` | modify |

Two tests are **added**; no existing test in the file changes, no assertion moves and none is
weakened. Nothing else in the story touches this file.

Read, and do not edit:
`poker-server/src/main/resources/db/migration/V8__recovery_email.sql` — the index this pins;
`poker-server/src/test/kotlin/duels/poker/server/db/DeviceBindingFinalityTest.kt`, specifically
`theTriggerIsScopedToTheRevokedAtColumn` — `V7`'s catalog precedent and its row-shape idiom;
`poker-server/src/test/kotlin/duels/poker/server/db/RecoveryEmailSchemaRefusalsTest.kt`,
specifically `noRecoveryTableCascadesOnDelete` — the count-and-names-**before**-any-property idiom
this copies verbatim;
`docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md` §1;
`docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md` §1.

## Scope

- Two tests added to `RecoveryEmailSchemaTest`, on the `freshDatabase()` + `Migrations.migrate`
  fixture the class already builds in `@BeforeEach`.
- **The catalog surface is `pg_index.indcollation`**, `unnest`ed `WITH ORDINALITY` and **`LEFT
  JOIN`**ed to `pg_collation`, filtered **by the table** `recovery_email` and never by index name.
  One row per index key: index name, key position, `collname`, `collprovider::text`.
- **`information_schema.columns.collation_name` is the wrong surface and must not be used.** `V8`
  puts the collation on the *index expression*, not the column — `address` is declared plain `TEXT`
  — so that view reports `NULL` for `recovery_email.address` **both with and without** the clause.
  It was probed on `postgres:16-alpine` in both states and reported `NULL` in both. A test written
  against it can never fail, which is the one way this ticket becomes worthless.
- `collprovider` is a `"char"`, so cast it `::text` in the query. ICU is `i`, the database default
  is `d`, libc is `c`. Assert the **provider as well as the name**: `und-x-icu` is only the root
  locale fold when ICU is providing it.
- **The join to `pg_collation` is a `LEFT JOIN` on purpose.** A non-collatable index key carries
  collation OID `0`, which an `INNER JOIN` drops silently — taking the row count with it, and with
  it the count assertion that is the whole vacuity guard. `recovery_email_pkey` is on a `UUID` and
  is exactly such a key, so this is not hypothetical.
- The behavioural test uses the **one code-point pair whose fold differs** between `und-x-icu` and
  the container's default on `postgres:16-alpine`, written as escapes rather than literal glyphs so
  the fixture survives any editor: `"İ@example.com"` (`İ`, Latin capital I with dot above) and
  `"i̇@example.com"` (`i` followed by U+0307 combining dot above). Under `und-x-icu` the first
  folds to the second and the index refuses the pair; under the default fold it does not.

## Out of scope

- **Editing `V8__recovery_email.sql`.** The migration is correct and migrations are immutable
  (`EPIC-04` non-negotiable). If either test fails, the migration is right and the test is wrong.
- **`player_display_name_unique`**, which carries the identical clause under `ADR-0029` §1 and is
  `V3`'s. Ungated today and **not ticketed anywhere** — widening this test's filter to reach it
  would make an unrelated migration fail a `STORY-0416` test, which is `TASK-041602`'s reason
  unchanged. **Say so in the PR** so it becomes a follow-up rather than an assumption.
- **A non-ASCII fold test built on an accented Latin letter.** `É`/`é`, `ẞ`/`ß` and `Ж`/`ж` were
  probed and `lower()` folds each **identically** under `und-x-icu` and under the container's
  default, so a test built on any of them would pass under the mutation and gate nothing. This is
  the trap that makes the fixture above look arbitrary; it is not.
- Asserting anything about `email_verification.address`, which has no unique index at all —
  `TASK-041602`'s refusal.
- The stored form. `theStoredAddressKeepsTheCaseThePlayerTyped` already holds that the fold is a
  collision test and never a stored value, and it is untouched.

## Tests

`RecoveryEmailSchemaTest`, two new methods.

| Test | Proves |
| --- | --- |
| `theAddressIndexIsPinnedToTheIcuRootCollation` | Over **every index key on `recovery_email`**: exactly **two** rows, named `recovery_email_address_unique` and `recovery_email_pkey` **read from the catalog**, asserted before any collation is examined; then `recovery_email_address_unique`'s single key has `collname = "und-x-icu"` and `collprovider = "i"`, and `recovery_email_pkey`'s has no collation at all |
| `twoSpellingsOnlyIcuFoldsTogetherAreOneAddress` | Two players. `recovery_email` accepts `"İ@example.com"` for the first; the second's `"i̇@example.com"` fails with `SQLState 23505` naming `recovery_email_address_unique`. The behavioural half: the pinned collation is not merely recorded, it decides an insert that the default fold would admit |

## Acceptance criteria

- [ ] `RecoveryEmailSchemaTest.theAddressIndexIsPinnedToTheIcuRootCollation` passes
- [ ] `RecoveryEmailSchemaTest.twoSpellingsOnlyIcuFoldsTogetherAreOneAddress` passes
- [ ] The catalog query filters on the **table** `recovery_email`; the string
      `recovery_email_address_unique` appears in no `WHERE` clause, so the index-name assertion is
      read from the catalog rather than handed to it
- [ ] The row-count assertion (`2`) and the index-name assertion both run **before** any
      `collname` is examined
- [ ] The query joins `pg_collation` with `LEFT JOIN`, and the file contains no `INNER JOIN` to it
- [ ] The file contains no reference to `information_schema.columns` and no read of any `.sql`
      migration file
- [ ] The provider is asserted as well as the collation name
- [ ] The four pre-existing `RecoveryEmailSchemaTest` tests pass **unchanged** — this ticket adds
      methods and edits none, and changes no behaviour any of them observes
- [ ] Every command in `verify:` exits 0

## Proof

Each mutation is applied to `V8__recovery_email.sql` or to the test, run, then reverted.

1. **The mutation this ticket exists for.** Drop `COLLATE "und-x-icu"` from the unique index,
   leaving `ON recovery_email (lower(address))`.
   **Both new tests redden, and nothing else in the file does.**
   `theAddressIndexIsPinnedToTheIcuRootCollation` fails on the collation assertion — *expected
   und-x-icu, got default* and *expected i, got d* — because a key with no explicit collation
   carries `pg_collation` OID `100`, whose `collname` is the literal string `default`, not `NULL`
   and not the absent row an `INNER JOIN` would have removed. `twoSpellingsOnlyIcuFoldsTogether
   AreOneAddress` fails at its `assertFailsWith`: **both inserts now succeed**, leaving two rows.
   The four pre-existing tests stay green, which is precisely `TASK-041601`'s recorded finding —
   this ticket's whole purpose is that this mutation now has somewhere to land.
2. **The vacuity control, and it has two readings — run both.** Rename the index in `V8` to
   `recovery_email_addr_uniq`. **`theAddressIndexIsPinnedToTheIcuRootCollation` reddens on the
   *name* assertion**, not the count: two index keys still exist on the table, so the count is
   still `2` and it is the name set that has moved. Then instead point the table filter at a table
   that does not exist. **Now the count assertion fires** — *expected 2, actual 0* — or, if the
   filter is written `'recovery_email'::regclass`, the query throws `relation … does not exist`
   before any assertion runs, which is louder still and equally acceptable. Both readings must be
   run and the PR must say which shape was chosen; a filter that matches nothing is the realistic
   way this test rots into an all-of-empty tautology, and it is the failure `TASK-041602`'s Notes
   recorded as imprecisely predicted last time.
3. **The surface control, and the most important run in this ticket.** Leave `V8` mutated as in
   step 1, and rewrite `theAddressIndexIsPinnedToTheIcuRootCollation` to read
   `information_schema.columns.collation_name` for `recovery_email.address` instead.
   **Nothing reddens** — the view reports `NULL` with the clause and `NULL` without it, because the
   collation is on the index expression and never on the column. Run it and record it in the PR.
   This is the version of this test a reasonable engineer writes first, it is the surface named in
   the request that produced this ticket, and it gates absolutely nothing.
4. Change the clause to a different real collation: `lower(address COLLATE "C")`.
   **`theAddressIndexIsPinnedToTheIcuRootCollation` reddens alone**, *expected und-x-icu, got C*
   and *expected i, got c*. `twoSpellingsOnlyIcuFoldsTogetherAreOneAddress` **also reddens**, since
   `C` folds neither spelling — so if only the catalog test reddens here, the behavioural fixture is
   not colliding and must be fixed before this merges. Revert.
5. Swap the behavioural fixture's second address for `"i@example.com"` — the form the *default*
   fold produces from `İ`, and the plausible "simplify the weird escape" edit.
   **`twoSpellingsOnlyIcuFoldsTogetherAreOneAddress` reddens** under the *unmutated* migration,
   because `und-x-icu` folds `İ` to `i` + U+0307 and not to a bare `i`, so the pair no longer
   collides. Run it: it is the control that proves the fixture depends on the exact code points and
   not on the two strings merely looking different. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Note

**The behavioural test's mutation-detection depends on the container's libc; the catalog test's
does not.** `postgres:16-alpine` is musl, whose default `lower()` maps `İ` to a bare `i`; ICU maps
it to `i` + U+0307. Every other case probed — `É`, `ẞ`, `Ж` — folds the same under both. If CI ever
moves off this image, `twoSpellingsOnlyIcuFoldsTogetherAreOneAddress` keeps *passing* correctly, but
may stop *detecting* step 1's mutation, because the pinned collation would then agree with the new
default. `theAddressIndexIsPinnedToTheIcuRootCollation` reads the pinned clause itself and reddens
on any platform. That asymmetry is why both tests exist and why neither replaces the other.
