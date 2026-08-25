---
schema: 2
id: TASK-041601
title: Three tables that cannot become a mailing list
type: task
status: done
parent: STORY-0416
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, db, migration, security]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.RecoveryEmailSchemaTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.MigrationsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`V8` exists and creates `recovery_email`, `email_verification` and `password_reset` exactly as
`ADR-0031` §2–§4 write them, and three properties of that schema are exit codes rather than
sentences.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/resources/db/migration/V8__recovery_email.sql` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/RecoveryEmailSchemaTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/resources/db/migration/V4__credential_and_auth_session.sql` — the comment
style and the *no `ON DELETE`* precedent this file copies;
`poker-server/src/test/kotlin/duels/poker/server/db/AuthSessionSchemaTest.kt` — the `freshDatabase()`
+ `Migrations.migrate` shape, the `SQLException.sqlState == "23505"` assertion and the
constraint-name check this class copies;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §2, §3, §4.

## Scope

- `V8__recovery_email.sql`, taking `V8` because `V7__device_binding.sql` is the highest merged
  number. Three `CREATE TABLE`s and one `CREATE UNIQUE INDEX`, transcribed from `ADR-0031` §2, §3
  and §4 **verbatim** — the column names, types, nullability, primary keys and named constraints
  are the ADR's, not the implementer's.
- The leading comment of `recovery_email` is `ADR-0031` §2's, kept because §6.5 makes the comment
  one of the five mechanisms: it is what the next person to add a column reads.
- **No `ON DELETE` clause on any of the three foreign keys**, matching `V4`'s comment and reason:
  `DEC-029` is unanswered and a cascade would answer part of it silently.
- `RecoveryEmailSchemaTest`, `internal`, calling `PostgresTestSupport.freshDatabase()` then
  `Migrations.migrate(dataSource)` in `@BeforeEach`, exactly as `AuthSessionSchemaTest` does.
- Private insert helpers per table, so each test reads as one fact.

## Out of scope

- The deliberate **absences** in this schema — that `email_verification` has no unique index on
  `address`, and that no foreign key cascades. Both are `TASK-041602`, and they are separated
  because a refusal needs its own test and its own argument, not a spare assertion at the bottom of
  a file about what the schema *does*.
- Any Kotlin port or statement against these tables — `TASK-041607` onward.
- `ProfileResponse.hasRecoveryEmail` — `TASK-041616`.
- Editing `V1`–`V7`. Migrations are immutable (`EPIC-04` non-negotiable); this is a new file.

## Tests

`RecoveryEmailSchemaTest`

| Test | Proves |
| --- | --- |
| `oneAddressBelongsToOnePlayerWhateverItsCase` | Two players. `recovery_email` accepts `Bob@example.com` for the first; the second's `bob@EXAMPLE.com` fails with `SQLState 23505` naming `recovery_email_address_unique`. Two differently-cased spellings of one mailbox are one row, so a reset is never ambiguous (`ADR-0031` §2) |
| `theStoredAddressKeepsTheCaseThePlayerTyped` | After inserting `Bob@Example.com`, `SELECT address` returns `Bob@Example.com` byte-for-byte. The fold is a collision test, never a stored form — that is what must be delivered to |
| `aPlayerHoldsOnePendingAddressAtATime` | One player, two `email_verification` rows with **different** `token_hash` values: the second fails with `23505` naming `email_verification_one_per_player`. The primary key alone would have admitted it, so this asserts the named `UNIQUE (player_id)` and not the key |
| `aPlayerHoldsOneLiveResetTokenAtATime` | The same, for `password_reset` and `password_reset_one_per_player` |

## Acceptance criteria

