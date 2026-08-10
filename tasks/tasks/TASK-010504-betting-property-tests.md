---
id: TASK-010504
title: Betting invariant and property tests
type: task
status: backlog
parent: STORY-0105
module: poker-engine
estimate: S
labels: [engine, test]
depends_on: [TASK-010503]
---

## Goal

Confidence that the betting rules hold not just in the cases someone thought to write down, but
across tens of thousands of games nobody designed.

## Context

- [`tasks/stories/STORY-0105-betting-rounds.md`](../stories/STORY-0105-betting-rounds.md).

## Scope

- A generator that plays hands by choosing uniformly among legal actions.
- Invariants asserted **after every single action**, not merely at the end of a hand:
  - `sum(stacks) + pot` equals the starting total,
  - no stack is negative,
  - no card appears twice in play,
  - the seat to act has not folded and is not all-in,
  - the current bet is at least the largest individual commitment this street,
  - a hand always terminates within a bounded number of actions.
- On failure, print the seed and the action sequence, so the case is reproducible exactly.

## Out of scope

- Showdown correctness — STORY-0106.
- The full simulation harness — `TASK-010803`. This is the same idea at hand scope; that one
  runs it at match scope.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/.../game/RandomActionGenerator.kt` | create |
| `poker-engine/src/test/kotlin/.../game/BettingInvariantTest.kt` | create |

## Acceptance criteria

- [ ] 10 000 randomly played hands complete with no invariant violation and no crash.
- [ ] Every invariant above is checked after every action.
- [ ] The generator uses seeded randomness, so a failing run is reproducible.
- [ ] A failure message contains a seed and action list sufficient to reproduce it.
- [ ] The suite runs in under thirty seconds.

## Tests

- `BettingInvariantTest` — as above.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
