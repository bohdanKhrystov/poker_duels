---
schema: 2
id: TASK-040111
title: One statement, three answers
type: task
status: done
parent: STORY-0401
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, db, identity]
depends_on: [TASK-040110]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresProfileWritesTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`PostgresProfileWrites` implements the port with the single `UPDATE` `ADR-0029` §5 specifies, and
turns what the database says into one of the three sealed answers — never an exception, and never a
`SQLException` escaping `duels.poker.server.db`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileWritesTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | read — the `withContext(Dispatchers.IO)` and `dataSource.connection.use` shape this file copies |
| `docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md` | read — §5, the statement and the three branches |

## Scope

- `public class PostgresProfileWrites(private val dataSource: DataSource) : ProfileWrites`, in
  `duels.poker.server.db`, same threading and connection handling as `PostgresProfileReads`.
- The write is exactly:
  `UPDATE player SET display_name = ? WHERE id = ? AND display_name IS NULL`.
- **One row updated → `NameSet`**, carrying the profile as it now stands, so the caller can answer
  `200` without a second round trip.
- **`SQLSTATE 23505` → `NameTaken`.** Matched on the code, never on the message.
- **Zero rows → read the stored name**: equal to `canonicalName` → `NameSet` (the idempotent retry);
  otherwise → `AlreadyNamed`.
- The `SQLState` translation lives here and nothing about it reaches a route.
- **No `SELECT` before the `UPDATE`.** The index is the reservation; a check-then-write reintroduces
  the race the one-statement form exists to avoid.

## Out of scope

- Two connections racing — `TASK-040112`, which needs a held transaction.
- The route and its status codes — `TASK-040115` and `TASK-040116`.
- Canonicalising the input: the port's contract says the caller has already done it.

## Tests

`PostgresProfileWritesTest`, against the container.

| Test | Proves |
| --- | --- |
| `anUnnamedPlayerTakesTheName` | `NameSet`, the row holds the name, and the returned profile's `displayName` is it |
| `theReturnedProfileCarriesTheBalanceToo` | the `NameSet` profile's `coinBalance` is the stored one — a profile built from thin air would read zero |
| `aNameHeldByAnotherPlayerIsRefused` | `NameTaken`, and the caller's row is still `NULL` |
| `aNameHeldInAnotherCaseIsRefused` | `Bob` held, `bob` requested → `NameTaken` — the fold is the database's, and this proves the mapping sees it |
| `sendingTheSameNameAgainSucceeds` | a second call with the identical canonical name → `NameSet`, and nothing changed |
| `aDifferentNameForANamedPlayerIsRefused` | → `AlreadyNamed`, and the stored name is untouched |
| `aDifferentCaseOfOwnNameIsRefused` | holder of `Bob` requesting `bob` → `AlreadyNamed`, not `NameSet`: identity is exact equality of the canonical form |
| `noSqlExceptionEscapes` | every case above returns a value; the class throws no `SQLException` for a refusal |

## Acceptance criteria

- [ ] All eight tests above pass
- [ ] The implementation contains exactly one `UPDATE` and no `SELECT` that precedes it
- [ ] `NameTaken` is decided from `SQLState == "23505"`, and no assertion or branch reads an
      exception message
- [ ] `aDifferentCaseOfOwnNameIsRefused` and `sendingTheSameNameAgainSucceeds` are both present —
      together they pin what "identical" means
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
