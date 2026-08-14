---
schema: 2
id: TASK-060210
title: The duel-table screens consume the card-surface tokens
type: task
status: ready
parent: STORY-0602
module: design
estimate: XS
tier: opus
review: light
files_touched: 2
labels: [design]
depends_on: [TASK-060108]
verify:
  - grep -q -- '--pd-shadow-card' design/screens/duel-table.html
  - grep -q -- '--pd-card-back-stripes' design/screens/duel-table.html
  - grep -q -- '--pd-shadow-card' design/screens/duel-table-states.html
  - grep -q -- '--pd-card-back-stripes' design/screens/duel-table-states.html
  - ./design/check-drift.sh
---

## Goal

TASK-060108 moved the card resting shadow and back-stripe texture into the sheet, but the
two duel-table screen cards still carry the old byte-copied literals — the exact unguarded
drift the tokens were born to kill, and the names-only drift check cannot see it
(#431 review, CONFIRMED finding).

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | edit — inline-declare and consume the two tokens |
| `design/screens/duel-table-states.html` | edit — inline-declare and consume the two tokens |

## Scope

- Add `--pd-shadow-card` and `--pd-card-back-stripes` to each card's inlined `:root`;
  replace the hardcoded shadow and gradient literals with `var()` at every use site.
- The back's inset ring stays local, as in the component (it scales with card size).

## Out of scope

- Any visual change — renders must be pixel-identical, as in TASK-060108.

## Tests

None — the verify greps pin the token names at use, and check-drift passes.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
