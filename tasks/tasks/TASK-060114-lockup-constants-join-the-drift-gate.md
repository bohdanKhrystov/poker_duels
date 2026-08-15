---
schema: 2
id: TASK-060114
title: The lockup constants join the drift gate
type: task
status: done
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
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -pi -e "s/0\.42em/0.43em/g" "$T/design/graphics/wordmark.html" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
---

## Goal

The lockup's em anatomy — 0.92em coin, 0.42em gap, 0.01em tracking, 0.06em ring — is
born in `graphics/wordmark.html` and copied wherever a card renders the mark
(`create-duel.html` today), and no gate compares the copies to the canonical (#456
review round 2; inventory corrected twice by the #474 review). A retune of the mark
leaves stale lockups behind with no failure anywhere. The glint half of the original
finding was settled by conforming to `ADR-0024 §2` instead — `TASK-060115`/`116`/`117`
put it on the sheet, where the existing gates cover it.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — pin every card's `.mark`/`.mark .coin` em constants to wordmark's |

## Scope

- The gate reads the four em constants from `wordmark.html`'s `.mark`/`.mark .coin`
  rules and fails any other card declaring `.mark .coin` whose values differ. Cards
  without the lockup stay silent — `duel-end.html` declares no `.mark` (its 22px/2px
  coin is its own scale, not a copy of the lockup's).
- **Decided by [`ADR-0033`](../../docs/adr/ADR-0033-component-anatomy-is-born-in-its-canonical-card.md)**:
  the anatomy is card-born and this gate clause is the mechanism. The gate reads the
  four em values, deliberately not the declaration blocks (a conformant copy may fold
  shared-`.coin` properties in). The question was minted as `DEC-032` and renumbered
  `DEC-034` on the register; the ticket proceeds when its `depends_on` chain reaches
  it.
- Stock macOS/Linux tools; setup failures exit 2, the gate's deliberate failure is
  exit 1; the scratch copy is left for the OS to purge, per the sibling gate tickets.
  The two negatives mutate the copy and the canonical respectively, so an
  implementation hard-coded to one file cannot pass both.

## Out of scope

- Token names, values, mirror pairs, symbol blocks — TASK-060106/060111/060112/060113.

## Tests

None — the verify runs the gate on the aligned tree and proves the negative path on
both sides: a drifted copy and a drifted canonical, each on a mutated scratch copy.
(The glint half of the original finding moved to `TASK-060115`–`117`, so no glint
negative lives here.)

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
