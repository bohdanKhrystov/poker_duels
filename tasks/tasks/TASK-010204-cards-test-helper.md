---
schema: 2
id: TASK-010204
title: cards("As Kd 7h") test helper for readable card lists
type: task
status: done
parent: STORY-0102
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [engine, test]
depends_on: [TASK-010203]
verify:
  - ./gradlew :poker-engine:test --tests '*CardsTest'
  - ./gradlew :poker-engine:check
---

## Goal

Tests across the engine can write a list of cards as one string, and a typo or a duplicated card
fails loudly instead of quietly changing what the test means.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/card/Cards.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/card/CardsTest.kt` | create |

Both live in **test** sources. Read `Card.kt` for `parse`.

## Scope

- `internal fun cards(notation: String): List<Card>` in package `duels.poker.engine.card`,
  under `src/test/kotlin`.
- Splits on runs of whitespace, parses each token with `Card.parse`, and returns them in the
  order written.
- `require` that the result is non-empty and that no card appears twice; the message names the
  duplicate.
- One KDoc line saying why this is test-only: production code has no reason to build cards from
  strings, and giving it one would put a parser on a hot path.

## Out of scope

- Production use. Nothing under `src/main/kotlin` may reference this helper.
- Parsing boards, hole cards or ranges — the types do not exist yet (`STORY-0104`).
- Any change to `Card.kt`; `TASK-010203` already provides everything needed.

## Tests

`CardsTest`

| Test | Proves |
| --- | --- |
| `parsesAWhitespaceSeparatedList` | `cards("As Kd 7h")` is exactly `[Ace of spades, King of diamonds, Seven of hearts]`, in that order, and extra spaces are tolerated |
| `rejectsADuplicateCard` | `cards("As Kd As")` throws `IllegalArgumentException` |
| `rejectsBlankNotation` | `cards("   ")` throws `IllegalArgumentException` |
| `rejectsAnInvalidCard` | `cards("As 10d")` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `CardsTest.parsesAWhitespaceSeparatedList` passes
- [ ] `CardsTest.rejectsADuplicateCard` passes
- [ ] `CardsTest.rejectsBlankNotation` passes
- [ ] `CardsTest.rejectsAnInvalidCard` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
