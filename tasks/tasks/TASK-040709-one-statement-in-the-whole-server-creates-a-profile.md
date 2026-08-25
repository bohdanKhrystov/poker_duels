---
schema: 2
id: TASK-040709
title: One statement in the whole server creates a profile
type: task
status: done
parent: STORY-0407
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, invariant, security]
depends_on: [TASK-040708]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.ProfileCreationIsOneStatementTest'
  - grep -qF 'assembled from constants' poker-server/src/test/kotlin/duels/poker/server/db/ProfileCreationIsOneStatementTest.kt
  - grep -qF 'file-set assertion' poker-server/src/test/kotlin/duels/poker/server/db/ProfileCreationIsOneStatementTest.kt
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

A gate that fails the build when any file other than `PostgresPlayerDirectory.kt` gains an
`INSERT INTO player` — so `STORY-0407`'s *"leaves no orphan profile behind"* survives the arrival of a
write path nobody has written yet.

## Why this exists

`TASK-040706` asserts the negative across the operations **this scenario performs**. That is the
direct half and it is not the durable one: an endpoint added in a year, by someone who never read
`ADR-0030`, is outside every bracket in that file.

`ADR-0030` §5 already made this argument once and acted on it — P1 and P2 are *"total over the schema,
so an endpoint added in a year trips them without anyone having updated a test"* — and
`IdentityMovesNoCoinTest.everyApiPathInTheRouteSourcesIsExercisedByTheScenario` acted on it a second
time, at the endpoint level, by reading source text. Neither reaches this claim. The coin invariant is
structurally blind to an orphan profile: a minted row has `coin_balance` `0` and no `duel_result`
rows, so P1 holds for it and both P2 sums are unmoved. The endpoint enumeration sees a new **route**,
not a new **statement**, and says nothing about the socket.

`ADR-0030` §2 states the property this ticket turns into an exit code: *"After `EPIC-04` there are
exactly three statements that write `player`"*, of which exactly one creates a row.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/ProfileCreationIsOneStatementTest.kt` | create |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/e2e/IdentityMovesNoCoinTest.kt` — specifically
`apiPathLiteralsInRouteSources` and the upward directory walk it uses, which this file copies;
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresPlayerDirectory.kt`;
`docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §2 and §5.

## Scope

- A new class `duels.poker.server.db.ProfileCreationIsOneStatementTest`, `internal`. **It needs no
  database and must not call `PostgresTestSupport.requireDocker()`** — it reads source text.
- One private top-level helper:

  ```kotlin
  private fun mainSourceFilesContaining(statement: String): Set<String>
  ```

  It finds `poker-server/src/main/kotlin` by walking **upward** from `File("").absoluteFile` — the
  same technique `apiPathLiteralsInRouteSources` uses, so it does not depend on whether Gradle's test
  working directory is the module root or the repository root — `error(...)`s naming the absolute path
  it searched from if the directory is not found, then walks it, keeps files whose name ends `.kt`,
  and returns the **file names** of those whose text `contains` [statement].
- File names, not paths, and a set, not a count. A count would be a magic number stale on the next
  refactor, and would be tripped by the class KDoc in `PostgresPlayerDirectory.kt` that quotes
  `INSERT INTO player` in prose — legitimately, since it explains that very statement.
- Two honest limits, in the class KDoc rather than left for a reader to find. **Each must contain the
  exact phrase in bold**, because that is what `verify:` greps for — a limit a reader cannot find is
  the same as a limit nobody wrote:
  1. It reads source text, not the compiled statement set. A statement **assembled from constants**,
     or with a line break between `INSERT INTO` and the table name, escapes it. Every insert in the
     repository today writes both words together on one line, and this test is the reason to keep
     doing so.
  2. It is a **file-set assertion**. A *second* `INSERT INTO player` added inside
     `PostgresPlayerDirectory.kt` escapes it. That is deliberate: minting is that class's job, and
     the defect this guards against is a **new write path somewhere else**.

## Out of scope

- `UPDATE player`. `ADR-0030` §2 names three writing statements; only creation is a profile nobody
  asked for, and the other two are gated by the coin invariant and `ADR-0029`'s permanence trigger.
- Migration SQL under `src/main/resources`. `V7__device_binding.sql` legitimately contains an
  `INSERT INTO device_binding` backfill, and a migration is reviewed as a schema change, not as a
  write path. The walk starts at `src/main/kotlin` and reaches no `.sql` file.
