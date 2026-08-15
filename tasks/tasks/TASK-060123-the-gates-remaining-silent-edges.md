---
schema: 2
id: TASK-060123
title: The lockup clause cannot go quiet
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
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -0777 -pi -e "s/\.mark \.coin \{/.lockup .disc {/g" "$T/design/screens/create-duel.html" && grep -q "lockup .disc" "$T/design/screens/create-duel.html" && ! grep -q "mark .coin" "$T/design/screens/create-duel.html" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
---

## Goal

Clause 6 counts the cards that copy the wordmark lockup and compares each one's
anatomy to the canonical, but nothing floors that count. Rename the classes in the
one card that copies it — `create-duel.html` — and the clause reports "the lockup
anatomy holds across 0 copies" and exits 0, having compared nothing (#511 review,
reproduced). A copy that stops being recognised must not read as no copy at all.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — the lockup clause refuses a silent zero |

## Scope

- Clause 4 is the shape to mirror: it floors its class with `[ "$svgs" -gt 0 ]` and
  fails when the graphics go invisible. Clause 6 needs the equivalent — a card whose
  markup plainly carries the lockup but yields no anatomy fails, rather than
  vanishing from the count. (Clause 5 floors its *canonical* set, not a copy count,
  so it is not the model here — #523 review corrected this.)
- The negative renames every `.mark .coin` rule in the card, asserts the rename
  actually applied before running the gate, and demands exit 1.

## Out of scope

- How the reader reads values, and how the name gate resolves names against the
  sheet — `DEC-035` and `TASK-060121` own that set; this ticket touches only the
  lockup clause's floor, so the two never edit the same logic.

## Tests

None — the verify runs the gate on the aligned tree and proves the negative on a
mutated scratch copy, per the sibling gate tickets.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
