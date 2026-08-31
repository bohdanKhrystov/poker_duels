---
schema: 2
id: TASK-121207
title: EPIC-12 Termination counts criteria under the audit focus
type: task
status: ready
parent: STORY-1212
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [process, qa, audit, meta]
depends_on: [TASK-121206]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk '/^## Termination/{s=1} /^## Definition of done/{s=0} s && index($0,"A(N-1)"){b=1} s && index($0,"rubric"){c=1} s && index($0,"top to bottom"){d=1} s && index($0,"A(N) = 0"){e=1} END{exit (b&&c&&d&&e)?0:1}' tasks/epics/EPIC-12-quality-and-defect-repair.md
  - awk '/^## Termination/{s=1} /^## Definition of done/{s=0} s && index($0,"B(N) >= B(N-1)"){a=1} s && index($0,"STOP_DIVERGING"){b=1} s && index($0,"baseline round"){c=1} s && index($0,"and `uat` focuses"){d=1} END{exit (a&&b&&c&&d)?0:1}' tasks/epics/EPIC-12-quality-and-defect-repair.md
  - awk 'index($0,"## The audit arithmetic"){a=1} index($0,"top to bottom"){b=1} END{exit (a&&b)?0:1}' .claude/agents/qa-manager.md
---

## Goal

`EPIC-12` §Termination says what rules 3 and 4 and the exit table mean under the audit focus, so
the epic's copy of the stopping rules and `qa-manager`'s copy say the same thing.

## Why this is a ticket rather than a sentence in another one

`STORY-1208` exists because §Termination rule 4 and `qa-manager`'s Step 6 drifted: both
special-cased round 1 and nothing else, so read literally they fired `STOP_DIVERGING` on the first
round in which repaired cards became measurable. Two copies of a rule drift, and the repair was two
tickets — `TASK-120801` for the manager, `TASK-120802` for the epic.

The audit focus adds a third arithmetic to the same two documents. `TASK-121206` landed the
manager's; this lands the epic's, in the same order and for the same reason.

**Nothing here is a new rule.** Every sentence is a transcription of `ADR-0096` §5, which is
merged.

## Files

| File | Action |
| --- | --- |
| `tasks/epics/EPIC-12-quality-and-defect-repair.md` | modify |

You may **read** `docs/adr/ADR-0096-…` §5 and `.claude/agents/qa-manager.md`
§*The audit arithmetic* — the copy this one must agree with, and which you may **not** modify.

## Scope

Three edits inside `## Termination`, and nothing outside it.

- **Rule 3** gains one sentence: under the **audit** focus the eight-ticket cap orders repair by
  **the rubric's own order, top to bottom** — a deterministic tiebreak with no judgment in it — and
  a finding the cap defers **stays an unmet criterion and is counted again next round**, because
  filing does not reduce `A(N)`, only repair does (`ADR-0096` §5).
- **Rule 4** gains one sentence: under the **audit** focus the quantity compared is `A(N)`, the
  number of criteria answered `not met`, so the rule reads `A(N) >= A(N-1)` and `A(N)` can never
  exceed the rubric's size — a ceiling known before the round starts. Round 1 has no `A(0)`,
  exactly as it has no `B(0)`. Write `A(N-1)` with an **ASCII hyphen**, matching the rule's
  existing `B(N-1)`.
- **The exit-state table's `PASS` row** gains its audit condition: `a round's report has zero
  blocker and zero high — or, under the audit focus, `A(N) = 0``. Write `A(N) = 0` with single
  spaces around the `=`; gate 2 reads that spelling.

## Out of scope

- **Rules 1, 2, 5 and 6.** Rule 2 was already scoped to `qa` and `uat` by `ADR-0096` §5 when that
  ADR merged, and its text is byte-correct today — gate 3 pins it. Rules 1, 5 and 6 are listed by
  `ADR-0096` as **applied unchanged**; a focus word added to any of them would be an invention.
- **A baseline-round exemption under this focus.** `ADR-0096` §5 lists the audit's termination
  rules and includes none, and rule 4's existing exemption is defined over a screen becoming
  conformance-judgeable through a merged card, which no audit round measures. `DEC-093` asks a
  different question about that rule and is untouched.
- **Any other section of the epic** — `## Goal`, `## Scope`, `## Out of scope`, `## Open
  decisions`, the answered-decisions table, `## Stories`, `## Definition of done` and `## Metrics`
  all stay as they are. §Metrics already carries `ADR-0096` §6's *criteria added per invocation*
  row, and §Open decisions carries `DEC-098`, which this ticket neither answers nor closes.
- **Any change to `.claude/agents/qa-manager.md`.** `TASK-121206` owns it; gate 4 only reads it.
- **Ticking any Definition-of-done box.** None of them asks for this.

## Tests

No test class — the deliverable is prose in a ticket file, so the gates are structural checks over
that text, **scoped to the section** so that a string already present elsewhere in the epic cannot
satisfy them. Every row was run on 2026-08-31 at commit `f8383c4e`.

| # | Gate | Proves | Today | With the edit |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | `awk` between `## Termination` and `## Definition of done`, over four literals | `A(N-1)`, the rubric, *top to bottom* and `A(N) = 0` are inside **§Termination itself** | **1** | 0 |
| 3 | `awk` over four surrounding anchors, same window | `B(N) >= B(N-1)`, `STOP_DIVERGING`, the baseline exemption and rule 2's `qa`/`uat` scoping all survive | 0 — a guard | 0 |
| 4 | `awk` over `qa-manager.md` | the manager's copy of the same arithmetic is there, so the two documents are edited in the right order | **1** today; **0** once `TASK-121206` merges | 0 |

**Gate 2's window is the whole point of the gate.** `A(N)`, `rubric` and `not met` all appear in
this file already — in the `DEC-096` and `DEC-097` rows of the answered-decisions table and in the
§Metrics row — so an unscoped `grep` would be a **tautology**, green before the edit and green
after it. The awk brackets the section between its two headings, and measured today the same four
literals inside that window give **1** while `A(N)` alone gives **0**, because rule 2's scoping
sentence already names it.

**Gate 4 is an ordering check, not a content check.** It is red until `TASK-121206` merges, which
is what makes the dependency real rather than declared. It cannot see that the two copies *agree* —
only that both exist. Agreement is the reviewer's, against `ADR-0096` §5, and `STORY-1208` is what
it costs when nobody checks.

## Acceptance criteria

- [ ] `EPIC-12` §Termination contains `A(N-1)`, `rubric`, `top to bottom` and `A(N) = 0` **inside
      the section** (gate 2).
- [ ] `B(N) >= B(N-1)`, `STOP_DIVERGING`, the baseline exemption and rule 2's `qa`/`uat` scoping
      all survive inside the section (gate 3).
- [ ] `.claude/agents/qa-manager.md` still carries `## The audit arithmetic` and `top to bottom`
      (gate 4).
- [ ] The diff touches exactly one file besides this ticket's own status and its `BOARD.md` cell.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
