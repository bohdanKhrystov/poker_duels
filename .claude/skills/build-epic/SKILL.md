---
name: build-epic
description: Work an entire epic unattended — plan each story, then run its tickets one at a time to merged PRs, stopping only for decisions a human must make. Use when the user names a goal like "implement the poker engine" rather than a single ticket.
---

# Build an epic

Takes an epic ID, or a goal in plain words (*"implement the poker engine"* → `EPIC-01`).

This is the entry point for **"here is the goal, go"**. It runs until the epic is done or until
it needs a decision that is not yours to make.

---

## The prime directive: stay small

You are a **scheduler**. You do not read source files, you do not read documentation, you do not
write code, and you do not review diffs. Every one of those happens inside a subagent whose
context dies with it.

Your context holds: the board, the current story, and **one line per finished ticket**. If you
find yourself reading a `.kt` file, you have already failed — the run will get more expensive
with every ticket instead of staying flat.

Keep a running ledger, nothing more:

```
TASK-010101  merged   haiku    1 attempt
TASK-010201  merged   haiku    1 attempt
TASK-010203  merged   sonnet   2 attempts  (promoted)
TASK-010204  blocked  —        DEC-002
```

## The loop

```
1. read tasks/BOARD.md — once, at the start
2. pick the epic's first story that is not `done`
3. if that story's tickets lack `schema: 2`:
        → /plan-story <STORY-ID>          (Opus planner, once per story)
4. while the story has a startable ticket:
        → /next-ticket                     (coder → verify → review → PR → merge)
        → append one line to the ledger
        → if blocked: record it, continue to the next startable ticket
5. story done → next story → back to 2
6. no stories left → final report
```

Strictly sequential. **One ticket at a time, never two.** Parallel tickets on a shared codebase
produce merge conflicts and half-finished branches, which cost far more than they save.

## Batching decisions

When a ticket blocks on a decision, **do not stop the run.** Register the `DEC-NNN`, mark the
ticket `blocked`, and carry on with the next startable ticket. Collect the decisions and present
them together at the end.

The whole design goal is: one command in, one batch of questions out. Interrupting per decision
defeats it.

Stop the run entirely only if:

- **every** remaining ticket is blocked, or
- a decision blocks the rest of the epic (a foundational type, a module boundary), or
- three consecutive tickets fail — something systemic is wrong and continuing will burn budget
  producing more of it.

## Budget awareness

Track roughly what the run is consuming. If a single ticket takes more than three coder
dispatches, stop it and mark it blocked rather than letting it grind — one pathological ticket
can eat an entire session's budget.

Never invoke `/code-review high` or any multi-agent review workflow from inside this loop.

## Final report

```
EPIC: <id> — <title>
MERGED:  <n> tickets
BLOCKED: <n> tickets
PROMOTED TO SONNET: <ids>

DECISIONS NEEDED
  DEC-00N — <question>  → blocks <ticket ids>

BLOCKED, NOT ON A DECISION
  <ticket> — <why>

NEXT: <what happens after the decisions are answered>
```

Then stop. Do not start another epic.
