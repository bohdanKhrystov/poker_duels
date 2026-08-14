---
schema: 2
id: TASK-060206
title: The off-state's hidden sizing row mirrors the live content
type: task
status: done
parent: STORY-0602
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - python3 -c "import re,sys; t=open('design/components/action-bar.html').read(); off=t[t.index('bar off'):]; sys.exit(0 if off.count('chip')>=5 and 'all-in' in off and re.search(r'\\d,\\d{3}', off) else 1)"
  - grep -q 'mirrors the live content' design/components/action-bar.html
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

- Markup only in the off frame: the full chip set, and a stepper amount of the live row's
  digit width, so wrap points match; a one-line caption records why.

## Out of scope

- Component behavior changes — visibility rules stay as merged.

## Tests

None — structural greps in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
