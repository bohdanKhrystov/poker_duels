---
schema: 2
id: TASK-060117
title: The Colors card's coin matches the canonical
type: task
status: done
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060115]
verify:
  - grep -q '36% 30%' design/tokens/colors.html
  - grep -q 'var(--pd-coin-glint)' design/tokens/colors.html
  - 'grep -qF "@media (forced-colors: active)" design/tokens/colors.html'
  - ./design/check-drift.sh
---

## Goal

The Colors card's coin swatch is the one already-drifted coin in the tree — `34% 30%`
lighting with no glint stop, against the canonical `36% 30%` + `--pd-coin-glint` that
wordmark's comment says "must match" — and its `.coin`/`.pback` swatches vanish in
forced-colors mode with no guard and no ticket owning the gap (#474 review, both
findings).

## Files

| File | Action |
| --- | --- |
| `design/tokens/colors.html` | edit — canonical lighting on the coin swatch; forced-colors guards on `.coin` and `.pback` |

## Scope

- The swatch adopts the canonical gradient shape (`36% 30%`, glint via the token,
  the same stop structure) at its own size — a faithful miniature, not a redesign.
- `@media (forced-colors: active)` gives `.coin` and `.pback` the `CanvasText`
  border idiom the component cards use, so the foundation card's swatches survive
  contrast themes.

## Out of scope

- The palette itself — no color value changes anywhere.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
