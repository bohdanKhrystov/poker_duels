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

> ### ⚠ Open decision — DEC-004
>
> *Who* shows at a showdown is not settled. "The loser may muck" is a permission belonging to a
> player, and the engine has no way to ask for it: either the loser mucks by default, or both
> hands are always shown, or showing becomes a `PlayerAction` — which adds a member to the
> engine's public action set and a decision point after `ShowdownReached`. Awarding the pot does
> not depend on the answer, so everything except the reveals ships first; `TASK-010615` is
> blocked until this is decided, and is re-split afterwards.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-010604](../tasks/TASK-010604-uncalled-bet-arithmetic.md) | Compute the uncalled part of a bet | ready |
| [TASK-010605](../tasks/TASK-010605-settle-to-one-winner.md) | Settle a swept hand to a single winner | backlog |
| [TASK-010606](../tasks/TASK-010606-split-pot-odd-chip.md) | Split a pot between two winners, odd chip out of position | backlog |
| [TASK-010607](../tasks/TASK-010607-fold-awards-the-pot.md) | A fold awards the pot and ends the hand | backlog |
| [TASK-010608](../tasks/TASK-010608-showdown-fixtures-hole-cards.md) | Give the synthetic showdown fixtures hole cards | backlog |
| [TASK-010609](../tasks/TASK-010609-terminal-check-by-what-it-accepts.md) | Pin the random hand's ending by what it accepts | backlog |
| [TASK-010610](../tasks/TASK-010610-showdown-winners.md) | Decide who wins a showdown | backlog |
| [TASK-010611](../tasks/TASK-010611-river-close-settles.md) | A closed river settles the showdown | backlog |
| [TASK-010612](../tasks/TASK-010612-run-out-settles.md) | A run-out settles the showdown it reaches | backlog |
| [TASK-010613](../tasks/TASK-010613-settlement-invariants-property.md) | Settlement invariants over a thousand random hands | backlog |
| [TASK-010614](../tasks/TASK-010614-folded-cards-in-no-event.md) | A folded hand appears in no event | backlog |
| [TASK-010615](../tasks/TASK-010615-showdown-reveals-and-muck.md) | Showdown reveals, reveal order and the muck | blocked (DEC-004) |

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
