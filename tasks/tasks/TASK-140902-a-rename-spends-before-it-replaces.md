---
schema: 2
id: TASK-140902
title: A rename spends before it replaces
type: task
status: done
parent: STORY-1409
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 4
atomic:
  - "`:poker-server:test` — `TakedownIsInvisibleTest.retiredFromIsReadInExactlyOneFile` is an exact set equality over the main sources naming `retired_from`, and `ADR-0134` §3 requires this class to write that column"
  - "`:poker-server:test` — four `PostgresProfileWritesTest` cases assert `AlreadyNamed` or a lost race for a rename the four-statement write now completes"
  - "`:poker-server:test` — `ProfileEndpointsDatabaseTest.aSecondNameForTheSameProfileIsForbidden` drives the whole route against a real database and asserts `403`"
labels: [server, db, account]
depends_on: [TASK-140901]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileWritesTest' -PrequireDocker=true
  - grep -q 'tests="17" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.db.PostgresProfileWritesTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.TakedownIsInvisibleTest' -PrequireDocker=true
  - grep -q 'tests="4" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.db.TakedownIsInvisibleTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileEndpointsDatabaseTest' -PrequireDocker=true
  - grep -q 'tests="7" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.http.ProfileEndpointsDatabaseTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.RetireDisplayNameTest' -PrequireDocker=true
  - grep -qF "FOR UPDATE" poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt
  - grep -qF "REPLACED" poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt
  - sh -c '! grep -qF "display_name IS NULL" poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt'
  - sh -c '! grep -qF "AND reason" poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`PostgresProfileWrites.setDisplayName` runs `ADR-0134` §2's four statements in one transaction —
lock the profile and read the name it holds, spend the new string, mark the string being left
`REPLACED` with `retired_from` set, hand the new string over — so a player who holds `Ann` and
writes `Bea` ends up holding `Bea` with `Ann` spent forever, and a rename that is refused leaves
**both** strings exactly where they were.

## Files

Four, **probed not remembered** (`ADR-0069`, `ADR-0070`). The four statements were written into
`PostgresProfileWrites.kt` alone and `./gradlew check -PrequireDocker=true` — the command
`.github/workflows/build.yml` runs on a pull request — was run in full: `1794 tests completed, 6
failed`, in the three test files below. The minimal propagation was applied at each and the command
was run again to `BUILD SUCCESSFUL`. Docker was available and every database suite ran, so this
enumeration is complete rather than a prefix.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | modify | The four statements, the lock, and the loss of `AND display_name IS NULL` |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileWritesTest.kt` | modify | `aDifferentNameForANamedPlayerIsRefused`, `noSqlExceptionEscapes`, `twoWritersRacingForTheSameProfileExactlyOneWins` and `aRefusedSecondNameLeavesNoRegistryRow` all assert the refusal this change removes |
| `poker-server/src/test/kotlin/duels/poker/server/db/TakedownIsInvisibleTest.kt` | modify | `retiredFromIsReadInExactlyOneFile` asserts `setOf("PostgresProfileReads.kt")` over every main source naming `retired_from`; statement 3 makes that set two |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileEndpointsDatabaseTest.kt` | modify | `aSecondNameForTheSameProfileIsForbidden` drives `PUT /api/me/name` twice against a container and asserts `403` on the second |

