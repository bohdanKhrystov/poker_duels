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

> ### DEC-004 — answered by ADR-0008
>
> [`ADR-0008`](../../docs/adr/ADR-0008-loser-mucks-at-showdown.md): **the loser mucks.** The
> engine emits `HandRevealed` only for hands actually shown, a mucked hand appears in no event
> exactly as a folded one does, the last aggressor on the final betting street shows first — the
> seat out of position when that street was checked through — and a player who wins on a fold
> shows nothing. `TASK-010615` was the placeholder held back by this decision; it is retired, and
> the work is `TASK-010617` through `TASK-010624`.

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
| TASK-010615 | *(retired — re-split into TASK-010617…TASK-010624 once DEC-004 was answered)* | — |
| [TASK-010616](../tasks/TASK-010616-split-with-uncalled-bet.md) | Pin a split pot that also returns an uncalled bet | done |
| [TASK-010617](../tasks/TASK-010617-mucked-cards-in-no-event.md) | Extend the secrecy suite from the fold to the muck | ready |
| [TASK-010618](../tasks/TASK-010618-last-aggressor-field.md) | Carry the last aggressor on `GameState` | ready |
| [TASK-010619](../tasks/TASK-010619-betting-records-the-aggressor.md) | A bet, a raise or a full all-in records its seat as the last aggressor | backlog |
| [TASK-010620](../tasks/TASK-010620-new-street-clears-the-aggressor.md) | A dealt street clears the last aggressor, a closed round does not | backlog |
| [TASK-010621](../tasks/TASK-010621-new-hand-clears-the-aggressor.md) | A new hand starts with no last aggressor | backlog |
| [TASK-010622](../tasks/TASK-010622-reveal-order.md) | Decide who shows at a showdown, and in what order | backlog |
| [TASK-010623](../tasks/TASK-010623-showdown-emits-the-reveals.md) | A showdown emits `HandRevealed` for the hands that are shown | backlog |
| [TASK-010624](../tasks/TASK-010624-tie-reveals-both-hands.md) | A tied showdown reveals both hands, the river aggressor first | backlog |

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
