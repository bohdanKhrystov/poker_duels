---
id: STORY-0603
title: Draw the graphics — suit glyphs, the duel coin, the wordmark
type: story
status: ready
parent: EPIC-06
module: design
labels: [design]
depends_on: [STORY-0601]
---

## Goal

The product's drawn assets exist as versioned SVG and one gallery card: a suit-glyph set so
every platform shows the same pips, the steel duel coin, and the wordmark lockup — all
reviewable in the claude.ai/design pane.

## Why

The table component ships with typographic corners; the drawn set replaces the pip without
touching geometry (`TASK-060201`'s note). The coin is the product's one emblem — profile,
favicon, victory screen — and `EPIC-03`/`EPIC-04` consume it.

## Design notes

- **Never golden** (docs/vision.md): the coin is steel — literals mirroring `--pd-coin`
  `#9fb2c4`, `--pd-coin-deep` `#64788c` — crossed rapiers behind the count "1".
- **Suits**: one drawn set on a 24×24 grid, four `<symbol>`s, colored at the use site with
  the suit tokens. Silhouettes must read at 20px.
- **Wordmark**: a CSS lockup, not a logotype — the system stack, bold "Poker", quiet
  "Duels", the coin as the mark at 0.92em. No bespoke lettering, no webfont.
- SVG files carry literal colors (assets travel without CSS scope) with a comment naming
  the tokens they mirror — token names spelled without leading dashes, since XML forbids
  `--` inside comments.
- The gallery card inlines both SVGs (`xmlns` stripped when inlined), group `Graphics`;
  the wordmark card is group `Brand`. Card conventions per `design/README.md`.
- Every SVG must parse as XML: verify uses `python3 -c "import xml.dom.minidom, …"`.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-060301](../tasks/TASK-060301-suit-glyph-set.md) | The suit-glyph set | done |
| [TASK-060302](../tasks/TASK-060302-duel-coin.md) | The duel coin | done |
| [TASK-060303](../tasks/TASK-060303-wordmark-card.md) | The wordmark card | ready |
| [TASK-060304](../tasks/TASK-060304-graphics-gallery-card.md) | The graphics gallery card | ready |

## Acceptance criteria

- [ ] The gallery and wordmark cards render in the pane under **Graphics** and **Brand**.
- [ ] The human has seen them there and signed off.
- [ ] Both SVGs parse as XML and contain nothing golden.

## Out of scope

- Full 52-card SVG faces and court-card art — a later story, if ever; the typographic
  corners may simply win.
- App-store iconography, favicons as files — `EPIC-07` delivery territory.
