---
schema: 2
id: TASK-041019
title: Two players in one database, and only one of them reads true
type: task
status: backlog
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, test, read-path, moderation]
depends_on: [TASK-041018]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`displayNameRemoved` is proved to be about the caller and about *now*: in one database, the player
whose name was taken reads `true`, the player who never set one reads `false`, and the player who has
since chosen a new name reads `false`.

## Why the fixtures must share a database

`ADR-0053` §4.3 names the mis-implementation: `EXISTS (SELECT 1 FROM name_registry WHERE reason =
'RETIRED')` — the correlation dropped — makes **every caller in the product** read `true` the instant
any takedown happens to anybody. *"It passes any test whose fixtures do not share a database."* Two
tests with one fixture each are worth nothing here, and §6 makes that a criterion rather than advice.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify — three new tests and one fixture helper |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | read — the statement `TASK-041018` added |
| `docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md` | read — §2's table and §6 |

## Scope

- Three tests in the existing class, plus one private helper that gives a player a name through
  `PostgresProfileWrites` and then retires it with `SELECT retire_display_name(?, ?)`. Both are the
  real mechanisms; nothing hand-writes a `RETIRED` row.
- Every test reads through `profileOf(DeviceId(...))` — the port, as the endpoint calls it.
- No existing test in the file is edited. `TASK-041004` already made its `setPlayerDisplayName`
  register, and `TASK-041018` added the column, so everything there is green before this ticket
  starts.

## Out of scope

- `DuelSummaryResponse` and anything about another player — `TASK-041020`.
- The HTTP layer. `ProfileEndpointsDatabaseTest` decodes `ProfileResponse` and needs no change;
  if it does, stop and say so rather than editing it here.
- Any assertion about the *number* of retired names a player has. The wire carries a boolean and
  `ADR-0053` §7 refuses a count.

## Tests

`PostgresProfileReadsTest`, `-PrequireDocker=true`. Three tests, and the first is the load-bearing
one because it holds both fixtures at once.

| Test | Proves |
| --- | --- |
| `aRemovedNameReadsTrueAndANeverNamedPlayerInTheSameDatabaseReadsFalse` | One database. Alice sets `"Ann"`, an operator retires it. Bob never sets a name. `profileOf(alice)` returns `displayName == null` **and** `displayNameRemoved == true`; `profileOf(bob)` returns `displayName == null` **and** `displayNameRemoved == false`. **The wrong implementation this must fail against** is the uncorrelated `EXISTS` of `ADR-0053` §4.3, which answers `true` for bob too — and which two single-fixture tests cannot see |
| `aPlayerWhoHasSinceChosenANewNameReadsFalse` | Alice's `"Ann"` is retired, alice then sets `"Bea"`. `profileOf(alice)` returns `displayName == "Bea"` and `displayNameRemoved == false`. This is `ADR-0053` §2's row 4 and the conjunct that fails silently: an expression missing `p.display_name IS NULL` passes the test above and shows a moderation notice to somebody who moved on |
| `aNamedPlayerNeverReadsTrue` | A player who holds `"Cid"` and has never had anything retired reads `displayNameRemoved == false`. `ADR-0053` §2 row 3 — the pair `(displayName != null, displayNameRemoved = true)` is a state no query can produce, and this asserts it as an invariant of the answer rather than of the type |

`aRemovedNameReadsTrueAndANeverNamedPlayerInTheSameDatabaseReadsFalse` must be **one** test method
with **both** assertions in it. Splitting it into two methods reintroduces exactly the hole §4.3
describes, and the long name is the reminder.

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.aRemovedNameReadsTrueAndANeverNamedPlayerInTheSameDatabaseReadsFalse`
      passes, is a single test method, and asserts both players' profiles
- [ ] `PostgresProfileReadsTest.aPlayerWhoHasSinceChosenANewNameReadsFalse` passes and asserts
      `displayName == "Bea"` as well as the boolean
- [ ] `PostgresProfileReadsTest.aNamedPlayerNeverReadsTrue` passes
- [ ] The retiring helper calls `retire_display_name` and does not write `name_registry` directly
- [ ] Every test already in `PostgresProfileReadsTest` passes with its assertions unchanged, and none
      is edited or renamed
- [ ] `aListOfThreeDuelsPreparesExactlyOneStatement` and any other statement-count assertion in the
      file still passes — the profile read is still one statement
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
