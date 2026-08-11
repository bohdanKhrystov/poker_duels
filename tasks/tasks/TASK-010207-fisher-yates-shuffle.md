---
schema: 2
id: TASK-010207
title: Shuffle the deck with Fisher-Yates driven by an injected Rng
type: task
status: ready
parent: STORY-0102
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, domain, determinism]
depends_on: [TASK-010205, TASK-010206]
verify:
  - ./gradlew :poker-engine:test --tests '*DeckShuffleTest'
  - ./gradlew :poker-engine:check
---

## Goal

A deck can be shuffled into an unbiased order that depends on nothing but the `Rng` handed to
it — the single mechanism every replay, simulation and seeded test in this project rests on.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/card/Deck.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/card/DeckShuffleTest.kt` | create |

Read `Rng.kt` and `SplitMix64Rng.kt`; do not modify them.

## Scope

- Add to `Deck`, exactly this algorithm — the downward Fisher–Yates traversal, one `nextInt`
  per step, the returned generator threaded into the next step:

  ```kotlin
  public fun shuffled(rng: Rng): Shuffle {
      val order = cards.toMutableList()
      var source = rng
      for (i in order.lastIndex downTo 1) {
          val draw = source.nextInt(i + 1)
          val j = draw.value
          val held = order[i]
          order[i] = order[j]
          order[j] = held
          source = draw.next
      }
      return Shuffle(Deck(order.toList()), source)
  }

  /** The shuffled deck and the generator left over, ready for the next draw. */
  public data class Shuffle(val deck: Deck, val rng: Rng)
  ```

- `shuffled` shuffles whatever remains, so it is valid on a partially dealt deck.
- KDoc on `shuffled` saying the traversal direction and the one-draw-per-step rule are part of
  the **durable replay contract**: any change reorders every deck ever recorded, and
  `TASK-010208` locks that fact down with recorded orderings.
- The receiver is untouched; the returned `Rng` is the advanced one, never the argument.

## Out of scope

- Recorded-ordering regression tests — `TASK-010208`.
- Uniformity and distribution statistics — `TASK-010209`.
- Any change to `Rng` or `SplitMix64Rng`. If `nextInt` looks wrong, stop and raise it rather
  than editing it here — `TASK-010208`'s recorded orderings depend on it.
- Cutting, riffling, partial shuffles, or shuffling in place.

## Tests

`DeckShuffleTest`

| Test | Proves |
| --- | --- |
| `shuffleIsAPermutationOfTheDeck` | kotest property over `Arb.long()`: dealing all 52 from the shuffled deck yields exactly `Card.all.toSet()` |
| `sameSeedShufflesIdentically` | two shuffles from `SplitMix64Rng(2024)` deal the same 52 cards in the same order |
| `differentSeedsShuffleDifferently` | seeds `1` and `2` produce different orderings |
| `shuffleReturnsAnAdvancedRng` | the returned `rng` differs from the one passed in, and shuffling again with it gives a different ordering |
| `shuffleDoesNotMutateTheReceiver` | after shuffling, the original deck still deals `Card.all` in order |
| `shufflesOnlyTheCardsThatRemain` | `Deck.full().deal(10).deck.shuffled(rng).deck.remaining == 42` and the 10 dealt cards are absent from it |

## Acceptance criteria

- [ ] `DeckShuffleTest.shuffleIsAPermutationOfTheDeck` passes
- [ ] `DeckShuffleTest.sameSeedShufflesIdentically` passes
- [ ] `DeckShuffleTest.differentSeedsShuffleDifferently` passes
- [ ] `DeckShuffleTest.shuffleReturnsAnAdvancedRng` passes
- [ ] `DeckShuffleTest.shuffleDoesNotMutateTheReceiver` passes
- [ ] `DeckShuffleTest.shufflesOnlyTheCardsThatRemain` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
