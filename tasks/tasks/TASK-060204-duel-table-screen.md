---
schema: 2
id: TASK-060204
title: The duel table screen, in play
type: task
status: done
parent: STORY-0602
module: design
estimate: S
tier: opus
review: light
files_touched: 2
labels: [design]
depends_on: [TASK-060201, TASK-060202, TASK-060203]
verify:
  - 'head -1 design/screens/duel-table.html | grep -q "<!-- @dsCard group=\"Screens\" -->"'
  - grep -q '<title>' design/screens/duel-table.html
  - grep -q -- '--pd-space-' design/screens/duel-table.html
  - grep -q -- '--pd-card-face' design/screens/duel-table.html
  - '! grep -q "http" design/screens/duel-table.html'
---

## Goal

The whole table at the hero's decision point, composed from the three components: opponent
seat, board with pot, hole cards, live action bar — one column, phone-first.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | create |
| `design/README.md` | edit — screens/ stops being planned; the group index gains Components and Screens |

## Scope

- Max 560px column on `--pd-bg`; opponent top, board + pot center, hero bottom, bar pinned.
- A realistic mid-hand moment: flop out, two undealt slots, facing a bet.
- Legible at 360px wide.

## Out of scope

- Waiting and showdown frames — `TASK-060205`.
- Result and rematch — `STORY-0604`.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
