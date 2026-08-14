---
schema: 2
id: TASK-060112
title: The drift gate reads the graphics
type: task
status: ready
parent: STORY-0601
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060110]
verify:
  - ./design/check-drift.sh
  - sh -c 'T=$(mktemp -d); cp -R design "$T/design"; perl -pi -e "s/26231f/26231e/g" "$T/design/graphics/suits.svg"; ! "$T/design/check-drift.sh" >/dev/null 2>&1'
---

## Goal

`check-drift.sh` sweeps only `*.html`, so the graphics are the first design-file class the
gate cannot see: `suits.svg` mirrors `--pd-suit-black`/`--pd-suit-red` as raw hex, and a
sheet re-hex leaves the SVG stale with no failure anywhere (#436 review, CONFIRMED
mechanism; the coin repeats the pattern).

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — sweep `design/graphics/*.svg` for token-mirror pairs |

## Scope

- Codify the convention the graphics already follow: an SVG names each token it mirrors
  beside the literal, `pd-NAME (#hex)`, in its head comment; every `fill`/`stop-color`
  hex that mirrors a token appears in such a pair.
- The gate extracts the pairs and fails when the sheet's `--pd-NAME` is missing or its
  value differs from the cited hex; an SVG with zero pairs fails (the class must not be
  invisible again).
- Stock macOS/Linux tools; keep the self-test pattern — the verify's second command
  proves a re-hexed mirror fails.

## Out of scope

- Non-token literals (gradient highlights, shadows drawn as art) — the pair convention
  marks exactly what claims to mirror the sheet.
- HTML cards — TASK-060111 guards their values.

## Tests

None — the verify runs the gate on the aligned tree and proves the negative path on a
mutated scratch copy.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
