---
schema: 2
id: TASK-060205
title: The table's other moments — waiting and showdown
type: task
status: ready
parent: STORY-0602
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060204]
verify:
  - 'head -1 design/screens/duel-table-states.html | grep -q "<!-- @dsCard group=\"Screens\" -->"'
  - grep -q '<title>' design/screens/duel-table-states.html
  - grep -q -- '--pd-win' design/screens/duel-table-states.html
  - '! grep -q "http" design/screens/duel-table-states.html'
---

## Goal

The table when it is not the hero's move: the waiting frame, and the showdown frame with the
winner banner and the muck, so the client never has to invent an in-between state.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table-states.html` | create |

## Scope

- Waiting: opponent on turn, hero's bar collapsed to one line.
- Showdown: reveal per `ADR-0008` (the loser mucks), banner in `--pd-win` with amount and
  hand name; the pot moving is stated, not animated.

## Out of scope

- Duel end (Victory / rematch) — `STORY-0604`.
- Replay affordances — v0.4.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
