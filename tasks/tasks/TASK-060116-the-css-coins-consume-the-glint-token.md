---
schema: 2
id: TASK-060116
title: The CSS coins consume the glint token
type: task
status: done
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 3
labels: [design]
depends_on: [TASK-060115]
verify:
  - grep -q 'var(--pd-coin-glint)' design/graphics/wordmark.html
  - grep -q 'var(--pd-coin-glint)' design/screens/create-duel.html
  - grep -q 'var(--pd-coin-glint)' design/screens/duel-end.html
  - ./design/check-drift.sh
---

## Goal

Three cards paint the coin's glint as a raw `#b8c6d6` literal; once `TASK-060115`
births `--pd-coin-glint`, each declares the token in its inlined `:root` and consumes
it in the gradient, so `TASK-060111`'s value gate covers the copies and a sheet
retune reaches every CSS coin (#474 review).

## Files

| File | Action |
| --- | --- |
| `design/graphics/wordmark.html` | edit — glint via `var(--pd-coin-glint)`, value inlined in `:root` |
| `design/screens/create-duel.html` | edit — same |
| `design/screens/duel-end.html` | edit — same |

## Scope

- Pixel-identical renders — the value does not change, only where it is born.
- Each card's `:root` gains the token with the sheet's value, per the inlining
  convention every card follows.

## Out of scope

- The Colors card's swatch — `TASK-060117` (it also corrects that swatch's lighting).
- The SVG — its literal is pair-pinned by `TASK-060115`/`TASK-060112`.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
