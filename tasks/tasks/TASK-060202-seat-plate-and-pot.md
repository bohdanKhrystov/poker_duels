---
schema: 2
id: TASK-060202
title: The seat plate and pot strip
type: task
status: done
parent: STORY-0602
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - 'head -1 design/components/seat-and-pot.html | grep -q "<!-- @dsCard group=\"Components\" -->"'
  - grep -q '<title>' design/components/seat-and-pot.html
  - grep -q 'tabular-nums' design/components/seat-and-pot.html
  - grep -q -- '--pd-accent' design/components/seat-and-pot.html
  - '! grep -q "http" design/components/seat-and-pot.html'
---

## Goal

One self-contained card shows a seat in each of its states — idle, on turn, in a grace
window — plus the dealer button and the pot strip.

## Files

| File | Action |
| --- | --- |
| `design/components/seat-and-pot.html` | create |

## Scope

- Plate: name, mono tabular stack, "D" pill; accent left edge + micro caps on turn;
  faint + "reconnecting…" in the grace window.
- Pot strip: mono pot amount, small muted "Blinds 75/150 · Hand 14" — level-2 blinds, the only ones hand 14 can play (docs/duel-rules.md).

## Out of scope

- The action bar — `TASK-060203`.
- Avatars and profiles — v0.2 territory, not yet ticketed.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
