---
schema: 2
id: TASK-121206
title: qa-manager — the audit arithmetic, A(N) and no severity
type: task
status: done
parent: STORY-1212
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, audit, meta]
depends_on: [TASK-121205]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk 'index($0,"## The audit arithmetic"){a=1} index($0,"A(N-1)"){b=1} index($0,"top to bottom"){c=1} index($0,"STOP_DIVERGING"){d=1} index($0,"A(N):"){e=1} END{exit (a&&b&&c&&d&&e)?0:1}' .claude/agents/qa-manager.md
  - awk 'index($0,"B(N) >= B(N-1)"){a=1} index($0,"## The UAT arithmetic"){b=1} index($0,"## Step 6"){c=1} END{exit (a&&b&&c)?0:1}' .claude/agents/qa-manager.md
  - awk 'index($0,"## The audit focus"){a=1} index($0,"rather than filing a second"){b=1} END{exit (a&&b)?0:1}' .claude/agents/qa-manager.md
  - awk 'index($0,"qa-cycle"){bad=1} END{exit bad?1:0}' .claude/agents/qa-manager.md
  - grep -rl "qa-cycle" .claude/skills .claude/agents | grep -Ev '^\.claude/(agents/(qa|uat|audit)\.md|skills/qa-(cycle|cases)/SKILL\.md)$' | awk 'END{exit (NR==0)?0:1}'
  - shasum -a 256 .claude/agents/qa.md | awk '{exit ($1=="eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42")?0:1}'
  - shasum -a 256 .claude/agents/uat.md | awk '{exit ($1=="d6c1cd3f619356ca3f0a9f4af4ad9441854818c1d67913efd271c5d378664cce")?0:1}'
---

## Goal

`qa-manager` can end an audit round: it computes `A(N)` as the number of criteria answered
`not met`, knows that there is no severity and no backlog under this focus, orders repair by the
rubric's own order, and emits a verdict from `A(N)` rather than `B(N)`.

## This is the second half of the section `TASK-121205` started

`TASK-121205` landed `## The audit focus` — the classifier, the proposed criteria, the frozen
rubric and *promote, do not duplicate*. This ticket lands the arithmetic, the verdict rows and the
report line, the same seam `TASK-120708`/`TASK-120709` used one focus earlier.

## Files

| File | Action |
| --- | --- |
| `.claude/agents/qa-manager.md` | modify |

You may **read** `docs/adr/ADR-0096-…` §5, `tasks/epics/EPIC-12-quality-and-defect-repair.md`
§Termination, and `.claude/agents/audit.md` — which you may **not** modify.

## Scope

One new section and two small edits.

- **`## The audit arithmetic`**, placed immediately after `## The audit focus`:
  - **`A(N)` is the number of criteria answered `not met` in round `N`** — not observations. A
    criterion failing at six beats is **one** unmet criterion whose ticket names all six, so
    `A(N)` can never exceed the rubric's size: **five today**, a ceiling known before the round
    starts. *"Each time report more and more bugs"* is not a shape this quantity can take.
  - **A round ends when every criterion has been answered at every beat.** The auditor has no
    discretion to keep looking.
  - **No severity, and no backlog.** `EPIC-12` §Termination rule 2 is scoped to `qa` and `uat`
    (`ADR-0096` §5) and stays byte-unchanged there. Under this focus a finding deferred by rule 3's
    eight-ticket cap **stays an unmet criterion and is counted again in the next round** — filing
    does not reduce `A(N)`, only repair does — and the cap orders repair by the rubric's own order,
    **top to bottom**, a deterministic tiebreak with no judgment in it.
  - **An audit round reports `A(N)` and no `B(N)`.** A functional defect the round stumbles on
    is filed to the one ledger and enters the next `qa` round's `B(N)`, never the audit's count —
    the same reason `ADR-0089` §4 and `ADR-0092` §5 keep three other classes out of `B(N)`: each
    count must measure one thing.
  - **Round 1 has no `A(0)` to compare against**, exactly as round 1 has no `B(0)`. State it, so
    nobody reads the missing comparison as an exemption somebody granted.
- **Step 6 gains its audit rows.** Under the audit focus the verdict is computed from `A(N)`:
  `PASS` at `A(N) == 0`; `PROCEED` at `A(N) > 0`, `N < 3` and `A(N) < A(N-1)` unless `N == 1`;
  `STOP_DIVERGING` at `A(N) >= A(N-1)` with `N > 1`; `STOP_BUDGET` at `N == 3`; `STOP_BLOCKED`
  unchanged. Say what `PASS` means and what it does not: every criterion in the frozen rubric was
  met, at every beat, at one commit, on one machine, at the two shapes `ADR-0096` §4 names — and
  **never** that the product is finished (`ADR-0089` §2c, `ADR-0093` §2). `STOP_DIVERGING` and
  `STOP_BUDGET` both say *the product is still raw and here is the list of how*.
