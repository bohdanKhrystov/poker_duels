---
schema: 2
id: TASK-120604
title: 05-04 walks the pages that exist, and says what a hidden Show more proves
type: task
status: backlog
parent: STORY-1206
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, harness]
depends_on: [TASK-120603]
verify:
  - awk -F'|' '/^\| `05-04` \|/ { if ($3 !~ /nextCursor/) bad=1 } END { exit bad }' docs/test-plan.md
  - awk -F'|' '/^\| `05-04` \|/ { if ($5 !~ /Show more/) bad=1 } END { exit bad }' docs/test-plan.md
  - awk -F'|' '/^\| `05-04` \|/ { if ($4 !~ /never increase/) bad=1 } END { exit bad }' docs/test-plan.md
  - awk -F'|' '/^\| `05-04` \|/ { if ($5 !~ /ASCII hyphen/) bad=1 } END { exit bad }' docs/test-plan.md
  - grep -qF '`05-04`' docs/test-plan.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`05-04` runs on a ladder of any size — checking the ordering and the minus sign over whatever pages
exist, and checking that *Show more* is offered exactly when there is another page — instead of
being blocked whenever the season fits on one screen.

## This is a harness defect, and what that means here

Filed under `ADR-0089` §4 and `EPIC-12` §Termination rule 6. It is **excluded from `B(2)`**, and
**no production code may be changed by this ticket** — the fix is one document. A `## Files` table
naming anything outside `docs/` is grounds to reject the diff on sight.

## The defect

Round 2 of `/qa-cycle regression`, 2026-08-29, commit `c7b35f4b`. `05-04` was **blocked**, not
failed, and the product is right in every particular.

The row's `do` cell is:

> on **L**'s ladder, walk the pages with `Show more`, reading each page's row lines with `A eval`,
> until L's own standing appears

The season holds **8 entries, all on the first page**, so `GET /api/standings` answers
`nextCursor: null`, `LadderScreen.tsx` renders *Show more* `hidden`, and the driver — since
`TASK-120505` — correctly reports *"found 1 match(es) for 'Show more', all invisible"* rather than
clicking a control no player can reach. There is nothing to page through and the prescribed walk
cannot be performed.

**The case's precondition is one a round cannot supply.** A page-2 needs more players than a round
has profiles, and `ADR-0089` §3 forbids seeding rows to reach a screen — so as written, `05-04` runs
only by luck of what the database happens to hold, which is the same rot `TASK-120503` removed from
five other rows.

**The property the case is after does not need the walk.** Verified this round on the one page that
exists: three `−1` rows, each a real U+2212 confirmed by code-point extraction, and standings running
1, 1, 1, 0, 0, −1, −1, −1 top to bottom — never increasing. The walk was the means; the ordering and
the minus sign are the end.

## Files

| File | Action |
| --- | --- |
| `docs/test-plan.md` | modify |

## Scope

- **Make the walk conditional on `nextCursor`.** Read `GET /api/standings`; read the rendered row
  lines; while the response carries a non-null `nextCursor`, press *Show more* and read again. On a
  one-page season the loop runs zero times and the case still runs.
- **Assert over every row read**, not over a page: a negative standing renders with U+2212, never an
  ASCII hyphen, and no standing is greater than one above it.
- **Gain one assertion the blocked case was silently not making**: *Show more* is offered exactly
  when `nextCursor` is non-null — visible with another page behind it, and not reachable by a player
  without one. That is the product behaviour `TASK-120505` established as correct, and it currently
  has no case at all.
- **Drop *"until L's own standing appears"***. A specific player's row is not the subject and it ties
  the case to which browser played which duel; a negative row is, and the round always produces one.

## Out of scope

- **Any file outside `docs/test-plan.md`.**
- **Seeding entries to force a second page.** `ADR-0089` §3 forbids it, and a case that needs it is
  a case that cannot run.
- **Weakening `05-04`'s `expect` or `fails if` to make it pass.** The minus-sign and ordering clauses
  are gated as preservation checks precisely so a rewrite cannot quietly drop them.
- **`05-04`'s `source` column** (`ADR-0090` §4).
- **A `drive.mjs` verb that reports a control's visibility.** `click` already prints
  *"all invisible"*, which is enough to write the new clause against. Not yet ticketed.

## Tests

None — the deliverable is document text, so the gates are structural checks over one row of it.

| Gate | Proves | Today |
| --- | --- | --- |
| `05-04` `$3 ~ /nextCursor/` | the walk is conditional on the response, not unconditional | **exits 1** |
| `05-04` `$5 ~ /Show more/` | the `fails if` judges the control's offer, which it never did | **exits 1** |
| `05-04` `$4 ~ /never increase/` | the ordering assertion survived the rewrite | exits 0 — it must keep doing so |
| `05-04` `$5 ~ /ASCII hyphen/` | the minus-sign assertion survived the rewrite | exits 0 — it must keep doing so |
| `05-04` still present | the row was not deleted to satisfy the gates | exits 0 — it must keep doing so |

All five were run against `docs/test-plan.md` at commit `c7b35f4b`; the first two exit `1` and the
last three exit `0`.

## Acceptance criteria

- [ ] `05-04`'s `do` cell reads `nextCursor` and presses *Show more* only while it is non-null.
- [ ] `05-04`'s `expect` asserts the ordering and the U+2212 minus over every row read, on a season
      of any size.
- [ ] `05-04`'s `fails if` fires when *Show more* is reachable with `nextCursor: null`, or
      unreachable without it.
- [ ] The row no longer names a particular player's standing as its stopping condition.
- [ ] The diff touches exactly one file, and it is `docs/test-plan.md`.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
