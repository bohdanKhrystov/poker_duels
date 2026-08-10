---
id: STORY-0106
title: Showdown and pot resolution
type: story
status: backlog
parent: EPIC-01
module: poker-engine
labels: [engine, rules]
depends_on: [STORY-0103, STORY-0105]
---

## Goal

A hand ends correctly: the pot goes to the right player, split pots split evenly, odd chips have
an owner, uncalled bets come back, and the cards that should be revealed are — and no others.

## Why

The evaluator ranks hands and the betting rounds move chips, but nothing yet decides who ends
up with them. This story closes the hand and is the last piece before a match can be played.

## Design notes

- Heads-up has no side pots in the multi-way sense. The only case is one player all-in for less
  than the other has bet, which is handled by capping and returning the uncalled remainder — not
  by building a side-pot structure. Doing it with the simpler mechanism is deliberate.
- Chip counts are integers. A split pot with an odd total gives the extra chip to the player out
  of position, per [`duel-rules.md`](../../docs/duel-rules.md).
- **Card revelation is a security boundary, not a presentation detail.** When a hand ends in a
  fold, the folding player's cards are never revealed — not in the events, not in the state, not
  anywhere a projection could reach them. The engine must not emit them at all.
- Reveal order at showdown: the last river aggressor shows first; with no river bet, the player
  out of position shows first. The loser may muck, and a mucked hand is not disclosed.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-010601](../tasks/TASK-010601-pot-accounting.md) | Pot accounting, all-in caps and uncalled bets | backlog |
| [TASK-010602](../tasks/TASK-010602-showdown-resolution.md) | Showdown resolution, splits and reveal order | backlog |
| [TASK-010603](../tasks/TASK-010603-hand-completion.md) | Hand completion events and hand history record | backlog |

## Acceptance criteria

- [ ] The best hand wins the pot; equal hands split it.
- [ ] An odd chip in a split goes to the player out of position, and total chips are unchanged.
- [ ] A bet the opponent cannot cover is capped, and the remainder is returned to its owner.
- [ ] A hand won by a fold reveals nothing — no event anywhere in the log contains the folded
      hole cards.
- [ ] A mucked losing hand at showdown is likewise never disclosed.
- [ ] Chip conservation holds across the complete hand: `stacks + pot` is constant from deal to
      award.

## Out of scope

- Match-level consequences of busting — STORY-0107.
- Presenting a replay of the showdown — EPIC-03.
- All-in EV or "who was ahead" analysis — EPIC-08.
