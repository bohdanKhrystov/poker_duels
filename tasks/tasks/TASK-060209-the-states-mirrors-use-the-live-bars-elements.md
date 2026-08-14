---
schema: 2
id: TASK-060209
title: The states' hidden mirrors use the live bar's elements
type: task
status: ready
parent: STORY-0602
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060205]
verify:
  - '! grep -q "<span class=\"chip\">" design/screens/duel-table-states.html'
  - '! grep -q "http" design/screens/duel-table-states.html'
---

## Goal

The states screen's hidden sizing rows use `<button class="chip">` like the live bars, so
span-vs-button line-height differences cannot break wrap-height parity by a few pixels.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table-states.html` | edit — span chips become buttons in both hidden rows |

## Scope

- Element swap only; no styling changes.

## Out of scope

- The component card — already button-based.

## Tests

None — the verify grep asserts no span chips remain.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
