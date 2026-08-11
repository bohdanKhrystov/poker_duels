---
id: STORY-0102
title: Cards, deck and deterministic shuffle
type: story
status: ready
parent: EPIC-01
module: poker-engine
labels: [engine, domain]
depends_on: [STORY-0101]
---

## Goal

The vocabulary of a card game: `Rank`, `Suit`, `Card`, `Deck`, and an injected `Rng` that makes
every shuffle reproducible from a seed.

## Why

Determinism has to be designed in at the bottom or it cannot be added at the top. If the deck
reaches for a global random source even once, replay, seeded property tests, and mass
simulation all become impossible, and the cost of retrofitting grows with every file added
above it. This is the story that makes [`ADR-0001`](../../docs/adr/ADR-0001-event-sourced-engine-contract.md)
enforceable.

## Design notes

- `Rank` and `Suit` are enums; `Card` is a value class over a compact integer encoding
  (`rank * 4 + suit`, 0–51) so that hot paths in the evaluator are cheap. The encoding is an
  implementation detail and must not leak into the public API.
- Text form is standard poker notation: `As`, `Th`, `2c`, `Kd`. Parsing and formatting are
  inverse and total over the 52 valid strings. This is what makes tests readable.
- `Rng` is an interface with one deterministic implementation. The engine never sees
  `kotlin.random.Random` directly, and the seed lives in `GameState` so it travels with the
  game.
- `Deck` is immutable: dealing returns a card and a new deck. It never shuffles in place.
- The shuffle is Fisher–Yates. The exact algorithm is part of the contract, because changing it
  invalidates every stored replay — a fact worth stating in the KDoc.

## Tasks

Schema 2. Waves in brackets: tickets in the same wave touch disjoint files and may run
concurrently.

| ID | Title | Est | Wave | Status |
| --- | --- | --- | --- | --- |
| [TASK-010201](../tasks/TASK-010201-rank-and-suit.md) | Rank and Suit enums | S | 1 | ready |
| [TASK-010202](../tasks/TASK-010202-card-value-type.md) | Card as a value class | S | 2 | backlog |
| [TASK-010205](../tasks/TASK-010205-splitmix64-rng.md) | Rng and SplitMix64Rng | S | 2 | backlog |
| [TASK-010203](../tasks/TASK-010203-card-notation.md) | Format and parse poker notation | S | 3 | backlog |
| [TASK-010206](../tasks/TASK-010206-immutable-deck.md) | Immutable Deck | S | 3 | backlog |
| [TASK-010210](../tasks/TASK-010210-no-ambient-random-test.md) | No ambient randomness, asserted | XS | 3 | backlog |
| [TASK-010204](../tasks/TASK-010204-cards-test-helper.md) | `cards("As Kd")` test helper | S | 4 | backlog |
| [TASK-010207](../tasks/TASK-010207-fisher-yates-shuffle.md) | Fisher–Yates shuffle | S | 4 | backlog |
| [TASK-010208](../tasks/TASK-010208-shuffle-determinism-test.md) | Recorded orderings for two seeds | XS | 5 | backlog |
| [TASK-010209](../tasks/TASK-010209-shuffle-distribution-test.md) | Shuffle distribution | S | 5 | backlog |

The PRNG is SplitMix64, written out in the module: 64 bits of state, one addition and two
multiplies per draw, and a published reference vector that proves the transliteration is right.
Bounded draws use rejection sampling, because a plain modulo would make low cards fractionally
more likely — and a deck is dealt from a bounded draw 51 times per hand.

## Acceptance criteria

- [ ] All 52 cards exist exactly once; no duplicates are constructible.
- [ ] `Card.parse(card.toString()) == card` for all 52 cards, and parsing rejects everything else.
- [ ] Two decks shuffled with the same seed produce identical orderings; different seeds do not.
- [ ] Dealing every card from a deck yields all 52 exactly once and leaves it empty.
- [ ] No file in the module references `kotlin.random.Random` outside the single `Rng`
      implementation, asserted by a test.

## Out of scope

- Hole cards, boards, or anything that knows about a game — STORY-0104.
- Hand strength — STORY-0103.
- Burn cards. They are deliberately not implemented; see [`duel-rules.md`](../../docs/duel-rules.md).
