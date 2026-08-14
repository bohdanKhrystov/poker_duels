---
schema: 2
id: TASK-060301
title: The suit-glyph set
type: task
status: done
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
  - grep -q 'symbol id="pd-spade"' design/graphics/suits.svg
  - grep -q 'symbol id="pd-heart"' design/graphics/suits.svg
  - grep -q 'symbol id="pd-diamond"' design/graphics/suits.svg
  - grep -q 'symbol id="pd-club"' design/graphics/suits.svg
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

## Deviations

The #436 review (4 CONFIRMED) reshaped the set before merge: the club's leaves and stem
never met, leaving an enclosed hole at its optical center (filled by pulling the bottom
leaves inward and raising the stem's flat top); the heart ran y 3..21.5 while the set
shares cap-line 1.5 and baseline 22.5 (extended, pixel-verified against spade/diamond);
each `<symbol>` gained explicit width/height so a bare `<use>` defaults to pip size; and
the ids took the `pd-` prefix because ids resolve document-wide once inlined — this
ticket's and TASK-060304's verify greps were updated to the new ids in the same PR.
`design/README.md` also drops "(planned)" from the graphics line, per the TASK-060204
precedent that the populating PR updates the layout note.
