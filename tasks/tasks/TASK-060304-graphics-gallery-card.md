---
schema: 2
id: TASK-060304
title: The graphics gallery card
type: task
status: ready
parent: STORY-0603
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060301, TASK-060302]
verify:
  - 'head -1 design/graphics/gallery.html | grep -q "<!-- @dsCard group=\"Graphics\" -->"'
  - grep -q '<title>' design/graphics/gallery.html
  - grep -q 'symbol id="pd-spade"' design/graphics/gallery.html
  - grep -q '9fb2c4' design/graphics/gallery.html
  - '! grep -q "http" design/graphics/gallery.html'
---

## Goal

The pane can show the drawn assets: one card inlining the coin at four sizes and the suit
set at two, with the token literals each mirrors.

## Files

| File | Action |
| --- | --- |
| `design/graphics/gallery.html` | create |

## Scope

- Inline both SVGs (strip `xmlns` when inlining; the card stays request-free).
- Coin at 192 / 96 / 32 / 20; suits at 72 and 20 on a card-face chip.

## Out of scope

- Editing the SVGs — feedback routes to `TASK-060301/02`.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
