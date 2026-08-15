---
name: build-epic
description: Work one or more epics unattended, in the order given — plan each story, then run its tickets to merged PRs, up to three at a time in isolated worktrees when they touch disjoint files, answering technical and product decisions through the architect and product-owner agents as they arise, carrying straight on to the next epic and stopping only for decisions that would change the product vision itself. Use when the user names a goal like "implement the poker engine", or a list like "EPIC-03 EPIC-04 EPIC-05", rather than a single ticket.
---

# Build an epic

Takes **one or more epic IDs**, or a goal in plain words (*"implement the poker engine"* → `EPIC-01`).
A list — `EPIC-03 EPIC-04 EPIC-05` — is worked **in the order given**, and the order is the human's
statement of dependency: a later epic may build on an earlier one, never the reverse.

This is the entry point for **"here is the goal, go"**. It runs until every epic named is done, or
until nothing anywhere in the list is startable.

**A blocked epic does not end the run — it ends that epic.** The whole point of taking a list is
that `EPIC-04` waiting on a product decision must not stop `EPIC-05` from being worked. Record what
blocked, move to the next epic, and collect the questions for one batch at the end.

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
2. for each epic in the list, in order:
3.     pick the epic's first story that is not `done`
4.     if that story's tickets lack `schema: 2`:
            → /plan-story <STORY-ID>          (Opus planner, once per story)
5.     while the story has a startable ticket:
            → select a compatible batch of 1–3 tickets   (see "Batching tickets")
            → run each in its own git worktree, concurrently
            → land them ONE AT A TIME (see "Landing")
            → append one line per ticket to the ledger
            → if blocked: record it, continue to the next startable ticket
6.     story done → report it in one line → back to 3
7.     no stories left, or nothing startable → one-line epic report → back to 2
8. no epics left → final report
```

Re-read `BOARD.md` once when an epic ends, not once per epic at the start. Earlier epics move the
board underneath the later ones — a story you judged unstartable an hour ago may be startable now
because its blocker landed.

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

## At an epic boundary, report and continue

Exactly as at a story boundary, one level up. When an epic's last story lands, say so in one line
and start the next epic in the list:

```
EPIC-03 done — 12 stories, 147 tickets merged. Starting EPIC-04.
```

Say it the same way when an epic ends **without** finishing, because that is the case a list exists
to survive:

```
EPIC-04 parked — 0 of 5 stories; every story gated on DEC-025. Starting EPIC-05.
```

An epic that had to be written first says so, because it is the one case where the run produced
scope rather than code:

```
EPIC-05 written — 4 stories, 2 decisions raised. Planning STORY-0501.
```

**Do not stop between epics**, and do not ask whether to go on. The list was the authorisation; a
run that pauses for permission it was already given is a stalled run with extra steps.

Three things make an epic *end* rather than *continue*:

- every story in it is `done` — finished
- nothing in it is startable, and what blocks it is a decision **the product owner escalated to the
  human** — parked

A product decision on its own no longer parks an epic: it is dispatched to the `product-owner`
agent and the epic continues once the ADR merges. Only a question the product owner *refuses* —
one that would change the vision rather than apply it — parks anything.

Only the first is success. Both are reasons to go to the next epic, not reasons to stop.

**"Not written" is not one of them.** `tasks/BOARD.md` states the policy: *epics are written when
the one before them is close to done, because writing them earlier means rewriting them.* An
unwritten epic reaching the front of the list is therefore the moment the workflow schedules it to
be written — not a gap, and not a reason to skip it. Reaching it and moving on would defeat the
policy exactly when it comes due.

So write it, then work it:

```
→ /plan-story <EPIC-ID>      (Opus planner, splitting an epic into stories)
```

The planner does this one level up from its usual job: same agent, same model, epic → stories
instead of story → tickets. It writes the epic file from `docs/vision.md` and the ADRs, and the
stories under it, and then the loop proceeds as normal — the first story gets split into tickets and
the run continues.

The guard is the one that governs everything else here. **An epic's scope is mostly a product
question** — what a coin is worth, what a season is, what the thing fundamentally is. The planner
writes only what the vision and the ADRs already settle, and registers the rest as `DEC-NNN`s. If
what is unsettled is the epic's *shape*, the epic parks on that decision like any other, and the run
moves on. What it must never do is invent the answer: an epic written around a guessed product
decision reads as settled and shapes every story under it.

An epic whose stories depend on an epic you just parked is very likely unstartable too — check its
first story rather than assuming either way. If it is, park it in the same breath and keep going;
the cost of checking is one board read.

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

## Merging

**Every PR in this run is yours to merge**, on its objective gates and without asking — code,
tickets, stories, epics, ADRs, design, process, agent definitions, permission files. There is no
class that is *docs-only-and-therefore-safe* or *process-and-therefore-sensitive*. There is only
green and not green.

Do not invent a category that needs human sign-off. That is `ADR-0006` returning one
reasonable-sounding exception at a time, and each one costs the run a stall.

**The single exception: `docs/vision.md`.** A PR that changes it waits for the human, however
green. The `product-owner` agent's boundary is *"apply the vision, escalate anything that would
change it"* — if the vision can move without the human, that boundary is decoration and the agent's
authority becomes self-granting. This one carve-out is what makes everything else safe to delegate.

The gates, unchanged by the authorisation:

1. CI green — `lint backlog`, `client`, and `check`.
2. The reviewer returned `pass`, and the ticket's `verify` exits 0.
3. One `BOARD.md`-touching PR in flight at a time.

A **decision** PR carries two more:

4. `grep -rn "DEC-0NN" docs/ tasks/` returns no row still listing the id as **open**, in any
   register: `docs/adr/README.md`, `tasks/BOARD.md`, and every `## Open decisions` table under
   `tasks/epics/`.
