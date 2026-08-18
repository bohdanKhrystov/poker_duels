---
schema: 2
id: TASK-041007
title: The display-name schema test registers what must land and keeps its refusals raw
type: task
status: done
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, test, fixtures, identity]
depends_on: [TASK-041006]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.DisplayNameSchemaTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`DisplayNameSchemaTest` keeps asserting `player_display_name_length`, `player_display_name_trimmed`
and `player_display_name_nfc` by name, because the writes those tests make stay raw while the writes
that must land register first.

## Why this file is the trap

`name_registry` repeats `player`'s three `CHECK`s **verbatim** (`ADR-0051` §1: *"the three `CHECK`s
hold every row in the same shape `player.display_name` is held in"*). So a registry insert placed in
front of a deliberately-bad name raises `23514` naming `name_registry_length`,
`name_registry_trimmed` or `name_registry_nfc` — the same `SQLSTATE`, a different constraint — and
five assertions in this file, all of which match on the constraint **name**, go red for a reason
that looks like a schema bug and is not.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNameSchemaTest.kt` | modify — one helper split into two, call sites reassigned |
| `poker-server/src/main/resources/db/migration/V5__name_registry.sql` | read — the five `CHECK`s |
| `poker-server/src/main/resources/db/migration/V3__player_display_name.sql` | read — the three `CHECK`s it mirrors |

## Scope

`insertPlayerWithName(displayName)` becomes two helpers with the same body except for one statement:

```kotlin
/** Registers the name, then inserts the player holding it: for names that must land. */
private fun insertPlayerWithName(displayName: String): UUID

/** Inserts the player holding the name without registering it: for names the CHECKs must refuse. */
private fun insertPlayerWithRawName(displayName: String): UUID
```

Which test uses which — this is the whole ticket, and it is per **call site**, not per test method,
because three methods make both kinds of write:

| Test | Registering | Raw |
| --- | --- | --- |
| `aNameOfThirtyTwoCodePointsIsStored` | `"a".repeat(32)` | — |
| `aNameOfThirtyThreeCodePointsIsRefused` | — | `"a".repeat(33)` |
| `theBoundIsCountedInCodePointsNotUtf16Units` | `"𝔄".repeat(17)` | — |
| `anEmptyNameIsRefused` | `"a"` (the paired acceptance) | `""` |
| `aNameWithLeadingOrTrailingSpaceIsRefused` | `"bob"` (the paired acceptance) | `" bob"`, `"bob "` |
| `aDecomposedNameIsRefused` | — | the NFD string |
| `theComposedFormOfTheSameNameIsStored` | `"élodie"` | — |
| `manyProfilesWithNoNameCoexist` | — (uses `insertPlayerWithoutName`) | — |

- After `TASK-041010`'s foreign key, the raw inserts still report the `player` `CHECK`: `CHECK`
  constraints are evaluated as the row is formed and foreign keys as `AFTER` triggers, so the
  constraint that fires first is the one these tests name.
- No assertion, no test name and no fixture string changes. `insertPlayerWithoutName` and
  `readDisplayName` are untouched.

## Out of scope

- Any assertion about a `name_registry_*` constraint. The registry's own `CHECK`s are `V5`'s and are
  covered by `TASK-041002`'s existence tests and `TASK-041011`'s monotonicity tests; this file is
  about `player`'s.
- Changing `assertTrue(exception.message?.contains(...))` into anything else.

## Tests

`DisplayNameSchemaTest`, `-PrequireDocker=true`. Nothing added; eight existing tests must pass.

| Test | Proves, after this change |
| --- | --- |
| `aNameOfThirtyThreeCodePointsIsRefused` | `23514` **and** the message still contains `player_display_name_length`. **The wrong implementation this must fail against**: routing the 33-character name through the registering helper, which answers `23514` with `name_registry_length` — same code, wrong constraint, and an assertion on `sqlState` alone would not notice |
| `aNameWithLeadingOrTrailingSpaceIsRefused` | The same, twice, for `player_display_name_trimmed`, and the paired acceptance of `"bob"` still lands |
| `aDecomposedNameIsRefused` | The same, for `player_display_name_nfc` |
| `anEmptyNameIsRefused` | `player_display_name_length` for `""`, and `"a"` still lands |
| `aNameOfThirtyTwoCodePointsIsStored`, `theBoundIsCountedInCodePointsNotUtf16Units`, `theComposedFormOfTheSameNameIsStored` | The registering helper lands a legal name. Without these three, a change that made *every* call raw would leave the refusal tests green and break nothing until the foreign key lands |

## Acceptance criteria

- [ ] Every test in `duels.poker.server.db.DisplayNameSchemaTest` passes with its assertions
      unchanged
- [ ] Two helpers exist: one that registers before inserting the player, one that does not
- [ ] The raw helper is called with exactly these five values and no others: `"a".repeat(33)`, `""`,
      `" bob"`, `"bob "`, and the NFD form of `"élodie"`
- [ ] No `assertTrue`/`assertEquals` in the file names a `name_registry_*` constraint
- [ ] No test method is added, removed, renamed or has an assertion changed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
