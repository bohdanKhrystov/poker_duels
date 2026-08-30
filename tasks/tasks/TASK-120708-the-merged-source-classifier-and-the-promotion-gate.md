---
schema: 2
id: TASK-120708
title: qa-manager — the merged-source classifier and the promotion gate
type: task
status: backlog
parent: STORY-1207
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, uat, meta]
depends_on: [TASK-120707]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk '/^## /{s=0} /^## The UAT focus$/{s=1} s{l=tolower($0)} s && index(l,"contradicts something merged"){a=1} s && index(l,"is a question"){b=1} s && index(l,"at most three"){c=1} s && index(l,"one per screen"){d=1} s && index(l,"product owner"){e=1} END{exit (a&&b&&c&&d&&e)?0:1}' .claude/agents/qa-manager.md
  - awk 'index($0,"earliest subsequent"){f=1} END{exit f?0:1}' .claude/agents/qa-manager.md
  - awk 'index($0,"qa-cycle"){f=1} END{exit f?1:0}' .claude/agents/qa-manager.md
  - grep -rl "qa-cycle" .claude/skills .claude/agents | grep -Ev '^\.claude/(agents/(qa|uat)\.md|skills/qa-(cycle|cases)/SKILL\.md)$' | awk 'END{exit (NR==0)?0:1}'
  - grep -rl "qa-cycle" .claude/skills .claude/agents | awk '{n++; f[$0]=1} END{exit (n==4 && f[".claude/agents/qa.md"] && f[".claude/agents/uat.md"] && f[".claude/skills/qa-cases/SKILL.md"] && f[".claude/skills/qa-cycle/SKILL.md"])?0:1}'
  - shasum -a 256 .claude/agents/qa.md | awk '{exit ($1=="eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42")?0:1}'
  - awk 'END{exit (NR<=230)?0:1}' .claude/agents/qa-manager.md
---

## Goal

`qa-manager` knows what a UAT report is: which observations it may file as findings, which it may
only promote as questions, how many of those it may promote, and to whom.

## The specification is `ADR-0092` §§3, 5, and it is binding

The bar has two halves and **both must hold** before a question is promoted. Nothing here is a
preference; §5 fixed the mechanics precisely so a second round cannot loosen them.

## Files

| File | Action |
| --- | --- |
| `.claude/agents/qa-manager.md` | modify |

You may **read** `docs/adr/ADR-0092-…` §§3, 4, 5, `.claude/agents/uat.md` (the report shape you
are told to parse), and `docs/workflow.md` §*Who answers a DEC*.

## Scope

One new section, headed **exactly** `## The UAT focus`, placed after `## What you are given` and
before `## Step 1 — Dedupe before anything else`. Gate 2 is scoped to that heading, so the string
matters. About 35 lines.

### 1. What you are given, under this focus

A `uat` report in the shape `.claude/agents/uat.md` fixes — `PER-SCREEN:`, `FINDINGS:`,
`QUESTIONS:`, `BLOCKED:` — the same round ledger, and the round number. Steps 1 to 6 all still
apply; this section says what changes inside them.

### 2. The classifier — a finding contradicts a merged source

- **File a finding only when the observation contradicts something merged**: a card under
  `design/screens/`, `design/tokens/tokens.css`, an owned literal, an ADR section, a
  `docs/duel-rules.md` heading, a `docs/vision.md` sentence. The phrase
  `contradicts something merged` is what gate 2 matches.
- **An observation with no merged source to contradict is a question**, never a finding, however
  well argued. Write the phrase `is a question`. This is `ADR-0090` §4 transposed from authoring
  to observing: there, a case whose expectation has no merged source is not written; here, a
  judgment with no merged source is not filed — because step 5 of the loop changes production code
  for whatever gets filed, and an invented expectation is a product claim.
- **The observer never grades its own question as a finding, and you never promote one to a
  finding either.** The route from a question to a ticket runs through a merged ADR, and only
  through one.

### 3. Missing cards — `ADR-0092` §4

- A screen **in scope** with no merged card is a finding, severity `high`; its repair ticket **is
  the card**, composed from the settled vocabulary as an ordinary dispatched ticket (`ADR-0091`
  §3, `module: design`, `review: light`), with the human's visual verdict trailing the merge.
- **The dedupe key is the card's own path.** File no missing-card ticket while `design/screens/`
  holds the slug's card or an open ticket names that path — the one key that keeps `ADR-0091` §5's
  debt in a single register across two filers.
- Card tickets **do** enter the fix set and consume its eight slots, after `blocker`s and after
  the `high`s that count in `B(N)`.
- A screen the catalogue cannot reach at all is **not in scope**, so no missing-card finding is
  filed for it; its cells read `out of scope`. `docs/test-plan.md` §*UAT* marks those rows
  `not walked`.

### 4. The promotion gate — at most three questions per round

- **You are the only promoter.** The `uat` agent asks and answers nothing; the `product-owner`
  answers by deriving from `docs/vision.md` and the merged ADRs.
- At triage you register **at most three** `DEC`s per round, **at most one per screen**, for the
  product owner. Write both phrases — `at most three` and `one per screen` — and `product owner`;
  gate 2 matches all three.
