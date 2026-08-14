---
schema: 2
id: TASK-060108
title: The card resting shadow and back texture become tokens
type: task
status: done
parent: STORY-0601
module: design
estimate: S
tier: opus
review: light
files_touched: 3
labels: [design]
depends_on: [TASK-060101]
verify:
  - grep -q -- '--pd-shadow-card:' design/tokens/tokens.css
  - grep -q -- '--pd-card-back-stripes:' design/tokens/tokens.css
  - grep -q -- '--pd-shadow-card' design/tokens/colors.html
  - grep -q -- '--pd-shadow-card' design/components/playing-card.html
  - ./design/check-drift.sh
---

## Goal

Two design values born in cards move to the sheet where values are born: the playing card's
resting shadow and the back's stripe texture — currently byte-copied across colors.html and
playing-card.html with nothing guarding agreement.

## Files

| File | Action |
| --- | --- |
| `design/tokens/tokens.css` | edit — add `--pd-shadow-card`, `--pd-card-back-stripes`; reword the one-shadow comment to name the exception |
| `design/tokens/colors.html` | edit — consume the new tokens |
| `design/components/playing-card.html` | edit — consume the new tokens |
| `web-client/src/styles/tokens.css` | edit — CI-forced mirror of the canonical sheet (see Deviations) |

## Scope

- `--pd-shadow-card: 0 1px 3px rgba(0, 0, 0, 0.4)` — the only resting shadow, cards only.
- `--pd-card-back-stripes` carries the repeating-gradient; the inset ring stays local (it
  scales with card size).
- The sheet's "one shadow" comment now says: one floating shadow, one card resting shadow,
  nothing else.

## Out of scope

- Any visual change — this is a value relocation; renders must be pixel-identical.

## Tests

None — the verify greps pin the new names at both birth and use, and check-drift passes.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.

## Deviations

The real footprint was four files, not three: `web-client/src/styles/tokens.test.ts`
pins the client's vendored sheet byte-identical to the canonical one, so any edit to
`design/tokens/tokens.css` must copy the sheet to `web-client/src/styles/tokens.css`
in the same PR (discovered on this ticket's first CI run). `files_touched` stays 3 —
it is the planning estimate, and the schema caps it there. Every future tokens.css
ticket must list the vendored sheet from the start.
