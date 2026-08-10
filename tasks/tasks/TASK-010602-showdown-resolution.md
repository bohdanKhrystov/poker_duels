---
id: TASK-010602
title: Showdown resolution, splits and reveal order
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: M
labels: [engine, rules, security]
depends_on: [TASK-010601, TASK-010305]
---

## Goal

At showdown the right player wins, ties split evenly, the odd chip has an owner, and no card is
revealed that should not be.

## Context

- [`docs/duel-rules.md`](../../docs/duel-rules.md) — showdown rules and reveal order.
- [`tasks/stories/STORY-0106-showdown-and-pots.md`](../stories/STORY-0106-showdown-and-pots.md)
  — revelation is a security boundary, not presentation.

## Scope

- Evaluate both seven-card hands and compare.
- Better hand takes the pot; equal hands split it.
- An odd chip in a split goes to the player out of position — the big blind.
- Reveal order: the last river aggressor shows first; with no river bet, the out-of-position
  seat shows first.
- The loser may muck, and a mucked hand is **not** included in any event.
- Emit `ShowdownReached`, `HandRevealed` per revealed hand, then `PotAwarded`.

## Out of scope

- Presenting any of this — EPIC-03. The engine says who revealed what; it does not animate.
- All-in EV, "who was ahead on the turn" — EPIC-08.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../game/Showdown.kt` | create |
| `poker-engine/src/test/kotlin/.../game/ShowdownTest.kt` | create |
| `poker-engine/src/test/kotlin/.../game/CardSecrecyTest.kt` | create |

## Acceptance criteria

- [ ] The better hand wins the whole pot.
- [ ] Equal hands split, and an odd chip goes to the seat out of position.
- [ ] Total chips are unchanged by any split.
- [ ] The last river aggressor reveals first; with no river bet, the out-of-position seat does.
- [ ] A mucked hand appears in no event anywhere in the log.
- [ ] A hand won by a fold reveals nothing — asserted by scanning the whole event log for the
      folder's hole cards.
- [ ] A board-plays split is handled: both seats play the board and split exactly.

## Tests

- `ShowdownTest` — winner, split, odd chip, reveal order, board plays.
- `CardSecrecyTest` — over generated hands, no event ever contains hole cards belonging to a
  seat that folded or mucked. This is the test that matters most in the ticket.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
