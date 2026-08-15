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
   ┌────────────────────────────────────────────────────────┐
   │                                                        │
   ▼                                                        │
 1 pick the next startable task        status → in-progress │
   │                                                        │
 2 read: the task, the 2–3 docs it links, the files it      │
   │  names.  Nothing else.                                 │
   │                                                        │
 3 branch:  <type>/TASK-XXXXXX-slug   from develop          │
   │                                                        │
 4 implement + tests, in one pass                           │
   │                                                        │
 5 run the build and the tests locally — green before       │
   │  anything leaves the machine                           │
   │                                                        │
 6 update the ticket and BOARD.md in the same commit        │
   │                                                        │
 7 push, open a PR into develop        status → in-review   │
   │                                                        │
 8 /code-review          ◄── mandatory, never skipped       │
   │      │                                                 │
   │      └── findings? ──► fix, push, review again ──┐     │
   │                                                  │     │
 9 wait for CI to pass                          ◄─────┘     │
   │                                                        │
10 squash merge into develop, head branch deleted           │
   │                                                        │
11 ticket → done, BOARD.md updated                          │
   │                                                        │
12 discard the context entirely ────────────────────────────┘
```

**A task is not finished when the code is written. It is finished when its pull request is
reviewed, green, and merged into `develop`.** There is no other definition, and there is no
state in which a task is "done except for the PR". If the PR is not merged, the task is
`in-review`, and the next task does not start.

Step 12 matters as much as the rest. A task is complete when the agent could forget it ever
existed and the repository would still tell the whole story.

## The review gate

**Every pull request is reviewed before it merges. No exceptions, including for documentation,
including for one-line changes, including when the author is certain it is fine.**

Review effort is priced by risk ([`ADR-0007`](adr/ADR-0007-token-lean-agent-workflow.md)):

| `review:` | Mechanism | For |
| --- | --- | --- |
| `light` | reviewer subagent (Haiku) | types, parsing, config, wiring — most tickets |
| `standard` | reviewer subagent (Sonnet) | ordinary logic |
| `deep` | reviewer subagent + `/code-review low` | hand evaluation, betting rules, pot and showdown, card secrecy, chip conservation |

> **`/code-review high` is never run from a loop.** Measured on one documentation PR: 132 379
> tokens, 18 agents, session limit exhausted, and an inconclusive result because 14 agents died
> mid-run. It stays available as a deliberate, human-initiated act.

It is not a formality bolted on at the end — it is the only place where the work is examined by
something that did not write it.

That distinction is the whole point. The agent that wrote the code is the worst possible
reviewer of it: it already believes the approach is right, it has forgotten which parts it was
unsure about, and it will read what it meant rather than what it wrote. A reviewer starting
from the diff alone has none of that baggage.

The rule for handling findings:

| Finding | Action |
| --- | --- |
| Real defect in this diff | Fix it in this PR, push, review again |
| Real problem outside this ticket's scope | New ticket in `backlog`. Do **not** fix it here |
| Disagreement with the finding | Say why, in the PR. Do not silently ignore it |

A review that returns nothing is a normal outcome and does not mean the review was skippable.
A review that is skipped is a process failure, and — since these numbers feed the case study —
it gets recorded as one.

### Who merges

**Nobody. The PR merges itself** once every objective gate is green — `verify` commands exit 0,
the reviewer returns `pass`, and CI is green. Superseding
[`ADR-0006`](adr/ADR-0006-mandatory-review-gate.md); reasoning in
[`ADR-0007`](adr/ADR-0007-token-lean-agent-workflow.md).

Stated plainly, because it is the riskiest decision in this document: **no human reads the code
before it reaches `develop`.** A subtly wrong poker rule now merges silently rather than being
caught by someone reading the diff.

What carries that risk instead:

- the gates are objective, not opinions — an exit code, not a judgement,
- one squashed commit per ticket, so any merge is one `git revert` away,
- the tests point at exactly the failures a human skim would miss anyway: chip conservation,
  determinism, evaluator agreement against a brute-force oracle, and the assertion that folded
  hole cards appear in no event.

If a wrong rule ever does reach `develop`, the metrics in `tasks/BOARD.md` are what should show
it, and the correct response is to move correctness-critical tickets back behind a human merge.

## The roles

Five agents, each seeing as little as its job allows.

| Role | Model | Runs | Sees |
| --- | --- | --- | --- |
| **Architect** | Fable | when a technical `DEC-NNN` blocks something | the decision, the tickets it blocks, the ADRs it touches |
| **Product owner** | Opus, max effort | when a product `DEC-NNN` blocks something | `docs/vision.md`, the decision, what it blocks, the ADRs it touches |
| **Planner** | Opus, high effort | once per story | the story, its epic, 2–3 linked docs |
| **Coder** | Haiku (promoted to Sonnet on failure) | once per ticket | one ticket + the ≤5 files it names |
| **Reviewer** | Haiku, or Sonnet when `review: deep` | once per ticket | the diff + the ticket |

Expensive reasoning happens **once** — in the architect for a decision, in the planner for a
story — and is frozen into tickets that cheap agents consume. There is deliberately no mid-level
planner: it would pay a second cold start to re-derive what the first already knows.

The architect exists because open decisions were the largest source of stalled runs, and most of
them were never questions only a human could answer. It decides **how**.

The product owner exists because the remainder were still stalling runs. It decides **what the
product does** — but only ever by *applying* `docs/vision.md`, never by extending it. It runs at
**max effort**, the most expensive setting in this workflow, on the reasoning that a wrong product
decision is the one class of error nothing downstream catches: a bad ticket fails its verify, a bad
design fails review, and a bad product decision ships. What remains the human's is the vision
itself. See [Who answers a DEC](#who-answers-a-dec).

Coders originally ran **strictly one at a time**, on the reasoning that parallel agents over a
shared codebase cost more, conflict, and leave half-finished work in flight. That held until
coders were given **isolated git worktrees**, which removes the shared tree the argument rested
on; up to three now run at once when their tickets touch disjoint files. Merging stayed
sequential, and every conflict that has shown up since came from a shared working tree, not from
concurrency itself — twice from `git add -A` sweeping another agent's in-progress files into an
unrelated PR.

The driver is a **scheduler, not a participant**. It reads no source, writes no code, reviews no
diff, and keeps one line per finished ticket — so its context stays flat across an epic instead
of growing with it.

Full design and the measurements behind it:
[`ADR-0007`](adr/ADR-0007-token-lean-agent-workflow.md).

## Commands

| Command | Does |
| --- | --- |
| `/build-epic EPIC-01` | Plans each story, then runs its tickets to merged PRs. Answers technical and product decisions through the `architect` and `product-owner` agents as they arise; stops only for a decision that would change the vision. |
| `/next-ticket [ID]` | One ticket, end to end. |
| `/plan-story STORY-0102` | Opus planning pass over one story. Run before its tickets. |

## Hard limits

| Limit | Value |
| --- | --- |
| Changed lines | **XS ≤ 40, S ≤ 120.** There is no `M` |
| Files touched | **≤ 3** |
| Files readable | **≤ 5, named in the ticket** |
| Tickets in flight | **1** |
| Coder dispatches before a ticket is blocked | 3 |
| Undocumented decisions | 0 |

## Done is an exit code

Every ticket carries commands that settle whether it is finished:

```yaml
verify:
  - ./gradlew :poker-engine:test --tests '*CardTest'
