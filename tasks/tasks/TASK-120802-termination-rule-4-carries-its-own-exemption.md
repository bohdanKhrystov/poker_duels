---
schema: 2
id: TASK-120802
title: EPIC-12 §Termination rule 4 carries its own exemption
type: task
status: done
parent: STORY-1208
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [process, qa, uat, meta, defect]
depends_on: [TASK-120801]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk '/^## /{s=0} /^## Termination$/{s=1} s && /^4\. /{r=1} s && /^5\. /{r=0} s && r{n++} s && r && index($0,"baseline"){f=1} END{exit (f&&n)?0:1}' tasks/epics/EPIC-12-quality-and-defect-repair.md
  - awk '/^## /{s=0} /^## Termination$/{s=1} s && /^4\. /{r=1} s && /^5\. /{r=0} s && r && index($0,"ADR-0092"){f=1} END{exit f?0:1}' tasks/epics/EPIC-12-quality-and-defect-repair.md
  - awk '/^## /{s=0} /^## Termination$/{s=1} s && /^6\. /{r=1} s && /^\*\*Exit states\*\*/{r=0} s && r && index($0,"harness"){f=1} END{exit f?0:1}' tasks/epics/EPIC-12-quality-and-defect-repair.md
  - awk '/^## /{s=0} /^## Termination$/{s=1} s && /^\| `STOP_BLOCKED` \| a `DEC` was raised that only the human can answer \|$/{f=1} END{exit f?0:1}' tasks/epics/EPIC-12-quality-and-defect-repair.md
  - awk '/^## /{s=0} /^## Step 6/{s=1} s && /^\| `STOP_DIVERGING` \|/ && index($0,"not a baseline round"){f=1} END{exit f?0:1}' .claude/agents/qa-manager.md
  - awk 'END{exit (NR<=242)?0:1}' .claude/agents/qa-manager.md
  - shasum -a 256 .claude/agents/qa.md | awk '{exit ($1=="eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42")?0:1}'
  - shasum -a 256 .claude/skills/qa-cycle/SKILL.md | awk '{exit ($1=="2101bef0d0975ecb45ed410453a0886748a1b08c9298d7316c751915cac31c8c")?0:1}'
---

## Goal

`EPIC-12` §Termination rule 4 states its own exemptions, so the ledger `qa-manager` is handed no
longer contradicts the brief `TASK-120801` just fixed.

## Why this is a second document and not the same edit

`.claude/agents/qa-manager.md` says *"**rule 4** skips comparing"* and *"**Rule 5's** three-round
budget binds regardless."* Those numbers belong to this file, which `qa-manager` is handed as the
round ledger in its own *What you are given*. Rule 4 here still reads *"If `B(N) >= B(N-1)` the
cycle **stops**"* with no exemption at all — so after `TASK-120801` the fixed document's
cross-reference points at an unfixed one, which is worse than before, because now the two
disagree.

`ADR-0089` §4's amendment set the precedent: it landed as §Termination **rule 6** *and* in the
register. `ADR-0092` §6's baseline rule got only the register row (`DEC-085`, point 5). This
closes it.

## Files

| File | Action |
| --- | --- |
| `tasks/epics/EPIC-12-quality-and-defect-repair.md` | modify |

You may **read** `.claude/agents/qa-manager.md` §*The UAT arithmetic* for the wording
`TASK-120801` left there, and
`docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md` §6.

## Scope

**One numbered item, in `## Termination` only.** Five or six lines appended to rule 4's existing
paragraph — the item beginning `4. **Convergence.**`. Do not renumber, do not add a rule 7, and do
not touch rules 1, 2, 3, 5 or 6.

The appended sentences must carry all three of:

- **Two rounds are exempt from the comparison, and rule 5 exempts neither**: round 1, which has no
  round 0, and a **baseline round**.
- **What makes a round a baseline round** — a screen becomes conformance-judgeable for the first
  time in round *N*, its card having merged in round *N-1*'s repairs.
- **Why** — the two rounds measured differently-sized judgeable sets, so the comparison would
  score the unlock as decay. Cite `ADR-0092` §6 as a link, in the same form as rule 6's link to
  `ADR-0089` two items below (relative path `../../docs/adr/…`). Gate 3 matches the string
  `ADR-0092` inside rule 4's block.

Rule 4's existing four lines stay as they are; the exemptions are appended to them.

## Out of scope

- **The `STOP_BLOCKED` row of §Termination's exit-state table.** It reads *"a `DEC` was raised
  that only the human can answer"*, and `ADR-0092` §5 assigned that scoping to *"the epic's
  register"* by name — `DEC-085` point 6 carries it. **Gate 5 pins the row byte-for-byte**, so
  fixing it here fails the ticket. That is deliberate: this ticket is the baseline rule and
  nothing else.
