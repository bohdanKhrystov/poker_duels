---
schema: 2
id: TASK-060104
title: The Spacing preview card
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
  - 'head -1 design/tokens/spacing.html | grep -q "<!-- @dsCard group=\"Spacing\" -->"'
  - grep -q '<title>' design/tokens/spacing.html
  - for t in space-1 space-2 space-3 space-4 space-5 space-6 space-7 space-8 space-9 radius-small radius-medium radius-card radius-pill focus focus-offset shadow-pop; do grep -q -- "--pd-$t" design/tokens/spacing.html || exit 1; done
  - '! grep -q "http" design/tokens/spacing.html'
---

## Goal

One self-contained HTML card shows the spacing ladder, the radii, surface elevation and the
focus ring — the structural half of the foundations.

## Files

| File | Action |
| --- | --- |
| `design/tokens/spacing.html` | create |

## Scope

- First line is exactly `<!-- @dsCard group="Spacing" -->`; styles inline; no external
  request.
- The spacing ladder as measured bars labeled with token names; radii on sample tiles,
  including the playing-card radius; bg → surface → raised elevation demo with hairline
  borders; the focus ring on a sample control.

## Out of scope

- Colors and type — `TASK-060102`, `TASK-060103`.
- Motion and shadows beyond elevation — not yet ticketed.

## Tests

None — structural gates in `verify:` (marker on line one, title present, spacing and radius
token names present, fully self-contained).

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.
- [ ] The card renders in the claude.ai/design pane under **Spacing**.

## Definition of done

Standard, per [`tasks/README.md`](../README.md), with the epic's recorded deviation: the
review is visual, in claude.ai/design.
