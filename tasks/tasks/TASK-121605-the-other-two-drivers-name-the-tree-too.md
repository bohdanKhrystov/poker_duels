---
schema: 2
id: TASK-121605
title: The other two drivers name the tree they drove too
type: task
status: backlog
parent: STORY-1216
module: process
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [process, qa, harness]
depends_on: [TASK-121602]
verify:
  - sh -c 'test $(grep -c "COMMIT: <git rev-parse --short HEAD>" .claude/agents/uat.md) -eq 0'
  - sh -c 'test $(grep -c "COMMIT: <git rev-parse --short HEAD>" .claude/agents/audit.md) -eq 0'
  - sh -c 'grep -qF "SERVED:" .claude/agents/uat.md'
  - sh -c 'grep -qF "SERVED:" .claude/agents/audit.md'
  - sh -c 'test $(grep -rl "COMMIT: <git rev-parse --short HEAD>" .claude/agents/ | wc -l) -eq 0'
  - git diff --exit-code -- .claude/agents/qa.md .claude/skills/qa-cycle/SKILL.md scripts/qa/stack.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`.claude/agents/uat.md` and `.claude/agents/audit.md` stop telling their agents to report
`COMMIT: <git rev-parse --short HEAD>`, and report the tree that was actually served instead — the
same repair `TASK-121602` made to `qa.md`.

## Why this exists

`STORY-1216` found a QA round reporting `COMMIT: ea4bd05c` while driving a stack rooted in
`.claude/worktrees/agent-af32ec8cf22f8819c` at `c3e1a830`, 92 commits behind, in which three of the
files it was there to exercise did not exist. Eight high-severity product defects were reported and
none was measured. The `COMMIT:` line was `git rev-parse --short HEAD` **in the agent's own
checkout**, which is not the tree being served.

`TASK-121602` repaired `qa.md`. Measured on `develop` after it, the identical line survives in two
other driver definitions:

    .claude/agents/uat.md:118   COMMIT: <git rev-parse --short HEAD>
    .claude/agents/audit.md:107 COMMIT: <git rev-parse --short HEAD>

`TASK-121602`'s own text names this as deferred, and no ticket was cut for it. This is that ticket.

**Both agents drive the running product.** `uat` walks the screens and reports what contradicts a
merged source; `audit` walks a whole duel beat by beat against a frozen rubric. Either can be handed
a stale stack by exactly the chain `STORY-1216` traced, and either would then report a commit it did
not drive — with the same consequence, which is not a wrong number but a report that reads as
measurement and is not.

## Files

| File | Action |
| --- | --- |
| `.claude/agents/uat.md` | modify |
| `.claude/agents/audit.md` | modify |

Read, do not edit: `.claude/agents/qa.md` as the worked example, `scripts/qa/stack.sh`'s `served`
verb, and `tasks/stories/STORY-1216-round-1-epic-14-the-stack-served-a-tree-three-days-old.md`.

## Scope

- Replace the `COMMIT:` line in each with the shape `qa.md` now carries: a `SERVED:` line reporting
  what `scripts/qa/stack.sh served` says is actually listening.
- Neither agent starts, stops or restarts a stack, and this ticket does not change that. It changes
  what they **report**.
- Whether either should also *refuse* on a mismatch is `TASK-121602`'s question for `qa`, answered
  there for the `qa-cycle` skill. Follow whatever shape `qa.md` landed with rather than inventing a
  second one — `ADR-0091` §2's *"a second copy in another document would drift"* applies to a rule
  in three agent files exactly as it does to one in three documents.

## Out of scope

- **`qa.md` and `qa-cycle/SKILL.md`.** `TASK-121602` owns them and this ticket must leave them
  byte-identical; the `git diff --exit-code` above says so.
- **`stack.sh`.** `TASK-121601` gave it `served` and `TASK-121603` is changing how it finds the
  database. Not this ticket's.
- **Any agent that does not drive the running product.** `coder`, `reviewer`, `planner`,
  `architect`, `product-owner` and `qa-manager` report no `COMMIT:` of this kind; the last gate
  above catches it if that is ever wrong.

## Tests

| Gate | What it pins |
| --- | --- |
| `COMMIT: <git rev-parse…>` count 0 in `uat.md` | the line is gone from the first driver |
| same in `audit.md` | and from the second |
| `SERVED:` present in each | it was replaced rather than merely deleted |
| zero matches across all of `.claude/agents/` | no driver anywhere still carries it |

## What would still pass if the coder got it wrong

- Deleting the `COMMIT:` line without adding a `SERVED:` one satisfies the first two gates and
  leaves both agents reporting **nothing** about which tree they drove, which is worse than a wrong
  commit because it reads as if the question were never relevant. The `SERVED:` gates are why.
- Repairing one file and not the other passes that file's own two gates; the repository-wide count
  is what refuses it.
- Writing a *different* shape than `qa.md` landed with passes every gate here and starts the drift
  `ADR-0091` §2 warns about — three drivers, three phrasings, and a rule nobody can grep for.

## Acceptance

- [ ] Neither `uat.md` nor `audit.md` contains `COMMIT: <git rev-parse --short HEAD>`, and both
      carry a `SERVED:` line in the shape `qa.md` uses
- [ ] No file under `.claude/agents/` matches that string
- [ ] `qa.md`, `qa-cycle/SKILL.md` and `scripts/qa/stack.sh` are byte-identical to `develop`
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0
