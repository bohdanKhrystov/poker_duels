---
schema: 2
id: TASK-121203
title: What audit answers, and the three criteria it may propose
type: task
status: ready
parent: STORY-1212
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, audit, meta]
depends_on: [TASK-121202]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk 'NR<=12 && NR==1 && $0 ~ /^-+$/{a=1} NR<=12 && $1=="name:" && $2=="audit"{b=1} NR<=12 && $1=="tools:"{e=1; if (index($0,"Write") || index($0,"Edit")) d=1} END{exit (a&&b&&e&&!d)?0:1}' .claude/agents/audit.md
  - awk 'index($0,"PER-CRITERION:"){a=1} index($0,"PROPOSED CRITERIA:"){b=1} index($0,"FUNCTIONAL:"){c=1} index($0,"BLOCKED:"){d=1} END{exit (a&&b&&c&&d)?0:1}' .claude/agents/audit.md
  - awk 'index($0,"R1"){a=1} index($0,"R2"){b=1} index($0,"R3"){c=1} index($0,"R4"){d=1} index($0,"R5"){e=1} END{exit (a&&b&&c&&d&&e)?0:1}' .claude/agents/audit.md
  - awk 'index($0,"not met"){a=1} index($0,"every shape it was answered at"){b=1} index($0,"three per round"){c=1} END{exit (a&&b&&c)?0:1}' .claude/agents/audit.md
  - awk 'index($0,"blocker"){bad=1} index($0,"medium"){bad=1} END{exit bad?1:0}' .claude/agents/audit.md
  - awk 'END{exit (NR<=155)?0:1}' .claude/agents/audit.md
  - grep -rl "qa-cycle" .claude/skills .claude/agents | grep -Ev '^\.claude/(agents/(qa|uat|audit)\.md|skills/qa-(cycle|cases)/SKILL\.md)$' | awk 'END{exit (NR==0)?0:1}'
  - shasum -a 256 .claude/agents/uat.md | awk '{exit ($1=="d6c1cd3f619356ca3f0a9f4af4ad9441854818c1d67913efd271c5d378664cce")?0:1}'
---

## Goal

`.claude/agents/audit.md` is finished: it says which closed list the observer answers, what a
`not met` must carry, that a criterion is `met` only at every shape it was answered at, that an
observation which is not an answer is a **proposed criterion** and never a finding, and it fixes
the report block `qa-manager` parses.

## This is the second half of the file `TASK-121202` started

`TASK-121202` landed the frontmatter, the walk, the stack sentence, the hands and the two shapes.
This ticket adds everything from the rubric onwards. **The intermediate state was inert** — nothing
dispatches `audit` until `TASK-121204` adds the focus to `qa-cycle`'s `SKILL.md`, one ticket later.

## Files

| File | Action |
| --- | --- |
| `.claude/agents/audit.md` | modify |

You may **read** `docs/adr/ADR-0096-…` §§2, 3 and 5, and `.claude/agents/uat.md` — the sibling
whose report shape this follows, and which you may **not** modify.

## Scope

**155 lines at most for the whole file**, gated. A 143-line draft carrying every clause of both
halves was written and measured on 2026-08-31.

- **`## The list is closed, and it is not yours`** — `ADR-0096` §2's rubric is the whole of what
  may be answered: `R1` to `R5`, in priority order, at every beat. Cite it; **do not transcribe the
  criteria**. Then five rules:
  - a criterion is **`met` or `not met`**, with nothing in between and no severity;
  - **`not met` carries a quoted observation** — a rendered string, a measured geometry, a recorded
    frame list — never *"this feels wrong"*;
  - a criterion failing at six beats is **one** unmet criterion whose entry names all six —
    criteria are counted, observations are not;
  - **one bar, checked more than once, never two bars**: `R2` and `R3` are answered at both shapes
    and a criterion is `met` only if it is met at **every shape it was answered at**; nothing
    defines a relaxed phone bar and no round may invent one;
  - **a finding needs no other merged source** — under this focus the criterion *is* the merged
    source (`ADR-0096` §2), and hunting for a card, a token or an owned literal is the `uat`
    classifier, which is not this agent's.
- **`## The three you may propose, and nothing else`** — an observation that is not an answer to a
  criterion is **not a finding**; it is a **proposed criterion**, a general standard the rubric
  does not yet have, at most **three per round**, one sentence each. The agent proposes and never
  adds; a criterion merged mid-invocation applies to the **next** invocation (`ADR-0096` §3).
  Then the two things that leave the count:
  - a **functional** defect stumbled on goes under `FUNCTIONAL:` with a reproduction, is not a
    criterion answer, and never enters the audit's count — `qa-manager` routes it to the next `qa`
    round (`ADR-0096` §5);
  - an unmet criterion **a looking human cannot reproduce** is a **harness** defect — a resize that
    silently did not apply, a geometry read taken mid-transition, a frame list read without arming
    `record` — filed against `EPIC-12` and excluded from every count (`ADR-0089` §4).
- **`## Report`** — one fenced block, each field name starting a line, because `qa-manager` parses
  on them. Use exactly these names: `SCOPE:`, `FOCUS: audit`, `STACK:`, `COMMIT:`, `SHAPES:`,
  `PER-CRITERION:`, `PROPOSED CRITERIA:`, `FUNCTIONAL:`, `BLOCKED:`. Each `PER-CRITERION` entry
  carries `CRITERION:`, `VERDICT: met | not met`, `BEATS:`, `SHAPES:` and `OBSERVATION:`, and the
  observation is **required when the verdict is `not met`**.
