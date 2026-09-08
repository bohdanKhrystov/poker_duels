---
schema: 2
id: TASK-140908
title: The removal bit reads false for a renamer, for two independent reasons
type: task
status: done
parent: STORY-1409
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, db, account]
depends_on: [TASK-140907]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - grep -q 'tests="55" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.db.PostgresProfileReadsTest.xml
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.TakedownIsInvisibleTest' -PrequireDocker=true
  - grep -q 'tests="4" skipped="0" failures="0" errors="0"' poker-server/build/test-results/test/TEST-duels.poker.server.db.TakedownIsInvisibleTest.xml
  - grep -qF "r.retired_from = p.id AND r.reason = 'RETIRED'" poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt
  - sh -c '! grep -qF "redundant under the partial index" poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - ./gradlew check -PrequireDocker=true
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A player who renamed themselves reads `displayNameRemoved: false`, and the two independent reasons
`ADR-0134` §4 gives for it are each pinned by a test of their own — so a later mechanism that wrote
`RETIRED` instead of `REPLACED`, or a later query that dropped the `reason` clause, reddens rather
than quietly showing a renamer a moderation notice.

## Files

Two. No statement changes: `ADR-0134` §4 says `PostgresProfileReads.PROFILE_OF_SQL` changes **not at
all**, and a `verify` command pins the correlated `EXISTS` character for character.

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify |

Read, and do not edit:
[`ADR-0134`](../../docs/adr/ADR-0134-a-rename-spends-before-it-replaces.md) §4;
[`ADR-0053`](../../docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md) §2 and §3;
[`ADR-0052`](../../docs/adr/ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md) §6.

## Scope

- **Two new `PostgresProfileReadsTest` cases**, both driving a real rename through
  `PostgresProfileWrites.setDisplayName` rather than hand-writing a registry row — the same
  discipline `RetiredNameIsSpentTest` keeps about `RETIRED`.
- **`PostgresProfileReads`'s comment is corrected, not deleted.** Its shipped line says
  `r.reason = 'RETIRED'` is *"redundant under the partial index … and stays"*. Under `ADR-0134` §3
  it stops being redundant: a `REPLACED` row now writes an entry into
  `name_registry_retired_from_idx` where a `TAKEN` row did not, so that clause is what tells the two
  ways a string left a player apart. The comment must say so. **No SQL in the file changes.**

## Out of scope

- **`PROFILE_OF_SQL`, `DUEL_LINES`, `RECENT_DUELS_SQL` and every other statement in that file.**
  `ADR-0134` §4 is explicit; the `verify` grep pins the `EXISTS` clause as it stands.
- **`PostgresProfileWrites`'s `false` literal for `displayNameRemoved`.** `ADR-0134` §4 keeps it:
  `NameSet` describes a player who now holds a name, so it is false by construction.
- **Narrowing `name_registry_retired_from_idx`.** `ADR-0134` §3: available later, buys nothing on a
  table this size.
- **`docs/adr/README.md`'s `ADR-0053` row.** Measured on `develop` at `1c3c7fd9`: the row already
  carries `ADR-0134` §4's fifth case and the corrected note about the `reason` clause — `ADR-0134`'s
  own merge did it. The register that is still stale is `docs/protocol.md`, and `TASK-140907` owns
  it.

## Tests

`PostgresProfileReadsTest` — **52** tests on `develop` (measured 2026-09-09), **55** after — two for the renamer, and a third added at review to pin the `reason` clause. The ticket was written against 50 at `1c3c7fd9`; `TASK-140806` added two password-flag cases to this class in between, so the baseline moved before this ticket started.

| Test | Proves |
| --- | --- |
| `aPlayerWhoRenamedThemselvesWasNotMovedOn` | one database, **two** players: one renamed through `setDisplayName` twice, one whose name an operator retired through `retire_display_name`. `profileOf` reads `displayNameRemoved: false` for the renamer and `true` for the taken-down player |
| `theRenamersRowIsReplacedAndNotRetired` | after that same rename, `SELECT reason FROM name_registry WHERE retired_from = <renamer>` is exactly `REPLACED` — and for the taken-down player it is exactly `RETIRED`. `ADR-0134` §4's second reason, asserted directly rather than inferred from the bit |

## What would still pass if the coder got it wrong

- **`ADR-0134` §4 warns that a test pinning only the conjunction would pass on a mechanism that
  wrote `RETIRED`.** That is exactly right, and it is why there are two tests:
  `aPlayerWhoRenamedThemselvesWasNotMovedOn` passes whether the row says `RETIRED` or `REPLACED`,
  because the renamer holds a name and the first conjunct is already false.
  `theRenamersRowIsReplacedAndNotRetired` is the one that fails.
- **Both tests carry two subjects that disagree.** The bit test asserts `false` for the renamer and
  `true` for the taken-down player in **one** database; the row test asserts `REPLACED` for one and
  `RETIRED` for the other. A query wired to a constant — always `false`, always `true`, always
  `RETIRED` — fails one half of each. A single-subject fixture at `false` could not tell a correct
  conjunction from a hard-coded literal, and `false` is exactly the value the defect leaves
  unchanged.
- **If `PROFILE_OF_SQL`'s `r.reason = 'RETIRED'` clause were dropped**, `ADR-0053`'s comment would be
  right that it is redundant *today* — the partial index only carries rows with a `retired_from` —
  but after `TASK-140902` a `REPLACED` row writes an entry too, so
  `aPlayerWhoRenamedThemselvesWasNotMovedOn` fails on the renamer. That is the whole reason the
  comment is corrected here rather than left describing a world that ended.
- **If either test hand-wrote its registry row**, it would prove the query and not the mechanism.
  Both go through `setDisplayName` and `retire_display_name`.

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.aPlayerWhoRenamedThemselvesWasNotMovedOn` passes, asserting `false`
      for the renamer **and** `true` for the taken-down player in one database
- [ ] `PostgresProfileReadsTest.theRenamersRowIsReplacedAndNotRetired` passes, asserting `REPLACED`
      for one player **and** `RETIRED` for the other
- [ ] `PostgresProfileReadsTest` reports exactly 52 tests, `failures="0" errors="0"` — 50 measured
      on `develop` at `1c3c7fd9` plus exactly the two above, and no merged assertion in the file is
      weakened
- [ ] `TakedownIsInvisibleTest` reports 4 tests and is not edited
- [ ] `PROFILE_OF_SQL` still contains `r.retired_from = p.id AND r.reason = 'RETIRED'`
- [ ] `PostgresProfileReads.kt` no longer says that clause is redundant
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
