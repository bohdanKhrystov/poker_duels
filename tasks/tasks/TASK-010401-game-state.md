---
id: TASK-010401
title: GameState and its sub-models
type: task
status: backlog
parent: STORY-0104
module: poker-engine
estimate: M
labels: [engine, domain, contract]
depends_on: [TASK-010203]
---

## Goal

An immutable `GameState` that describes one hand of heads-up hold'em completely — complete
enough that it can be copied, stored, and resumed with nothing else.

## Context

- [`docs/adr/ADR-0001-event-sourced-engine-contract.md`](../../docs/adr/ADR-0001-event-sourced-engine-contract.md)
  — the contract this type belongs to.
- [`docs/duel-rules.md`](../../docs/duel-rules.md) — what a hand consists of.

## Scope

- `GameState`: hand number, `Street`, seats, board, pot, current bet, the seat to act, the
  minimum raise, the `Rng` state, and the remaining deck.
- `Seat`: index (`0` or `1`), stack, hole cards, chips committed this street, total committed
  this hand, and whether the player has folded or is all-in.
- `Street` enum: `PREFLOP`, `FLOP`, `TURN`, `RIVER`, `SHOWDOWN`, `COMPLETE`.
- `Board`: zero, three, four or five cards — no other size is representable.
- Everything is a `data class` of `val`s. No `var`, no mutable collection, anywhere.
- Seats are identified by index, never by "hero" and "villain" — those are viewer-relative and
  belong in the projection layer.
- Derived values (`potTotal`, `toCall(seat)`, `isHandOver`) are computed properties, so they
  cannot fall out of sync with the fields they depend on.

## Out of scope

- Actions and events — `TASK-010402`, `TASK-010403`.
- Any rule. This ticket describes a position; it does not know how a position is reached.
- Match-level state such as the blind level — STORY-0107.
- Serialization — STORY-0108.
- `PlayerView` redaction — specified in STORY-0104, implemented in EPIC-02.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../game/GameState.kt` | create |
| `poker-engine/src/main/kotlin/.../game/Seat.kt` | create |
| `poker-engine/src/main/kotlin/.../game/Street.kt` | create |
| `poker-engine/src/main/kotlin/.../game/Board.kt` | create |
| `poker-engine/src/test/kotlin/.../game/GameStateTest.kt` | create |

## Acceptance criteria

- [ ] No `var` and no mutable collection type appears in any domain class, asserted by a test
      over the public API via reflection.
- [ ] `copy` on `GameState` produces an independent value; mutating nothing is possible.
- [ ] A board can only hold 0, 3, 4 or 5 cards; 1, 2 or 6 is rejected at construction.
- [ ] Two states with equal contents are equal, including nested seats and board.
- [ ] Derived properties agree with the fields they are computed from, over generated states.
- [ ] A state carries enough to resume a hand: no information needed to continue lives outside
      it.

## Tests

- `GameStateTest` — construction, equality, board arity, derived properties.
- Property: `state.copy() == state` for generated states.
- `ImmutabilityTest` — reflective scan asserting no mutable members.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
