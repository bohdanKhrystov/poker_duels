---
schema: 2
id: TASK-060112
title: The drift gate reads the graphics
type: task
status: backlog
parent: STORY-0601
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060110, TASK-060111]
verify:
  - ./design/check-drift.sh
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -pi -e "s/26231f/26231e/g" "$T/design/graphics/suits.svg" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -pi -e "s/fill=\"#26231f\"/fill=\"#26231e\"/g" "$T/design/graphics/suits.svg" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -pi -e "s/pd-suit-(black|red) \(#[0-9a-f]{6}\)//g" "$T/design/graphics/suits.svg" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && mv "$T/design/graphics/suits.svg" "$T/suits.away" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -pi -e "s/cy=\"13.2\"/cy=\"13.3\"/g" "$T/design/graphics/gallery.html" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
---

## Goal

`check-drift.sh` sweeps only `*.html` token names, so the graphics escape it twice over:
`suits.svg` mirrors `--pd-suit-black`/`--pd-suit-red` as raw hex, and a sheet re-hex
leaves the SVG stale with no failure anywhere (#436 review, CONFIRMED mechanism; the
coin repeats the pattern); and `gallery.html` inlines verbatim copies of the canonical
`<symbol>` geometry that no gate compares against the source files, so an asset retune
leaves the gallery silently stale (#449 review, the same mechanism re-opened for
html-vs-svg).

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — sweep `design/graphics/*.svg` for token-mirror pairs |

## Scope

- Codify the convention the graphics already follow: an SVG names each token it mirrors
  beside the literal, `pd-NAME (#hex)`, in its head comment; every `fill`/`stop-color`
  hex that mirrors a token appears in such a pair.
- The gate extracts the pairs and fails when the sheet's `--pd-NAME` is missing or its
  value differs from the cited hex; an SVG with zero pairs fails, and so does finding
  zero SVGs under `design/graphics/` — the class must not go invisible again, vacuously
  or otherwise (suits.svg is merged, so at least one always exists).
- The gate also extracts every `<symbol id="pd-…">…</symbol>` block a card inlines and
  compares it, whitespace-normalized, against the same-id symbol in the canonical
  `design/graphics/*.svg` — a copy that drifts from its source fails the sweep.
- Stock macOS/Linux tools; keep the self-test pattern. The five negative-path verify
  commands pin one guard clause each — cited-pair drift, an orphaned fill hex, a
  pair-less SVG, zero SVGs, a mutated inlined symbol copy — and demand the gate's
  deliberate exit 1, with setup failures forced to exit 2 so a broken scratch copy
  cannot green a proof.
- Runs after TASK-060111 (`depends_on`): both gate tickets rewrite the same sweep
  region of `check-drift.sh`, so they are ordered, never concurrent.

## Out of scope

- Non-token literals (gradient highlights, shadows drawn as art) — the pair convention
  marks exactly what claims to mirror the sheet.
- HTML cards — TASK-060111 guards their values.

## Tests

None — the verify runs the gate on the aligned tree and proves the negative path on a
mutated scratch copy. The copy is left for the OS to purge: agent permission profiles
here deny `rm`, and a cleanup trap would stall an autonomous run on a prompt — ~100KB
per run under `mktemp -d` is the recorded trade-off.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
