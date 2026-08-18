---
schema: 2
id: TASK-041004
title: Three fixtures register the name they hand a player
type: task
status: done
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, db, test, fixtures, identity]
depends_on: [TASK-041003]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DuelHistoryFilterDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The three `setPlayerDisplayName` helpers that hand a player a name with raw SQL insert the
`name_registry` row first, so they still work once `TASK-041010` adds the foreign key.

## The rule this ticket and the next five apply

> **Register the name only where the write is expected to succeed. Leave the write raw wherever the
> test expects a refusal from a `player`-side constraint or trigger.**

`name_registry` repeats `player`'s three `CHECK`s and its fold, so a registry insert placed in front
of a write that is *meant* to fail changes which constraint reports the failure. Every use in this
ticket's three files is a name that must land, so all of them register — that is why this ticket is
the mechanical one and the next five are not.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify — one helper body |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileWritesTest.kt` | modify — one helper body, one assertion |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelHistoryFilterDatabaseTest.kt` | modify — one helper body |
| `poker-server/src/main/resources/db/migration/V5__name_registry.sql` | read — the table and its `reason` values |

## Scope

Each of the three files has one private helper that writes `display_name` with raw SQL. Each gains
one statement in front of its existing `UPDATE`, on the same connection:

```sql
INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')
```

- **`PostgresProfileReadsTest.setPlayerDisplayName(playerId: String, displayName: String?)`** and
  **`PostgresProfileWritesTest.setPlayerDisplayName(playerId: PlayerId, displayName: String?)`** take
  a nullable name and are called with `null` (`PostgresProfileReadsTest` lines 324 and 890 today).
  **Skip the registry insert when the name is `null`** — there is nothing to register, and
  `INSERT ... VALUES (NULL, 'TAKEN')` violates `NOT NULL`.
- **`DuelHistoryFilterDatabaseTest.setPlayerDisplayName(playerId: String, displayName: String)`**
  takes a non-null name; no branch is needed.
- No `ON CONFLICT` clause. Every call in these three files registers a name no other call in the same
  test has registered, and a fresh database is created per test — a conflict here would be a real
  fixture collision and should fail loudly rather than be swallowed.
- Nothing else in `PostgresProfileReadsTest` or `DuelHistoryFilterDatabaseTest` changes: no test
  method, no assertion, no other helper.

## The one assertion that moves, and why it must

`PostgresProfileWritesTest.aDifferentCaseOfOwnNameIsRefused` gives a player `"Bob"` with the fixture
and then calls `setDisplayName(player, "bob")`, asserting `SetNameResult.AlreadyNamed`. That answer
is a consequence of the fixture **not** registering: the registry insert of `"bob"` succeeds, the
`UPDATE` matches no row, and the zero-row branch answers `AlreadyNamed`.

Once the fixture registers `"Bob"`, `ADR-0051` §2's **first** statement is the one that raises —
`23505` from `name_registry_folded` — and this player does not hold that *exact canonical form*, so
§2's table answers `NameTaken`. That is the ADR's decision, not a choice made here: *"Held by somebody
else, blocked, retired, or retired from this very player — all of them are `409`."*

So this ticket changes exactly one assertion:

- `aDifferentCaseOfOwnNameIsRefused`: `assertEquals(SetNameResult.AlreadyNamed, result)` becomes
  `assertEquals(SetNameResult.NameTaken, result)`.
- Its second assertion — `assertEquals("Bob", storedDisplayNameOf(player.id))` — is **unchanged and
  not weakened**: the player still holds the name they had.
- Its name still describes it: a different case of your own name is refused. Only the code changes.
- **No other assertion in the file moves.** `aDifferentNameForANamedPlayerIsRefused` and
  `noSqlExceptionEscapes` both send a genuinely different, unregistered name, so their `AlreadyNamed`
  comes from the zero-row branch and stands.
- **No HTTP test is affected.** `ProfileEndpointsDatabaseTest.aSecondNameForTheSameProfileIsForbidden`
  sends `"Diana"` then `"Eleanor"` — different strings — and keeps its `403`. If any other test in
  the module turns red, stop and say so rather than editing it here.

## Out of scope

- `DisplayNameUniquenessTest` (`TASK-041005`), `DisplayNamePermanenceTest` (`TASK-041006`),
  `DisplayNameSchemaTest` (`TASK-041007`), `SchemaConstraintsTest` (`TASK-041008`) and
  `PostgresProfileWritesConcurrencyTest` (`TASK-041009`). Each of those has at least one use where
  the write is expected to fail, and each gets its own ticket for that reason.
- A shared fixture helper across test files. Three one-line inserts in three private helpers is a
  smaller change than a new file every one of them has to import.
- The foreign key — `TASK-041010`.

## Tests

No new test. This ticket's guarantee is that **every existing test in the three files passes
unchanged**, which the `verify` block checks by running two of the three suites by name and then the
whole module.

`PostgresProfileReadsTest` is the one to watch: it is 1198 lines and its `setPlayerDisplayName` is
called from thirteen places, two of them with `null`.

| Suite | Proves |
| --- | --- |
| `duels.poker.server.db.PostgresProfileReadsTest` | Eleven named fixtures still land their names and the two `null` calls still clear nothing. Fails against an implementation that registers `null` (`23502`, not-null violation) |
| `duels.poker.server.http.DuelHistoryFilterDatabaseTest` | The opponent-search fixtures still land `"Halvard"` and `"Halvardsen"` |
| `duels.poker.server.db.PostgresProfileWritesTest` | `aDifferentCaseOfOwnNameIsRefused` now asserts `NameTaken` and still asserts the stored name is `"Bob"`. Its three new tests from `TASK-041003`, and every other test in the file, pass with their assertions unchanged |

## Acceptance criteria

- [ ] Every test in `duels.poker.server.db.PostgresProfileReadsTest` passes with its assertions
      unchanged
- [ ] Every test in `duels.poker.server.http.DuelHistoryFilterDatabaseTest` passes with its
      assertions unchanged
- [ ] `PostgresProfileWritesTest.aDifferentCaseOfOwnNameIsRefused` passes, asserting
      `SetNameResult.NameTaken` and `"Bob"` still stored
- [ ] Every other test in `duels.poker.server.db.PostgresProfileWritesTest` passes with its
      assertions unchanged
- [ ] Each of the three helpers issues `INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')`
      before its `UPDATE player`, on the same connection
- [ ] The two nullable helpers skip the insert when the name is `null`
- [ ] No test method in any of the three files is added, removed or renamed, and the only assertion
      changed anywhere is the single `AlreadyNamed` → `NameTaken` above
- [ ] `ProfileEndpointsDatabaseTest` and `ProfileRouteTest` are unmodified and pass
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
