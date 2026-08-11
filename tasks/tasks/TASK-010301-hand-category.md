---
schema: 2
id: TASK-010301
title: HandCategory, ordered low to high
type: task
status: ready
parent: STORY-0103
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [engine, rules]
depends_on: []
verify:
  - ./gradlew :poker-engine:test --tests '*HandCategoryTest'
  - ./gradlew :poker-engine:check
---

## Goal

The nine poker hand categories exist as an enum whose declaration order *is* their ranking, so
comparing categories never needs a table or a `when`.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/hand/HandCategory.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/hand/HandCategoryTest.kt` | create |

Read `docs/duel-rules.md` (the *Showdown* section) for the ranking order. Nothing else.

## Scope

- New package `duels.poker.engine.hand`.
- `public enum class HandCategory`, declared **weakest first** so that `compareTo` and `ordinal`
  both increase with strength:

  ```kotlin
  HIGH_CARD, PAIR, TWO_PAIR, THREE_OF_A_KIND, STRAIGHT,
  FLUSH, FULL_HOUSE, FOUR_OF_A_KIND, STRAIGHT_FLUSH
  ```

- KDoc on the enum stating that declaration order is the ranking and that reordering it silently
  changes who wins every hand.
- Nothing else on the type: no cards, no evaluation, no formatting.

## Out of scope

- `HandRank`, tiebreaks, comparison of whole hands — `TASK-010302`.
- Turning cards into a category — `TASK-010305`.
- A human-readable description such as *"two pair, kings and sevens"* — not ticketed.
- A royal flush constant. A royal flush is an ace-high straight flush and gets no category of
  its own.

## Tests

`HandCategoryTest`

| Test | Proves |
| --- | --- |
| `thereAreExactlyNineCategories` | `HandCategory.entries.size == 9` |
| `categoriesAreDeclaredWeakestFirst` | `HandCategory.entries` equals the exact list above, in that order |
| `strongerCategoriesCompareGreater` | every adjacent pair satisfies `entries[i] < entries[i + 1]`, and `STRAIGHT_FLUSH > FOUR_OF_A_KIND > FULL_HOUSE > FLUSH > STRAIGHT > THREE_OF_A_KIND > TWO_PAIR > PAIR > HIGH_CARD` |

## Acceptance criteria

- [ ] `HandCategoryTest.thereAreExactlyNineCategories` passes
- [ ] `HandCategoryTest.categoriesAreDeclaredWeakestFirst` passes
- [ ] `HandCategoryTest.strongerCategoriesCompareGreater` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
