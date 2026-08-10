# The ticket system

The backlog lives here, in the repository, as markdown. It is versioned with the code,
greppable, diffable, and readable without a network call — which is what makes it usable by an
agent. See [`ADR-0004`](../docs/adr/ADR-0004-branching-and-ticket-workflow.md) for why it is
not GitHub Issues.

`BOARD.md` is the index. Start there.

## Hierarchy

```
EPIC-01                     a body of work. Months. Never implemented directly.
  └── STORY-0103            something a user or a consumer of the module can do.
        └── TASK-010301     one agent, one branch, one PR. 100–300 lines.
        └── TASK-010302
```

Only **tasks** get implemented. Epics and stories exist to give tasks a reason and an order.

## IDs

| Type | Format | Example | Means |
| --- | --- | --- | --- |
| Epic | `EPIC-EE` | `EPIC-01` | epic 1 |
| Story | `STORY-EESS` | `STORY-0103` | epic 1, story 3 |
| Task | `TASK-EESSTT` | `TASK-010302` | epic 1, story 3, task 2 |

The ID encodes the whole ancestry, so a branch name or a commit message tells you where the
work sits without opening anything. IDs are never reused, never renumbered, and never recycled
after a ticket is dropped.

## Layout

```
tasks/
├── README.md                 this file
├── BOARD.md                  the index and the current state of play
├── templates/
│   ├── epic.md
│   ├── story.md
│   └── task.md
├── epics/    EPIC-01-poker-engine.md
├── stories/  STORY-0103-hand-evaluator.md
└── tasks/    TASK-010301-hand-rank-model.md
```

Filename is always `<ID>-<kebab-slug>.md`. The slug is for humans; the ID is the identity.

## Frontmatter

Every ticket starts with YAML frontmatter. CI rejects anything malformed — see
`.github/scripts/lint_tickets.py`.

```yaml
---
id: TASK-010302
title: Evaluate a five-card hand into a comparable rank
type: task
status: ready
parent: STORY-0103
module: poker-engine
estimate: M
labels: [engine, rules]
depends_on: [TASK-010301]
---
```

| Field | Epic | Story | Task | Notes |
| --- | --- | --- | --- | --- |
| `id` | required | required | required | must match the filename |
| `title` | required | required | required | imperative, no trailing period |
| `type` | required | required | required | `epic` \| `story` \| `task` |
| `status` | required | required | required | see below |
| `parent` | forbidden | required | required | must exist and be one level up |
| `estimate` | — | — | required | `S` ≤ 100 lines, `M` ≤ 300, `L` → split it |
| `module` | optional | optional | optional | `poker-engine`, `poker-server`, `web-client`, … |
| `labels` | optional | optional | optional | free-form list |
| `depends_on` | — | optional | optional | ticket IDs that must be `done` first |

### Statuses

```
backlog  →  ready  →  in-progress  →  in-review  →  done
                            ↕
                         blocked
```

| Status | Meaning |
| --- | --- |
| `backlog` | written down, but not startable — either under-specified or still blocked |
| `ready` | on a **task**: an agent could start it right now. On an epic or story: specified. |
| `in-progress` | a branch exists |
| `in-review` | a PR is open |
| `blocked` | waiting on a dependency or a decision — say which, in the ticket |
| `done` | merged into `develop`, acceptance criteria all ticked |

An epic or story is `done` when all of its children are.

## What makes a task ready

A task is `ready` only when an agent could complete it without asking anything. That means:

- **The goal is one sentence.** If it needs two, it is two tasks.
- **Scope and out-of-scope are both written down.** The second is what stops scope creep.
- **The files are named.** Which files to create, which to touch.
- **Acceptance criteria are checkable**, not matters of taste. "Handles the wheel correctly" —
  good. "Well designed" — not a criterion.
- **The tests are specified**, at least by name and intent.
- **Its `depends_on` tickets are `done`.** CI enforces this on tasks: a `ready` task with an
  unfinished dependency fails the lint. It means the board never advertises work that cannot
  actually be started.

Anything that requires a decision is not ready. Make the decision first, in an ADR.

A task that is fully specified but still waiting on a dependency stays in `backlog` and moves to
`ready` in the PR that finishes the thing it was waiting for. That keeps a single question —
*what can be picked up right now?* — answerable by grepping for one word.

## Size

| Estimate | Changed lines | Files touched | Roughly |
| --- | --- | --- | --- |
| `XS` | ≤ 40 | 1–2 | one type, or one function with its tests |
| `S` | ≤ 120 | ≤ 3 | a small component with tests |

`M` and `L` do not exist. If work does not fit in `S`, it is two tickets — the failure mode of
this project is tickets that grew, never tickets that were too small.

### Schema 2

Tickets an agent workflow consumes carry `schema: 2` and four extra fields:

| Field | Meaning |
| --- | --- |
| `tier` | which model runs it — `haiku` (default), `sonnet`, `opus` |
| `review` | `light`, `standard` or `deep` — effort priced by risk |
| `files_touched` | 1–3, enforced by the linter |
| `verify` | shell commands that decide done. **All must exit 0.** |

Legacy `schema: 1` tickets still validate, so stories migrate one at a time via
`/plan-story`. Run it before starting a story whose tickets lack `schema: 2`.

If a task turns out to be larger than its estimate once started: stop, split it, and keep the
original ID for the first piece. Growing a change past its ticket is the failure mode this
whole system exists to prevent.

## Lifecycle

```
write ticket                              status: backlog
specify it fully, dependencies done       status: ready
branch <type>/TASK-XXXXXX-slug            status: in-progress
implement + tests, green locally
push, open PR into develop                status: in-review
/code-review  ──► fix findings ──► push ──┐
                                          │
CI green  ◄───────────────────────────────┘
squash merge into develop                 status: done  →  update BOARD.md
```

The status change is part of the same PR as the work. A ticket whose status disagrees with
reality is a broken build in spirit, if not in CI.

**A task reaches `done` only by way of a merged pull request into `develop`, and only after
`/code-review` has run on it.** There is no path from `in-progress` to `done` that skips
either. If the PR is open, the ticket is `in-review` — and the next task does not start.

Full reasoning: [`docs/workflow.md`](../docs/workflow.md#the-review-gate).

## Discovering new work mid-task

You will find things. Bugs, missing abstractions, a file that badly needs renaming.

**Do not fix them.** Write a new ticket in `backlog` and carry on with the one you have. The
digression is almost always worth less than the loss of a small, reviewable diff.

The one exception: if the current task is impossible without the fix, stop, say so, and let the
ticket be re-scoped rather than silently widened.
