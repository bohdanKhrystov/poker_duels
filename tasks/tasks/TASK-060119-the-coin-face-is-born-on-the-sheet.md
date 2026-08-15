---
schema: 2
id: TASK-060119
title: The coin face is born on the sheet
type: task
status: ready
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 2
labels: [design]
depends_on: []
verify:
  - grep -qF -- '--pd-coin-face:' design/tokens/tokens.css
  - cmp design/tokens/tokens.css web-client/src/styles/tokens.css
  - ./design/check-drift.sh
---

## Goal

The canonical coin gradient is a hand-copied literal in four cards, and no gate pins
CSS gradient geometry — the next canonical retune strands three stale copies with
every clause green (#509 review). The repo already holds the mechanism:
`--pd-card-back-stripes` is a gradient-valued sheet token whose inlined copies the
value gate enforces. The coin face takes the same route, and ADR-0033's consumer test
agrees: four independent consumers make it vocabulary, not anatomy.

## Files

| File | Action |
| --- | --- |
| `design/tokens/tokens.css` | edit — `--pd-coin-face: radial-gradient(circle at 36% 30%, var(--pd-coin-glint), var(--pd-coin) 55%, var(--pd-coin-deep))` beside the coin colors |
| `web-client/src/styles/tokens.css` | edit — vendored mirror; client CI pins byte-identity |

## Scope

- The token is born composed from the three coin color tokens, so a palette retune
  flows through it; consumption is `TASK-060120`'s and `TASK-060121`'s.

## Out of scope

- Card edits — the consumer tickets. The SVG — its lighting is its own gradient,
  pair-pinned already.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
