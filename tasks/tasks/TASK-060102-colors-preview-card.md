---
schema: 2
id: TASK-060102
title: The Colors preview card
type: task
status: ready
parent: STORY-0601
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060101]
verify:
  - 'head -1 design/tokens/colors.html | grep -q "<!-- @dsCard group=\"Colors\" -->"'
  - grep -q '<title>' design/tokens/colors.html
  - grep -q -- '--pd-bg' design/tokens/colors.html
  - grep -q -- '--pd-card-face' design/tokens/colors.html
  - '! grep -q "http" design/tokens/colors.html'
---

## Goal

One self-contained HTML card shows the whole palette — surfaces, text, accent, semantic,
table & cards — with each swatch carrying its token name, hex and one-line usage.

## Files

| File | Action |
| --- | --- |
| `design/tokens/colors.html` | create |

## Scope

- First line is exactly `<!-- @dsCard group="Colors" -->`; styles inline; no external request.
- Swatches grouped as in `tokens.css`, values inlined from it, names shown verbatim.
- A closing demo strip proving the hierarchy: dark table, paper-white cards brightest,
  one accent action.

## Out of scope

- Type and spacing — `TASK-060103`, `TASK-060104`.
- Editing `tokens.css` — feedback on values goes back through `TASK-060101`.

## Tests

None — structural gates in `verify:` (marker on line one, title present, token names
present, fully self-contained).

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.
- [ ] The card renders in the claude.ai/design pane under **Colors**.

## Definition of done

Standard, per [`tasks/README.md`](../README.md), with the epic's recorded deviation: the
review is visual, in claude.ai/design.
