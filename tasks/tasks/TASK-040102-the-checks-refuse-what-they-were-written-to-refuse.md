---
schema: 2
id: TASK-040102
title: The three checks refuse what they were written to refuse
type: task
status: backlog
parent: STORY-0401
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, schema, tests, identity]
depends_on: [TASK-040101]
verify:
  - ./gradlew :poker-server:test --tests '*DisplayNameSchemaTest' -PrequireDocker=true
---

## Goal

Each of the three `CHECK` constraints `TASK-040101` added is proven to fire, by planting the exact
violation it was written for — and to *not* fire on the value beside it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNameSchemaTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/SchemaConstraintsTest.kt` | read — `PostgresTestSupport.freshDatabase()`, the `insertPlayer` helper shape, how a `SQLException` is asserted |

## Scope

- A new test class in `duels.poker.server.db`, set up like `SchemaConstraintsTest`: a fresh
  container database, `Migrations.migrate`, and a private helper that inserts a `player` row with a
  given name.
- **Every refusal is asserted on `sqlState`, never on the message text.** A check violation is
  `23514`. Asserting the constraint *name* as well is fine — asserting only the English is not.
- **Each refusal is paired with the acceptance beside it**, so a test cannot pass against a column
  that refuses everything.

## Out of scope

- Uniqueness (`TASK-040103`) and permanence (`TASK-040104`). Different guarantees, different files.
- The Kotlin canonical form (`TASK-040105`). This ticket is about what the database refuses when
  something bypasses the write path, which is the whole reason the constraints exist.

## Tests

`DisplayNameSchemaTest`

| Test | Proves |
| --- | --- |
| `aNameOfThirtyTwoCodePointsIsStored` | 32 characters are accepted — the boundary from the allowed side |
| `aNameOfThirtyThreeCodePointsIsRefused` | 33 characters raise `23514` naming `player_display_name_length` |
| `theBoundIsCountedInCodePointsNotUtf16Units` | 17 astral characters (`U+1D504`, 34 UTF-16 units, 17 code points) are **accepted** — `char_length` counts what the write path will count |
| `anEmptyNameIsRefused` | `''` raises `23514`; the lower bound is not decorative |
| `aNameWithLeadingOrTrailingSpaceIsRefused` | `' bob'` and `'bob '` each raise `23514` naming `player_display_name_trimmed` |
| `aDecomposedNameIsRefused` | `"élodie"` raises `23514` naming `player_display_name_nfc` |
| `theComposedFormOfTheSameNameIsStored` | `"élodie"` is accepted and reads back identical — the pair above is what makes the NFC check meaningful |
| `manyProfilesWithNoNameCoexist` | three rows with `display_name IS NULL` insert without complaint |

## Acceptance criteria

- [ ] All eight tests above pass
- [ ] Every refusal asserts `sqlState == "23514"`, and no assertion depends on the wording of a
      Postgres message
- [ ] The astral test uses a code point above `U+FFFF` and a length that would fail if the column
      counted UTF-16 units
- [ ] The NFC pair uses the same visible name in both forms, so the only difference asserted is the
      normalisation
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
