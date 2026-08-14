---
schema: 2
id: TASK-060107
title: Align the shipped foundation cards with the later contract fixes
type: task
status: done
parent: STORY-0601
module: design
estimate: S
tier: opus
review: light
files_touched: 3
labels: [design]
depends_on: [TASK-060102, TASK-060103]
verify:
  - '! grep -E "[♠♥♦♣]([^︎]|$)" design/tokens/colors.html'
  - '! grep -E "[♠♥♦♣]([^︎]|$)" design/tokens/type.html'
  - grep -q 'components/' design/README.md
  - grep -q 'Components' design/README.md
---

## Goal

The two earliest cards and the README catch up with contracts fixed later in review: suit
glyphs carry the U+FE0E text-presentation selector everywhere, the eyebrow carries the
micro weight it specifies, and the README's layout tree and group index tell the truth
again.

## Files

| File | Action |
| --- | --- |
| `design/tokens/colors.html` | edit — U+FE0E on suits, eyebrow weight 500 |
| `design/tokens/type.html` | edit — U+FE0E on suits in the ranks panel |
| `design/README.md` | edit — components/ and screens/ in the tree; groups list gains Components, Screens, Graphics, Brand |

## Scope

- Append `&#xFE0E;` to every raw ♠♥♦♣ so OEM emoji fallbacks cannot repaint the suits.
- `.eyebrow` gains `font-weight: 500` in colors.html (type.html already has it).
- README enumerations match the directory as it exists at this ticket's merge, and the
  push procedure gains the `_ds_manifest.json` step — without it a new card's file exists
  in the project but never appears in the pane (the 2026-08-14 incident).

## Out of scope

- The card resting-shadow and back-texture tokens — `TASK-060108`.
- Migrating card chrome from px literals to rem — cosmetic, still unticketed.

## Tests

None — the verify greps assert no bare suit glyph remains and the README names the new
directory and group.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
