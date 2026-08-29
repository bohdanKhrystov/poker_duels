---
id: EPIC-12
title: Quality and defect repair — the cycle that tests, triages and stops
type: epic
status: blocked
labels: [process, meta, qa]
---

## Goal

A repeatable cycle that finds defects, prioritises them, fixes them through the existing ticket
workflow, and **provably stops**.

Every gate in this repository today answers one question: *did this diff do what its ticket said?*
Nothing answers *does the product work?* `ADR-0088` measured that gap precisely and accepted it —
four things that fail green, discovered "at a release, not a pull request". This epic is what turns
"at a release" into a scheduled act with a named owner.

The cycle is five steps:

```
qa (scope)  →  qa-manager (triage)  →  build-epic (repair)  →  qa (retest)  →  qa (smoke)
     ↑                                                                             │
     └────────────────────── only if the manager says PROCEED ─────────────────────┘
```

## Why now

`EPIC-01` through `EPIC-06` are closed. 915 of 919 tickets are `done`. The product runs: a duel was
played end to end through two browser storage partitions on 2026-08-29, and the four gaps
`ADR-0088` named were measured rather than assumed. What is missing is not features — it is any
standing answer to *"is it still working?"* between now and whenever a human next looks.

The human's instruction on 2026-08-29 was explicit about the failure mode to design against:

> This cycle should increase product quality but also we do not want to run infinitely or get
> stuck (each time report more and more bugs). So the manager in the middle should prevent such
> a scenario.

That sentence is the epic's hardest requirement, and §Termination below is the answer to it.

## Scope

- A `qa` agent that takes a **scope** — `epic <ID>`, `smoke`, or `regression` — brings the stack
  up, executes the test catalogue for that scope, and reports findings. It fixes nothing and files
  nothing.
- A `qa-manager` agent that takes one QA report plus the round ledger, dedupes it against what is
  already known, assigns severity, and decides what enters this round's fix set — bounded by
  §Termination's budgets. It is the only thing that writes bug tickets.
- A `qa-cycle` skill that runs the loop and enforces the stopping rules.
- A **test catalogue** — the core suite for the v0.1 spine and a smoke suite, plus a per-epic
  template filled in as each epic is tested.
- **Stack lifecycle** that uses no denied verb: `docker-compose` for the database, CDP
  `Browser.close` for browsers, harness task-stop for the server and dev server. `kill`, `pkill`
  and `killall` are denied in `settings.json` and deny beats allow, so no local override can
  reach them. This is a constraint on the design, not a wish.

## Out of scope

- **Any change to CI.** `build.yml` keeps its two jobs. This cycle is run by a human's command,
  never by a pull request.
- **Any browser dependency in `web-client/package.json`.** `ADR-0088` §1 forbids it by name and
  `DEC-082` does not reopen that clause — the QA harness drives Chrome over the DevTools protocol
  from the agent, and the client's dependency list is untouched.
- Fixing the defects. Repair runs as `build-epic` over ordinary tickets, which is the whole reason
  bugs are filed as `task`s: the linter knows `epic`/`story`/`task` and nothing else, so a bug that
  is a task needs **no change to a merged gate** and `build-epic` runs it unmodified.
- Performance, load and security testing.

## Open decisions

| ID | Question | Where | Due |
| --- | --- | --- | --- |
| `DEC-082` | **The architect's** — may a browser-driving QA harness live in this repository, given [`ADR-0088`](../../docs/adr/ADR-0088-the-two-browser-proof-is-a-written-hand-check.md) §1 refuses one by name? §1 says *"No browser drives this client, here or in CI"* and forbids a browser runner; §5 prices reversal at *"a superseding ADR plus one story"* and states the reversibility **is** the reason for the choice. What is asked here is narrower than the thing §1 refused: no CI job, no `package.json` dependency, no pull-request gate — an agent-run harness a human invokes. Settle whether that is inside §1's refusal or outside it; if inside, what supersedes and in what terms; if outside, say so in a merged record so the next reader is not left reinterpreting a merged ADR from prose. **Blocks this entire epic** | [`ADR-0088`](../../docs/adr/ADR-0088-the-two-browser-proof-is-a-written-hand-check.md) §1 and §5 | before any story here is split |

## Stories

| ID | Title | Status |
| --- | --- | --- |
| `STORY-1201` | The QA harness — two agents, one skill, one catalogue | blocked on `DEC-082` |
| `STORY-1202`+ | One story per QA round; the round number lives in the story, not the id | not written |

Rounds are numbered in the story body rather than encoded in the id, because a round is created
when it is run and the ids stay sequential without arithmetic.

## Termination

**This is the epic's load-bearing section.** A quality loop that never ends is worse than no loop:
it consumes the budget that would have shipped something, and it trains everyone to ignore its
reports. Five rules, all enforced by `qa-manager`, and all of them measurable rather than
judgemental.

1. **The round's bug set is frozen at triage.** Only defects in round *N*'s QA report are eligible
   for repair in round *N*. Anything found during retest belongs to round *N+1*'s report and
   cannot extend round *N*. Without this rule the loop has no fixed point, because retest always
   finds something.
2. **Only `blocker` and `high` are repaired in-cycle.** `medium` and `low` are filed to the
   backlog and never scheduled by this cycle. This bounds the work per round to the severities
   that justify interrupting the roadmap.
3. **A fix set is at most eight tickets.** A round that would exceed it takes the eight
   highest-priority and files the rest to the backlog. A round is a bounded unit of work, not a
   queue drain.
4. **Convergence.** Let `B(N)` be the count of `blocker` + `high` in round *N*'s report. If
   `B(N) >= B(N-1)` the cycle **stops** and reports non-convergence. This is the direct answer to
   *"each time report more and more bugs"*: the loop is permitted to continue only while it is
   demonstrably winning.
5. **At most three rounds per invocation**, whatever else is true.

**Exit states**, all terminal and all reported:

| State | Condition |
| --- | --- |
| `PASS` | a round's report has zero `blocker` and zero `high` |
| `STOP_BUDGET` | three rounds ran |
| `STOP_DIVERGING` | rule 4 tripped |
| `STOP_BLOCKED` | a `DEC` was raised that only the human can answer |
| `STOP_INFRA` | the stack could not be brought up, twice |

A cycle that ends in any state other than `PASS` is a **successful run** — it stops and says why.
An agent that keeps going because it has not finished is the failure this section exists to
prevent.

## Definition of done

- [ ] `DEC-082` is answered by a merged ADR.
- [ ] `STORY-1201` is `done`: the two agents, the skill and the catalogue exist, and the skill's
      stack lifecycle uses no denied verb.
- [ ] One full cycle has run end to end and terminated in a named exit state, with its round
      ledger committed.
- [ ] The cycle demonstrably stops: a round whose `B(N)` did not decrease ends the run, proved by
      a recorded round rather than by argument.
- [ ] No file under `web-client/` gained a browser-driver dependency.
- [ ] `.github/workflows/build.yml` still has two jobs.

## Metrics

Filled in when the epic closes; feeds the Product B case study.

| | |
| --- | --- |
| Rounds run | |
| Defects found / repaired / deferred | |
| Exit state distribution | |
| Escaped defects — found by a human after a `PASS` | |
