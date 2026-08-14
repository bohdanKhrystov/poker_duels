---
schema: 2
id: TASK-060301
title: The suit-glyph set
type: task
status: ready
parent: STORY-0603
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - python3 -c "import xml.dom.minidom; xml.dom.minidom.parse('design/graphics/suits.svg')"
  - grep -q 'symbol id="spade"' design/graphics/suits.svg
  - grep -q 'symbol id="heart"' design/graphics/suits.svg
  - grep -q 'symbol id="diamond"' design/graphics/suits.svg
  - grep -q 'symbol id="club"' design/graphics/suits.svg
  - grep -q '26231f' design/graphics/suits.svg
  - grep -q 'bf3b30' design/graphics/suits.svg
---

## Goal

One drawn suit set — four 24×24 `<symbol>`s — so every platform shows the same pips instead
of its own font's.

## Files

| File | Action |
| --- | --- |
| `design/graphics/suits.svg` | create |

## Scope

- Hand-authored paths: spade, heart, diamond, club; consistent visual weight; legible at
  20px.
- A demo row in the file itself, colored with the suit-token literals.

## Out of scope

- Replacing the component's font pips — EPIC-03 wires assets.
- Court-card art — not ticketed.

## Tests

None — XML parse and structural greps in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
