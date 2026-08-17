---
schema: 2
id: TASK-040412
title: One credential row, and the player table untouched across it
type: task
status: backlog
parent: STORY-0404
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, auth, db, security]
depends_on: [TASK-040411]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.SignUpDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Against a real database: a sign-up writes exactly one `credential` row pointing at the profile the
request already resolved to, and the `player` table is a byte-identical multiset across it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/SignUpDatabaseTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt` | read — the container setup, the `duelServer(serverComponents(config, dataSource))` boot, and the handshake that mints a profile |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt` | read — `requireDocker()` and `containerCoordinates()` |
| `poker-server/src/main/kotlin/duels/poker/server/db/Argon2Phc.kt` | read — `parseArgon2PhcOrNull`, used to prove the stored string is a PHC string |

## Scope

- One test class, booting the shipped server against the container exactly as
  `DuelServerRoutesTest` does, driving `POST /api/auth/sign-up` over HTTP and then reading the
  database with raw SQL.
- **The `player` snapshot reads its columns from `ResultSetMetaData`, never from a hard-coded
  column list.** `ADR-0049` moves the device→profile edge out of `player` into its own
  `device_binding` table in `STORY-0406`, which drops `player.device_id`; a snapshot naming its
  columns would then either fail or, worse, keep passing while silently checking fewer columns than
  it did. A generic snapshot keeps meaning across that migration and across any column added later.
  Order the rows deterministically (`ORDER BY id`) and compare the two lists as values.
- Two `player` rows exist in every test in this file, and only one of them signs up. A snapshot with
  one row cannot distinguish *nothing changed* from *the only row was rewritten to the same thing*,
  and cannot catch a write that lands on the wrong player.

## Out of scope

- The ledger properties and the coin readback — `TASK-040413`, which adds to this same file.
- The whole-flow scenario `ADR-0030` §5 describes and the reusable P1/P2 fixture — `STORY-0406`
  owns those, and `EPIC-04`'s story table says so. This ticket asserts the properties for sign-up
  only, inline.
- Concurrency.

## Tests

`SignUpDatabaseTest`, `-PrequireDocker=true`, one migrated database per test.

| Test | Proves |
| --- | --- |
| `aSignUpWritesExactlyOneCredentialRow` | after one `201`, `SELECT count(*) FROM credential` is `1`, and that row's `player_id` equals the `playerId` the profile endpoint reports for that device, its `kind` is `password`, and its `identifier` is the **folded** handle (sign up as `Bob_1`, read back `bob_1`) |
| `theStoredSecretIsAPhcStringAndNotThePassword` | that row's `secret_hash` is not the password, does not contain it, and `parseArgon2PhcOrNull` accepts it |
| `noSecondPlayerRowAppears` | `SELECT count(*) FROM player` is the same before and after — the single most expensive mistake available in this story is an `INSERT INTO player` for the new account (`ADR-0030` §1) |
| `thePlayerTableIsTheSameMultisetAfterTheSignUp` | the generic snapshot of **every column of every `player` row** is equal before and after, with **two players in the database** and one of them signing up. **The wrong implementations this must fail against are an `INSERT INTO player`, any `UPDATE player`, and a `duel_result` copy that renumbers a player** |
| `noDuelResultRowIsWrittenMovedOrDeleted` | `duel_result` is empty before and after — sign-up is one `INSERT INTO credential` and `ADR-0030` §1 forbids the copy that a naive reading of "migrate my history" produces |

## Acceptance criteria

- [ ] All five tests above pass
- [ ] The `player` snapshot derives its column names from `ResultSetMetaData` and hard-codes none
- [ ] `thePlayerTableIsTheSameMultisetAfterTheSignUp` runs with two `player` rows and asserts on the
      whole list, not on one column of one row
- [ ] `aSignUpWritesExactlyOneCredentialRow` asserts the identifier is `bob_1` after signing up as
      `Bob_1`
- [ ] `theStoredSecretIsAPhcStringAndNotThePassword` asserts all three things — not equal, does not
      contain, and parses
- [ ] Every assertion in the file reads the database with raw SQL rather than through
      `PostgresCredentials`, which would prove the implementation consistent with itself
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
