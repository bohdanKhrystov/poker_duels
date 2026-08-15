---
schema: 2
id: TASK-060123
title: The gate's remaining silent edges
type: task
status: backlog
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: [TASK-060122]
verify:
  - ./design/check-drift.sh
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -0777 -pi -e "s/\.mark \.coin \{/.lockup .disc {/g" "$T/design/screens/create-duel.html" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -0777 -pi -e "s/--pd-bg: #131211;/--pd-bg: #131211; --pd-ghost: 1px;/" "$T/design/screens/duel-end.html" && perl -0777 -pi -e "s|/\* Surfaces|/* --pd-ghost lives only here. Surfaces|" "$T/design/tokens/tokens.css" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
---

## Goal

Two ways the gate still passes something it should refuse, both found by the #511
review:

- **The lockup clause can go quiet.** `anat_copies` has no floor, so if the one card
  that copies the lockup renames its classes, the clause reports "holds across 0
  copies" and exits 0 — the vacuous pass the sibling clauses each guard against.
- **The name gate reads comments.** A card may inline a `--pd-` token the sheet only
  *mentions* in prose; clause 1 vouches for the name from that comment and clause 3
  finds no sheet declaration to compare against, so an undeclared token passes both.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — floor the anatomy clause; the name gate reads declarations, not prose |

## Scope

- The anatomy floor mirrors clause 5's shape: the canonical side is already floored,
  so the copy side fails when a card that plainly carries the lockup markup produces
  no anatomy — a renamed-class copy must not read as "no copies".
- The name gate keeps accepting a token *printed* on a card (clause 1's stated
  contract) but resolves names against the sheet's declared set — the reader's output
  — rather than against every string in the sheet, comments included.
- Both negatives are in `verify:`, each red on today's gate — the class-rename
  substitution carries `/g` so it renames every `.mark .coin` rule in the card, not
  just the first (#511 review: without it the fixture passed for an unrelated reason).

## Out of scope

- `web-client/src/styles/theme.test.ts` carries a third `--pd-` parser with a wider
  name language (`[\w-]` against the gate's `[a-z0-9-]`); it agrees today only because
  the sheet writes one declaration per line. That divergence belongs to the client
  module and is recorded here rather than fixed here.

## Tests

None — the verify runs the gate on the aligned tree and proves both negatives on
mutated scratch copies, per the sibling gate tickets.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
