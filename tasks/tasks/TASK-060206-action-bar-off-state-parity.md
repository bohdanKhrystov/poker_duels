---
schema: 2
id: TASK-060206
title: The off-state's hidden sizing row mirrors the live content
type: task
status: backlog
parent: STORY-0602
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - grep -q 'class="bar off"' design/components/action-bar.html
  - grep -c 'class="chip"' design/components/action-bar.html | grep -qv '^0$'
  - '! grep -q "http" design/components/action-bar.html'
---

## Goal

The action-bar card's off-turn demo hides a sizing row with the *same* content as the live
bar, so the height-parity guarantee holds even at widths where the row wraps — the gap the
TASK-060204 review measured at phone width.

## Files

| File | Action |
| --- | --- |
| `design/components/action-bar.html` | edit — the `.off` frame's hidden sizing row carries the full chip set and stepper; a caption states the wrap-parity rule |

## Scope

- Markup only in the off frame; a one-line caption records why the hidden content must
  mirror the live content.

## Out of scope

- Component behavior changes — visibility rules stay as merged.

## Tests

None — structural greps in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
