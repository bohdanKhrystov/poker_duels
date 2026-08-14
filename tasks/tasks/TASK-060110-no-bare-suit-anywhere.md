---
schema: 2
id: TASK-060110
title: No bare suit glyph anywhere, enforced in the drift check
type: task
status: done
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 2
labels: [design]
depends_on: [TASK-060106]
verify:
  - ./design/check-drift.sh
  - '! grep -rE "[♠♥♦♣]([^︎]|$)" design --include="*.html"'
---

## Goal

A suit glyph without U+FE0E cannot land in any card, present or future: the drift check
sweeps every card for bare suits, and the one leak it immediately finds (spacing.html's
demo tile) is fixed.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — add the bare-suit sweep beside the name check |
| `design/tokens/spacing.html` | edit — the A♠ demo tile gains its selector |

## Scope

- The check reports file and glyph on failure, like the name check.

## Out of scope

- New suit renderings — the gallery inlines SVG and is exempt by construction.

## Tests

None — the check is its own gate, run over the real cards in `verify:`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