5. The ADR's `## Consequences` names a cost. One that lists none was a preference, and it goes back
   to the agent rather than into `develop`.

Merge as a **bare command**, never chained:

```
gh pr merge <n> --squash --delete-branch
```

`Bash(gh pr merge:*)` is allowed in `.claude/settings.json`, but the allowlist matches a command
*prefix* — `gh pr merge … && git checkout develop` does not match it, is sent to the classifier,
and is refused. Merge first; `git checkout develop` as a separate call.

**Say so when you change your own rules.** A merged PR touching `.claude/agents/*`,
`.claude/skills/*` or `.claude/settings*.json` gets its own line in the final report. It merges like
anything else; it is never silent. Product B is a legible process log, and *"the agent changed its
own operating rules"* is the line a reader must be able to find rather than reconstruct.

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
  concurrency and failure semantics. Dispatch the **`architect`** agent to answer it and
  write the ADR. Do this *while* the run continues; it is not a reason to wait. The test is
  whether two competent engineers with the same requirements would land in the same place.
- **Product** — what a player sees, what a duel *is*, what a coin is worth, which risks inside the
  software are acceptable to ship with. Dispatch the **`product-owner`** agent (Opus, max effort)
  to answer it and write the ADR, exactly as you would the architect, and keep the run moving while
  it works. It derives the answer from `docs/vision.md` and escalates to the human anything that
  would *change* the vision rather than apply it — so you do not have to pre-judge the line.
- **The human's, and only the human's** — money in any form, adding to or subtracting from the
  vision's *"What it is" / "What it is not"*, reordering the roadmap, and risk with consequences
  outside the software. The product owner returns these with `FOR THE HUMAN:` set; collect those
  and present them together at the end. If a question is plainly one of these, skip the agent run
  and collect it directly.

A question with both halves is two `DEC-NNN`s. Split it and route each half. Never ask the human a
technical question because it is hard, and never let the architect answer a product one because it
is blocking — the second failure is far more expensive, because it reads as settled.

The design goal is: one command in, **one small batch of vision-level questions out** — and often
none at all.

