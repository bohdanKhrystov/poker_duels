---
id: EPIC-12
title: Quality and defect repair — the cycle that tests, triages and stops
type: epic
status: ready
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

The first three are not this epic's self-restraint. They are the **standing conditions**
[`ADR-0089`](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
§2 attaches to the permission itself: any one of them failing withdraws the licence and returns the
question as a new `DEC`.

- **Any change to CI.** `build.yml` keeps its two jobs. A cycle is started by a human's command —
  not a pull request, not a merge, not a cron, not another skill invoking it as a step
  (`ADR-0089` §2b).
- **Any browser dependency in `web-client/package.json`**, or in any other module's dependency set.
  `ADR-0088` §1 forbids it by name and `ADR-0089` leaves that sentence byte-unchanged — the harness
  drives Chrome over the DevTools protocol using only Node built-ins, and no dependency list is
  touched (`ADR-0089` §2a).
- **Any claim of coverage.** A round's output is a dated record. Neither it nor `docs/test-plan.md`
  may be cited in an epic's `Metrics`, a Definition of done or a ticket's `verify:` (`ADR-0089` §2c).
- **Writing application state to reach a screen.** The driver reads anything and writes nothing but
  `pd.roomCode`; a case that seeds the store, a socket frame or a row is a client asserting a game
  fact, which `ADR-0002` forbids (`ADR-0089` §3).
- Fixing the defects. Repair runs as `build-epic` over ordinary tickets, which is the whole reason
  bugs are filed as `task`s: the linter knows `epic`/`story`/`task` and nothing else, so a bug that
  is a task needs **no change to a merged gate** and `build-epic` runs it unmodified.
- Performance, load and security testing.

## Open decisions

**None.** The only one, `DEC-082`, is answered below and unblocked this epic.

### Answered since this epic was written

| ID | Answered by | What it means here |
| --- | --- | --- |
| `DEC-082` | [ADR-0089](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md) | **A browser drives this client for a QA round, never for a gate.** The harness may live here. It **was** inside `ADR-0088` §1's words — *"here or in CI"* says *here* — so §1's **heading** is amended in the open and nothing else is: `EPIC-03` still ships no fifteenth story, §2's eleven-step hand-check is still the proof of record, `build.yml` keeps its two jobs, no browser dependency enters `web-client/package.json`, and `ADR-0032` §4's *"still jsdom, still no network"* still holds of every test suite. **Four things change for this epic.** (1) The three constraints in *Out of scope* are no longer this epic's self-restraint but **standing conditions on the permission** — no dependency, no gate, no coverage claim — and any one failing returns the question as a new `DEC`. (2) §3: the driver **reads anything and writes nothing** but `pd.roomCode`; no case may seed store, socket or database state to reach a screen, because that is a client asserting a game fact (`ADR-0002`). (3) §4 adds the rule §Termination lacked — a failing case that does **not reproduce by hand** is a **harness** defect, filed against this epic, **excluded from `B(N)`**, and never repaired in production code. (4) §5: a case quoting player-facing text cites the module that owns the literal. A `PASS` is a dated record of one run on one machine at one commit, and `dist/` stays unproven |

## Stories

| ID | Title | Status |
| --- | --- | --- |
| `STORY-1201` | The QA harness — two agents, one skill, one catalogue | ready to split |
| `STORY-1202` | Round 1 — the smoke suite passed, and one case did not run as written | done |
| `STORY-1203`+ | One story per QA round; the round number lives in the story, not the id | not written |

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
6. **A failure that does not reproduce by hand is a harness defect, and never enters `B(N)`.**
   Added by [`ADR-0089`](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
   §4. Before `qa-manager` may file a `blocker` or `high`, the failing case must reproduce by hand —
   by the matching step of `ADR-0088` §2 where one exists, or by a stated sequence of player actions
   where it does not. It reproduces → a product defect, counted in `B(N)`. It does not → a **harness**
   defect: filed against this epic, repaired in `scripts/qa/` or `docs/test-plan.md`, excluded from
   `B(N)`, and **no production code may be changed to make it pass**. Rules 1–5 bound how much work
   a round does; this one bounds whether the work is real. Without it a stale catalogue reads as a
   product getting worse and trips rule 4 on a healthy product, or step 4 of the loop merges a diff
   to satisfy a string the client moved.

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

- [x] `DEC-082` is answered by a merged ADR —
      [`ADR-0089`](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md),
      2026-08-29.
- [ ] A harness defect and a product defect have been told apart at least once, on the record: a
      failing case that did not reproduce by hand was filed against this epic and kept out of
      `B(N)` (§Termination rule 6). Until that happens the rule is untested prose.
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
