---
schema: 2
id: TASK-120707
title: The uat focus of the qa-cycle skill
type: task
status: backlog
parent: STORY-1207
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, qa, uat, meta]
depends_on: [TASK-120706]
verify:
  - python3 .github/scripts/lint_tickets.py
  - awk 'index($0,"/qa-cycle uat "){f=1} END{exit f?0:1}' .claude/skills/qa-cycle/SKILL.md
  - awk '/^## /{s=0} /^## The loop$/{s=1} s && index($0,"`uat`"){f=1} END{exit f?0:1}' .claude/skills/qa-cycle/SKILL.md
  - awk 'index($0,"never chains into"){a=1} index($0,"practice, never a checked precondition"){b=1} END{exit (a&&b)?0:1}' .claude/skills/qa-cycle/SKILL.md
  - awk 'index($0,"baseline round"){a=1} index($0,"PASS (conformance unjudged on"){b=1} index($0,"gates a member of the current fix set"){c=1} END{exit (a&&b&&c)?0:1}' .claude/skills/qa-cycle/SKILL.md
  - awk -F'|' '/^## /{s=0} /^## The loop$/{s=1} s && $2 ~ /`PASS`/ { seen=1; if (index($0,"/qa-cycle")) bad=1; if (!index($0,"end")) bad=1 } END{exit (seen && !bad)?0:1}' .claude/skills/qa-cycle/SKILL.md
  - awk '/^## Report to the user$/{s=1;next} /^## /{s=0} s && index($0,"/qa-cycle"){f=1} END{exit f?1:0}' .claude/skills/qa-cycle/SKILL.md
  - grep -rl "qa-cycle" .claude/skills .claude/agents | awk '{n++; f[$0]=1} END{exit (n==4 && f[".claude/agents/qa.md"] && f[".claude/agents/uat.md"] && f[".claude/skills/qa-cases/SKILL.md"] && f[".claude/skills/qa-cycle/SKILL.md"])?0:1}'
  - shasum -a 256 .claude/agents/qa.md | awk '{exit ($1=="eca3f411be3fde1089ac0c2bb067cc366093bda7ab08b1716477f07e1a4b1b42")?0:1}'
  - awk 'END{exit (NR<=300)?0:1}' .claude/skills/qa-cycle/SKILL.md
---

## Goal

`/qa-cycle uat smoke`, `/qa-cycle uat epic <ID>` and `/qa-cycle uat regression` run the same loop
with the `uat` agent in step 1, a per-screen table in the round record, a baseline round that
skips rule 4's comparison, and a verdict line qualified inline.

## The specification is `ADR-0092` §§1, 6, 8, and it is binding

Read them before writing a word. The whole existing lifecycle — stack, browsers, notifications,
teardown, the five exit states — is **reused, never duplicated**: `ADR-0092` §8 refused a
`uat-cycle` skill precisely because *"the stopping rules would exist in two prose copies and
drift"*.

## Files

| File | Action |
| --- | --- |
| `.claude/skills/qa-cycle/SKILL.md` | modify |

You may **read** `docs/adr/ADR-0092-…` §§1, 6, 8, `.claude/agents/uat.md`, and
`tasks/epics/EPIC-12-quality-and-defect-repair.md` §Termination.

## Scope

**300 lines at most for the whole file**, gated. It is 228 today, so about 70 are available; if
that feels tight, something is being restated rather than referenced.

### 1. The invocation block gains a focus

The fence at the top gains the three UAT forms, and one line saying what the focus is: the same
loop, over the same catalogue's routes, judging conformance to the merged card, reachability and
copy instead of function.

```
/qa-cycle uat smoke          the same screens, judged against the cards and the merged copy
/qa-cycle uat epic EPIC-03
/qa-cycle uat regression
```

### 2. Three corollaries, written as refusals — `ADR-0092` §1

`ADR-0092` §1 states these *"so no helpful someone adds the check"*, so write all three as
prohibitions, in the condition-**b** bullet or immediately under it:

