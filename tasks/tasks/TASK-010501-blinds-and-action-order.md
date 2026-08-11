---
id: TASK-010501
title: Blinds, button and heads-up action order
type: task
status: backlog
parent: STORY-0105
module: poker-engine
estimate: M
labels: [engine, rules]
depends_on: [TASK-010427]
---

## Goal

A hand starts correctly: blinds posted, hole cards dealt, and the right player to act — which in
heads-up is not the player most implementations assume.

## Context

- [`docs/duel-rules.md`](../../docs/duel-rules.md) — positions and blinds. Read this section
  before writing any code; heads-up inverts the usual arrangement.

## Scope

- `startHand(...)` producing the opening `GameState` and its events: `HandStarted`,
  `BlindPosted` × 2, `HoleCardsDealt` per seat, `ActionOn`.
- **The button posts the small blind.** It acts first preflop and last on every later street.
- A player with fewer chips than their blind posts all-in for what they have.
- Hole cards are dealt from the shuffled deck one at a time, alternating, starting with the
  non-button — matching how a real deal proceeds, so that a replay of the deck order is
  meaningful.
- `actingSeat` for a given street: button preflop, non-button afterwards.

## Out of scope

- Legality of actions — `TASK-010502`.
- Ending the round or dealing the flop — `TASK-010503`.
- Blind *levels*, which are a match concern — STORY-0107. This ticket takes small and big blind
  amounts as parameters.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../game/HandSetup.kt` | create |
| `poker-engine/src/main/kotlin/.../game/DefaultPokerEngine.kt` | create |
| `poker-engine/src/test/kotlin/.../game/BlindsAndOrderTest.kt` | create |

## Acceptance criteria

- [ ] The button posts the small blind; the other seat posts the big blind.
- [ ] The button acts first preflop.
- [ ] The button acts last on the flop, turn and river.
- [ ] A seat too short to post its blind goes all-in for its stack, and the hand continues.
- [ ] Each seat receives two distinct hole cards, and no card appears twice across both seats.
- [ ] The opening event sequence is exactly as listed, in order.
- [ ] `DefaultPokerEngine` passes `PokerEngineContract`.

## Tests

- `BlindsAndOrderTest` — position, order, short blind, the event sequence.
- Property: over generated stacks, chips are conserved through the deal.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
