---
schema: 2
id: TASK-060302
title: The duel coin
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
  - python3 -c "import xml.dom.minidom; xml.dom.minidom.parse('design/graphics/duel-coin.svg')"
  - grep -q '9fb2c4' design/graphics/duel-coin.svg
  - grep -q '64788c' design/graphics/duel-coin.svg
  - '! grep -qi "gold" design/graphics/duel-coin.svg'
---

## Goal

The product's one emblem: a steel coin, crossed rapiers behind the count, legible from 20px
favicon to full-bleed victory screen.

## Files

| File | Action |
| --- | --- |
| `design/graphics/duel-coin.svg` | create |

## Scope

- 96×96 viewBox; rim, radial steel field, two crossed rapiers as geometry, an opaque inner
  disc carrying the "1".
- Colors mirror the coin tokens, stated in the file comment.

## Out of scope

- Counter variants ("2", "37") — the client renders counts in type; the coin stays "1".
- Favicon/app-icon export files — `EPIC-07`.

## Tests

None — XML parse and color greps in `verify:`; nothing golden, asserted.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
