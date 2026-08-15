---
schema: 2
id: TASK-060120
title: The lockup coins consume the face token
type: task
status: backlog
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 3
labels: [design]
depends_on: [TASK-060119]
verify:
  - grep -q 'var(--pd-coin-face)' design/graphics/wordmark.html
  - grep -q 'var(--pd-coin-face)' design/screens/create-duel.html
  - grep -q 'var(--pd-coin-face)' design/screens/duel-end.html
  - ./design/check-drift.sh
---

## Goal

Three cards paint the coin face as a hand-copied gradient literal; once
`TASK-060119` births `--pd-coin-face`, each declares the token in its inlined
`:root` and consumes it, so the value gate covers the copies and a canonical retune
reaches every CSS coin (#509 review).

## Files

| File | Action |
| --- | --- |
| `design/graphics/wordmark.html` | edit — `background: var(--pd-coin-face)`, value inlined in `:root` |
| `design/screens/create-duel.html` | edit — same |
| `design/screens/duel-end.html` | edit — same |

## Scope

- Pixel-identical renders — the gradient does not change, only where it is born.

## Out of scope

- The Colors card's swatch — `TASK-060121`.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
