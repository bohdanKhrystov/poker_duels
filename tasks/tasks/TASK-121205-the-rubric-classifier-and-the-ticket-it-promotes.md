---
schema: 2
id: TASK-121205
title: qa-manager — the rubric classifier and the ticket it promotes
type: task
status: done
parent: STORY-1212
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, audit, meta]
depends_on: [TASK-121204]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk 'index($0,"## The audit focus"){a=1} index($0,"PER-CRITERION"){b=1} index($0,"rather than filing a second"){c=1} index($0,"PROPOSED CRITERIA"){d=1} END{exit (a&&b&&c&&d)?0:1}' .claude/agents/qa-manager.md
  - awk 'index($0,"## The UAT focus"){a=1} index($0,"## The UAT arithmetic"){b=1} index($0,"at most three questions per round"){c=1} END{exit (a&&b&&c)?0:1}' .claude/agents/qa-manager.md
  - awk 'index($0,"qa-cycle"){bad=1} END{exit bad?1:0}' .claude/agents/qa-manager.md
  - grep -rl "qa-cycle" .claude/skills .claude/agents | grep -Ev '^\.claude/(agents/(qa|uat|audit)\.md|skills/qa-(cycle|cases)/SKILL\.md)$' | awk 'END{exit (NR==0)?0:1}'
  - shasum -a 256 .claude/agents/qa.md | awk '{exit ($1=="eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42")?0:1}'
  - shasum -a 256 .claude/agents/uat.md | awk '{exit ($1=="d6c1cd3f619356ca3f0a9f4af4ad9441854818c1d67913efd271c5d378664cce")?0:1}'
---

## Goal

`.claude/agents/qa-manager.md` triages an `audit` report: it knows that under this focus a finding
contradicts a **criterion** and needs no other merged source, where a proposed criterion goes, and
that an unmet criterion whose repair is already a `status: backlog` ticket is **promoted, never
re-filed**.

## This ticket writes half a section, deliberately

`qa-manager.md` gained the UAT focus in two tickets — `TASK-120708` for the classifier and the
promotion gate, `TASK-120709` for the arithmetic — and the audit focus lands on the same seam.
**This one** is `## The audit focus`;
[`TASK-121206`](TASK-121206-the-audit-arithmetic-a-of-n-and-no-severity.md) is
`## The audit arithmetic`, Step 6's verdict rows and the report block.

The intermediate state is safe for the reason it was safe there: an unfinished manager section
changes no behaviour until a human types `/qa-cycle audit`, and nothing in this repository does
that for them (`ADR-0089` §2b).

## Files

| File | Action |
| --- | --- |
| `.claude/agents/qa-manager.md` | modify |

You may **read** `.claude/agents/audit.md` — the observer whose report this parses, and which you
may **not** modify — and `docs/adr/ADR-0096-…` §§2, 3 and 5.

## Scope

One new section, `## The audit focus`, placed after `## The UAT arithmetic` and before
`## Step 1 — Dedupe before anything else`, so the two focuses read as siblings.

- **What arrives.** An `audit` report — `PER-CRITERION:`, `PROPOSED CRITERIA:`, `FUNCTIONAL:`,
  `BLOCKED:`, the shape `.claude/agents/audit.md` fixes — reads onto the same round ledger and
  round number as a `qa` or `uat` report. Steps 1 to 6 all still apply; this section says what
  changes inside them.
- **The classifier, relocated rather than replaced.** Under this focus an observation is a finding
  when it contradicts a **criterion in `ADR-0096` §2's frozen rubric**, and it needs **no other
  merged source** — no card, no token, no owned literal. Say in one sentence why the merged-source
  principle still holds: the rubric is merged, closed and **general**, and what was missing was
  never the rule but a merged source of the general kind. Then say the other half out loud:
  **`ADR-0092` §3 stands byte-unchanged for the `qa` and `uat` focuses**, where the same
  observation is a question capped at three — one file now holds two readings, one per focus, and
  a reader must not carry either across.
- **Dedupe spans three focuses now.** One ledger is load-bearing; a defect seen under two focuses
  is one ticket. Extend the existing sentence rather than writing a second one.
- **The proposed criteria.** At most **three per round**, each a general standard rather than an
  observation. They are **recorded in the round story** and routed exactly as `ADR-0092` §5 routes
  a question: a `DEC` for the **product owner** where `docs/vision.md` settles it, the **human**
  where it does not, and a merged PR either way. You never add one to the rubric yourself.
- **The rubric is frozen for the invocation** (`ADR-0096` §3). No round may add a criterion to
  itself or to a later round of the same invocation; a criterion merged mid-invocation applies to
  the **next** invocation. This is §Termination rule 1's frozen set one level up, and it is what
  makes `A(N)` and `A(N-1)` comparable at all.
