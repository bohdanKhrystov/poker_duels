---
schema: 2
id: TASK-121204
title: The audit focus of the qa-cycle skill
type: task
status: done
parent: STORY-1212
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, audit, meta]
depends_on: [TASK-121203]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk 'index($0,"/qa-cycle audit"){a=1} index($0,"audit.md"){b=1} index($0,"A(N)"){c=1} index($0,"390 664"){d=1} index($0,"720 900"){e=1} index($0,"A(1)"){f=1} END{exit (a&&b&&c&&d&&e&&f)?0:1}' .claude/skills/qa-cycle/SKILL.md
  - awk 'index($0,"never chains into"){n++} END{exit (n>=2)?0:1}' .claude/skills/qa-cycle/SKILL.md
  - awk 'index($0,"No gate, and one caller"){a=1} index($0,"Frozen set"){b=1} index($0,"STOP_INFRA"){c=1} END{exit (a&&b&&c)?0:1}' .claude/skills/qa-cycle/SKILL.md
  - awk 'index($0,"qa-cycle"){bad=1} END{exit bad?1:0}' .claude/agents/qa-manager.md
  - grep -rl "qa-cycle" .claude/skills .claude/agents | awk '{n++; f[$0]=1} END{exit (n==5 && f[".claude/agents/qa.md"] && f[".claude/agents/uat.md"] && f[".claude/agents/audit.md"] && f[".claude/skills/qa-cases/SKILL.md"] && f[".claude/skills/qa-cycle/SKILL.md"])?0:1}'
  - grep -rl "qa-cycle" .claude/skills .claude/agents | grep -Ev '^\.claude/(agents/(qa|uat|audit)\.md|skills/qa-(cycle|cases)/SKILL\.md)$' | awk 'END{exit (NR==0)?0:1}'
  - shasum -a 256 .claude/agents/qa.md | awk '{exit ($1=="eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42")?0:1}'
  - shasum -a 256 .claude/agents/uat.md | awk '{exit ($1=="d6c1cd3f619356ca3f0a9f4af4ad9441854818c1d67913efd271c5d378664cce")?0:1}'
---

## Goal

`/qa-cycle audit <scope>` is a focus this skill can run: it dispatches `audit` instead of `qa` or
`uat`, allocates the round's shots directory, states the two shapes in the round record, and
enforces the audit's own stopping rules — `A(N)`, no severity, no backlog.

## Files

| File | Action |
| --- | --- |
| `.claude/skills/qa-cycle/SKILL.md` | modify |

You may **read** `.claude/agents/audit.md` — the observer this focus dispatches, and which you may
**not** modify — `docs/adr/ADR-0096-…` §§1, 4, 5 and `docs/adr/ADR-0097-…` §§2, 3, 6.

## Scope

Five edits to one file. Keep them inside the sections that already exist; this is a third focus of
one skill, not a second skill.

- **The invocation block** at the top gains three lines beside the `uat` ones:
  `/qa-cycle audit smoke`, `/qa-cycle audit epic EPIC-03`, `/qa-cycle audit regression`. Then one
  sentence: `audit` is a **third focus** of this same cycle (`ADR-0096` §1) — the same loop, the
  same manager, the same ledger — judging a **whole duel against a frozen rubric** rather than a
  screen against a card. Add that **the scope word is recorded and narrows nothing**: the walk is
  `ADR-0096` §1's eight beats, every round (§5).
- **The three refusals become three.** The existing corollary *"The QA focus never chains into the
  UAT focus"* gains its sibling: **no focus ever chains into another**, in a sentence that uses the
  words *never chains into* so gate 3 can see it. One turn that runs two focuses is a skill running
  a cycle as one of its steps — `ADR-0089` §2b failing, `ADR-0090`'s exact holding. Neither of the
  other two corollaries changes: no report prints another focus's command, and a preceding round of
  any focus is practice and never a checked precondition.
- **Step 1 of `## The loop`** dispatches `audit` under the audit focus, with the scope and the two
  browser ports, and allocates the shots directory the same way the UAT focus does
  (`S=$(mktemp -d)`). Add the two shapes as a named part of the round: the whole walk at **`phone`
  390 664**, `R2`/`R3` re-answered at **`laptop` 720 900** at the beats where a player is asked to
  act, **both tabs moved together and both returned to `phone`** before the walk continues
  (`ADR-0097` §3). Say that both shapes are stated in the round record, and that the record names
  where every `size` was issued — a **forgotten** restore has no catch (`ADR-0097` §Consequences),
  and §2b forbids making one.
- **Step 2** gains the audit round story's shape: a per-criterion table rather than a per-screen
  one, `A(N)` rather than `B(N)`, and no severity column.
- **`## The stopping rules`** gains the audit's, restated the way the other focuses' are, because a
  skill that hides its own exit conditions is how a loop becomes infinite:
  - **`A(N)`** is the number of criteria answered `not met` in round `N`, so `A(N)` can never
    exceed the rubric's size — five today — a ceiling known before the round starts;
  - **`PASS` at `A(N) == 0`**, meaning the list was satisfied at one commit on one machine at the
    two declared shapes, and **never** that the product is finished (`ADR-0089` §2c, `ADR-0093`
    §2);
  - **`A(N) >= A(N-1)` stops with `STOP_DIVERGING`**, and the three-round budget binds regardless;
  - **no severity and no backlog under this focus** — a finding deferred by the eight-ticket cap
    **stays an unmet criterion and is counted again next round**, and the cap orders repair by the
    rubric's own order, top to bottom (`ADR-0096` §5);
  - **an audit round reports `A(N)` and no `B(N)`** — a functional defect it stumbles on is filed
    to the one ledger and enters the next `qa` round's count.
