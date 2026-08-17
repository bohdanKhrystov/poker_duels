---
schema: 2
id: TASK-040303
title: One token hash, one row — and no foreign key cascades
type: task
status: ready
parent: STORY-0403
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, schema, auth, tests]
depends_on: [TASK-040302]
verify:
  - ./gradlew :poker-server:test --tests '*AuthSessionSchemaTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`auth_session` refuses a repeated token hash and an unknown player, and the claim that **no**
foreign key on either new table carries an `ON DELETE` action is proven by enumerating them rather
than by reading the migration.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/AuthSessionSchemaTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/CredentialSchemaTest.kt` | read — the helper and assertion shape this file follows |
| `poker-server/src/main/resources/db/migration/V4__credential_and_auth_session.sql` | read — the constraints under test |

## Scope

- A new test class in `duels.poker.server.db`, same container setup as `CredentialSchemaTest`.
- Helpers: `insertPlayer(deviceId): UUID` and
  `insertAuthSession(tokenHash: ByteArray, playerId, issuedAt, expiresAt)`. `token_hash` is written
  with `setBytes`, because the column is `BYTEA` and a `String` would silently store its text.
- **The cascade test is a catalogue query, not two more `DELETE` attempts.** `ADR-0027` says *no
  `ON DELETE` clause anywhere*; a universal claim is proven by enumerating what it quantifies over:

  ```sql
  SELECT c.conname, c.confdeltype
  FROM pg_constraint c
  JOIN pg_class t ON t.oid = c.conrelid
  WHERE c.contype = 'f' AND t.relname IN ('credential', 'auth_session')
  ORDER BY c.conname
  ```

  `confdeltype = 'a'` is `NO ACTION`. The assertion is on the **whole collected list**: exactly two
  rows, both `'a'`. A third foreign key added later without a delete rule fails this test, which is
  the point.

## Out of scope

- `credential`'s own refusals — `TASK-040302`.
- Issuing, storing, hashing or expiring a session token. `auth_session` gets no Kotlin in this
  story at all; `STORY-0405` writes rows into it.
- `SessionToken`, the value class — `TASK-040307`.

## Tests

`AuthSessionSchemaTest`, against the container.

| Test | Proves |
| --- | --- |
| `aSecondRowWithTheSameTokenHashIsRefused` | `23505`, message names `auth_session_pkey` — the token hash is the primary key |
| `twoRowsWithDifferentTokenHashesForOnePlayerAreAccepted` | a phone and a laptop: `ADR-0027` §2's *"a player may hold many sessions at once"*, and the guarantee that the previous test is not passing on a constraint over `player_id` |
| `anAuthSessionForAPlayerThatDoesNotExistIsRefused` | `23503` |
| `deletingAPlayerThatHoldsAnAuthSessionIsRefused` | `23503` on `DELETE FROM player` |
| `noForeignKeyOnTheTwoNewTablesHasAnOnDeleteAction` | the catalogue query returns exactly `credential`'s and `auth_session`'s two foreign keys and every `confdeltype` is `'a'` |

## Acceptance criteria

- [ ] All five tests above pass
- [ ] `noForeignKeyOnTheTwoNewTablesHasAnOnDeleteAction` asserts the **collected list** — both the
      set of constraint names and every delete type. A test that checks one constraint would pass
      with the other cascading
- [ ] `twoRowsWithDifferentTokenHashesForOnePlayerAreAccepted` uses the **same `player_id`** as its
      neighbouring refusal test, so the pair pins that the key is `token_hash` and not `player_id`
- [ ] Every refusal asserts on `sqlState` first; no test branches on message prose
- [ ] `token_hash` is written with `setBytes` and read back with `getBytes`, compared with
      `assertContentEquals`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
