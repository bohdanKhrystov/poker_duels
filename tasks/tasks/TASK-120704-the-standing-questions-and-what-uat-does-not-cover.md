---
schema: 2
id: TASK-120704
title: The standing questions, and what UAT does not cover
type: task
status: done
parent: STORY-1207
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [process, qa, uat, meta]
depends_on: [TASK-120703]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk -F'|' '/^### /{s=0} /^### The standing questions/{s=1} s && $2 ~ /`UAT-Q[0-9]`/ { n++; if (NF!=5) bad++; q=$3; gsub(/[ \t]+$/,"",q); if (q !~ /\?$/) bad++; if (index($4,"ADR-0092")==0) bad++ } END{ exit (n==4 && bad==0)?0:1 }' docs/test-plan.md
  - awk '/^## /{s=0} /^## UAT —/{s=1} s && index($0,"contradicts a merged source"){a=1} s && index($0,"question"){b=1} END{exit (a&&b)?0:1}' docs/test-plan.md
  - awk '/^## /{s=0} /^## What this catalogue does not cover/{s=1} s && index($0,"UAT"){f=1} END{exit f?0:1}' docs/test-plan.md
  - awk -F'|' '/^### /{s=0} /^### Not yet written/{s=1} s && $2 ~ /EPIC-0[456]/ { n++; if ($3 ~ /until its first round/) bad=1 } END{ exit (n==3 && !bad)?0:1 }' docs/test-plan.md
  - awk -F'|' '/^### /{s=0} /^### Not yet written/{s=1} s && $2 ~ /EPIC-04/ { seen=1; if ($3 ~ /not written/) bad=1; if ($3 !~ /suite/) bad=1 } END{ exit (seen && !bad)?0:1 }' docs/test-plan.md
  - awk -F'|' '/^### /{s=0} /^### Not yet written/{s=1} s && $2 ~ /EPIC-05/ { seen=1; if ($3 ~ /not written/) bad=1; if ($3 !~ /suite/) bad=1 } END{ exit (seen && !bad)?0:1 }' docs/test-plan.md
  - awk -F'|' '/^### /{s=0} /^### The screen inventory/{s=1} s && $2 ~ /`/ { n++; if (NF!=7) bad++ } END{ exit (n==13 && bad==0)?0:1 }' docs/test-plan.md
---

## Goal

The `## UAT` section carries the four standing questions a round asks at every screen, the
sentence that separates a finding from a question, and what UAT does not cover — and three stale
rows in §*Not yet written* stop saying something that has been false for two rounds.

## Files

| File | Action |
| --- | --- |
| `docs/test-plan.md` | modify |

You may **read** `docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md`
§Context and §3, and `tasks/stories/STORY-1205-…` and `STORY-1206-…` frontmatter only (to confirm
two rounds have run).

## Scope

### 1. `### The standing questions` — four rows, three columns

Under the screen inventory `TASK-120703` wrote. `ADR-0092` §7: *"the standing question list,
transcribed from the human's 2026-08-30 request with this ADR as its `source`."* The human's
words are in that ADR's §Context, verbatim:

> focus is on UX (also question the ux like: is the main info properly highlighted? is it clear
> to user what is going on? are all options accessible? etc) and client match the designs

Four questions, ids `UAT-Q1` … `UAT-Q4`, each ending in a question mark, each `source` cell
citing `ADR-0092` §Context. Transcribe; do not invent a fifth and do not paraphrase one away.

| # | the question | source |
| --- | --- | --- |

### 2. The sentence that makes the list safe

Immediately under the table, in the same section, `ADR-0092` §3's classifier in its own words:
an observation may be filed as a finding **only when it contradicts a merged source** — a card,
`design/tokens/tokens.css`, an owned literal, an ADR section, a `docs/duel-rules.md` heading, a
`docs/vision.md` sentence — and an answer that contradicts nothing merged is a **question**, whose
only route is the `uat` agent's `QUESTIONS` section, at most three per screen. The phrase
`contradicts a merged source` is what gate 3 matches, so write it, not a synonym.

### 3. One bullet in `## What this catalogue does not cover`

Following the section's existing shape — *"stated so nobody reads a `PASS` as more than it is"* —
one bullet naming what a UAT round does **not** see:

- **Taste.** A judgment with no merged source to contradict is a question, never a finding, and
  no round answers one (`ADR-0092` §§3, 5). The `product-owner` answers; the human answers only
  what would change the vision.
- **`#/verify` and `#/reset`**, for the reason this section already gives about recovery — carried
  across as a cross-reference, not restated at length.

### 4. Three stale rows in `### Not yet written`