- [ ] `RecoveryEmailSchemaTest.oneAddressBelongsToOnePlayerWhateverItsCase` passes
- [ ] `RecoveryEmailSchemaTest.theStoredAddressKeepsTheCaseThePlayerTyped` passes
- [ ] `RecoveryEmailSchemaTest.aPlayerHoldsOnePendingAddressAtATime` passes
- [ ] `RecoveryEmailSchemaTest.aPlayerHoldsOneLiveResetTokenAtATime` passes
- [ ] The two one-per-player tests use **two different `token_hash` values**, so a passing test
      cannot be explained by the primary key
- [ ] `MigrationsTest` passes unchanged — `V8` applies on top of `V7` and no earlier file moved
- [ ] `V8__recovery_email.sql` contains no `ON DELETE`, asserted by eye in review and by
      `TASK-041602` as a test
- [ ] `verified_at` is declared `NOT NULL`, so the table cannot represent an unproven address
- [ ] Every command in `verify:` exits 0

## Proof

Run each mutation against `V8` alone, then revert.

1. Drop `COLLATE "und-x-icu"` from the unique index, leaving `lower(address)`. **Nothing reddens** —
   and that is the finding to record in review, not a failure of this ticket: ASCII folds
   identically under both collations, so `oneAddressBelongsToOnePlayerWhateverItsCase` cannot see
   the difference. The pin is defended by `ADR-0029` §1's argument (the test container and
   `EPIC-07`'s deployment must fold alike), not by a test, and this ticket does not pretend
   otherwise. Revert.
2. Change the index to `ON recovery_email (address)` — no `lower()`.
   **`oneAddressBelongsToOnePlayerWhateverItsCase` reddens alone**, at the `assertFailsWith`: the
   second insert now succeeds and no `SQLException` is thrown.
   `theStoredAddressKeepsTheCaseThePlayerTyped` inserts one row and is unaffected. Revert.
3. Change the index to `ON recovery_email (lower(address ...))` **plus** store the folded form —
   that is, make the insert helper write `address.lowercase()`.
   **`theStoredAddressKeepsTheCaseThePlayerTyped` reddens alone**, *expected Bob@Example.com, got
   bob@example.com*. This is the positive control the first test cannot supply: without it, storing
   the fold would pass every uniqueness assertion in the file. Revert.
4. Delete the line `CONSTRAINT email_verification_one_per_player UNIQUE (player_id)`.
   **`aPlayerHoldsOnePendingAddressAtATime` reddens alone** — no exception is thrown, because the
   two rows differ in the primary key. `aPlayerHoldsOneLiveResetTokenAtATime` is a different table
   and stays green, which is what makes the two separate tests worth having. Revert.
5. Delete `CONSTRAINT password_reset_one_per_player UNIQUE (player_id)`.
   **`aPlayerHoldsOneLiveResetTokenAtATime` reddens alone**, symmetrically. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The Proof predicted its own null result, and was right.** Dropping `COLLATE "und-x-icu"` reddens
nothing — ASCII addresses fold identically under both collations — and this ticket said so in advance,
framing it as *a finding to record in review, not a failure of this ticket*. Coder and reviewer each
ran it and confirmed. That is the thirteenth `## Proof` examined in this run and the first to correctly
predict a mutation that would **not** redden; the other twelve were wrong or incomplete by claiming a
red that did not appear.

**The clause is defended by an ADR, not by a gate.** `ADR-0029` §1 requires the test container and the
deployment to fold alike, which is the argument for keeping the collation explicit. `V7` set the
precedent for gating an unobservable clause on the system catalog — its `BEFORE UPDATE OF revoked_at`
is held by a `pg_trigger`/`tgattr` assertion because behaviour cannot reach it — and the same move is
available here via `information_schema.columns.collation_name`.

**Not filed as a follow-up yet, deliberately.** `DEC-071` — which strings count as an address — is
still open, so whether a non-ASCII address ever reaches this collation is undecided. A catalog
assertion written now would gate a clause whose reason may change when that decision lands. Revisit
when `DEC-071` merges; if the answer admits non-ASCII, this becomes a ticket.
