---
schema: 2
id: TASK-060207
title: The fold ending — a win with nothing shown
type: task
status: ready
parent: STORY-0602
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060205]
verify:
  - grep -q 'folds' design/screens/duel-table-states.html
  - '! grep -q "http" design/screens/duel-table-states.html'
---

## Goal

The most common heads-up ending gets its frame: a win by fold — banner with the amount and
no hand name, "ImKate folds" in the reserved line, nobody's cards shown (ADR-0008: the fold
winner shows nothing).

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table-states.html` | edit — a third frame, same skeleton |

## Scope

- Same full-fidelity table skeleton; only text and dimming differ from the showdown frame.

## Out of scope

- Duel-end and rematch — STORY-0604.

## Tests

None — structural greps in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
