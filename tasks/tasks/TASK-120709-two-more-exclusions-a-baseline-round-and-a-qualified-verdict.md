---
schema: 2
id: TASK-120709
title: qa-manager — two more exclusions, a baseline round and a qualified verdict
type: task
status: done
parent: STORY-1207
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, uat, meta]
depends_on: [TASK-120708]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk '/^## /{s=0} /^## The UAT arithmetic$/{s=1} s{l=tolower($0)} s && index(l,"harness"){a=1} s && index(l,"missing card"){b=1} s && index(l,"decision-born"){c=1} s && index(l,"baseline round"){d=1} s && index(l,"pass (conformance unjudged on"){e=1} END{exit (a&&b&&c&&d&&e)?0:1}' .claude/agents/qa-manager.md
  - awk '/^## /{s=0} /^## Step 6/{s=1} s && index($0,"gates a member of the current fix set"){f=1} END{exit f?0:1}' .claude/agents/qa-manager.md
  - awk '/^## /{s=0} /^## Step 1/{s=1} s{l=tolower($0)} s && index(l,"focus"){f=1} END{exit f?0:1}' .claude/agents/qa-manager.md
  - awk '/^## /{s=0} /^## The UAT focus$/{s=1} s{l=tolower($0)} s && index(l,"contradicts something merged"){a=1} s && index(l,"at most three"){b=1} END{exit (a&&b)?0:1}' .claude/agents/qa-manager.md
  - awk 'index($0,"qa-cycle"){f=1} END{exit f?1:0}' .claude/agents/qa-manager.md
  - grep -rl "qa-cycle" .claude/skills .claude/agents | grep -Ev '^\.claude/(agents/(qa|uat)\.md|skills/qa-(cycle|cases)/SKILL\.md)$' | awk 'END{exit (NR==0)?0:1}'
  - shasum -a 256 .claude/agents/qa.md | awk '{exit ($1=="eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42")?0:1}'
  - awk 'END{exit (NR<=230)?0:1}' .claude/agents/qa-manager.md
---

## Goal

`qa-manager` computes `B(N)` with all three exclusions, knows when a round is a baseline round,
qualifies a UAT verdict inline, and ends a cycle on a human-only escalation only when that
escalation gates the current fix set.

## The specification is `ADR-0092` §§5, 6, and it is binding

`B(N)` measures the product or it measures nothing (`ADR-0089` §4). Every clause below exists
because counting the wrong thing flips a verdict on a healthy or improving product.

## Files

| File | Action |
| --- | --- |
| `.claude/agents/qa-manager.md` | modify |

You may **read** `docs/adr/ADR-0092-…` §§5, 6, `docs/adr/ADR-0089-…` §4, and
`tasks/epics/EPIC-12-quality-and-defect-repair.md` §Termination.

## Scope

About 40 lines, in three places.

### 1. A new section headed **exactly** `## The UAT arithmetic`

Immediately after `## The UAT focus`, which `TASK-120708` added. Gate 2 is scoped to that heading.

- **`B(N)` counts product defects alone. Three exclusions, and every one of them is prose:**
  1. **Harness defects** — a failing case that does not reproduce by hand (`ADR-0089` §4, already
     in this file at step 2). Write the word `harness` here too, in the list, so the three read as
     one list rather than as one rule and two footnotes.
  2. **Missing cards** — registered `ADR-0091` §5 debt being collected, not the product decaying.
     Counting six of them in `B(1)` would set round 2 the bar of *beat six* over a queue of design
     authoring: rule 4 governing the wrong quantity.
  3. **Decision-born tickets** — a ticket produced by a `product-owner` answer. At the moment it
     was observed, the shipped state contradicted **no** merged source; it is the product being
     asked to improve, not found broken. Counting it would make a round that asked good questions
     read as a product getting worse and trip `STOP_DIVERGING` on a product that is improving.
     Write the phrase `decision-born`, which gate 2 matches.
- **A manager that forgets one exclusion flips a verdict.** Say that, and say that each exclusion
  is stated with its reason in the round story.
- **The baseline rule.** When a screen becomes conformance-judgeable for the first time in round
  *N* — its card merged in round *N−1*'s repairs — round *N* is a **baseline round**: rule 4 does
  **not** compare its `B(N)` against `B(N−1)`, exactly as round 1 is not compared with a round 0
  that does not exist, because the two rounds measured differently-sized judgeable sets and the
  comparison would score the unlock as decay. **Rule 5's three-round budget binds regardless.**
- **The qualified verdict.** A UAT round record carries the per-screen table — checks
  **a**/**b**/**c**, each `judged`, `BLOCKED — no card`, or `out of scope` — and a verdict over
  any `BLOCKED` cell carries the qualification **inline, in the verdict line itself**:
  `PASS (conformance unjudged on 6 of 7 screens)`. Write that example literally; gate 2 matches
  `pass (conformance unjudged on`. The terminal report repeats the line **verbatim**.

### 2. One paragraph inside `## Step 1 — Dedupe before anything else`

