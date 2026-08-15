---
schema: 2
id: TASK-060121
title: A final declaration without a semicolon still enters the gate
type: task
status: ready
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - ./design/check-drift.sh
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -0777 -pi -e "s/--pd-focus-offset: 2px;/--pd-focus-offset: 3px/" "$T/design/screens/create-duel.html" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
---

## Goal

The value gate's extractor matches declarations only up to a `;`, so a `:root`'s
final declaration written without one — legal CSS that renders identically — never
enters the compared set: the #510 review proved a semicolon-less `--pd-` declaration
escapes value comparison forever, on both the sheet side and the card side, the
exact silent-stale class the gate chain exists to close.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — `EXTRACT` accepts a declaration terminated by `;` or by its block's `}`; the self-test probes the semicolon-less form |

## Scope

- The extractor's declaration walk ends at `;` or `}`, whichever comes first; the
  emitted normal form is unchanged, so every existing comparison keeps working.
- The self-test gains a semicolon-less fixture, and the negative verify drifts a
  card's final declaration after stripping its semicolon — the gate must still
  catch it.

## Out of scope

- Every other clause of the gate.

## Tests

None — the verify runs the gate on the aligned tree and proves the negative path on
a mutated scratch copy, per the sibling gate tickets.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
