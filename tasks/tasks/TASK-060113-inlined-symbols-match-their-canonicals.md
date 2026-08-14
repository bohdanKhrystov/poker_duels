---
schema: 2
id: TASK-060113
title: The gallery's inlined symbols match their canonicals
type: task
status: ready
parent: STORY-0601
module: design
estimate: S
tier: opus
review: light
files_touched: 3
labels: [design]
depends_on: [TASK-060112]
verify:
  - ./design/check-drift.sh
  - grep -q 'symbol id="pd-coin"' design/graphics/duel-coin.svg
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -pi -e "s/<symbol id=\"pd-club\"/<symbol id=\"pd-club\" data-drift=\"x\"/" "$T/design/graphics/gallery.html" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
---

## Goal

`gallery.html` inlines copies of the canonical symbol geometry that no gate compares
against the source files, so an asset retune leaves the gallery silently stale
(#449 review; split out of TASK-060112 by the #453 review because the spec needs a
canonical `pd-coin` symbol to exist first — `duel-coin.svg` has none today, the gallery
synthesizes its own wrapper).

## Files

| File | Action |
| --- | --- |
| `design/graphics/duel-coin.svg` | edit — wrap the artwork as `<symbol id="pd-coin" viewBox="0 0 96 96" width="96" height="96">` + `<use>`, render pixel-identical |
| `design/graphics/gallery.html` | edit — its `pd-coin` block becomes the canonical symbol verbatim |
| `design/check-drift.sh` | edit — compare every inlined `pd-*` symbol block against its same-id canonical |

## Scope

- `duel-coin.svg` keeps rendering standalone exactly as before (`<use href="#pd-coin"/>`
  after the symbol); the ticket's proof is a before/after screenshot compare.
- The gate extracts every `<symbol id="pd-…">…</symbol>` block from cards under
  `design/` and from `design/graphics/*.svg`, strips XML comments, normalizes
  whitespace, and fails when a card's block has no same-id canonical or differs from it.
- The negative verify mutates symbol *structure* (an injected attribute), not a geometry
  literal, so a legitimate retune of the artwork never falsifies the proof (#453 review).
- Stock macOS/Linux tools; setup failures exit 2, the gate's deliberate failure is
  exit 1, like the sibling gate tickets.

## Out of scope

- Token names, values, and mirror pairs — TASK-060106/060111/060112 own those clauses.

## Tests

None — the verify runs the gate on the aligned tree, pins the canonical symbol's
existence, and proves the negative path on a mutated scratch copy. The scratch copy is
left for the OS to purge, as recorded in the sibling gate tickets.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