**Dedupe spans the two focuses.** One ledger is load-bearing: a UAT walk stumbles on functional
defects too — it does not hunt them — and a defect found under both focuses must be **one**
ticket, or `B(N)` double-counts it. Match on behaviour, not on which focus saw it, and not on
wording. Gate 4 matches the word `focus` inside this step.

### 3. One amended row in `## Step 6`'s verdict table

`STOP_BLOCKED` currently reads *"a decision is needed that only the human can answer"*. It becomes:
a human-only escalation ends the cycle **only when the unanswered decision gates a member of the
current fix set** — write that literal, which gate 3 matches. Otherwise
`notify.py blocked --decision DEC-NNN` goes out while the run is warm, the question is carried
open in the round story, the terminal report restates it, and the cycle **continues**, because no
step of the loop waits on a UX answer. Read literally, the old row would end three budgeted rounds
over one aesthetic escalation.

## Out of scope

- **Writing the string `qa-cycle` anywhere in this file.** `ADR-0090` §2 as amended by `ADR-0092`
  §2: the declared set is four and this file is not in it. Gates 6 and 7 both go red.
- **Adding a sixth exit state, a fourth severity, or a fourth `B(N)` exclusion.** `ADR-0092` named
  three; a fourth is a new `DEC`.
- **Loosening rule 2, 3 or 5.** Only `blocker` and `high` are repaired in-cycle; the fix set is at
  most eight tickets; at most three rounds per invocation. `ADR-0092` §6 says the machinery
  *"carries over unchanged"* apart from the two additions above.
- **Changing what `STOP_DIVERGING` does when it fires.** Only the `STOP_BLOCKED` row is scoped.
- **The classifier and the promotion gate.** `TASK-120708` wrote them; gate 5 only checks they
  survived this edit.
- **Any change to `.claude/agents/qa.md`** (gate 8 pins its `sha256`), `uat.md` or either skill.
- **Computing a verdict from a round that has not been handed to you**, or handing round *N*'s
  report to a second triage. The frozen set is rule 1 and this ticket does not touch it.

## Tests

No test class — one prose document. Every row was run on 2026-08-30 at commit `cfcc6a4e`.

| # | Gate | Proves | Today | After |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | `awk` scoped to `## The UAT arithmetic` | all three exclusions, the baseline round and the inline-qualified verdict example are written **inside that section** | **1** | 0 |
| 3 | `awk` scoped to `## Step 6` | `STOP_BLOCKED` is scoped to *gates a member of the current fix set* | **1** | 0 |
| 4 | `awk` scoped to `## Step 1` | dedupe knows there are two focuses | **1** | 0 |
| 5 | `awk` scoped to `## The UAT focus` | `TASK-120708`'s classifier and budget survived this edit | **1** until it merges | 0 |
| 6 | `awk` over `qa-cycle` in this file | the manager still names the cycle nowhere | 0 — a **guard** | 0 |
| 7 | `ADR-0092` §2's own command | no undeclared file names the cycle | 0 — a guard | 0 |
| 8 | `sha256` of `qa.md` | byte-unchanged | 0 — a guard | 0 |
| 9 | `awk 'END{exit (NR<=230)?0:1}'` | the file did not run away — 148 lines before `TASK-120708` | 0 | 0 |

**Gates 3 and 4 are section-scoped, and that scoping is the only thing making them non-vacuous.**
`gates a member of the current fix set` written into a preamble instead of the verdict table would
leave the table saying the old thing while the gate went green; keying it to `## Step 6` is what
stops that. The same for gate 4 and step 1. **Measured**: all four of gates 2–5 exit 1 on today's
file.

**What no gate here can see** is whether a triage actually applies an exclusion. Three prose rules
inside a count is exactly the cost `ADR-0092` §Consequences accepted out loud — *"the count is
more honest about the product and harder to compute mechanically; a manager that forgets one
exclusion flips a verdict"* — and it accepted it because every alternative miscounts something
real: rot as decay, debt as decay, or improvement as decay.

**So the review is to read `## The UAT arithmetic` against `ADR-0092` §§5 and 6** and reject any
sentence that puts a harness defect, a missing card or a decision-born ticket back into `B(N)`,
that compares a baseline round against its predecessor, or that lets a verdict line omit its
qualification.

## Acceptance criteria

- [ ] `.claude/agents/qa-manager.md` has a section headed exactly `## The UAT arithmetic` naming
      all three `B(N)` exclusions — harness, missing card, decision-born (gate 2).
- [ ] That section states the baseline rule, that rule 5's budget binds regardless, and the
      inline-qualified verdict `PASS (conformance unjudged on 6 of 7 screens)` (gate 2).
- [ ] `## Step 6`'s `STOP_BLOCKED` row says the escalation must **gate a member of the current fix
      set** (gate 3).
- [ ] `## Step 1` says dedupe spans both focuses and that a defect found by both is one ticket
      (gate 4).
- [ ] `## The UAT focus` still carries the classifier and the three-question budget (gate 5).
- [ ] The file contains the string `qa-cycle` **nowhere** (gates 6 and 7).
- [ ] `.claude/agents/qa.md` is unchanged (gate 8), and `qa-manager.md` is 230 lines or fewer
      (gate 9).
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
