---
schema: 2
id: TASK-010206
title: Immutable Deck that deals without mutating itself
type: task
status: ready
parent: STORY-0102
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, domain]
depends_on: [TASK-010202]
verify:
  - ./gradlew :poker-engine:test --tests '*DeckTest'
  - ./gradlew :poker-engine:check
---

## Goal

A deck exists that hands out cards by returning a new deck, so no caller can deal the same card
twice by holding a stale reference.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/card/Deck.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/card/DeckTest.kt` | create |

Read `Card.kt` for `Card.all`.

## Scope

- This shape:

  ```kotlin
  public data class Deck private constructor(private val cards: List<Card>) {
      public val remaining: Int get() = cards.size

      public fun deal(count: Int): Deal

      /** The cards taken, in order, and the deck that remains. */
      public data class Deal(val cards: List<Card>, val deck: Deck)

      public companion object {
          public fun full(): Deck // Card.all, in order
      }
  }
  ```

- The **top of the deck is the front of the list**: `deal(2)` returns the first two cards and a
  deck holding the other 50, and a following `deal` continues where the first stopped.
- `require(count in 0..remaining)`, with a message giving `count` and `remaining`. Dealing more
  than remains never returns a short list.
- `override fun toString(): String = "Deck(remaining=$remaining)"`, with a one-line *why*: the
  contents of a live deck are secret, and a data class default would print them into any log
  that touches one.
- KDoc on the class: immutable, dealing returns a new deck, and the initial order is `Card.all`.

## Out of scope

- `shuffled` and Fisher–Yates — `TASK-010207`, which reopens this file. Do not add a shuffle,
  an `Rng` parameter, or an import from `duels.poker.engine.random` here.
- Burn cards. Deliberately not implemented; see `docs/duel-rules.md`.
- Dealing to seats, boards or hole cards — `STORY-0104`.
- Any public accessor for the undealt cards. `remaining` and `deal` are the whole surface.

## Tests

`DeckTest`

| Test | Proves |
| --- | --- |
| `fullDeckHoldsEveryCardOnce` | `Deck.full().remaining == 52` and `deal(52).cards == Card.all` |
| `dealTakesFromTheTopAndLeavesTheRest` | `deal(2).cards == Card.all.take(2)`, the returned deck has `remaining == 50`, and its next `deal(1)` yields `Card.all[2]` |
| `dealingEveryCardEmptiesTheDeck` | `deal(52).deck.remaining == 0` and `deal(0)` on it succeeds with an empty list |
| `dealingMoreThanRemainsIsRejected` | `Deck.full().deal(53)` and `deal(-1)` both throw `IllegalArgumentException` |
| `dealDoesNotMutateTheReceiver` | dealing twice from the same deck value returns the same cards both times |
| `toStringDoesNotRevealTheCards` | `Deck.full().toString()` contains `remaining=52` and does not contain `As` |

## Acceptance criteria

- [ ] `DeckTest.fullDeckHoldsEveryCardOnce` passes
- [ ] `DeckTest.dealTakesFromTheTopAndLeavesTheRest` passes
- [ ] `DeckTest.dealingEveryCardEmptiesTheDeck` passes
- [ ] `DeckTest.dealingMoreThanRemainsIsRejected` passes
- [ ] `DeckTest.dealDoesNotMutateTheReceiver` passes
- [ ] `DeckTest.toStringDoesNotRevealTheCards` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
