---
schema: 2
id: TASK-060101
title: The canonical design token sheet
type: task
status: done
parent: STORY-0601
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - test -f design/tokens/tokens.css
  - grep -q -- '--pd-bg:' design/tokens/tokens.css
  - grep -q -- '--pd-accent:' design/tokens/tokens.css
  - grep -q -- '--pd-card-face:' design/tokens/tokens.css
  - grep -q -- '--pd-space-4:' design/tokens/tokens.css
  - grep -q -- '--pd-fs-body:' design/tokens/tokens.css
  - '! grep -qiE "gold|felt|mahogany" design/tokens/tokens.css'
---

## Goal

One CSS file declares every color, type size, spacing step, radius and surface the product
may use — the single place a design value is allowed to be born.

## Files

| File | Action |
| --- | --- |
| `design/tokens/tokens.css` | create |

## Scope

- Custom properties on `:root`, prefixed `--pd-`, grouped: surfaces, text, accent, semantic
  (win/loss/warn), table & cards, type (family/size/weight), spacing, radii, focus.
- Dark values only; the file says so at the top.
- A comment per group saying what the group is *for*, not what each value is.

## Out of scope

- Preview cards — `TASK-060102..04`.
- Light theme values — out of scope epic-wide.

## Tests

None — structural gates in `verify:` (file exists, required token names present, and no
casino vocabulary anywhere in the sheet).

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

The human's acceptance of the palette is a **story-level** criterion (`STORY-0601`), judged
on the rendered cards — it is deliberately not restated here, so this task's `done` claims
only what is true at its merge.

## Definition of done

Standard, per [`tasks/README.md`](../README.md), with the epic's recorded deviation: the
review is visual, in claude.ai/design.
