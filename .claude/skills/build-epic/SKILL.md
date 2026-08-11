---
name: build-epic
description: Work an entire epic unattended — plan each story, then run its tickets to merged PRs, up to three at a time in isolated worktrees when they touch disjoint files, stopping only for decisions a human must make. Use when the user names a goal like "implement the poker engine" rather than a single ticket.
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
TASK-010201  merged   haiku    1 attempt   (batch A)
TASK-010202  merged   haiku    1 attempt   (batch A)
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
        → select a compatible batch of 1–3 tickets   (see "Batching tickets")
        → run each in its own git worktree, concurrently
        → land them ONE AT A TIME (see "Landing")
        → append one line per ticket to the ledger
        → if blocked: record it, continue to the next startable ticket
5. story done → next story → back to 2
6. no stories left → final report
```

## Batching tickets

Up to **three** tickets may be in flight at once. They are only compatible if all of these hold:

- **No dependency between them.** None appears in another's `depends_on`, directly or
  transitively.
- **Disjoint files.** The `Files` tables of the tickets in the batch must not overlap. Two
  tickets that both modify `gradle/libs.versions.toml` or `build.gradle.kts` are *not*
  compatible — that is a guaranteed conflict, not a risk.
- **Disjoint verify surface.** Two tickets that both introduce a build plugin will fight over
  build configuration even with disjoint files. When in doubt, run them sequentially.

Reading each candidate ticket's `Files` table is the *one* exception to "do not read files" — it
is the input to the scheduling decision. Read nothing else.

If only one ticket is compatible, run one. A batch of one is the normal case for build and
scaffold tickets, which nearly always share build files. Batching pays off on domain tickets that
each own their own source files.

## Isolation

Each concurrent ticket gets its own git worktree, so three coders never share a working tree:

- dispatch the coder with `isolation: "worktree"`
- one branch per ticket, as usual
- run that ticket's `verify` inside its own worktree

**Never** run two coders in the same working tree. They will overwrite each other's edits and
the failure looks like a model error rather than a scheduling error.

## Landing

Merging stays **strictly sequential**, one PR at a time, even when three tickets built in
parallel:

1. Pick a finished, verified, reviewed ticket.
2. Rebase its branch on the current `develop`.
3. Re-run its `verify` **after** the rebase — the other tickets moved `develop` underneath it.
4. `BOARD.md` and ticket-status edits are made by **you**, at landing time, never by the coder.
   Every ticket touches `BOARD.md`, so letting coders edit it guarantees three-way conflicts.
5. Merge, then move to the next.

If a rebase conflicts or a post-rebase verify fails, that ticket goes back for one more coder
dispatch against the updated `develop`. If it fails again, block it and land the others.

## Backpressure — drop to sequential

Fall back to **one ticket at a time** as soon as any of these is true:

- the run is short on tokens, or the context is filling up
- two or more tickets in a batch came back blocked or failing
- rebase conflicts appeared when landing a batch
- the remaining tickets share build files (the usual case for scaffold work)

Sequential is the safe default; parallelism is the optimisation. When they conflict, sequential
wins — three half-finished branches cost far more than they save.

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