- **The `STOP_DIVERGING` row of that same table.** It reads *"rule 4 tripped"*, which stays true
  and stays correct once rule 4 carries its exemptions — that is the whole point of putting them
  in rule 4 rather than in the table.
- **Every other section of this epic** — `## Scope`, `## Open decisions`, `## Stories`,
  `## Metrics`, `## Definition of done`. `DEC-085`'s register row already records the baseline
  rule and is not rewritten.
- **`.claude/agents/qa-manager.md`.** `TASK-120801` finished it; gates 6 and 7 only check it
  survived — the `STOP_DIVERGING` row still exempts a baseline round, and the file is still within
  the 242-line budget `STORY-1208` argued.
- **`.claude/agents/qa.md` and `.claude/skills/qa-cycle/SKILL.md`** (gates 8 and 9, `sha256`).
- **A rule 7, a sixth exit state, or any loosening of rules 2, 3 and 5.** `ADR-0092` §6: the
  machinery *"carries over unchanged"* apart from its two named additions.

## Tests

No test class — one prose document. Every row was run at commit `efa3b6fd`. The **After** column
was measured on a probe copy of this epic in `/tmp` carrying the edit above; the probe was
discarded and nothing in this repository changed.

| # | Gate | Proves | Today | After |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | `awk` scoped to rule 4's own numbered block: contains `baseline`, and the block is non-empty | the exemption is in rule 4, not merely somewhere in the epic | **1** | 0 |
| 3 | `awk`, same block: contains `ADR-0092` | the exemption cites what licenses it, as rule 6 cites `ADR-0089` | **1** | 0 |
| 4 | `awk` scoped to rule 6's block: contains `harness` | rule 6 was not clipped while editing its neighbour | 0 — a **guard** | 0 |
| 5 | `awk`: the `STOP_BLOCKED` exit-state row matches its current text **exactly**, `^` to `$` | the refusal in *Out of scope* is an exit code — `ADR-0092` §5 gave that row to the register | 0 — a **negated control** | 0 |
| 6 | `awk` over `qa-manager.md`'s `STOP_DIVERGING` row | `TASK-120801` is merged and was not reverted | **1** until `TASK-120801` merges | 0 |
| 7 | `awk 'END{exit (NR<=242)?0:1}'` over `qa-manager.md` | the budget `STORY-1208` raised once still holds after `TASK-120801` | 0 | 0 |
| 8 | `sha256` of `qa.md` | byte-unchanged | 0 — a guard | 0 |
| 9 | `sha256` of `qa-cycle/SKILL.md` | the third prose copy was left alone, deliberately | 0 — a guard | 0 |

**Gate 2's scoping is the whole of its value.** The word `baseline` already appears in this file
today — `DEC-085`'s register row uses it — so a file-wide `grep` would be green before the edit
and would prove nothing. Bounding the match between `^4\. ` and `^5\. ` inside `## Termination` is
what makes it red; the `n` counter in the same command fails the gate if rule 4's block is deleted
rather than amended, so it cannot be satisfied by removing the rule.

**Gate 5 is a refusal, not a requirement**, and it is the reason this ticket cannot quietly grow:
the adjacent, similar-looking `STOP_BLOCKED` row is the obvious thing to fix while you are here,
and `ADR-0092` §5 put it somewhere else on purpose.

**What no gate can see** is whether rule 4's new sentences say the right thing. **So the review is
to read rule 4 against `ADR-0092` §6** and reject any wording that exempts a baseline round from
rule 5's three-round budget, that makes the exemption automatic rather than conditional on a
screen becoming judgeable for the first time, or that contradicts
`.claude/agents/qa-manager.md` §*The UAT arithmetic*, which is the same rule for the same reader.

## Acceptance criteria

- [ ] §Termination rule 4's own numbered block names a `baseline` round (gate 2).
- [ ] That same block cites `ADR-0092` (gate 3).
- [ ] Rule 6's block still names the harness exclusion (gate 4).
- [ ] The `STOP_BLOCKED` exit-state row is byte-identical to its current text (gate 5).
- [ ] `.claude/agents/qa-manager.md`'s `STOP_DIVERGING` row still says `not a baseline round`, and
      the file is 242 lines or fewer (gates 6 and 7).
- [ ] `.claude/agents/qa.md` and `.claude/skills/qa-cycle/SKILL.md` are byte-unchanged (gates 8
      and 9).
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