- `EPIC-04` and `EPIC-05` both read *"provisional until its first round"*. **Both have been run
  twice** — round 1 on 2026-08-29 (`STORY-1205`) and round 2 on 2026-08-30 (`STORY-1206`) — so
  the clause is false as written. Replace it with what is true: authored 2026-08-29 from merged
  sources, **run in rounds 1 and 2**, whose corrections are tracked under `STORY-1205` and
  `STORY-1206`. Each cell must keep the word `suite` and must not contain `not written` — gates 5
  and 6 are `TASK-120401`'s and `TASK-120402`'s own, re-run here because this ticket edits the
  rows they pin.
- `EPIC-06` reads *"not written — and mostly not testable this way; `qa` is told not to report
  styling"*. The second clause is now false: `qa` is still told not to report styling, and the
  **`uat` focus reports exactly that**. Rewrite the cell to say so and to point at the `## UAT`
  section. Keep `not written` in this one — no `EPIC-06` case suite exists and none is asked for.

## Out of scope

- **Deleting either suite's `> **Provisional** — authored …` line.** `ADR-0090` §5 gives that
  deletion to *"the round record that first runs it"*, and that record must also *name the cases
  that round corrected*. Three of those corrections — `TASK-120504`, `TASK-120603`, `TASK-120604`
  — have not merged, so the line is not yet false and striking it here would be a claim this
  ticket cannot support. It belongs to `STORY-1205`/`STORY-1206`, and the §*Not yet written* rows
  are corrected precisely because they say something that **is** already false.
- **Writing an `EPIC-06` case suite**, or any case at all.
- **The screen inventory table.** `TASK-120703` wrote it; gate 8 only checks it survived.
- **Adding a fifth standing question, or an answer to any of the four.** The `uat` agent asks and
  answers nothing; `qa-manager` promotes at most three per round; the `product-owner` answers.
- **Registering a `DEC`.** None is open here — every sentence above is transcription or an
  application of `ADR-0092` §§3, 5, 7.
- **Any file but `docs/test-plan.md`.**

## Tests

No test class — the deliverable is document prose. Every row was run on 2026-08-30 at commit
`cfcc6a4e`, against the tree as it stands and against a four-row draft.

| # | Gate | Proves | Today | After |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | question table | four `UAT-QN` rows at three columns, each cell-3 ending in `?`, each cell-4 naming `ADR-0092` | **1** | 0 |
| 3 | classifier phrase, scoped to `## UAT` | the finding/question line is written **in that section** | **1** | 0 |
| 4 | `## What this catalogue does not cover` names UAT | the section says what a UAT round does not see | **1** | 0 |
| 5 | no `EPIC-0[456]` row says *until its first round* | the stale clause is gone from all three rows, and there are exactly three rows | **1** | 0 |
| 6 | `EPIC-04` row still says `suite`, never `not written` | `TASK-120401`'s own gate survives the edit | 0 | 0 |
| 7 | `EPIC-05` row still says `suite`, never `not written` | `TASK-120402`'s own gate survives the edit | 0 | 0 |
| 8 | inventory still thirteen rows at seven fields | `TASK-120703`'s table survived | **1** until that ticket merges | 0 |

**Gates 3 and 4 are string checks and cannot see obedience.** Gate 3 passes the moment the phrase
`contradicts a merged source` appears inside the `## UAT` section; it cannot see that any observer
applies it. That half is `.claude/agents/uat.md`'s, in `TASK-120706`, and the reviewer's here. The
gates are worth having anyway for the reason `TASK-120301` gave: they put the sentence in words a
later editor has to **delete** rather than merely fail to add.

**Gate 2 is not a string check.** Four rows, a fixed column count, a question mark at the end of
each question and a `source` naming the ADR are four independent shape facts, and a mutation that
drops a row, adds a fifth, or writes a statement instead of a question turns it red.

**Gate 5 is measured against today's tree, not computed.** Three rows match `EPIC-0[456]` in
§*Not yet written* today; `EPIC-04`'s and `EPIC-05`'s carry *until its first round* and
`EPIC-06`'s does not, so the gate is red on two rows and the row count pins the third.

## Acceptance criteria

- [ ] `### The standing questions` holds exactly four rows, `UAT-Q1`…`UAT-Q4`, each a question
      ending in `?` and each sourced to `ADR-0092` §Context (gate 2).
- [ ] The `## UAT` section states that a finding must contradict a merged source and that anything
      else is a question (gate 3).
- [ ] `## What this catalogue does not cover` names taste and the two unwalkable recovery
      addresses as things a UAT round does not see (gate 4).
- [ ] No `EPIC-04`, `EPIC-05` or `EPIC-06` row in §*Not yet written* says *"until its first
      round"*, and there are still exactly three of them (gate 5).
- [ ] The `EPIC-04` and `EPIC-05` rows still say `suite` and still do not say `not written`
      (gates 6 and 7).
- [ ] The `EPIC-06` row says that `qa` still refuses styling and that the `uat` focus reports it.
- [ ] Neither suite's `Provisional` line is touched.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
