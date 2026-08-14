---
schema: 2
id: TASK-060114
title: The lockup constants join the drift gate
type: task
status: backlog
parent: STORY-0601
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060113]
verify:
  - ./design/check-drift.sh
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -pi -e "s/0\.42em/0.43em/g" "$T/design/screens/create-duel.html" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -pi -e "s/#b8c6d6/#b8c6d7/g" "$T/design/screens/duel-end.html" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
---

## Goal

The wordmark lockup's constants — the 0.92em coin, the 0.42em gap, the 0.01em tracking,
the 0.06em inset ring, and the glint `#b8c6d6` — are born in `graphics/wordmark.html`
and copied by other cards (`create-duel.html` today, the gallery's CSS coin, the
duel-end coin), and no gate compares the copies to the canonical (#456 review round 2,
graded follow-up-ticket material with the `TASK-060108` precedent named). A retune of
the mark leaves stale lockups behind with no failure anywhere.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — pin every card's lockup/coin-glint constants to the canonical wordmark's |

## Scope

- The gate reads the canonical constants from `graphics/wordmark.html` (single source)
  and fails any card whose `.mark`/`.coin` rules carry a differing value for the same
  constant, or whose coin gradient cites a glint hex differing from the canonical's.
- Cards that lack the lockup entirely stay silent — the gate compares copies, it does
  not demand adoption.
- Whether the constants should instead be born on the sheet as tokens is a live
  alternative the implementer may propose back; this ticket takes the gate route
  because em-ratios of the mark are the mark's anatomy, not shared design decisions
  (the sheet's own header scopes it to color, size, spacing and radius).
- Stock macOS/Linux tools; setup failures exit 2, the gate's deliberate failure is
  exit 1; the scratch copy is left for the OS to purge, per the sibling gate tickets.

## Out of scope

- Token names, values, mirror pairs, symbol blocks — TASK-060106/060111/060112/060113.

## Tests

None — the verify runs the gate on the aligned tree and proves two negative paths
(a drifted em constant, a drifted glint hex) on mutated scratch copies.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
