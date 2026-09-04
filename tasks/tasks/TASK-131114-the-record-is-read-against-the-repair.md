---
schema: 2
id: TASK-131114
title: The record is read against the repair, one row per driven path
type: task
status: backlog
parent: STORY-1311
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [client, docs, qa]
depends_on: [TASK-131113]
verify:
  - awk '{ n += gsub(/NOT-YET-READ/, "&") } END { exit (n != 0) }' tasks/stories/STORY-1311-only-a-running-duel-refuses-another-screen.md
  - awk '/^\| `P[1-6]/ { n++ } END { exit (n != 7) }' tasks/stories/STORY-1311-only-a-running-duel-refuses-another-screen.md
  - grep -qF 'STORY-1310-the-refresh-paths-nobody-drove.md' tasks/stories/STORY-1311-only-a-running-duel-refuses-another-screen.md
  - awk '/^\| `P[1-6]/ { if ($0 !~ /TASK-1311[0-9][0-9]/ && $0 !~ /NO-REPAIR-OWED/) { print; bad = 1 } } END { exit bad ? 1 : 0 }' tasks/stories/STORY-1311-only-a-running-duel-refuses-another-screen.md
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Every path `STORY-1310` drove is answered in writing — by the ticket in this story that repaired it,
or by a stated reason no repair is owed — so `EPIC-13`'s refresh Definition-of-done row closes on
evidence rather than on a claim.

## Files

| File | Action |
| --- | --- |
| `tasks/stories/STORY-1311-only-a-running-duel-refuses-another-screen.md` | modify |

Read [`STORY-1310`](../stories/STORY-1310-the-refresh-paths-nobody-drove.md)'s seven `P` rows —
that is the input and it is the only long read this ticket needs — and
`docs/adr/ADR-0117-the-proofs-of-record-load-the-built-bundle.md` §7. **No source file is opened and
none is changed.**

## Scope

- Fill the seven rows of *`STORY-1310`'s record, read against this story's repair*. Each row's second
  cell must name **either** a `TASK-1311NN` id from this story **or** the literal `NO-REPAIR-OWED`,
  and then say in one or two sentences what was observed and what answers it. The `awk` gate refuses
  a row containing neither.
- Where a row is `NO-REPAIR-OWED`, the reason is a **merged source cited by name**, not an opinion.
  The three shapes available, and each is already written down somewhere:
  - the drive found the behaviour correct;
  - the behaviour is **accepted product behaviour** — `ADR-0118` §3's stale waiting screen, said out
    loud precisely so that a later reader files nothing;
  - the repair is **owed elsewhere and named there** — `ADR-0117` §7 for anything that re-drives on
    `built`, `DEC-110` for the server's absent seat check, `ADR-0114` §6 for the one-frame gap.
- Where two honest drives disagreed (`P1` has two), say so in the row. A disagreement is a finding,
  not a thing to average.

## Out of scope

- **Driving anything.** No stack, no browser, no `vite preview`. This is a read of a merged record
  against merged tickets, and `ADR-0089` §2b is why it can be a `verify:` gate at all while the
  drives it reads could not.
- **Editing `STORY-1310`.** Its rows are a record of what was observed at a named commit and are not
  rewritten because the code changed afterwards (`ADR-0089` §2c).
- **Editing `tasks/epics/EPIC-13-the-living-table.md`.** The DoD row is ticked when the epic closes,
  by whoever closes it; this ticket's job is to make the row citable. Leave the epic file alone —
  it is the most contended file in the repository and a second writer on it costs a rebase.
- **Claiming coverage.** `ADR-0089` §2c: a drive is a statement about one run, on one machine, at
  one commit. A row may say *this path was driven and this ticket repairs what it found*; it may not
  say *this path is now covered*.

## What the gates can and cannot check

They check that the table still has its seven rows, that no `NOT-YET-READ` placeholder survives,
that every row names a ticket or declares no repair owed, and that `STORY-1310` is cited by path.
**They cannot check that any sentence in a row is true.** That is the reviewer's, exactly as it was
for `TASK-131003`, and the ticket says so rather than dressing a document check up as a proof.

## Tests

**No test is written and the reason is merged.** There is no runtime behaviour here; the four gates
above are document-shape checks and one conformance check.

| Gate | Proves | Today |
| --- | --- | --- |
| no `NOT-YET-READ` remains | every row was read | **red** |
| the table still has seven `P` rows | no row was lost to a reformat or a rebase | green — a regression guard |
| every `P` row names a `TASK-1311NN` or `NO-REPAIR-OWED` | no row is answered with prose alone | **red** |
| `STORY-1310` is cited by filename | the story's last acceptance criterion, which asks for a path | green — a regression guard |

## Acceptance criteria

- [ ] All seven rows are filled, each naming a `TASK-1311NN` id or `NO-REPAIR-OWED`
- [ ] Every `NO-REPAIR-OWED` row cites the merged source that makes it so, by name
- [ ] `P1`'s row records that two honest drives disagreed
- [ ] `STORY-1310` is unedited — `git diff` touches one file
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
