---
id: TASK-010201
title: Rank, Suit and Card value types
type: task
status: backlog
parent: STORY-0102
module: poker-engine
estimate: S
labels: [engine, domain]
depends_on: [TASK-010101]
---

## Goal

A `Card` type that can represent exactly the 52 real cards, cheaply enough to be used in the
evaluator's hot path.

## Context

- [`docs/architecture.md`](../../docs/architecture.md) — what may and may not live in the engine.
- [`tasks/stories/STORY-0102-cards-deck-shuffle.md`](../stories/STORY-0102-cards-deck-shuffle.md)
  — the encoding decision and why it must not leak.

## Scope

- `Rank` enum: `TWO` … `TEN`, `JACK`, `QUEEN`, `KING`, `ACE`, each with a numeric value where
  ace is high (14).
- `Suit` enum: `CLUBS`, `DIAMONDS`, `HEARTS`, `SPADES`.
- `Card` as a value class over an `Int` in `0..51`, encoded `rank.ordinal * 4 + suit.ordinal`,
  exposing `rank` and `suit` as properties.
- A constructor path that makes an out-of-range card impossible to build.
- `Card.all` — the 52 cards, in a stable order.

## Out of scope

- Parsing and `toString` — `TASK-010202`.
- `Deck` — `TASK-010203`.
- Hand strength, comparison of cards by poker value, or anything that knows about a game.
- Making `Rank` comparable for straight detection; that belongs with the evaluator, where the
  wheel exception lives.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../card/Rank.kt` | create |
| `poker-engine/src/main/kotlin/.../card/Suit.kt` | create |
| `poker-engine/src/main/kotlin/.../card/Card.kt` | create |
| `poker-engine/src/test/kotlin/.../card/CardTest.kt` | create |

## Acceptance criteria

- [ ] `Card.all` has exactly 52 distinct entries covering every rank/suit combination once.
- [ ] `Card(rank, suit).rank == rank` and `.suit == suit` for all 52 combinations.
- [ ] Constructing a card from an integer outside `0..51` is impossible or rejected.
- [ ] The integer encoding is not part of the public API surface beyond what the evaluator
      needs; nothing outside the package depends on the specific numeric layout.
- [ ] Two cards with the same rank and suit are equal and have equal hash codes.

## Tests

- `CardTest` — round-trips all 52 combinations through construction and property access.
- Property: `Card.all.toSet().size == 52`.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
