---
id: STORY-0107
title: Duel format and match progression
type: story
status: backlog
parent: EPIC-01
module: poker-engine
labels: [engine, rules, duel]
depends_on: [STORY-0106]
---

## Goal

Hands become a duel: equal starting stacks, a rising blind schedule, an alternating button, and
an end condition that produces one winner and one duel coin.

## Why

This is the story that turns a poker implementation into *this* product. It is also the layer
that guarantees a duel terminates — without a rising blind schedule, a heads-up freezeout can
run indefinitely.

## Design notes

- The format is **configuration**, not code. `DuelFormat` carries the starting stack, the blind
  schedule and the end condition. [`DEC-001`](../../docs/duel-rules.md) is still open, and
  changing the answer must never require an engine change. Any task here that hardcodes 50/100
  or "ten hands per level" is wrong.
- The default format is a freezeout: play until one player holds every chip.
- The alternative under consideration — a fixed-length match decided on chip count — must be
  expressible in the same `DuelFormat` type. If it is not, the type is wrong. This is the
  concrete test of whether the abstraction actually holds.
- Match state is separate from hand state. `GameState` describes one hand; `MatchState` holds
  the format, the hand number, the blind level, stacks between hands and the button.
- Blind levels advance on hand boundaries, never mid-hand.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-010701](../tasks/TASK-010701-duel-format.md) | DuelFormat: stacks, blind schedule, end condition | backlog |
| [TASK-010702](../tasks/TASK-010702-match-progression.md) | Match progression, button alternation, blind levels | backlog |
| [TASK-010703](../tasks/TASK-010703-match-conclusion.md) | Match conclusion and duel result | backlog |

## Acceptance criteria

- [ ] A complete duel plays end to end in a test and declares exactly one winner.
- [ ] The button alternates every hand, and blind levels rise only between hands.
- [ ] Both the freezeout and the fixed-length format are expressible in `DuelFormat` and both
      are covered by tests.
- [ ] Every duel under the default format terminates — asserted over a large number of
      simulated matches with a hand-count ceiling that must never be reached.
- [ ] `MatchFinished` carries the winner, the hand count and the final stacks.
- [ ] Total chips in the match are constant from the first deal to the last.

## Out of scope

- Rating, ladder position, or what a duel coin is worth — EPIC-05.
- Rematch and challenge flow — EPIC-02.
- Persisting a match — EPIC-02.
