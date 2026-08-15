---
schema: 2
id: TASK-060210
title: The duel-table screens consume the card-surface tokens
type: task
status: done
parent: STORY-0602
module: design
estimate: XS
tier: opus
review: light
files_touched: 2
labels: [design]
depends_on: [TASK-060108]
verify:
  - grep -qF -- 'var(--pd-shadow-card)' design/screens/duel-table.html
  - grep -qF -- 'var(--pd-card-back-stripes)' design/screens/duel-table.html
  - grep -qF -- 'var(--pd-shadow-card)' design/screens/duel-table-states.html
  - grep -qF -- 'var(--pd-card-back-stripes)' design/screens/duel-table-states.html
  - test "$(grep -c -- '0 1px 3px' design/screens/duel-table.html)" -eq 1
  - test "$(grep -c -- '0 1px 3px' design/screens/duel-table-states.html)" -eq 1
  - test "$(grep -c -- 'repeating-linear-gradient' design/screens/duel-table.html)" -eq 1
  - test "$(grep -c -- 'repeating-linear-gradient' design/screens/duel-table-states.html)" -eq 1
  - ./design/check-drift.sh
---

## Goal

TASK-060108 moved the card resting shadow and back-stripe texture into the sheet, but the
two duel-table screen cards still carry the old byte-copied literals at their use sites
(#431 review, CONFIRMED finding). Consuming the tokens puts the screens back on the single
source when a value is retuned; the inline `:root` copies the convention requires stay
byte-copies, and guarding those at the value level is TASK-060111's job.

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

None — the verify pins `var()` consumption per file and pins each value literal to
exactly its `:root` declaration (count of one), so a half-done replacement cannot pass;
check-drift stays green.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.
