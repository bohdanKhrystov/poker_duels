---
schema: 2
id: TASK-060115
title: The coin glint is born on the sheet
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
  - grep -qF -- '--pd-coin-glint: #b8c6d6' design/tokens/tokens.css
  - cmp design/tokens/tokens.css web-client/src/styles/tokens.css
  - 'grep -qF "pd-coin-glint (#b8c6d6)" design/graphics/duel-coin.svg'
  - ./design/check-drift.sh
---

## Goal

`#b8c6d6` — the coin's glint — is a color, and `ADR-0024 §2` says every color is born
in the sheet; today it is born in `duel-coin.svg` and copied by three CSS coins with
no gate anywhere (#474 review, which chose this token route over a bespoke gate
clause precisely so `TASK-060111`'s value loop and `TASK-060112`'s mirror-pair gate
cover the copies with zero new gate code).

## Files

| File | Action |
| --- | --- |
| `design/tokens/tokens.css` | edit — `--pd-coin-glint: #b8c6d6` beside the coin pair, with its why |
| `web-client/src/styles/tokens.css` | edit — vendored mirror; client CI pins byte-identity |
| `design/graphics/duel-coin.svg` | edit — the head comment's mirror pairs gain `pd-coin-glint (#b8c6d6)` |

## Scope

- The token is born beside `--pd-coin`/`--pd-coin-deep` with a comment naming it the
  glint — the lighting the SVG and every CSS coin share.
- The SVG's gradient stop keeps its literal (SVG cannot `var()`); the mirror-pair
  comment is what `TASK-060112`'s gate reads, so a sheet retune fails the stale SVG.
- Consumption by the CSS coins is `TASK-060116`'s — this ticket only births the value.

## Out of scope

- Card edits — `TASK-060116` (consumers) and `TASK-060117` (the Colors swatch).

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
