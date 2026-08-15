---
schema: 2
id: TASK-060119
title: The coin face is born on the sheet
type: task
status: done
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 3
labels: [design]
depends_on: []
verify:
  - 'grep -qF -- "--pd-coin-face: radial-gradient(circle at 36% 30%, var(--pd-coin-glint), var(--pd-coin) 55%, var(--pd-coin-deep));" design/tokens/tokens.css'
  - cmp design/tokens/tokens.css web-client/src/styles/tokens.css
  - grep -q 'var(--pd-coin-face)' design/tokens/colors.html
  - 'grep -qF -- "--pd-coin-face:" design/tokens/colors.html'
  - grep -q '36% 30%' design/tokens/colors.html
  - ./design/check-drift.sh
---

## Goal

The canonical coin gradient is a hand-copied literal in four cards, and no gate pins
CSS gradient geometry — the next canonical retune strands stale copies with every
clause green (#509 review). The repo already holds the mechanism:
`--pd-card-back-stripes` is a gradient-valued sheet token whose inlined copies the
value gate enforces. The coin face takes the same route (ADR-0033's consumer test:
four independent consumers make it vocabulary), and per the #510 review the Colors
swatch converts in the same PR, the way `TASK-060115` paired the birth with a citing
file.

## Files

| File | Action |
| --- | --- |
| `design/tokens/tokens.css` | edit — the token beside the coin colors, carrying wordmark's why-comment (36% 30% mirrors `duel-coin.svg`'s cx/cy) |
| `web-client/src/styles/tokens.css` | edit — vendored mirror; client CI pins byte-identity |
| `design/tokens/colors.html` | edit — the swatch consumes the token; `:root` inlines the full declaration |

## Scope

- The verify pins the full declared value, not the name — a transposed or
  non-composed birth must fail at birth (#510 review).
- The sheet declaration carries the derivation comment migrating from wordmark
  (the sheet's own convention: a token derived from another says so), so the record
  of why 36% 30% survives the birth's move.
- Recorded residual: the token's percentages and `duel-coin.svg`'s cx/cy are held
  equal by that comment and review only — no clause compares CSS geometry to SVG
  attributes. A future gate clause may close it; this ticket records it honestly.

## Out of scope

- The three lockup coins — `TASK-060120`. The SVG itself — unchanged.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
