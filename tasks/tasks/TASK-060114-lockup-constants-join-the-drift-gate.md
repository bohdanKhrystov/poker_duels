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

Two constant families travel between cards with no gate comparing the copies (#456
review round 2; inventory corrected by the #474 review). The lockup's em anatomy —
0.92em coin, 0.42em gap, 0.01em tracking, 0.06em ring — is born in
`graphics/wordmark.html` and copied wherever a card renders the mark
(`create-duel.html` today). The glint `#b8c6d6` is born in `duel-coin.svg` — the
canonical lighting, which wordmark's own comment defers to — and is copied by every
CSS coin (`wordmark.html`, `create-duel.html`, `duel-end.html`; the gallery's copy is
an inlined SVG symbol already pinned verbatim by `TASK-060113`). A retune of either
source leaves stale copies behind with no failure anywhere.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — pin lockup anatomy to wordmark's and every CSS glint to duel-coin.svg's |

## Scope

- Clause 1, the glint: the gate reads the canonical `#b8c6d6` stop from
  `duel-coin.svg` and fails any card whose coin `radial-gradient` cites a differing
  glint hex — wordmark included, so the SVG and CSS renderings cannot split silently.
- Clause 2, the anatomy: the gate reads the four em constants from `wordmark.html`'s
  `.mark`/`.mark .coin` rules and fails any other card declaring `.mark .coin` whose
  values differ. Cards without the lockup stay silent — `duel-end.html` has no
  `.mark` and is touched by clause 1 only (its 22px/2px coin is its own scale, not a
  copy of the lockup's).
- The recorded alternative: `ADR-0024 §2` and `design/README.md` say every color is
  born in the sheet, which argues for tokenizing the glint (`--pd-coin-glint`) rather
  than gating it; the em anatomy could be read the same way. This ticket deviates
  deliberately — the glint is the SVG's internal lighting mirrored into CSS copies
  (the `TASK-060112` mirror-pair class, not a shared decision the sheet retunes), and
  four single-use em ratios would be sheet noise. The implementer may counter-propose
  the token route; the deviation is recorded here either way, per the #474 review.
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
