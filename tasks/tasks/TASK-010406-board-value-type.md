---
schema: 2
id: TASK-010406
title: Board value type that can only hold 0, 3, 4 or 5 cards
type: task
status: done
parent: STORY-0104
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [engine, domain, contract]
depends_on: [TASK-010405]
verify:
  - ./gradlew :poker-engine:test --tests '*BoardTest'
  - ./gradlew :poker-engine:check
---

## Goal

The community cards are a type that cannot represent an impossible board — one, two or six
cards throw at construction rather than travelling through the engine.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Board.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/BoardTest.kt` | create |

Read `poker-engine/src/main/kotlin/duels/poker/engine/card/Card.kt` for the `Card` API. Do not
modify it.

## Scope

- `Board.kt`, package `duels.poker.engine.game`:

  ```kotlin
  public data class Board(val cards: List<Card>) {
      init {
          require(cards.size in LEGAL_SIZES) { "A board holds 0, 3, 4 or 5 cards, not ${cards.size}" }
          require(cards.toSet().size == cards.size) { "Duplicate card on board $cards" }
      }

      public val size: Int get() = cards.size

      /** This board plus [more], validated by the same rules. */
      public fun dealt(more: List<Card>): Board = Board(cards + more)

      public companion object {
          public val EMPTY: Board = Board(emptyList())
      }
  }
  ```

  `LEGAL_SIZES` is a `private val` set (or a `private val` list) in the file — detekt runs with
  `maxIssues: 0` and flags bare literals.
- KDoc: the board is the community cards only; hole cards live on `Seat`.

## Out of scope

- Any link between `Board.size` and `Street` — `GameState` enforces that in `TASK-010409`.
- Dealing the flop, turn or river — STORY-0105.

## Tests

`BoardTest`, JUnit 5. Build cards with the existing test helper
`duels.poker.engine.card.cards("As Kd ...")`.

| Test | Proves |
| --- | --- |
| `acceptsTheFourLegalSizes` | boards of 0, 3, 4 and 5 cards all construct |
| `rejectsAnyOtherSize` | 1, 2 and 6 cards each throw `IllegalArgumentException` |
| `rejectsADuplicateCard` | `"As As Kd"` throws `IllegalArgumentException` |
| `dealtExtendsTheBoard` | `Board.EMPTY.dealt(cards("As Kd Qh")).cards` is those three, and `.dealt(cards("2c"))` on it gives a size-4 board |
| `dealtRejectsAnIllegalResult` | dealing one card onto `Board.EMPTY` throws |
| `emptyIsTheZeroCardBoard` | `Board.EMPTY.size == 0` |

## Acceptance criteria

- [ ] `BoardTest.acceptsTheFourLegalSizes` passes
- [ ] `BoardTest.rejectsAnyOtherSize` passes
- [ ] `BoardTest.rejectsADuplicateCard` passes
- [ ] `BoardTest.dealtExtendsTheBoard` passes
- [ ] `BoardTest.dealtRejectsAnIllegalResult` passes
- [ ] `BoardTest.emptyIsTheZeroCardBoard` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