- **The QA focus never chains into the UAT focus.** One turn that runs both is a skill running a
  cycle as one of its steps — condition **b** failing, `ADR-0090`'s exact holding. The human types
  two commands, on two occasions of their choosing. Use the literal `never chains into`, which
  gate 4 matches.
- **Neither focus's report prints the other's command.** A standing *"next: `/qa-cycle uat …`"*
  line would teach the reader that UAT follows every QA pass, which is false.
- **A preceding QA cycle is the human's practice, never a checked precondition.** A step that
  verified *"has a QA round passed at this commit?"* would cite a round as a gate — §2c failing.
  Use the literal `practice, never a checked precondition`, which gate 4 matches.

### 3. Step 1 of the loop dispatches by focus

`## The loop` step 1 currently reads *"Dispatch the `qa` agent"*. It becomes: dispatch `qa` under
the QA focus and **`uat`** under the UAT focus, with the scope and the two browser ports, plus a
**shots directory from `mktemp -d`** for the UAT focus — the same way profile directories are
allocated today, and the directory `ADR-0092` §2a's `shot` verb writes into. Screenshots are
**never committed** and are not read back by the skill.

### 4. The per-screen table and the qualified verdict — `ADR-0092` §6

- A UAT round record carries a per-screen table: checks **a**/**b**/**c**, each `judged`,
  `BLOCKED — no card`, or `out of scope`.
- A verdict over any `BLOCKED` cell carries the qualification **inline, in the verdict line
  itself**: `PASS (conformance unjudged on 6 of 7 screens)`. Write that example literally — gate 5
  matches `PASS (conformance unjudged on`. The terminal report repeats the line **verbatim**, so
  say so where the report shape is defined.

### 5. Two stopping-rule amendments in `## The stopping rules`

The section already exists and already restates `B(N)` and the harness exclusion. It gains:

- **The baseline rule.** A round in which a screen becomes conformance-judgeable for the first
  time — its card merged in the previous round's repairs — is a **baseline round**: rule 4 does
  not compare its `B(N)` against `B(N−1)`, exactly as round 1 is not compared with a round 0 that
  does not exist. **Rule 5's three-round budget binds regardless**, and say that, because it is
  the only thing keeping the loop finite in a baseline run.
- **`STOP_BLOCKED` scoped.** A human-only escalation ends the cycle **only when the unanswered
  decision gates a member of the current fix set**. Otherwise `notify.py blocked --decision
  DEC-NNN` goes out while the run is warm, the question is carried open in the round story, and
  the cycle **continues** — no step of the loop waits on a UX answer. Use the literal
  `gates a member of the current fix set`, which gate 5 matches.

## Out of scope

- **Any step that runs the UAT focus after the QA focus, conditionally or otherwise.** This is the
  single failure the ticket exists to prevent, and `ADR-0090` §Consequences forecloses the
  conditional forms by name. Gate 6 pins the `PASS` row against it; the rest is the review's.
- **Any check for a preceding QA round.** §2c. Nothing verifies it, and §2 above is why.
- **Printing `/qa-cycle uat …` in the report to the user.** Gate 7 refuses it in that section.
- **A second copy of the stopping rules, a second lifecycle, a second teardown, or any new skill
  or agent file.** `ADR-0092` §8: one skill, one manager, one ledger. A fifth file naming
  `qa-cycle` fails gate 8.
- **The promotion gate, the `B(N)` exclusions, the classifier and the dedupe rule.** They are
  `qa-manager`'s and land in `TASK-120708` and `TASK-120709`. The skill's job is to obey a verdict,
  not to compute one — its own words: *"`qa-manager` computes and enforces them; this skill obeys
  without arguing."*
- **Any change to `.claude/agents/qa.md`** (gate 9 pins its `sha256`), `uat.md`, `qa-manager.md`
  or `qa-cases/SKILL.md`.
- **Any change to the notification apparatus, the cron, the teardown order, or `notify.py`.** The
  run state's `--clear` was repaired in `TASK-120701`; the rest is correct and is reused.