```

Acceptance criteria map one-to-one onto named tests, and the ticket names those tests so the
coder writes the thing the gate runs. **A criterion that cannot be a passing test is not a
criterion** — it is a ticket that needs sharpening or splitting. "Handles edge cases correctly"
is the exact phrasing that makes a cheap model fail repeatedly and expensively.

## Model promotion

A ticket runs at its declared `tier`. Two `verify` failures → the driver rewrites it to
`tier: sonnet`, retries, and commits that change with the work. Three → `blocked`.

Guessing low is the cheaper mistake, and the record of which tickets Haiku could not handle is
real data for the case study rather than speculation.

## Permissions

`.claude/settings.json` is committed, and it is deliberately permissive: **245 allow rules, 68
deny rules, and `defaultMode: acceptEdits`** so that file edits never prompt at all.

The point is not convenience for its own sake. An agent that stops for approval forty times an
hour trains the human to approve without reading — and a human who has learned to click
*approve* reflexively is a worse safeguard than no prompt at all. Approval fatigue is a
security problem, not an ergonomic one. The response is to make the routine path silent and put
the real gate somewhere it cannot be clicked through.

**That gate is the pull request.** `main` and `develop` are protected, nothing merges without
review, and every change is one squashed, revertable commit. That is the control. The
permission list only decides how much friction there is on the way to it.

So the allow list covers essentially everything the work needs: git, the GitHub CLI, Gradle,
npm and the Node toolchain, Python, Docker, and the ordinary shell utilities. What matters more
is what is refused.

### Denied outright

| | |
| --- | --- |
| **History** | force push (including `--force-with-lease`), hard reset, `filter-branch`, `branch -D`, remote branch deletion |
| **Protected branches** | any push to `main` or `develop` |
| **Destruction** | `rm` in any form, `dd`, `mkfs`, `chown`, `docker rm`/`rmi`/`prune` |
| **Privilege** | `sudo`, `su`, `launchctl`, `crontab` |
| **Process control** | `kill`, `killall`, `pkill` |
| **Repository** | `gh repo delete`, `gh repo archive`, `gh api -X DELETE` |
| **Credentials** | `gh secret`, `gh auth token`, reads of `.env`, `secrets/`, `*.pem`, `*.key`, `~/.ssh`, `~/.aws`, `~/.config/gh` |
| **Self-modification** | editing `.claude/settings.json` |
| **Remote code** | `curl … \| sh`, `wget … \| bash` |
| **Publishing** | `npm publish`, `npm login` |

Deny always beats allow, so a broad allow such as `Bash(git push:*)` is still cut off from
`main` and `develop` by the denials above.

The last row of that list is the load-bearing one: **an agent cannot widen its own
permissions.** Every other rule is a judgement call that can be revisited; that one is not.

### The deliberate trade

`Bash(rm:*)` is denied, which means an agent cannot clean up its own temporary files without
asking. That is annoying perhaps once a week, and it is the correct side of the trade — a
misfired `rm -rf` costs far more than the interruptions it prevents.

Conversely `Bash(curl:*)`, `Bash(npx:*)` and `Bash(python3:*)` are all allowed, and each of
them can run arbitrary code. They are listed here rather than buried, because a permission you
have forgotten about is one you have not really granted.

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

### Who answers a DEC

Decisions are routed by kind, not by difficulty.

| Kind | Answered by | Examples |
| --- | --- | --- |
| **Technical** | the `architect` agent, which writes the ADR | where a type lives, which of two designs, schema shape, wire format, concurrency and failure semantics |
| **Product** | the `product-owner` agent, which writes the ADR | what a player sees, what a duel *is*, how a feature behaves, which risks *inside the software* are acceptable to ship with |
| **Vision** | the human, and nothing else | money in any form; adding to or subtracting from the vision's *"What it is" / "What it is not"*; reordering the roadmap; risk with consequences **outside** the software; anything about Product B |

The test for "technical": **would two competent engineers with the same requirements land in the
same place?** If yes, it is the architect's.

The test for "product": **does `docs/vision.md`, plus the shipped ADRs and `duel-rules.md`, already
contain the answer?** If applying what is written lands somewhere defensible, it is the product
owner's — and its ADR must cite the sentence that licensed it. If answering would *add a commitment
the vision does not make*, it is the human's, and no amount of reasoning substitutes, because there
is nothing to reason from.

The product owner is not trusted because it is clever. It is trusted because it is **required to
refuse**: returning `FOR THE HUMAN:` with no ADR is a successful run, and that escape hatch is what
makes delegating the rest safe. An agent that answered everything would invent the product.

A question with more than one half is that many `DEC-NNN`s. Split it and route each part.

This exists because open decisions were stalling runs. `DEC-001`, `DEC-002` and `DEC-007` through
`DEC-009` sat open across four epics while the tickets they blocked waited for a human who was not
at the keyboard — and most of them were never product questions at all. The seven product decisions
answered on 2026-08-15 were the remainder: all seven turned out to be derivable from the vision,
which is the observation the `product-owner` role is built on.

### Landing a decision

An ADR is not an answer until it is **merged**. A `DEC-NNN` answered in a branch blocks exactly as
hard as one never asked.

A PR whose whole diff is an ADR plus its register rows — no code, no ticket, no story, no epic, no
change to `docs/vision.md` — is **merged by the driver without asking**. Standing authorisation,
given deliberately: waiting for a human to click merge reintroduces the stall the two decision
agents exist to remove. The bar is unchanged — CI green, a `## Consequences` section that names a
cost, and no register still listing the id as open.

**Every register.** A `DEC-NNN` can appear in four places: `docs/adr/README.md`, `tasks/BOARD.md`,
and the `## Open decisions` table of any epic under `tasks/epics/`. The PR that answers a decision
strikes every row it answers, in the same PR. A strike deferred to somebody else's next PR is a
strike nobody makes — that has happened here, and `DEC-035` sat listed open-and-answered
simultaneously for weeks as a result.

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
