---
schema: 2
id: TASK-000108
title: A count is pinned only where the ticket writes
type: task
status: backlog
parent: STORY-0001
module: process
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [process, planner, gates]
verify:
  - sh -c 'test $(grep -c "absolute test count" .claude/agents/planner.md) -ge 1'
  - sh -c 'test $(grep -c "git diff --exit-code" .claude/agents/planner.md) -ge 1'
  - sh -c 'grep -q "does not modify" .claude/agents/planner.md'
  - sh -c 'grep -q "measured at" .claude/agents/planner.md'
  - sh -c 'test $(grep -c "^## " .claude/agents/planner.md) -eq 7'
  - sh -c 'test $(grep -rlF "absolute test count" .claude/agents/ .claude/skills/ | wc -l) -eq 1'
  - sh -c 'grep -q "### Size" .claude/agents/planner.md'
  - sh -c 'grep -q "### The verify block" .claude/agents/planner.md'
  - sh -c 'awk "END { exit (NR > 340) }" .claude/agents/planner.md'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`.claude/agents/planner.md` states where an absolute per-file test count may be pinned and where it
may not: **only for a file the ticket itself modifies**, and never as an unchanged-guard for a file
it does not touch, which is what `git diff --exit-code` already proves without rotting.

## Why this exists

`EPIC-14` corrected **thirty-one** stale count literals. Every one had the same cause: a count
measured when a ticket was *authored*, and never re-measured when it was *dispatched*. In a serial
chain — `STORY-1409` ran nineteen tickets, `STORY-1410` twenty — each merge invalidates the counts
in every ticket behind it, so the defect is not occasional, it is the default.

The cost is not the wrong number. It is that a coder cannot start: the gate fails on a tree the
ticket never anticipated, the coder correctly reports `blocked` without writing code, and the driver
lands a ticket-only correction PR before re-dispatching. Four of those ran back to back in
`STORY-1415`, at roughly two minutes of agent time and one PR each.

**Most of them were counts for files the ticket does not modify.** `TASK-141508` pinned
`App.test.tsx` at 36, `Lobby.test.tsx` at 113 and `RematchNotice.test.tsx` at 16 — three files, of
which it modifies exactly one. Those counts were doing the job of *"this file is unchanged"*, and
`git diff --exit-code -- <path>` does that job exactly, proves strictly more (no edit at all, not
merely the same number of tests), and cannot go stale. Several of those same tickets already carried
both, so the count was redundant on arrival.

Where a ticket **does** modify a file, the count is worth pinning: it catches a deleted test, which
is the failure `ADR-0070` §3 cares about. It still needs a baseline that says when it was true.

## Files

| File | Action |
| --- | --- |
| `.claude/agents/planner.md` | modify |

Read, do not edit: `docs/adr/ADR-0070-a-ticket-is-a-contract-and-its-gates-are-the-terms.md` §3,
`tasks/tasks/TASK-141508-the-app-mounts-the-panel-and-it-follows-onto-every-screen.md` as the worked
example, and `.claude/agents/coder.md` for the reporting rule the coders already follow.

## Scope

- One section in `.claude/agents/planner.md` stating, in this order:
  1. **An absolute test count may be pinned only for a file the ticket's own Files table names.**
  2. **For a file the ticket does not modify, use `git diff --exit-code -- <path>`.** It proves the
     file is untouched rather than merely equinumerous, and no later merge invalidates it.
  3. **A pinned count names the commit it was measured at**, in the ticket's prose, so a coder
     finding a disagreement can tell drift from error without guessing.
  4. **A coder that finds a disagreement reports `blocked` with three numbers** — asked, measured,
     and what the change makes it — and edits neither the gate nor a test. This is already the
     practice; writing it beside the rule is what makes the rule actionable rather than aspirational.
- The section is placed with the other `verify:`-block guidance, not appended at the end.

## Out of scope

- **A linter rule.** Making `lint_tickets.py` check counts against the tree would need it to run the
  suite, which is a CI job's work and not a linter's. The gain here is in what planners write, not
  in a new gate, and inventing one that cannot be applied is worse than a rule named as judgement.
- **Rewriting merged tickets.** The thirty-one corrections stand as history. This changes what is
  written next.
- **`ADR-0070`.** Its §3 already says a gate must fail on a smaller commit; nothing here contradicts
  it, and this is the working copy, not a new decision.

## Tests

| Gate | What it pins |
| --- | --- |
| `absolute test count` present | rule 1 is stated |
| `git diff --exit-code` present | rule 2 names the replacement, not just the prohibition |
| `does not modify` present | rule 2's condition is stated, not implied |
| `measured at` present | rule 3 is stated |
| `^## ` == 7 | the section is new, not an overwrite of an existing one |
| exactly one file under `.claude/` names it | `ADR-0091` §2's one-place discipline, applied here too |

## What would still pass if the coder got it wrong

- Stating the prohibition without naming `git diff --exit-code` leaves a planner with a rule and no
  replacement, which is how the counts got written in the first place. Gate 2 is why it must appear.
- Appending the section at the end rather than beside the `verify:` guidance leaves it where a
  planner writing a verify block will not read it. The `### The verify block` guard is the anchor.
- Overwriting an existing `## ` section to satisfy the content greps passes every content gate and
  loses a rule. The heading count is what refuses it.

## Acceptance

- [ ] All ten `verify:` commands exit 0
- [ ] Deleting the new section reddens the four content gates and the heading count, and leaves the
      two structural guards green
- [ ] `.claude/agents/planner.md` stays under 340 lines
