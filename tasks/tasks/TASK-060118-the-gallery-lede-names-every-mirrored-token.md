---
schema: 2
id: TASK-060118
title: The gallery lede names every mirrored token
type: task
status: done
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - grep -q -- '--pd-coin-glint' design/graphics/gallery.html
  - ./design/check-drift.sh
---

## Goal

The gallery lede enumerates the tokens the graphics mirror, and the list predates
`--pd-coin-glint` (#508 review): a reader auditing the mirrors trusts the card's own
enumeration and concludes a glint retune cannot reach the SVGs, when clause 4 of the
drift gate in fact pins `duel-coin.svg` to that token.

## Files

| File | Action |
| --- | --- |
| `design/graphics/gallery.html` | edit — the lede's mirror list gains `--pd-coin-glint` |

## Scope

- One prose line; no geometry, no CSS values.

## Out of scope

- Everything else on the card.

## Tests

None — structural gates in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