- **Any new exit state.** There are five and `ADR-0092` added none.

## Tests

No test class — one prose document. Every row was run on 2026-08-30 at commit `cfcc6a4e`.

| # | Gate | Proves | Today | After |
| --- | --- | --- | --- | --- |
| 1 | `lint_tickets.py` | ticket, story and board rows agree | 0 | 0 |
| 2 | `awk` over `/qa-cycle uat ` | the focus is invocable and documented | **1** | 0 |
| 3 | `awk` scoped to `## The loop` | step 1 names the `uat` agent — the dispatch actually moved | **1** | 0 |
| 4 | `awk` over two refusal literals | *never chains into* and *practice, never a checked precondition* are written down | **1** | 0 |
| 5 | `awk` over three literals | the baseline rule, the inline-qualified verdict example, and `STOP_BLOCKED`'s scoping are written down | **1** | 0 |
| 6 | `awk -F'\|'` on the `PASS` row of `## The loop` | the `PASS` verdict still ends the cycle and does **not** name a command to run next | 0 — a **guard** | 0 |
| 7 | `awk` scoped to `## Report to the user` | the terminal report prints no `/qa-cycle` command | 0 — a **guard** | 0 |
| 8 | four-file set | exactly the four files `ADR-0092` §2 declares name the cycle | **1** until `TASK-120705` merges | 0 |
| 9 | `sha256` of `qa.md` | `ADR-0092` §8's byte-unchanged clause held | 0 — a guard | 0 |
| 10 | `awk 'END{exit (NR<=300)?0:1}'` | the file did not double | 0 | 0 |

### What these gates cannot see — read this before reviewing

**This is the weakest-gated ticket in `STORY-1207`, and saying so is more useful than a gate that
cannot fail.** Its three load-bearing requirements are **prohibitions**, and a prohibition obeyed
leaves no artefact:

- Gates 2–5 pass the moment the coder types those strings. They are worth having for
  `TASK-120301`'s reason — a sentence a later editor must **delete** rather than merely fail to
  add — and for nothing more.
- Gates 6 and 7 are the only ones that can catch the named failure, and they are **guards**: both
  exit 0 today and go red only if someone makes `PASS` chain onward or makes the terminal report
  print the other focus's command. A guard that never goes red on a correct diff is doing its job;
  it is not evidence that the diff is correct.
- **No gate can see a step added three sections later that runs the cycle.** `ADR-0090` §2 says
  this in its own words: *"print this command and run this command are the same string."*

**So the review is the gate.** Read the finished `SKILL.md` against `ADR-0092` §1's three
corollaries and reject: any step that starts a second cycle, any conditional that does, any check
for a preceding round, and any sentence that reads a `PASS` as the thing that made the product
ready (§2c — no round may be cited as that, and the bar itself is `DEC-086`).

## Acceptance criteria

- [ ] The invocation block documents `/qa-cycle uat smoke`, `epic <ID>` and `regression` (gate 2).
- [ ] `## The loop` step 1 dispatches `uat` under the UAT focus and allocates a `mktemp -d` shots
      directory (gate 3).
- [ ] The file states, as refusals, that the QA focus **never chains into** the UAT focus, that
      neither report prints the other's command, and that a preceding QA cycle is **practice,
      never a checked precondition** (gate 4).
- [ ] The file states the baseline rule, that rule 5's three-round budget binds regardless, the
      inline-qualified verdict `PASS (conformance unjudged on 6 of 7 screens)`, and that
      `STOP_BLOCKED` fires only when the decision **gates a member of the current fix set**
      (gate 5).
- [ ] The `PASS` row of the loop's verdict table still ends the cycle and names no next command
      (gate 6), and `## Report to the user` prints no `/qa-cycle` command (gate 7).
- [ ] Exactly four files under `.claude/` name `qa-cycle` (gate 8), and `qa.md` is unchanged
      (gate 9).
- [ ] `.claude/skills/qa-cycle/SKILL.md` is **300 lines or fewer** (gate 10).
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