- **`## Report` gains one line for this focus**: `A(N): <count>   A(N-1): <count or n/a>` in place
  of the `B(N):` line, no `BASELINE:` line, and `PROPOSED CRITERIA:` naming what was routed and to
  whom. Write `A(N-1)` with an **ASCII hyphen**, matching the file's existing `B(N-1)`; gate 2
  reads that spelling.

## Out of scope

- **Any change to `## The UAT arithmetic`, the three `B(N)` exclusions, the baseline rule or the
  qualified verdict.** They govern `qa` and `uat` and nothing here touches them. Gate 3 pins three
  anchors.
- **A baseline-round exemption under the audit focus.** `ADR-0096` §5 lists this focus's
  termination rules and includes none, and `ADR-0092` §6's rule is defined over a screen becoming
  conformance-judgeable through a merged card — which no audit round measures. `DEC-093`, which
  asks whether that rule extends to other instruments, is a different question and is untouched.
- **Any severity for the audit focus, and any change to Step 3's severity table.** `ADR-0096` §5:
  a criterion is `met` or `not met`, and Step 3 keeps governing the other two focuses word for
  word.
- **Changing `EPIC-12` §Termination.** Its copy of these rules is
  [`TASK-121207`](TASK-121207-termination-counts-criteria-under-the-audit-focus.md), one ticket
  later, and `STORY-1208` is the precedent for landing the two copies as two diffs.
- **Naming `qa-cycle` anywhere in this file.** Gate 5 refuses the string.
- **Transcribing `ADR-0096` §2's criteria, or naming any of the five.** The arithmetic counts them;
  it does not need to know what they say.
- **Any change to `.claude/agents/qa.md`, `.claude/agents/uat.md`, `.claude/agents/audit.md` or
  `.claude/skills/qa-cycle/SKILL.md`.** Gates 7 and 8 pin two of them.

## Tests

No test class — the deliverable is one prose document, so the gates are structural checks over that
text. Every row was run on 2026-08-31 at commit `f8383c4e`. **Two of the eight are red today —
gates 2 and 4 — and all eight are green with the section.** Gate 4 turns green when `TASK-121205`
merges, which is what makes this ticket's dependency real rather than declared; gate 2 stays red
until this ticket lands.

| # | Gate | Proves | Today | With the section |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | `awk` over five literals | the section exists, the convergence comparison is spelled `A(N-1)`, repair is ordered **top to bottom**, and the report block carries `A(N):` | **1** | 0 |
| 3 | `awk` over three surrounding anchors | `B(N) >= B(N-1)`, `## The UAT arithmetic` and `## Step 6` all survive — the audit arithmetic is **added**, never written over the UAT one | 0 — a guard | 0 |
| 4 | `awk` over two `TASK-121205` literals | the classifier half is still there, so this ticket did not replace it | **1** today; **0** once `TASK-121205` merges | 0 |
| 5 | `awk` over `qa-cycle` | the manager still names the cycle **nowhere** | 0 — a guard | 0 |
| 6 | `ADR-0097` §4's own command | no sixth file names the cycle | 0 — a guard | 0 |
| 7 | `sha256` of `qa.md` | byte-identical to `f8383c4e` | 0 — a guard | 0 |
| 8 | `sha256` of `uat.md` | byte-identical to `f8383c4e` | 0 — a guard | 0 |

**Gate 2 pins a spelling on purpose.** `A(N-1)` with an ASCII hyphen is what the file's existing
`B(N-1)` uses, and `ADR-0096` §5 writes the same quantity with a Unicode minus. A gate that matched
either would pass on a file that mixed both, and a manager whose two arithmetics are spelled
differently is one a reader mistrusts. The ticket says which spelling to use rather than leaving
the coder to lose a round to it.

**Gate 3 is the load-bearing guard.** The worst outcome here is a coder that converts the existing
`B(N)` verdict table into a focus-switched one instead of adding rows — which would put the two
counts in one table and make `ADR-0096` §5's *"each count must measure one thing"* unreadable. Gate
3 fails the moment `B(N) >= B(N-1)` leaves the file. What it cannot see is a table that keeps the
row and changes its meaning; that half is the reviewer's.

**What no gate here sees** is whether `A(N)` is ever computed correctly, because that needs a
round. The first audit round's story is where it shows, and `ADR-0089` §2c forbids citing that
round as anything more.

## Acceptance criteria

- [ ] `.claude/agents/qa-manager.md` contains `## The audit arithmetic`, `A(N-1)`, `top to bottom`,
      `STOP_DIVERGING` and `A(N):` (gate 2).
- [ ] `B(N) >= B(N-1)`, `## The UAT arithmetic` and `## Step 6` all survive (gate 3).
- [ ] `## The audit focus` and `rather than filing a second` still stand from `TASK-121205`
      (gate 4).
- [ ] `.claude/agents/qa-manager.md` contains `qa-cycle` nowhere (gate 5).
- [ ] No sixth file under `.claude/` names the cycle (gate 6).
- [ ] `.claude/agents/qa.md` and `.claude/agents/uat.md` are byte-identical to `f8383c4e`
      (gates 7 and 8).
- [ ] The diff touches exactly one file besides this ticket's own status and its `BOARD.md` cell.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