- **`## Report to the user`** gains one line under the audit focus: `A: <A(1)> → <A(2)> → …` in
  place of the `B:` line, and no `BASELINE:`. Say that the `EXIT` line names the two shapes walked.

## Out of scope

- **A second skill, a second manager, or a `uat`-style per-screen table under this focus.**
  `ADR-0092` §8 applies unamended (`ADR-0097` §4): one manager, one ledger, one copy of the
  stopping rules.
- **Any change to the QA or UAT focus's own text.** The `uat` invocation lines, the per-screen
  table, the baseline-round rule and the qualified verdict all stay exactly as they are. Gate 4
  pins three anchors of the surrounding document.
- **Any change to `## The three conditions this harness runs under`.** `ADR-0097` §6 re-checked all
  three and none moves: no dependency, no gate, no coverage claim.
- **Any change to `.claude/agents/qa.md`, `.claude/agents/uat.md` or
  `.claude/agents/audit.md`.** Gates 8 and 9 pin two of them; the third is finished.
- **Naming `qa-cycle` in `.claude/agents/qa-manager.md`.** Gate 5 refuses it. This is the most
  likely accident in this ticket's neighbourhood, because describing the audit focus invites
  writing the command into the manager's brief.
- **A baseline-round rule for this focus.** `ADR-0096` §5 lists the audit's termination rules and
  includes none; `ADR-0092` §6's rule is defined over a screen becoming conformance-judgeable
  through a merged card, which no audit round measures. Inventing one is inventing an exemption.
- **Any change to the teardown, the notification block or the cron.** One run state, one `Stop`
  hook, one heartbeat serving three focuses — nothing about them is focus-specific.
- **Transcribing `ADR-0096` §1's beat table or §2's rubric.** Cite the sections. The observer's
  budget is the ADR and the skill's job is to dispatch it.

## Tests

No test class — the deliverable is one prose document, so the gates are structural checks over that
text. Every row was run on 2026-08-31 at commit `f8383c4e`. **Three of the nine are red today — 2,
3 and 6 — and all nine are green with the edit.** Gates 4, 5, 7, 8 and 9 are guards and were 0.

| # | Gate | Proves | Today | With the edit |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | `awk` over six literals | the focus is real: the command form, the observer it dispatches, the arithmetic it enforces, both shapes, and the `A(1)` report line | **1** — the file names `audit` nowhere | 0 |
| 3 | `awk` counting *never chains into* | the refusal is stated for **more than one pair of focuses** | **1** — it appears once, for QA into UAT | 0 |
| 4 | `awk` over three surrounding anchors | `## The three conditions`'s **b**, the frozen-set rule and `STOP_INFRA` all survive the edit | 0 — a guard | 0 |
| 5 | `awk` over `qa-cycle` in `qa-manager.md` | the manager still names the cycle **nowhere** (`ADR-0090` §2 as amended) | 0 — a guard | 0 |
| 6 | `grep -rl` set equality | exactly the **five** declared files name the cycle | **1** today; **0** once `TASK-121202` merges | 0 |
| 7 | `ADR-0097` §4's own command | no **sixth** file names the cycle | 0 — a guard | 0 |
| 8 | `sha256` of `qa.md` | byte-identical to `f8383c4e` | 0 — a guard | 0 |
| 9 | `sha256` of `uat.md` | byte-identical to `f8383c4e` | 0 — a guard | 0 |

**Gate 3 is the only gate here that counts rather than matches**, which makes it the only one that
cannot be satisfied by leaving the file as it is and adding a heading. It is red today at one
occurrence and needs a second, and a second occurrence is exactly the refusal `ADR-0089` §2b needs
stated for a third focus.

**Gate 5 is a guard against this ticket's most likely accident.** Describing `/qa-cycle audit`
inside `qa-manager.md` would be natural and is forbidden: `ADR-0090` §2's declared set is five and
the manager is in none of them. Measured today at **0**; run against `.claude/agents/uat.md` — a
file that legitimately names the cycle once — the same gate exits **1**.

**What no gate here sees** is whether a round actually restores `phone` after a `laptop` check.
`ADR-0097` §Consequences says so in as many words: nothing catches a resize that was *forgotten*,
the mitigation is a printed transcript a reader audits, and `ADR-0089` §2b forbids turning it into
a gate. That half is the reviewer's, then the round record's.

## Acceptance criteria

- [ ] `.claude/skills/qa-cycle/SKILL.md` names `/qa-cycle audit`, `audit.md`, `A(N)`, `390 664`,
      `720 900` and `A(1)` (gate 2).
- [ ] *never chains into* appears at least **twice** (gate 3).
- [ ] `No gate, and one caller`, `Frozen set` and `STOP_INFRA` all survive (gate 4).
- [ ] `.claude/agents/qa-manager.md` contains `qa-cycle` nowhere (gate 5).
- [ ] Exactly five files under `.claude/skills` and `.claude/agents` name `qa-cycle`, and they are
      the five `ADR-0097` §4 declares (gates 6 and 7).
- [ ] `.claude/agents/qa.md` and `.claude/agents/uat.md` are byte-identical to `f8383c4e`
      (gates 8 and 9).
- [ ] The diff touches exactly one file besides this ticket's own status and its `BOARD.md` cell.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
