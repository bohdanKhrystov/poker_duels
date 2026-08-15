---
schema: 2
id: TASK-060121
title: A final declaration without a semicolon still enters the gate
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
  - ./design/check-drift.sh
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -0777 -pi -e "s/--pd-shadow-card:[^;]*;/--pd-shadow-card:0 1px 4px rgba(0,0,0,0.4)/" "$T/design/tokens/tokens.css" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -0777 -pi -e "s/--pd-focus-offset:\s*2px;/--pd-focus-offset:2px/" "$T/design/screens/create-duel.html" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1'
---

## Goal

The value gate's extractor matches declarations only up to a `;`, and a final
declaration legally written without one misbehaves differently on each side — both
proved by the #510 review's experiments. Sheet side: the sheet's last declaration
(`--pd-shadow-card`, nothing after it but `}`) never enters the compared set, so
drifting it semicolon-less passes every clause silently. Card side: the walk merges
forward across `}` to the next `;`, so an aligned semicolon-less value garbles into
a **false** drift report. The fix — the walk ends at `;` or the block's `}` — cures
both at once.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — `EXTRACT` terminates a declaration at `;` or `}`; the self-test probes the semicolon-less form |

## Scope

- The emitted normal form is unchanged, so every existing comparison keeps working
  (verified byte-identical extraction across all current files by the review).
- The verify's two fixtures are discriminating, red on the unfixed tree by the
  review's own runs: the sheet-side drift must start failing (it silently passes
  today), and the card-side aligned mutation must stop failing (it false-positives
  today).

## Out of scope

- Every other clause of the gate.

## Tests

None — the verify runs the gate on the aligned tree and proves both sides of the
termination fix on mutated scratch copies, per the sibling gate tickets.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.

## Deviations

- The fix's shape changed under review. Adding `}` to the regex's terminator class
  delivered the goal but introduced five reproduced regressions — a value carrying a
  brace left the gate on both sides in silence, commented-out declarations and code
  samples became live, a brace inside a quoted value truncated it — so the value gate
  moved onto the reader `SYMEXTRACT` and `ANATOMY` already use: HTML comments dropped,
  `<style>` blocks scoped, CSS comments stripped quote-aware, declarations walked with
  quotes opaque, and candidates cross-counted against emissions so an unreadable shape
  fails loudly instead of leaving enforcement. Extraction is byte-identical to the
  previous reader across all fifteen design files, proved by diffing both programs
  over every one.
- That closes three pre-existing holes the review found beside the regressions: a
  commented-out declaration in a card or the sheet no longer reads as live, and a
  comment quoting a superseded value no longer licenses that value in a card.
- `files_touched: 1` counts the design file; the bookkeeping flips ride along per the
  `TASK-060108` precedent.
