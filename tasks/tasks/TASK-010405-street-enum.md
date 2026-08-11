---
schema: 2
id: TASK-010405
title: Street enum with board size and successor
type: task
status: done
parent: STORY-0104
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [engine, domain, contract]
depends_on: []
verify:
  - ./gradlew :poker-engine:test --tests '*StreetTest'
  - ./gradlew :poker-engine:check
---

## Goal

The six phases a hand passes through exist as one enum, each knowing how many board cards it
shows and which phase follows it.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Street.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/StreetTest.kt` | create |

New package `duels.poker.engine.game`. Nothing else in the repository needs to be read.

## Scope

- `Street.kt`, package `duels.poker.engine.game`:

  ```kotlin
  public enum class Street(public val boardCards: Int) {
      PREFLOP(0),
      FLOP(3),
      TURN(4),
      RIVER(5),
      SHOWDOWN(5),
      COMPLETE(5),
      ;

      /** True while chips can still go in: preflop through the river. */
      public val isBetting: Boolean get() = this <= RIVER

      /** The street that follows, or null after [COMPLETE]. */
      public val next: Street? get() = entries.getOrNull(ordinal + 1)
  }
  ```

- KDoc on the enum: `boardCards` is how many cards are face up **once the street has been
  dealt**; a hand that ends by a fold can sit in `COMPLETE` with fewer.
- Declaration order is contractual — `next` and `isBetting` are both derived from it.

## Out of scope

- Dealing anything, or deciding when a street advances — STORY-0105.
- `Board` — `TASK-010406`.

## Tests

`StreetTest`, JUnit 5 (`org.junit.jupiter.api.Test`, `Assertions.assertEquals`), matching the
style of the existing tests in `poker-engine/src/test/kotlin`.

| Test | Proves |
| --- | --- |
| `boardCardsGrowWithTheStreet` | `PREFLOP`=0, `FLOP`=3, `TURN`=4, `RIVER`=5, `SHOWDOWN`=5, `COMPLETE`=5 |
| `nextFollowsDeclarationOrder` | `PREFLOP.next == FLOP`, `FLOP.next == TURN`, `TURN.next == RIVER`, `RIVER.next == SHOWDOWN`, `SHOWDOWN.next == COMPLETE` |
| `completeHasNoSuccessor` | `COMPLETE.next == null` |
| `bettingRunsFromPreflopToTheRiver` | `isBetting` is true for `PREFLOP`, `FLOP`, `TURN`, `RIVER` and false for `SHOWDOWN`, `COMPLETE` |

## Acceptance criteria

- [ ] `StreetTest.boardCardsGrowWithTheStreet` passes
- [ ] `StreetTest.nextFollowsDeclarationOrder` passes
- [ ] `StreetTest.completeHasNoSuccessor` passes
- [ ] `StreetTest.bettingRunsFromPreflopToTheRiver` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
