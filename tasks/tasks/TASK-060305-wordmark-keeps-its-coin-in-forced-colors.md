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
  - 'grep -qF "@media (forced-colors: active)" design/graphics/wordmark.html'
  - 'grep -qF "border: 1px solid CanvasText" design/graphics/wordmark.html'
  - ./design/check-drift.sh
---

## Goal

Forced-colors mode strips `background-image` (gradients included) and `box-shadow`,
so the wordmark's CSS coins — painted by exactly those two — vanish into empty
spacers. `playing-card.html` guards this identical pitfall with a `CanvasText`
border; `create-duel.html` and `duel-end.html` adopted the guard when their copies
were reviewed, which makes the canonical the last card without one
(verifier-confirmed inherited gap). The card has **five** coin instances — the four
lockups plus the standalone 40px swatch in "The mark alone" — and all five vanish by
the same mechanism (#474 review).

## Files

| File | Action |
| --- | --- |
| `design/graphics/wordmark.html` | edit — `@media (forced-colors: active)` ring on `.coin`, all five instances |

## Scope

- One rule in the card's CSS, duel-end's exact idiom targeting the bare `.coin`
  class so every instance is covered — the standalone swatch included:
  `@media (forced-colors: active) { .coin { border: 1px solid CanvasText; } }`.
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
