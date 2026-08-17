---
schema: 2
id: TASK-040404
title: One SELECT, and it answers for that player and that kind only
type: task
status: done
parent: STORY-0404
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, auth, db]
depends_on: [TASK-040403]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresCredentialsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresCredentials.holdsCredential` answers from one `SELECT` correlated to both the player and
the kind, so the guard cannot be true for a player who holds nothing.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresCredentials.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresCredentialsTest.kt` | modify |

## Scope

- Implement `holdsCredential` with one statement, in the shape the rest of the class already uses —
  `withContext(Dispatchers.IO) { dataSource.connection.use { … } }`, a `PreparedStatement`, both
  parameters bound:

  ```sql
  SELECT EXISTS (SELECT 1 FROM credential WHERE player_id = ? AND kind = ?)
  ```

  `player_id` is bound with `UUID.fromString(playerId.value)` and `kind` with `kind.value`, exactly
  as `insertCredential` binds them.
- It reads no `secret_hash`. The column does not appear in this statement at all.
- It runs no Argon2 work, which is the point of asking before `create`: a player who already holds a
  credential is refused without spending one of the four verification slots `ADR-0027` §1 allows.

## Out of scope

- Any locking, `SELECT … FOR UPDATE`, unique index or migration. `ADR-0030` §7 adds no constraint
  and no index, and `TASK-040403`'s KDoc records why the residual race is accepted rather than
  closed here.
- Changing `create` or `verify`. Their SQL, their `23505` mapping and their dummy verification are
  `TASK-040312`'s and `TASK-040313`'s and do not move.

## Tests

`PostgresCredentialsTest`, against the container, with `runBlocking`. Four tests are added; **no
existing test in the file changes**, because this ticket adds a statement rather than altering one.

| Test | Proves |
| --- | --- |
| `aPlayerWithNoCredentialHoldsNone` | a freshly inserted `player` row answers `false` |
| `aPlayerHoldsThePasswordItJustCreated` | after `create` succeeds for that player, the same query answers `true` |
| `aCredentialOfAnotherKindIsNotHeld` | a player holding `password` answers `false` for `CredentialKind("passkey")`. **The wrong implementation this must fail against is one that binds only `player_id`** |
| `onePlayersCredentialIsNotAnothers` | **two `player` rows in one database**: the first holds a `password` credential, the second holds none, and the second answers `false`. **The wrong implementation this must fail against is the uncorrelated `SELECT EXISTS (SELECT 1 FROM credential WHERE kind = ?)`**, which answers `true` for everybody the moment anybody signs up — and which passes every test that holds only one fixture |

## Acceptance criteria

- [ ] All four tests above pass
- [ ] `onePlayersCredentialIsNotAnothers` inserts **two** players into one database and asserts the
      second answers `false` while the first answers `true`
- [ ] `aCredentialOfAnotherKindIsNotHeld` asserts `false` for a second kind while the first kind
      still answers `true`
- [ ] The statement binds two parameters and names `player_id` and `kind`; it does not name
      `secret_hash`
- [ ] The seven tests `TASK-040312` wrote and the enumeration test `TASK-040313` wrote pass
      unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
