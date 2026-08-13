---
schema: 2
id: TASK-060103
title: The Type preview card
type: task
status: done
parent: STORY-0601
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060101]
verify:
  - 'head -1 design/tokens/type.html | grep -q "<!-- @dsCard group=\"Type\" -->"'
  - grep -q '<title>' design/tokens/type.html
  - grep -q -- '--pd-fs-' design/tokens/type.html
  - grep -q 'tabular-nums' design/tokens/type.html
  - '! grep -q "http" design/tokens/type.html'
---

## Goal

One self-contained HTML card shows the type system — families, the size ladder, weights, and
the numeric style — using real duel copy, never lorem ipsum.

## Files

| File | Action |
| --- | --- |
| `design/tokens/type.html` | create |

## Scope

- First line is exactly `<!-- @dsCard group="Type" -->`; styles inline; no external request.
- The size ladder labeled with token names, demonstrated with duel vocabulary
  ("Challenge sent", "Your turn", "Rematch?").
- Chips, pots and the clock in the mono family with `tabular-nums`; card ranks with suit
  glyphs in both suit colors.

## Out of scope

- Colors and spacing — `TASK-060102`, `TASK-060104`.
- Web fonts — the system stack is the decision; a bespoke face would be its own ticket.

## Tests

None — structural gates in `verify:` (marker on line one, title present, size-token names
present, tabular numerals used, fully self-contained).

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.
- [ ] The card renders in the claude.ai/design pane under **Type**.

## Definition of done

Standard, per [`tasks/README.md`](../README.md), with the epic's recorded deviation: the
review is visual, in claude.ai/design.
