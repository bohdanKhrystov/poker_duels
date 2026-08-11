---
schema: 2
id: TASK-010202
title: Card as a value class over a compact integer encoding
type: task
status: ready
parent: STORY-0102
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, domain]
depends_on: [TASK-010201]
verify:
  - ./gradlew :poker-engine:test --tests '*CardTest'
  - ./gradlew :poker-engine:check
---

## Goal

Exactly the 52 real cards are constructible, each one allocation-free, and the integer encoding
that makes the evaluator cheap is invisible from outside the type.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/card/Card.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/card/CardTest.kt` | create |

Read `Rank.kt` and `Suit.kt` for the ordinals; do not modify them.

## Scope

- This shape exactly — it compiles on Kotlin 2.0.21 and passes ktlint and detekt as configured:

  ```kotlin
  private const val SUIT_COUNT = 4

  @JvmInline
  public value class Card private constructor(private val code: Int) {
      public val rank: Rank get() = Rank.entries[code / SUIT_COUNT]
      public val suit: Suit get() = Suit.entries[code % SUIT_COUNT]

      public companion object {
          public fun of(rank: Rank, suit: Suit): Card =
              Card(rank.ordinal * SUIT_COUNT + suit.ordinal)

          public val all: List<Card> = /* rank-major: 2c 2d 2h 2s 3c … As */
      }
  }
  ```

- The constructor is `private` and the underlying `code` is `private`: an out-of-range card is
  not constructible, and no caller can depend on the numeric layout.
- `Card.all` is rank-major — for each `Rank` in order, each `Suit` in order — so `all[0]` is the
  two of clubs and `all[51]` is the ace of spades.
- KDoc on `Card` and on `all`, stating that the encoding is an implementation detail while the
  **order of `all` is contractual**, because the recorded shuffle in `TASK-010208` starts from it.

## Out of scope

- `toString`, `parse`, `parseOrNull` — `TASK-010203` adds them to this file.
- `Deck` — `TASK-010206`.
- Comparing cards by poker strength, or anything that knows about a hand or a game.
- Widening the encoding into the public API: no `code` accessor, no `Card(Int)` factory.

## Tests

`CardTest`

| Test | Proves |
| --- | --- |
| `allHoldsFiftyTwoDistinctCards` | `Card.all.size == 52` and `Card.all.toSet().size == 52` |
| `everyRankAndSuitRoundTrips` | for all 13 × 4 pairs, `Card.of(r, s).rank == r` and `.suit == s` |
| `allIsOrderedByRankThenSuit` | for every `i in 0..51`, `Card.all[i] == Card.of(Rank.entries[i / 4], Suit.entries[i % 4])` |
| `cardsWithTheSameRankAndSuitAreEqual` | `Card.of(ACE, SPADES)` equals another such card with an equal `hashCode`, and differs from `Card.of(ACE, HEARTS)` |

## Acceptance criteria

- [ ] `CardTest.allHoldsFiftyTwoDistinctCards` passes
- [ ] `CardTest.everyRankAndSuitRoundTrips` passes
- [ ] `CardTest.allIsOrderedByRankThenSuit` passes
- [ ] `CardTest.cardsWithTheSameRankAndSuitAreEqual` passes
- [ ] `Card.kt` exposes no public constructor and no public accessor for the integer code
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
