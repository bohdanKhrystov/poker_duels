---
schema: 2
id: TASK-040302
title: One identifier, one kind, one row — and the player it points at must exist
type: task
status: done
parent: STORY-0403
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, schema, auth, tests]
depends_on: [TASK-040301]
verify:
  - ./gradlew :poker-server:test --tests '*CredentialSchemaTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Every guarantee `V4` puts on `credential` is proven by an attempted violation the database refuses,
identified by `SQLSTATE` and constraint name rather than by message text.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/CredentialSchemaTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/SchemaConstraintsTest.kt` | read — the `assertFailsWith<SQLException>` + `sqlState` + constraint-name shape this file copies |
| `poker-server/src/main/resources/db/migration/V4__credential_and_auth_session.sql` | read — the constraints under test |

## Scope

- A new test class in `duels.poker.server.db`, `@BeforeEach` taking
  `PostgresTestSupport.freshDatabase()` and `Migrations.migrate(dataSource)`, exactly as
  `SchemaConstraintsTest` does.
- Two private helpers only: `insertPlayer(deviceId): UUID` and
  `insertCredential(playerId, kind, identifier, secretHash)`. Copy their shape from
  `SchemaConstraintsTest`; do not extract a shared base class.
- **Every refusal asserts `exception.sqlState` and the constraint name.** The constraint name is
  matched inside the message because PostgreSQL puts it there, but the *decision* is the SQLSTATE —
  a test that asserts only on prose breaks when a locale or a version reworded it.

## Out of scope

- `auth_session` and the enumeration of foreign-key delete actions — `TASK-040303`.
- Any Kotlin that writes a credential — `TASK-040312`. This ticket writes rows with raw SQL, because
  what is under test is the schema.
- Character rules on `identifier`. `ADR-0031` §1 puts the handle fold in the write path, not in a
  `CHECK`; the fold is `TASK-040310`.

## Tests

`CredentialSchemaTest`, against the container.

| Test | Proves |
| --- | --- |
| `aSecondCredentialWithTheSameKindAndIdentifierIsRefused` | `23505`, message names `credential_kind_identifier_unique` — the constraint that makes a lookup by identifier a function |
| `theSameIdentifierUnderADifferentKindIsAccepted` | the same string as `identifier` under `kind = 'passkey'` inserts successfully — so the unique key is the **pair**, and the previous test is not passing on a constraint over `identifier` alone |
| `aCredentialForAPlayerThatDoesNotExistIsRefused` | `23503` — a random UUID as `player_id` is refused by the foreign key |
| `deletingAPlayerThatHoldsACredentialIsRefused` | `23503` on `DELETE FROM player` — the absence of `ON DELETE CASCADE` is a behaviour, not a comment |
| `aCredentialRowMayHaveNoSecretHash` | a row with `secret_hash = NULL` inserts, and reads back `NULL` — `ADR-0027` §1's nullable column is real |

## Acceptance criteria

- [ ] All five tests above pass
- [ ] `theSameIdentifierUnderADifferentKindIsAccepted` uses the **same identifier string** as
      `aSecondCredentialWithTheSameKindAndIdentifierIsRefused` — the pair of tests is what pins what
      "unique" means here, and a different string in each would prove neither
- [ ] Every refusal asserts on `sqlState` first; no test branches on, or asserts only on, message
      prose
- [ ] `deletingAPlayerThatHoldsACredentialIsRefused` asserts `23503` — if it ever reports zero rows
      deleted instead, the migration grew a cascade and the ticket is not done
- [ ] `SchemaConstraintsTest.kt` is unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