- Close with the sentence that makes the round terminate: **every criterion appears under
  `PER-CRITERION:` whatever its verdict**, and a round ends when all five have been answered at all
  eight beats, because there is nothing else on the list to look at (`ADR-0096` §5).

## Out of scope

- **Transcribing `ADR-0096` §2's five criteria, or restating what any of them requires.** Cite the
  section. A copy is a second register that rots, and it is the reason `STORY-1212` registers
  `DEC-098` instead of creating one.
- **Adding a sixth criterion, editing one of the five, or ranking them differently.** `ADR-0096`
  §3: the rubric is frozen for the invocation and no round may grow it. §2's order is the repair
  order, and it is the ADR's.
- **Any severity, band or scale.** Gate 6 refuses `blocker` and `medium` anywhere in the file.
- **`B(N)`, a backlog, or a deferral rule.** Those are `qa-manager`'s under `TASK-121206`, and the
  audit has no backlog at all (`ADR-0096` §5).
- **Deciding which questions get promoted, or writing a `DEC`.** The observer proposes; the manager
  promotes and registers. `DEC-088`'s ordering question is the `uat` promotion gate's and is not
  reached by this focus.
- **Any change to the first half's sections** — frontmatter, the walk, the stack sentence, the
  hands, the two shapes. `TASK-121202` merged them and gates 2, 8 and 9 hold them.
- **Any change to `.claude/agents/qa.md` or `.claude/agents/uat.md`**, or to any file outside
  `.claude/agents/audit.md`. Gates 8 and 9 hold two of those from both directions.
- **A sixth file naming `qa-cycle`.** Gate 8 is `ADR-0097` §4's own command.

## Tests

No test class — the deliverable is prose, so the gates are structural checks over that text. Every
row was run on 2026-08-31 at commit `f8383c4e`, in three worlds: the tree as it stands, the 76-line
half-one draft `TASK-121202` lands, and the 143-line full draft. **Three of the nine are red after
`TASK-121202` merges — 3, 4 and 5 — and all nine are green with the file.** Gate 7 is the one that
inverts: it is red today only because the file is missing, and it becomes a **guard** the moment
half one exists.

| # | Gate | Proves | Today | After `TASK-121202` | With the file |
| --- | --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 | 0 |
| 2 | `awk` over the frontmatter | the file is still **dispatchable as `audit`** and still names neither `Write` nor `Edit` | **2** — no such file | 0 | 0 |
| 3 | `awk` over four report field names | the report block carries `PER-CRITERION:`, `PROPOSED CRITERIA:`, `FUNCTIONAL:` and `BLOCKED:` — the names `qa-manager` parses on | **2** | **1** | 0 |
| 4 | `awk` over `R1`…`R5` | all five criterion ids are named, so every one has somewhere to be answered | **2** | **1** — half one names `R1`, `R2`, `R3` and no more | 0 |
| 5 | `awk` over three phrases | `not met`, *every shape it was answered at*, and *three per round* are written down | **2** | **1** | 0 |
| 6 | `awk` over two forbidden literals | **no severity** — `blocker` and `medium` appear nowhere (`ADR-0096` §5) | **2** | 0 | 0 |
| 7 | `awk 'END{exit (NR<=155)?0:1}'` | the whole file fits the budget, and is too short to have transcribed `ADR-0096` §§1–2 | **2** | 0 — a guard until the second half lands | 0 |
| 8 | `ADR-0097` §4's own command | no **sixth** file names the cycle | 0 — a guard | 0 | 0 |
| 9 | `sha256` of `uat.md` | the UAT observer is byte-identical to `f8383c4e` — `ADR-0096` §2 freezes its classifier | 0 — a guard | 0 | 0 |

**Gate 4 is the sharpest progress gate and it is still a string check.** It is red after
`TASK-121202` for a real reason — half one names `R1`, `R2` and `R3` in passing and never `R4` or
`R5` — so it cannot be satisfied by the previous ticket's text. What it cannot see is whether the
agent answers them; no gate can, and `TASK-120301` priced this class honestly: a string gate puts a
sentence into a file in words a later editor has to **delete** rather than merely fail to add.

**Gate 6 is the one worth the most**, for exactly that reason: *no severity* is a refusal, and a
refusal is the one thing a string gate expresses well.

**Gate 7 is doing two jobs and only one of them is checkable.** It bounds the file, which is real;
it is also the structural reason the rubric cannot be transcribed here, which is an argument rather
than a proof — 155 lines is enough room for five criteria if somebody insists. That half is the
reviewer's, against `STORY-1212` §*The rubric is cited, never transcribed*.

## Acceptance criteria

- [ ] `.claude/agents/audit.md` is still dispatchable as `audit` with neither `Write` nor `Edit` in
      its `tools:` (gate 2).
- [ ] The report block carries `PER-CRITERION:`, `PROPOSED CRITERIA:`, `FUNCTIONAL:` and
      `BLOCKED:` (gate 3).
- [ ] The file names all five of `R1`, `R2`, `R3`, `R4` and `R5` (gate 4).
- [ ] The file contains `not met`, `every shape it was answered at` and `three per round`
      (gate 5).
- [ ] The file contains neither `blocker` nor `medium` (gate 6).
- [ ] The file is **155 lines or fewer** (gate 7).
- [ ] Exactly the five declared files name `qa-cycle`, and `uat.md` still has
      `sha256 d6c1cd3f619356ca3f0a9f4af4ad9441854818c1d67913efd271c5d378664cce` (gates 8 and 9).
- [ ] The diff touches exactly one file besides this ticket's own status and its `BOARD.md` cell.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
