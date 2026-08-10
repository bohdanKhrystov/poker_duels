# The development workflow

This describes how work actually gets done on this project: a single developer directing Claude
Code agents against a ticketed backlog. It is also Product B — the case study — so it is
written to be read by someone who wants to copy it.

## The premise

The scarce resource is not the model's ability to write code. It is **context**. A model that
reads 120 000 lines to change 50 of them will be slow, expensive, and worse at the job than one
that reads three files. Every rule below exists to keep the amount of context per unit of work
small.

The corollary: the expensive part of this project is not writing code, it is **defining work
precisely enough that it can be done in isolation**. That is why the backlog is the centre of
the process.

## The unit of work

One ticket. A ticket is a markdown file in `tasks/` that contains everything an agent needs:
the goal, the scope, what is explicitly out of scope, the files involved, acceptance criteria,
and the tests required.

A well-formed task is:

- **Small** — 100–300 changed lines, under ten files.
- **Self-contained** — readable without reading its siblings.
- **Verifiable** — its acceptance criteria are checkable without judgement calls.
- **Boring** — if it needs a decision, that decision belongs in an ADR made *before* the ticket
  is picked up.

Tickets are hierarchical: **Epic → Story → Task**. Only tasks get implemented. See
[`../tasks/README.md`](../tasks/README.md).

## The loop

```
   ┌──────────────────────────────────────────────────┐
   │                                                  │
   ▼                                                  │
pick the next `ready` task                            │
   │                                                  │
   ├─ read: the task, the 2–3 docs it links, the      │
   │        files it names.  Nothing else.            │
   │                                                  │
   ├─ branch:  feature/TASK-XXXXXX-slug               │
   │                                                  │
   ├─ implement + tests, in one pass                  │
   │                                                  │
   ├─ run the build and the tests locally             │
   │                                                  │
   ├─ open a PR into develop                          │
   │                                                  │
   ├─ review pass ─── findings? ──► fix ──┐           │
   │                                      │           │
   ├─ squash merge, branch deleted  ◄─────┘           │
   │                                                  │
   ├─ ticket → done, BOARD.md updated                 │
   │                                                  │
   └─ discard the context entirely ───────────────────┘
```

That last step matters as much as the others. A task is finished when the agent could forget
it existed and the repository would still tell the whole story.

## The roles

Not seven agents running at once — one at a time, each with a narrow brief. Running many
agents in parallel over a shared codebase costs more and produces merge pain, which is exactly
the wrong trade for a solo project on a personal subscription.

| Role | Input | Output |
| --- | --- | --- |
| **Architect** | a problem or a story | an ADR, module boundaries, interfaces |
| **Planner** | an epic or story | stories and tasks, split until each is small |
| **Coder** | one task | the implementation and its tests |
| **Reviewer** | a diff | findings on correctness, design, naming |
| **Tester** | a module | property-based and edge-case tests |
| **Poker expert** | engine code | rule correctness: min-raise, split pots, the wheel, all-in caps |
| **Security** | server code | trust boundaries, information leaks, replay and race conditions |

The **poker expert** role is not decoration. Most bugs in a poker engine are not crashes; they
are rules that are subtly wrong and silently produce plausible results.

## Hard limits

These are enforced by habit and by review, and violating one means the ticket was wrong:

| Limit | Value |
| --- | --- |
| Changed lines per task | ≤ 300 |
| Files in context | ≤ 10 |
| Tasks per PR | 1 |
| Undocumented architectural decisions | 0 |

## Permissions

`.claude/settings.json` holds a committed allowlist of the commands an agent runs constantly:
reading and searching files, editing the project's own directories, read-only git, the build
and test commands, the ticket linter, and the GitHub CLI operations the PR flow needs.

The point is not convenience for its own sake. An agent that stops for approval forty times an
hour trains the human to approve without reading, which is strictly worse than a considered
allowlist plus a real review gate. Approval fatigue is a security problem, not an ergonomic one.

What the allowlist deliberately does **not** cover:

- `git push` to `main` or `develop`, force pushes, hard resets, branch deletion, rebases —
  denied outright. Server-side branch protection is the real control; these denials just stop
  an agent wasting a turn discovering that.
- `rm -rf`, `sudo`, piping a download into a shell, deleting the repository — denied.
- Reading `.env`, key material or `secrets/` — denied.
- Editing `.claude/settings.json` itself — denied, so an agent cannot widen its own permissions.

One rule is genuinely broad and is a deliberate choice: `Bash(python3 -)` permits running an
arbitrary script from stdin, which is how bulk edits across many ticket files get done in one
pass instead of forty. It is listed here rather than buried, because a permission you have
forgotten about is one you have not really granted.

The real safety net is not the allowlist. It is that `main` and `develop` are protected, every
change arrives by pull request, and nothing merges unreviewed.

## Documents as memory

An agent does not remember the project. It re-reads the parts it needs. So the documents have
to be small, current, and non-overlapping:

| Document | Answers |
| --- | --- |
| `CLAUDE.md` | the working agreement; always loaded |
| `docs/vision.md` | what we are building and what we refuse to build |
| `docs/architecture.md` | modules, dependency rules, the engine contract |
| `docs/duel-rules.md` | the rules the engine implements |
| `docs/adr/` | why each significant decision was made |
| `tasks/` | what to do next |

If a document and the code disagree, that is a bug in the document, and fixing it is part of
whatever ticket exposed it.

## Decisions

Anything that would make a future reader ask *"why is it like this?"* becomes an ADR. They are
cheap to write, they are what stops an agent re-litigating settled questions, and collectively
they are the most interesting artefact this project will produce.

Open questions that are **not yet decided** are marked in-place with a `DEC-NNN` marker (see
`duel-rules.md`) so they are impossible to miss and impossible to accidentally answer in code.

## Metrics

Part of Product B is being honest about how well this works. Tracked in `tasks/BOARD.md` and
reported per epic:

```
tasks completed
accepted on the first review pass    %
average review iterations
lines of test code / lines of production code
tasks that had to be re-scoped mid-flight
manual edits by the human            %
```

These numbers are only worth anything if they are recorded when they are unflattering.