- Changing any main source. `PostgresPlayerDirectory.kt` is already the only file this test permits;
  the ticket makes that a fact a gate holds.
- Extending `IdentityMovesNoCoinTest`. Its `SCENARIO_ENDPOINTS` is a fact about *that* scenario, and
  this test needs no database, so putting it there would gate it behind Docker for nothing.

## Tests

`ProfileCreationIsOneStatementTest`

| Test | Proves |
| --- | --- |
| `onlyThePlayerDirectoryCreatesAProfile` | `mainSourceFilesContaining("INSERT INTO player")` equals `setOf("PostgresPlayerDirectory.kt")`. A second file gaining that statement fails the build until either it is removed or this set is extended with a comment saying why a second profile-creating path is correct |
| `theScanTellsTwoStatementsApart` | `mainSourceFilesContaining("INSERT INTO duel_result")` equals `setOf("PostgresDuelResultStore.kt")`. **Two inputs, two different expected answers** — and the vacuity guard the test above cannot do without: a scan that matched nothing would satisfy an empty-versus-empty comparison, and one that matched everything would fail here |

## Acceptance criteria

- [ ] `ProfileCreationIsOneStatementTest.onlyThePlayerDirectoryCreatesAProfile` passes
- [ ] `ProfileCreationIsOneStatementTest.theScanTellsTwoStatementsApart` passes
- [ ] The two tests call the same `mainSourceFilesContaining` helper with two different arguments
- [ ] Neither test hard-codes a file **count**; both compare sets of file names
- [ ] The class does not call `PostgresTestSupport.requireDocker()` and opens no `DataSource`
- [ ] The class KDoc contains the literal phrases `assembled from constants` and
      `file-set assertion`, one per limit — the two `grep -qF` commands in `verify:` are the check
- [ ] The helper `error(...)`s naming the path it searched from when `poker-server/src/main/kotlin`
      is not found, rather than returning an empty set
- [ ] The diff touches exactly one file, and it is the one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

Add a second profile-creating path where none belongs: in
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt`, add a top-level
`private const val ORPHAN_MINT_SQL: String = "INSERT INTO player (id) VALUES (?)"`.

**`onlyThePlayerDirectoryCreatesAProfile` reddens alone**, with
*expected [PostgresPlayerDirectory.kt], got [PostgresPlayerDirectory.kt, PostgresProfileWrites.kt]*.
`theScanTellsTwoStatementsApart` searches a different statement and is unaffected. `detekt` may also
report the constant as unused, which is expected of a throwaway mutation and not a second finding.
Revert.

A second mutation, reddening the other test alone, and it is the one that demonstrates limit 1. In
`PostgresDuelResultStore.kt`, break the insert across two lines inside its raw string:

```
INSERT INTO
duel_result (duel_id, player_id, coin_delta) VALUES (?, ?, ?)
```

The SQL is still valid and the database suite still passes. **`theScanTellsTwoStatementsApart` reddens
alone**, with *expected [PostgresDuelResultStore.kt], got []* — `contains` matches an exact substring,
and the newline plus indentation now sits between the two words. `onlyThePlayerDirectoryCreatesAProfile`
is unaffected. This is exactly the escape route the class KDoc has to name, and running it is how the
KDoc gets written truthfully rather than defensively. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Ten ADRs already point at an epic nobody has written.** A test that fails the build is the only
kind of deferral that cannot be forgotten; a sentence in an ADR saying *"no other path may create a
profile"* has no exit code, and this story's whole subject is a negative over the schema — the shape
that goes ungated by default.

**The two `grep -qF` refusals are what make the blind spots part of the artefact.** This test cannot
see a statement assembled from constants, nor one with a line break between `INSERT INTO` and the
table name — the reviewer confirmed the second by planting a multi-line insert in a new file and
watching both tests stay green. Those are boundaries this ticket's Scope names, not gaps it failed to
close, and the verify block forces them into the class KDoc so the next writer reads them where the
rule lives rather than in a ticket nobody reopens.

**The vacuity guard is the second argument, not an emptiness check.** Making
`mainSourceFilesContaining` ignore its parameter and always return `{PostgresPlayerDirectory.kt}`
reddens the `duel_result` test while the `player` test passes — so two different non-empty expected
answers catch a helper that returns a constant, which a bare `isNotEmpty()` would not. The coder
considered adding a third emptiness test mirroring `IdentityMovesNoCoinTest`'s and declined, because
this ticket's Tests table already frames the two-argument shape as the guard. That was the right
call: one fixture default cannot tell a copy from a constant.
