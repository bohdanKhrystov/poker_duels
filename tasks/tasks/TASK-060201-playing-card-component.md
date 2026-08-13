---
schema: 2
id: TASK-060201
title: The playing-card component
type: task
status: ready
parent: STORY-0602
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - 'head -1 design/components/playing-card.html | grep -q "<!-- @dsCard group=\"Components\" -->"'
  - grep -q '<title>' design/components/playing-card.html
  - grep -q -- '--pd-card-face' design/components/playing-card.html
  - grep -q -- '--pd-radius-card' design/components/playing-card.html
  - '! grep -q "http" design/components/playing-card.html'
---

## Goal

One self-contained card shows the playing card in every state the table needs: face at the
three reference widths, the back, and the undealt slot.

## Files

| File | Action |
| --- | --- |
| `design/components/playing-card.html` | create |

## Scope

- Corner index top-left, rank over suit glyph; both suit colors demonstrated.
- Board 72px, hole 96px, mini 40px — 5:7 ratio, radius scaling with width.
- The back (stripes + inset border) and the dashed empty slot beside them.

## Out of scope

- SVG card faces — `STORY-0603`.
- The table composition — `TASK-060204`.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
