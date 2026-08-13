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

Reading source to settle a suspicious review is occasionally the right call, but treat it as a
debt: it is what fills the scheduler's context and forces a compaction mid-run. Prefer sending
the specific question back to the reviewer, which costs a subagent's context instead of yours.

Keep a running ledger, nothing more:

```
TASK-010101  merged   haiku    1 attempt
TASK-010201  merged   haiku    1 attempt   (batch A)
TASK-010202  merged   haiku    1 attempt   (batch A)
TASK-010203  merged   sonnet   2 attempts  (promoted)
TASK-010204  blocked  —        DEC-002
```

## Report on landing, not on progress

Narration is the scheduler's largest avoidable cost, because it is paid on every ticket. Speak
when a ticket **lands**, when one **blocks**, and when the run **ends**. "Dispatched X", "checks
still pending", "waiting on review" tell the human nothing the ledger will not, and they are
written dozens of times per epic.

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
5. story done → report it in one line → back to 2
6. no stories left → final report
```

## At a story boundary, report and continue

When a story's last ticket merges, say so in one line and go straight on to the next story:

```
STORY-0104 done — 23 tickets merged. Planning STORY-0105.
```

**Do not stop.** Not for approval, not for a context reset, not to summarise. The run continues
while there are startable tickets and budget to work them; the only stops are the blockers listed
under "Batching decisions".

A story does leave the scheduler carrying a lot it no longer needs — dead ledgers, resolved
conflicts, review arguments that already landed — and a `/clear` between stories would reclaim
all of it. But `/clear` is a client-side command that cannot be invoked from inside the run, so
waiting for one means stalling until a human appears. **A stalled run is worse than an expensive
one.** If the human is present and wants the reset, they can `/clear` and re-issue
`/build-epic <EPIC-ID>` at any point; the board is what carries the state, so nothing is lost.

Do not simulate the reset either — summarising your own context or "starting fresh" in place
reclaims nothing and costs a turn.

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
   When the rebase pulled in nothing the ticket's files actually depend on, the PR's own CI run
   is that gate; do not run it locally as well. Re-run locally when the rebase brought in code
   this ticket touches or builds on.
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
ticket `blocked`, and carry on with the next startable ticket.

Then route it by kind — this is the difference between a run that stalls and one that does not:

- **Technical** — where a type lives, which of two designs, schema shape, wire format,
  concurrency and failure semantics. Dispatch the **`architect`** agent (Fable) to answer it and
  write the ADR. Do this *while* the run continues; it is not a reason to wait. The test is
  whether two competent engineers with the same requirements would land in the same place.
- **Product** — what a player sees, what a duel *is*, what a coin is worth, which risks are
  acceptable to ship with. Collect these and present them together at the end. Only the human can
  answer them, and no amount of technical reasoning substitutes.

A question with both halves is two `DEC-NNN`s. Split it and route each half. Never ask the human
a technical question because it is hard, and never let the architect answer a product one because
it is blocking — the second failure is far more expensive, because it reads as settled.

The design goal is: one command in, **one batch of product questions out**.

Stop the run entirely only if:

- **every** remaining ticket is blocked on a *product* decision, or
- a product decision blocks the rest of the epic (what the thing fundamentally is), or
- three consecutive tickets fail — something systemic is wrong and continuing will burn budget
  producing more of it.

## Model tiers

- **coder** — the ticket's `tier`. Promote only under the retry policy.
- **reviewer** — `haiku` for `review: light` and `review: standard`; `sonnet` only for
  `review: deep`. A shallow review is fixed by asking a sharper question — naming the specific
  defect to hunt for — not by buying a bigger model on every ticket.
- **architect** — Fable, always. Runs on demand, when a technical `DEC-NNN` blocks something. It
  answers the decision and writes the ADR; the planner then writes tickets from that ADR.
- **planner** — Opus, always. It runs once per story, and how precisely it specifies a ticket
  decides whether the coder needs one dispatch or three. This is the last place to economise:
  a well-planned story lands in one dispatch per ticket, a vague one burns dispatches and
  promotions.

## When a subagent dies

Connection drops, stalls and machine sleeps are infrastructure, not verdicts — they are not
review failures and do not count against the retry policy. Before re-dispatching, **look at the
worktree**: the work is frequently committed already, and then it needs only verifying and
reviewing. If the agent can be resumed, resume it with `SendMessage`; a fresh dispatch pays full
context re-entry to rediscover what the dead one already knew.

## Budget awareness

Track roughly what the run is consuming. If a single ticket takes more than three coder
dispatches, stop it and mark it blocked rather than letting it grind — one pathological ticket
can eat an entire session's budget.

Never invoke `/code-review high` or any multi-agent review workflow from inside this loop.

## Arm the resume before you need it

**You cannot schedule anything once the tokens are gone.** A usage limit does not politely warn
you and let you tidy up — it terminates the turn, and every turn after it, until the reset. Any
plan that begins "when the limit hits, schedule a resume" is a plan that never runs.

So arm it at the **start** of the run, while the budget is healthy:

```
CronCreate(cron: "23 */2 * * *", recurring: true, prompt: "<guarded resume, see below>")
```

Recurring rather than one-shot, because the reset time is not knowable in advance — it is only
ever stated in the error you will not be alive to read. A periodic check costs one cheap turn
when the run is healthy and recovers it when it is not.

The prompt must **guard against firing into a healthy run**, since jobs fire whenever the REPL is
idle and a working scheduler is idle between every ticket. Make the first thing it does a
liveness check, and make the do-nothing path cheap and explicit:

- last commit on `develop` under ~25 minutes old → reply `still running`, stop
- nothing startable and nothing in flight → reply `nothing startable`, stop
- otherwise → resume the loop

Write the resume half so it can restart cold — epic id, repo path, and the instruction to inspect
`git status` and the current branch before dispatching anything. A feature branch often already
holds committed, unreviewed work, and the cheap recovery is to review it, not to re-dispatch a
coder over the top of it.

**Delete the job when the epic lands** (`CronDelete`), so a finished run stops waking up.

A usage limit is **infrastructure, not a verdict** — the same class as a dropped connection. It
does not count against the retry policy. If you are still alive when one hits (it interrupted a
subagent rather than you), land whatever is already reviewed, say so in one line, and stop. Do
not write the final report: the epic is paused, not finished.

Two constraints bound how much this can be trusted:

- Cron jobs are **session-only**. Nothing is written to disk, and the job dies with the CLI
  session. It survives a usage limit, which leaves the process running; it does not survive
  quitting Claude Code or a reboot. When the session may not outlive the outage, hand the human
  the resume command instead of scheduling a job they will never see fire.
- Jobs fire only while the REPL is **idle**, which is why the liveness guard above is mandatory
  rather than decorative.

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