**Land the decision before you use it.** A `DEC-NNN` is answered when the ADR is *merged*, not when
the agent reports. Open the PR, wait for CI, and merge it yourself — like every other PR in the run
(see [Merging](#merging)). A ticket unblocked against an unmerged ADR is a ticket built on a file
that may still change.

Leave the **current epic** and start the next one when:

- **every** remaining ticket in it is blocked on a decision the product owner escalated to the
  human, or
- such a decision blocks the rest of it (what the thing fundamentally is).

An epic blocked only on *answerable* product decisions is not parked — it is waiting on an agent
run you should have already dispatched.

Neither ends the run while an epic remains in the list. Park it, keep the questions, carry on.

Stop the **whole run** only if:

- every epic in the list is done or parked — there is nothing left to work, or
- three consecutive tickets fail — something systemic is wrong, and continuing will burn budget
  producing more of it. This one stops everything, not just the epic: a systemic failure follows
  you into the next epic, and three more failures there prove nothing the first three did not.

## Model tiers

- **coder** — the ticket's `tier`. Promote only under the retry policy.
- **reviewer** — `haiku` for `review: light` and `review: standard`; `sonnet` only for
  `review: deep`. A shallow review is fixed by asking a sharper question — naming the specific
  defect to hunt for — not by buying a bigger model on every ticket.
- **architect** — whatever `.claude/agents/architect.md` declares (Opus, max effort); do not
  override it per dispatch. It is pinned there rather than here because it gets switched when a
  model is unavailable, and a tier hardcoded in two places drifts in one of them.

  **Fable is preferred when the account has capacity**, and frontmatter cannot say so — `model`
  takes a single value, with no list and no fallback syntax. So: override with `model: fable` only
  when capacity is *known* to exist (the human said so, or a Fable dispatch already succeeded this
  session), never speculatively. On any model-availability or rate-limit failure, re-dispatch with
  **no override** and carry on — the pinned value always works, so falling back is always safe, and
  a run must never stall on a busy model. The same applies to `product-owner`. Runs on demand, when a technical `DEC-NNN`
  blocks something. It answers the decision and writes the ADR; the planner then writes tickets
  from that ADR.
- **product-owner** — whatever `.claude/agents/product-owner.md` declares (Opus, max effort); do not
  override it per dispatch, for the same reason. Runs on demand, when a *product* `DEC-NNN` blocks
  something. Max effort is deliberate and is not an economy to make: a wrong product decision is
  discovered only when the product turns out to be the wrong one, and by then every story beneath it
  was built on it.
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

Write the resume half so it can restart cold — **the full remaining epic list**, repo path, and the
instruction to inspect `git status` and the current branch before dispatching anything. A feature
branch often already holds committed, unreviewed work, and the cheap recovery is to review it, not
to re-dispatch a coder over the top of it.

Carrying the *whole* list matters: a resume that names only the epic in flight silently drops every
epic after it, and the run ends early looking like it succeeded. Rewrite the job's prompt as each
epic ends, so the list it carries is always what is left.

**Delete the job when the last epic in the list ends** (`CronDelete`), not when the first one does —
a run with epics remaining still needs its resume armed.

A usage limit is **infrastructure, not a verdict** — the same class as a dropped connection. It
does not count against the retry policy. If you are still alive when one hits (it interrupted a
subagent rather than you), land whatever is already reviewed, say so in one line, and stop. Do
not write the final report: the run is paused, not finished, and naming the epics still unstarted
is the one thing the resume needs from you.

Two constraints bound how much this can be trusted:

- Cron jobs are **session-only**. Nothing is written to disk, and the job dies with the CLI
  session. It survives a usage limit, which leaves the process running; it does not survive
  quitting Claude Code or a reboot. When the session may not outlive the outage, hand the human
  the resume command instead of scheduling a job they will never see fire.
- Jobs fire only while the REPL is **idle**, which is why the liveness guard above is mandatory
  rather than decorative.

## Report while you run

The human cannot see the ledger. From outside, *working through ticket 40 of 60* and *dead since
11pm* look identical, and a run designed never to stop is a run that is silent for hours by
construction. So it reports itself, over Telegram, through `scripts/notify/notify.py`
(`ADR-0042`, `docs/notifications.md`).

This section sits beside *Arm the resume before you need it* because it shares that section's
cause: **both are set up while the budget is healthy, or not at all.**

At the **start of the run**, alongside the resume job:

```
notify.py state --epic <first epic> --epics <the whole remaining list> --cron-armed armed|not-armed
CronCreate(cron: "0 */2 * * *", recurring: true, prompt: "run: python3 scripts/notify/notify.py heartbeat")
```

Pass `--cron-armed armed` **only once `CronCreate` has returned successfully** — never
optimistically. The whole value of that flag is that the human can trust it.

Then, the four reports:

- **heartbeat** — the cron sends it every two hours. Call `notify.py heartbeat --force` yourself
  at each **epic boundary**, so a landing is not held until the clock comes round. It
  deduplicates on the last-sent stamp, so a forced report and the cron cannot both fire.
- **stop** — carried by the `Stop` hook, which is silent unless the run state names a
  `current_epic`. Keep `--epic` current as epics change, and run `notify.py state --clear` when
  the list is finished, so the hook falls silent with it.
- **blocked** — `notify.py blocked --decision DEC-NNN --question "…"` when an epic parks on a
  decision the product owner escalated. Send it when you park, not at the final report: the
  human can answer it while the run works the next epic.
- **budget** — `notify.py budget --cron-armed armed|not-armed|unknown` **the moment a usage
  warning appears, before doing anything else.** A usage limit ends the turn and every turn
  after it, so a plan that reports after the limit never reports. This is the one report whose
  ordering is load-bearing: send it first, then land what is already reviewed.

Reporting is not narration. The prohibition above still holds — no message per dispatch, per
merge or per ticket. A heartbeat is a digest, and the notifier is what makes staying quiet in the
terminal safe.

Failures here are ignorable by design: every command exits 0 whether or not it sent. **Never let
a notification failure stop, retry or slow the run.**

## Final report

Written **once, at the end of the whole list** — not once per epic. Epics report themselves in one
line as they end; this is the run's report.

```
RUN: <epic ids, in the order given>

EPIC-03 — Web client            done    12 stories, 147 tickets
EPIC-04 — Identity and profiles parked  0 of 5 stories — DEC-025
EPIC-05 — Ranking and coins     written, then parked  4 stories — DEC-036

MERGED:  <n> tickets
BLOCKED: <n> tickets
PROMOTED TO SONNET: <ids>

DECISIONS ANSWERED AND LANDED
  DEC-00N — <ADR-NNNN, one line>  (architect | product owner)

OWN RULES CHANGED
  <PR> — <what changed in .claude/agents, .claude/skills or .claude/settings*.json>

DECISIONS NEEDED FROM THE HUMAN
  DEC-00N — <question>  → blocks <epic or ticket ids>

BLOCKED, NOT ON A DECISION
  <ticket> — <why>

NEXT: <what unblocks the most, first>
```

Every epic named gets a line, including the ones never started, and each says how it ended. An epic
missing from the report is indistinguishable from one silently skipped — and an epic the run
**wrote** says so, because that is scope no human reviewed, and it should be the first thing they
look at.

`DECISIONS ANSWERED AND LANDED` exists so the human can audit what was decided in their absence —
it is the section most worth reading, because it is scope nobody reviewed. Name the agent that
answered each one.

`OWN RULES CHANGED` is the other one. Omit the heading when nothing under `.claude/` changed; never
omit it when something did. A run that quietly rewrote the rules it runs under is the one entry a
reader of this process log must not have to reconstruct from the git history.

Order `DECISIONS NEEDED FROM THE HUMAN` by how much each unblocks — the question gating a whole epic
goes above one gating a ticket. The human answers them in the order you print them, so printing them
in arrival order wastes the ordering. This list should now be **short or empty**: anything the
`product-owner` agent could settle from `docs/vision.md` has already been settled and landed.

Then stop. The list is finished; do not add epics to it that the human did not name.