- **The audit promotes, it does not duplicate** (`ADR-0096` §5). Where an unmet criterion's repair
  is already a `status: backlog` ticket from a `qa` or `uat` round, **move that ticket into the
  audit round's fix set rather than filing a second** — one ledger, `ADR-0092` §6. Use the words
  *rather than filing a second* so gate 2 can see the rule. Say that seventeen such tickets existed
  when `ADR-0096` was written, and that this is the mechanism by which a criterion reaches them.
- **A `not met` with no quoted observation is not yet an answer.** Send it back or record it as
  `BLOCKED`; never invent the evidence. And an unmet criterion a looking human cannot reproduce is
  a **harness** defect under `ADR-0089` §4 — filed against `EPIC-12`, repaired in `scripts/qa/`,
  excluded from every count, and never repaired in production code.

## Out of scope

- **`A(N)`, the verdict table, the exit states and the report block.** That is `TASK-121206`, and
  splitting there is what keeps this ticket inside `S`.
- **Any change to `## The UAT focus`, `## The UAT arithmetic` or the promotion gate's three-question
  cap.** `ADR-0096` §2 freezes `ADR-0092` §3 for that focus; gate 3 pins all three anchors.
- **Any severity for the audit focus.** `ADR-0096` §5: there is none. The existing severity table
  in Step 3 stays exactly as it is and keeps governing `qa` and `uat` word for word.
- **Answering `DEC-088`.** Its ordering question is the *UAT* promotion gate's three slots. The
  audit's proposed criteria are a different cap with a different bar, and `ADR-0096` §5 says the
  two gates are untouched by each other.
- **Naming `qa-cycle` anywhere in this file.** `ADR-0090` §2 as amended by `ADR-0092` §2 and
  `ADR-0097` §4 declares five files and the manager is in none of them. Gate 4 refuses the string,
  and it is this ticket's most likely accident.
- **Transcribing `ADR-0096` §2's criteria.** Cite the section. A manager that carried its own copy
  of the rubric would be the second register `STORY-1212` §*The rubric is cited, never transcribed*
  refuses.
- **Any change to `.claude/agents/qa.md`, `.claude/agents/uat.md`, `.claude/agents/audit.md` or
  `.claude/skills/qa-cycle/SKILL.md`.** Gates 6 and 7 pin two of them.

## Tests

No test class — the deliverable is one prose document, so the gates are structural checks over that
text. Every row was run on 2026-08-31 at commit `f8383c4e`. **One of the seven is red today — gate
2 — and all seven are green with the section.** Five are guards and were 0.

| # | Gate | Proves | Today | With the section |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | `awk` over four literals | the section exists, names the two report fields it parses, and carries the **promote, do not duplicate** rule in the words the ADR uses | **1** | 0 |
| 3 | `awk` over three UAT anchors | `## The UAT focus`, `## The UAT arithmetic` and the three-question cap all survive — the audit focus is **added**, never written over the UAT one | 0 — a guard | 0 |
| 4 | `awk` over `qa-cycle` | the manager still names the cycle **nowhere** | 0 — a guard | 0 |
| 5 | `ADR-0097` §4's own command | no sixth file names the cycle | 0 — a guard | 0 |
| 6 | `sha256` of `qa.md` | byte-identical to `f8383c4e` | 0 — a guard | 0 |
| 7 | `sha256` of `uat.md` | byte-identical to `f8383c4e` | 0 — a guard | 0 |

**Gate 3 is the load-bearing guard and it is the reason this ticket is not risky.** The single
worst outcome here is a coder that rewrites the UAT classifier to be *"the classifier, switched by
a scope word"* — which is the exact arrangement `ADR-0092` §8 built two observer files to prevent
and `ADR-0097` §4 spent a declared-file slot to avoid. Gate 3 fails the moment any of the three
anchors is edited away. What it cannot see is a *reworded* UAT paragraph that keeps its heading;
that half is the reviewer's, against `ADR-0096` §2's *byte-unchanged for `qa` and `uat`*.

**Gate 4 was measured both ways.** It exits **0** on `qa-manager.md` today and **1** on
`.claude/agents/uat.md`, a file that legitimately names the cycle once — so it is a check about
this file rather than a tautology.

**Gate 2 is a string check and is worth what `TASK-120301` said its kind is worth**: it puts four
phrases into the file in words a later editor has to delete. It cannot see that the manager
actually promotes a backlog ticket instead of filing a second — that is the first audit round's
triage, and the round record is where it will show.

## Acceptance criteria

- [ ] `.claude/agents/qa-manager.md` contains `## The audit focus`, `PER-CRITERION`,
      `PROPOSED CRITERIA` and `rather than filing a second` (gate 2).
- [ ] `## The UAT focus`, `## The UAT arithmetic` and `at most three questions per round` all
      survive (gate 3).
- [ ] `.claude/agents/qa-manager.md` contains `qa-cycle` nowhere (gate 4).
- [ ] No sixth file under `.claude/` names the cycle (gate 5).
- [ ] `.claude/agents/qa.md` and `.claude/agents/uat.md` are byte-identical to `f8383c4e`
      (gates 6 and 7).
- [ ] The diff touches exactly one file besides this ticket's own status and its `BOARD.md` cell.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