Read, and do not edit:
[`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) §2 and §3;
[`ADR-0051`](../../docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md) §2's rollback;
`poker-server/src/main/resources/db/migration/V5__name_registry.sql` — `retire_display_name`'s
statement order, which statement 1's lock order copies.

## Scope

- **Statement 1, before every write:** `SELECT display_name FROM player WHERE id = ? FOR UPDATE`.
  The profile is locked **before any registry row**, which is the order `retire_display_name`
  already takes. No row is unreachable and is the `check(rows.next())` shape `readProfile` already
  uses — a `500`, never a fabricated refusal (`ADR-0134` §2).
- **Statement 2 stays first among the writes**, unchanged: `INSERT INTO name_registry (name,
  reason) VALUES (?, 'TAKEN')`, and `23505` still routes to the existing conflict branch.
- **Statement 3 runs only when statement 1 returned a name:**
  `UPDATE name_registry SET reason = 'REPLACED', retired_from = ? WHERE name = ?`. It carries
  **no** `AND reason = 'TAKEN'`, deliberately — with the predicate an impossible row updates zero
  rows in silence and the trigger then passes; without it, `ADR-0051` §3's monotonicity trigger
  raises `23001` and the whole call fails loudly (`ADR-0134` §2).
- **Statement 4 loses `AND display_name IS NULL`.** Statement 1's `FOR UPDATE` is the interlock now.
  A zero-row result is unreachable and is a `check`, not a fourth outcome.
- **`SetNameResult.AlreadyNamed` is no longer returned by this class.** It still exists on the port
  after this ticket — deleting it is `TASK-140903` — and the `when`/`if` here must simply never
  produce it.
- **`ADR-0051` §2's rollback is untouched and now protects two strings.** Anything other than one
  row from statement 4 rolls the whole transaction back, so the new string is un-spent *and* the
  old string goes back to `TAKEN`.
- **`TakedownIsInvisibleTest.retiredFromIsReadInExactlyOneFile` gains one file name**, to
  `setOf("PostgresProfileReads.kt", "PostgresProfileWrites.kt")`, and its KDoc says why: `ADR-0134`
  §3 makes this class the second writer of that column, and the set is still an exact equality so a
  third file still fails.

## Out of scope

- **Deleting `SetNameResult.AlreadyNamed`, the `403` branch or anything in `ProfileRoutes.kt`.**
  `TASK-140903`.
- **The budget.** `TASK-140906`. This ticket adds no `AttemptBudget` and no `429`.
- **`PostgresProfileReads`.** `ADR-0134` §4: it changes *not at all*.
- **The `hasRecoveryEmail` literal in `toProfile()`.** It stays a literal `false`. `ADR-0132`
  §Residuals owns that defect and `STORY-1408` names it as an owed ticket; fixing it here would
  widen scope, and asserting it would freeze a known defect as intended behaviour.
- **`retire_display_name`, and any migration.** `TASK-140901` shipped the schema.
- **A data-modifying CTE.** `ADR-0134` §Alternatives rejects it: sub-statements share one snapshot
  and the `player` trigger must observe the retirement the same statement performs.

## Tests

`PostgresProfileWritesTest` — **15** tests on `develop` (measured 2026-09-08), **17** after this ticket. The ticket was written against 13 at `1c3c7fd9`; `TASK-140807` merged two password-flag tests into this same class afterwards, so the baseline moved before this ticket was started.

| Test | Proves |
| --- | --- |
| `aDifferentNameForANamedPlayerReplacesTheOne` | *(rewritten from `…IsRefused`)* a holder of `Alice` writing `Alicia` gets `NameSet`, the column reads `Alicia`, and `Alice`'s registry row reads `REPLACED` |
| `theReplacedRowRecordsThePlayerThatLeftIt` | *(new)* after that same rename, `name_registry.retired_from` for `Alice` equals that player's id — and for a **second** player who renamed in the same database it equals *their* id, not the first's |
| `aRefusedRenameLeavesBothStringsWhereTheyWere` | *(rewritten from `aRefusedSecondNameLeavesNoRegistryRow`)* a holder of `Ann` writing a name another player holds gets `NameTaken`, `Ann` is still `TAKEN`, the column still reads `Ann`, and the refused string has exactly one row — the one its real holder owns |
| `aRenameThatFailsAfterTheRetirementRollsTheRetirementBack` | *(new)* the two-string rollback `ADR-0134` §2 calls more load-bearing than before: force statement 4 to fail (a raw row already holding the arriving string, so `player_display_name_unique` fires) and assert the old row is back to `TAKEN` |
| `twoWritersRacingForTheSameProfileBothSucceedAndOneStringIsLeft` | *(rewritten)* both concurrent writes answer `NameSet` — `ADR-0134` §2's *"two concurrent PUTs from one player are both legal now"* — the column holds one of the two, and the other string is `REPLACED`, never `TAKEN` |
| `noSqlExceptionEscapes` | *(modified)* the second call is a rename that now succeeds; the `NameTaken` half is unchanged |
| `sendingTheSameNameAgainSucceeds`, `aDifferentCaseOfOwnNameIsRefused`, `theSameNameAgainIsStillTheSameProfile`, `twoPlayersRacingForTheSameNameExactlyOneGetsIt` | *(unchanged, must stay green)* the idempotent retry, the case-fold `409`, the clean-transaction read and the two-player race |

`TakedownIsInvisibleTest`

| Test | Proves |
| --- | --- |
| `retiredFromIsReadInExactlyOneFile` | *(modified)* the exact set of main sources naming `retired_from` is those two files and no third |
| `retiredFromAppearsExactlyTwiceInPostgresProfileReads` | *(unchanged, must stay green)* the count inside the reads file is still two |

`ProfileEndpointsDatabaseTest`

| Test | Proves |
| --- | --- |
| `aSecondNameForTheSameProfileReplacesTheFirst` | *(rewritten)* two `PUT`s from one device answer `200` and `200`, and `GET /api/me` afterwards reads the **second** name |

## What would still pass if the coder got it wrong

- **If statement 3 were skipped entirely**, statement 4's trigger raises `23001` and
  `aDifferentNameForANamedPlayerReplacesTheOne` fails — the schema catches it. But **if statement 3
  set `RETIRED` instead of `REPLACED`**, the trigger passes and every behavioural test stays green.
  Only the explicit `assertEquals("REPLACED", …)` in that test catches it, which is why it is there
  rather than an assertion on the column alone.
- **If `retired_from` were left `NULL`**, the trigger still passes, `REPLACED` is still written and
  nothing behavioural moves — `theReplacedRowRecordsThePlayerThatLeftIt` is the only gate.
  **It uses two players who disagree**: a wrong-but-constant `retired_from` (the first player, a
  hard-coded id, the arriving name's holder) satisfies one row and fails the other. One fixture at
  one id could not tell a correlation from a constant.
- **If statement 1's `FOR UPDATE` were dropped**, everything single-threaded stays green;
  `twoWritersRacingForTheSameProfileBothSucceedAndOneStringIsLeft` is what fails, because without
  the lock both writers read the same old string and the loser's statement 3 meets a row that is no
  longer `TAKEN`, raising `23001`.
- **If statement 3 grew an `AND reason = 'TAKEN'`**, nothing in the happy path notices — it is the
  silent-zero-rows defect `ADR-0134` §2 names. A `verify` command greps the file for that predicate
  rather than trusting a test, because the state it protects against is unreachable from the server
  and so cannot be constructed in a test at all.
- **If the rollback were narrowed to the new string**,
  `aRenameThatFailsAfterTheRetirementRollsTheRetirementBack` is the only failure, and it is the
  defect `ADR-0051` §9 makes unrepairable — a player holding a string the registry says is spent.

## Acceptance criteria

- [ ] `PostgresProfileWritesTest.aDifferentNameForANamedPlayerReplacesTheOne` passes
- [ ] `PostgresProfileWritesTest.theReplacedRowRecordsThePlayerThatLeftIt` passes, with two players
      whose `retired_from` values differ
- [ ] `PostgresProfileWritesTest.aRefusedRenameLeavesBothStringsWhereTheyWere` passes
- [ ] `PostgresProfileWritesTest.aRenameThatFailsAfterTheRetirementRollsTheRetirementBack` passes
- [ ] `PostgresProfileWritesTest.twoWritersRacingForTheSameProfileBothSucceedAndOneStringIsLeft` passes
- [ ] `PostgresProfileWritesTest` reports exactly 17 tests, `failures="0" errors="0"` — 15 measured
      on `develop` at `1c3c7fd9` plus the two new cases named above
- [ ] `sendingTheSameNameAgainSucceeds`, `aDifferentCaseOfOwnNameIsRefused`,
      `theSameNameAgainIsStillTheSameProfile` and `twoPlayersRacingForTheSameNameExactlyOneGetsIt`
      pass with **no assertion weakened**
- [ ] `TakedownIsInvisibleTest` reports exactly 4 tests and `retiredFromIsReadInExactlyOneFile`
      asserts a two-element set, still by equality
- [ ] `ProfileEndpointsDatabaseTest` reports exactly 7 tests and none asserts `Forbidden`
- [ ] `RetireDisplayNameTest` passes with its file unedited
- [ ] `display_name IS NULL` appears nowhere in `PostgresProfileWrites.kt`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