- **Both halves of the bar must hold**: the question names a **concrete choice answerable in one
  sentence** — *"should the pot be the most prominent number on the table screen?"*, never *"does
  this feel right?"* — **and** it bears on a player's ability to tell what is going on or what
  they may do.
- Below the bar, or over the budget: **recorded in the round story unanswered**, not re-recorded
  while the screen is unchanged, and **never a ticket**.
- **An answered question becomes a merged source.** Either the ADR changes what the product should
  show — the shipped state now contradicts a merged source, and you file the ticket at the next
  triage you hold — or it blesses what shipped, closing the question permanently, so a later round
  re-raising it would contradict a merged source and the suppression is mechanical.

### 5. The frozen set survives — `EPIC-12` §Termination rule 1

A `DEC` registered at round *N*'s triage is answered on its own clock, off the cycle's path. A
ticket its answer yields enters the **earliest subsequent** round's triage, or the ordinary
backlog when the cycle has ended. Write the phrase `earliest subsequent`, which gate 3 matches.
**Nothing a mid-cycle answer produces may extend the round that asked.**

## Out of scope

- **Writing the string `qa-cycle` anywhere in this file.** `ADR-0090` §2 declared three files;
  `ADR-0092` §2 grows the set to **four** by adding `agents/uat.md` **mention-only** and changes
  nothing else — *"`.claude/agents/qa-manager.md` names it nowhere today and gains no licence
  to."* Describing the focus invites typing `/qa-cycle uat`; do not. Gates 4 and 5 both go red on
  it. Say *"the UAT focus"*.
- **`B(N)`'s exclusions, the baseline round, the qualified verdict line, `STOP_BLOCKED`'s scoping
  and cross-focus dedupe.** All of that is `TASK-120709`, the next ticket over this same file.
- **Any change to steps 1–6 themselves.** This ticket adds a section; the next one amends step 1
  and step 6.
- **Answering a question, or letting any agent but the `product-owner` answer one.** `ADR-0092`
  §5 and `docs/workflow.md`'s routing table. The human answers only what would change the vision.
- **A fourth severity, a second ledger, a `uat-manager`, or a second copy of the stopping rules.**
  `ADR-0092` §8 refused all of them.
- **Any change to `.claude/agents/qa.md`** (gate 7 pins its `sha256`), `uat.md` or either skill.
- **Registering `DEC-086`, or answering it.** It is open, it is the product owner's, and it blocks
  nothing — no round may be cited as what made the product ready, whatever the bar turns out to be.

## Tests

No test class — one prose document. Every row was run on 2026-08-30 at commit `cfcc6a4e`.

| # | Gate | Proves | Today | After |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | `awk` scoped to `## The UAT focus` | the classifier and both halves of the promotion budget are written **inside that section**, lowercased before matching so `**At most three**` still hits | **1** | 0 |
| 3 | `awk` over `earliest subsequent` | the frozen-set timing is written down | **1** | 0 |
| 4 | `awk` over `qa-cycle` in this file | the manager still names the cycle **nowhere** | 0 — a **guard**, and the one most likely to be tripped by this ticket | 0 |
| 5 | `ADR-0092` §2's own command, verbatim | no undeclared file names the cycle — the same trip, from the other side | 0 — a guard | 0 |
| 6 | four-file set | the declared set is exactly four | **1** until `TASK-120705` merges | 0 |
| 7 | `sha256` of `qa.md` | `ADR-0092` §8's byte-unchanged clause held | 0 — a guard | 0 |
| 8 | `awk 'END{exit (NR<=230)?0:1}'` | the file did not grow past two `S` tickets — 148 today | 0 | 0 |

**Gate 4 is the sharpest thing here and it is a negative.** It exits 0 today because
`qa-manager.md` contains the string zero times — measured — and goes red the instant a coder
writes `/qa-cycle uat` while explaining the focus. That is the realistic accident this ticket
carries, and it is mechanically caught from both directions (gates 4 and 5).

**Gate 2 is a section-scoped string check and cannot see obedience.** It proves the classifier and
the budget are written, in a section a later editor must **delete** rather than merely fail to
add. It cannot see a triage that promotes four questions, and no gate can — `ADR-0092`
§Consequences prices this out loud as *"the fourth judgment-not-exit-code rule in this
structure… weaker than a gate"*, with the manager's written-reason discipline and the ordinary
review on every ticket as the only mitigations.

**So the review is to read the finished section against `ADR-0092` §§3 and 5** and reject any
sentence that lets a judgment with no merged source become a ticket, that promotes more than three
questions or more than one per screen, or that lets a mid-cycle answer extend the round that asked.

## Acceptance criteria

- [ ] `.claude/agents/qa-manager.md` has a section headed exactly `## The UAT focus` (gate 2).
- [ ] That section states that a finding must contradict something merged and that anything else
      **is a question** (gate 2).
- [ ] That section states the budget — **at most three** per round, **one per screen** — and names
      the **product owner** as who answers (gate 2).
- [ ] The file states that a decision-born ticket enters the **earliest subsequent** triage
      (gate 3).
- [ ] The file contains the string `qa-cycle` **nowhere** (gates 4 and 5).
- [ ] Exactly four files under `.claude/` name `qa-cycle` (gate 6), and `qa.md` is unchanged
      (gate 7).
- [ ] `.claude/agents/qa-manager.md` is **230 lines or fewer** (gate 8).
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
