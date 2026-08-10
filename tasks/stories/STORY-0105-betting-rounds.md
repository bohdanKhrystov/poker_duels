---
id: STORY-0105
title: Betting rounds
type: story
status: backlog
parent: EPIC-01
module: poker-engine
labels: [engine, rules]
depends_on: [STORY-0104]
---

## Goal

A hand can be played from the posting of blinds to the river: blinds, action order, legal
actions, min-raise arithmetic, all-ins, and advancing from street to street.

## Why

This is where the rules actually live, and where heads-up differs most from what everyone
assumes. It is also where the majority of real poker-engine bugs are found.

## Design notes

Rules are specified in [`duel-rules.md`](../../docs/duel-rules.md); this story implements them
and adds nothing.

The traps, listed so they are impossible to miss:

- **The button is the small blind in heads-up.** It acts *first* preflop and *last* on every
  street afterwards. Getting this backwards produces a game that looks entirely normal and is
  wrong in every hand.
- Min-raise is the size of the largest previous bet or raise **on the current street**, not the
  size of the current bet.
- An all-in for less than a full raise **does not reopen** betting for a player who already
  faced the full raise. They may call, not re-raise.
- A bet larger than the opponent's stack is capped, and the excess returns as an uncalled bet.
- The big blind has the option to raise preflop when the action is merely called.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-010501](../tasks/TASK-010501-blinds-and-action-order.md) | Blinds, button and heads-up action order | backlog |
| [TASK-010502](../tasks/TASK-010502-action-legality.md) | Action legality and min-raise arithmetic | backlog |
| [TASK-010503](../tasks/TASK-010503-street-progression.md) | Betting round completion and street advance | backlog |
| [TASK-010504](../tasks/TASK-010504-betting-property-tests.md) | Betting invariant and property tests | backlog |

Move to `ready` once STORY-0104 is merged and the domain types exist.

## Acceptance criteria

- [ ] The button posts the small blind, acts first preflop and last on every later street.
- [ ] Illegal actions are rejected with a reason, and leave the state untouched.
- [ ] Min-raise is computed from the largest raise increment on the current street.
- [ ] A short all-in does not reopen betting for a player who has already faced a full raise.
- [ ] The big blind may raise preflop after a call.
- [ ] Chip conservation holds after every single action, asserted as a property.
- [ ] A street ends exactly when both players have acted and matched, or one is all-in.

## Out of scope

- Awarding the pot — STORY-0106.
- Anything spanning more than one hand — STORY-0107.
- Action clocks and timeouts — EPIC-02.
