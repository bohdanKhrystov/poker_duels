---
schema: 2
id: TASK-060121
title: The Colors swatch consumes the face token
type: task
status: backlog
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060119]
verify:
  - grep -q 'var(--pd-coin-face)' design/tokens/colors.html
  - ./design/check-drift.sh
---

## Goal

The Colors card's coin swatch is the fourth hand-copied face gradient; it consumes
`--pd-coin-face` like its siblings so the value gate covers it (#509 review).

## Files

| File | Action |
| --- | --- |
| `design/tokens/colors.html` | edit — the swatch consumes the token; `:root` inlines the value |

## Scope

- Pixel-identical render; the token row prose stays as `TASK-060117` left it.

## Out of scope

- Everything else on the card.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
