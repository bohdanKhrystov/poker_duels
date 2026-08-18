---
schema: 2
id: TASK-041016
title: A blocked name is refused when it is set, and the screen fails closed
type: task
status: ready
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, test, moderation, identity]
depends_on: [TASK-041015]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.NameBlocklistTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A `BLOCKED` row refuses a claim of that string in any case, a blocklist entry cannot be added over a
name somebody is holding, a registry that cannot be reached refuses rather than accepts, and each of
the three reasons refuses on its own.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/NameBlocklistTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | read — what it does with a `SQLSTATE` that is not `23505` |
| `docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md` | read — §5 in full, and §2's fail-closed bullet |
| `docs/adr/ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md` | read — the fail-closed requirement |

## Scope

- One new test class. Blocklist rows are created with `ADR-0051` §5's own statement and nothing else:

  ```sql
  INSERT INTO name_registry (name, reason) VALUES (normalize(btrim(?), NFC), 'BLOCKED')
  ```

- **Fail-closed is planted, not assumed.** `ADR-0038` calls this the single thing most likely to be
  got backwards, and `ADR-0051` §2 answers it structurally: *"a blocklist that cannot be read is a
  database that cannot be reached, and then statement 1 has not succeeded and nothing is written."*
  The plant is one statement on the test's own fresh database:

  ```sql
  ALTER TABLE name_registry RENAME TO name_registry_unreachable
  ```

  The foreign key follows the table, so `player`'s constraint is intact and the only thing that has
  changed is that `INSERT INTO name_registry` no longer resolves — `42P01`, which
  `PostgresProfileWrites` does not translate and therefore rethrows. Do **not** plant it with
  `REVOKE`: the Testcontainers user is the database owner and grants do not bind it.
- Every refusal asserts `SetNameResult.NameTaken` and a still-`NULL` `display_name`, exactly as
  `TASK-041015` does. No answer says which source refused.

## Out of scope

- Seeding any blocklist content into a migration. `ADR-0051` §5: *"the contents are data, never a
  migration"*, and *"v0.1 ships an empty table"*.
- Substring, pattern or script matching. `ADR-0051` §5 and `ADR-0038` both refuse it; a test asserting
  that `"Scunthorpe"` is accepted beside a blocked `"cunt"` would be pinning a rule this project has
  deliberately not made. Homoglyphs likewise (`ADR-0038`).
- Re-screening a name already held. `ADR-0051` §5: *"screening is a set-time event and nothing is
  re-screened."* The fourth test below is the positive statement of that.
- The HTTP layer and status codes.

## Tests

`NameBlocklistTest`, `-PrequireDocker=true`. Six tests.

| Test | Proves |
| --- | --- |
| `aBlockedNameIsRefused` | With `('Slur', 'BLOCKED')` in the registry, `setDisplayName(alice, "Slur")` answers `NameTaken` and alice is still nameless |
| `aBlockedNameIsRefusedInAnotherCase` | The same with `"slur"`. The blocklist is consulted through `name_registry_folded`, which is `ADR-0029` §1's ICU fold — **the wrong implementation this must fail against** is any screen that folds in the JVM, which disagrees with the index on exactly the confusable strings a blocklist is for. It is also the story's own criterion |
| `blockingIsRefusedForANameInUse` | Alice holds `"Ann"`; inserting `('Ann', 'BLOCKED')` raises `23505` and the row for `"Ann"` is still `TAKEN`. `ADR-0051` §5: the schema *"refuses to express a third, quieter state in which a player keeps displaying a name the operator has decided is unacceptable"* |
| `aNameAlreadyHeldIsNotRescreened` | Alice holds `"Ann"`; `('Anne', 'BLOCKED')` is added; alice still holds `"Ann"` and reading her profile still returns it. A deliberately mundane test that fails the day somebody adds a re-screening job |
| `aRegistryThatCannotBeReachedRefusesTheName` | After the `ALTER TABLE … RENAME`, `setDisplayName(alice, "Fresh")` throws rather than returning a result, and alice's `display_name` is still `NULL`. **Fails against** a write path with a `catch (SQLException)` that falls through to the old single `UPDATE`, which is the fail-open implementation `ADR-0038` names |
| `eachOfTheThreeReasonsRefusesOnItsOwn` | In **one** database: `"Held"` is `TAKEN` (bob set it), `"Barred"` is `BLOCKED` (inserted), `"Gone"` is `RETIRED` (carol set it, an operator retired it). Three further players claim one each and all three get `NameTaken`; a fourth claims `"Free"` and gets `NameSet`. This is the story's *"the three sources of truth are each shown to refuse independently"*, and the fourth claim is what stops it passing against a write path that refuses everything |

`eachOfTheThreeReasonsRefusesOnItsOwn` uses a distinct claimant per string so that no refusal can be
explained by the claimant already holding a name — which is `AlreadyNamed`, a different answer, and
the easiest way for this test to pass for the wrong reason.

## Acceptance criteria

- [ ] `NameBlocklistTest.aBlockedNameIsRefused` passes
- [ ] `NameBlocklistTest.aBlockedNameIsRefusedInAnotherCase` passes
- [ ] `NameBlocklistTest.blockingIsRefusedForANameInUse` passes and asserts the row is still `TAKEN`
- [ ] `NameBlocklistTest.aNameAlreadyHeldIsNotRescreened` passes
- [ ] `NameBlocklistTest.aRegistryThatCannotBeReachedRefusesTheName` passes, plants the failure with
      `ALTER TABLE name_registry RENAME TO …`, and asserts `display_name` is still `NULL`
- [ ] `NameBlocklistTest.eachOfTheThreeReasonsRefusesOnItsOwn` passes, uses one database, three
      distinct claimants and a fourth successful claim
- [ ] No migration file contains an `INSERT INTO name_registry` with `'BLOCKED'`
- [ ] No file outside this ticket is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
