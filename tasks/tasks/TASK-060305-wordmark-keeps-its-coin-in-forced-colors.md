---
schema: 2
id: TASK-060305
title: The wordmark keeps its coin in forced colors
type: task
status: ready
parent: STORY-0603
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - grep -q 'forced-colors' design/graphics/wordmark.html
  - grep -q 'CanvasText' design/graphics/wordmark.html
  - ./design/check-drift.sh
---

## Goal

Forced-colors mode strips `background-image` (gradients included) and `box-shadow`,
so the wordmark's CSS coin — painted by exactly those two — vanishes into an empty
flex spacer. `playing-card.html` guards this identical pitfall with a `CanvasText`
border; `create-duel.html` adopted the guard when its copy was reviewed (#456), which
makes the canonical the last lockup without one (verifier-confirmed inherited gap).

## Files

| File | Action |
| --- | --- |
| `design/graphics/wordmark.html` | edit — `@media (forced-colors: active)` ring on the coin, all four lockups |

## Scope

- One rule in the card's CSS, the playing-card idiom: the coin gains a
  `1px solid CanvasText` border under forced colors, nothing else changes.
- Render-identical outside forced colors — the border exists only inside the media
  block.

## Out of scope

- The screens' lockup copies — create-duel already carries the guard; other cards
  adopt it when their own tickets touch them.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
