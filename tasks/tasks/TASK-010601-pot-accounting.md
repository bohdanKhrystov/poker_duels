---
id: TASK-010601
title: Pot accounting, all-in caps and uncalled bets
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: M
labels: [engine, rules]
depends_on: [TASK-010504]
---

## Goal

Chips end up where they belong: a bet nobody can cover is capped, and the excess goes back to
the player who bet it.

## Context

- [`tasks/stories/STORY-0106-showdown-and-pots.md`](../stories/STORY-0106-showdown-and-pots.md)
  — why heads-up needs no side-pot structure.

## Scope

- Track each seat's total commitment for the hand.
- The contested pot is `2 × min(commitments)`; anything above that is uncalled and returns to
  its owner.
- Emit `UncalledBetReturned` before `PotAwarded`, always in that order, so a replay shows chips
  moving in a sensible sequence.
- Handle the case where a seat is all-in for less than the other's bet, on any street.

## Out of scope

- Deciding *who* wins — `TASK-010602`.
- Multi-way side pots. Two players cannot produce them, and building the general structure here
  would be speculative complexity.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../game/PotAccounting.kt` | create |
| `poker-engine/src/test/kotlin/.../game/PotAccountingTest.kt` | create |

## Acceptance criteria

- [ ] Equal commitments produce a pot of twice one commitment and no return.
- [ ] Unequal commitments cap the pot and return the excess to its owner.
- [ ] A fold to an uncalled bet returns the uncalled portion, and the bettor wins the rest.
- [ ] `UncalledBetReturned` precedes `PotAwarded`.
- [ ] Chip conservation holds across the whole hand: starting stacks equal ending stacks.
- [ ] Over generated hands, no chips are created or destroyed.

## Tests

- `PotAccountingTest` — equal and unequal commitments, all-in on each street, fold to a bet.
- Property: chips conserved over generated hands.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
