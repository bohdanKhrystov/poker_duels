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

- **Any change to CI, and any second caller.** `build.yml` keeps its two jobs. A cycle is started by
  **the human's own message and nothing else** — not a pull request, not a merge, not a cron, not a
  hook, and not another skill invoking it as a step, whatever started that skill
  ([`ADR-0089`](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
  §2b, heading and sentence amended by
  [`ADR-0090`](../../docs/adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md) §1).
  **Writing catalogue cases and running them are two commands.** Authoring is licensed as its own
  skill, `qa-cases`, which lands cases through ordinary reviewed PRs and whose terminal act is a
  report naming the command the human types next — it runs no browser and invokes no cycle
  (`ADR-0090` §3).
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

**None.** All three — `DEC-082`, `DEC-083` and `DEC-085` — are answered below. The first unblocked
this epic; the second settles how its catalogue gets written; the third adds the `uat` focus and its
arithmetic. (`DEC-086`, raised by `ADR-0092` and blocking nothing here, is registered open in
`docs/adr/README.md` and `tasks/BOARD.md`.)

### Answered since this epic was written

| ID | Answered by | What it means here |
| --- | --- | --- |
| `DEC-085` | [ADR-0092](../../docs/adr/ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) | **UAT is a second focus of this cycle, not a second cycle.** `/qa-cycle uat <scope>` — the human's own message, first act of its turn; the QA focus never chains into it, neither report prints the other's command, and a preceding QA cycle is practice, never a checked precondition (a check would cite a round as a gate, `ADR-0089` §2c). Raised 2026-08-30 by the human's request for a UAT pass — UX and design conformance over the same catalogue — with two halves settled by the human mid-decision: **a UX question raised during UAT is delegated to the `product-owner`**, whose decision may create a bug ticket through `build-epic`; and **UAT runs now, filing the six missing cards**, rather than waiting for `ADR-0091` §5's retrofit story. **Six things change for this epic.** (1) A `uat` agent joins `qa` as the observer under the second focus — no `Write`, same report shape plus a `QUESTIONS` section capped at three per screen; `qa.md` stands byte-unchanged. (2) `qa-manager` triages both focuses on one ledger — dedupe spans them — and applies the classifier: **a finding must contradict a merged source** (a card, a token, an owned literal, an ADR section, a vision sentence); a judgment with no merged source is a question, and `qa-manager` promotes at most **three** per round (one per screen, each a concrete choice answerable in one sentence and bearing on a player's ability to tell what is going on) as `DEC`s for the product owner, whose answers become merged sources either way; tickets born of answers enter the earliest **subsequent** triage, preserving §Termination rule 1's frozen set. (3) A missing card is a `high` finding whose repair **is** the card (composed per `ADR-0091` §3, the human's verdict trailing; dedupe key the card's own path); the screen is walked for reachability and copy, and only its conformance check reads `BLOCKED — no card`. (4) **`B(N)` gains its second and third exclusions**: missing cards (registered debt) and decision-born tickets (improvement), because counting either trips rule 4 on a cycle doing its job — `B(N)` counts product defects alone. (5) A round in which a screen becomes conformance-judgeable for the first time is a **baseline round** — rule 4 skips its comparison, exactly as it skips round 1's; rule 5's three-round budget binds regardless — and every UAT verdict line is qualified inline (`PASS (conformance unjudged on 6 of 7 screens)`), repeated verbatim in the terminal report. (6) `STOP_BLOCKED` is scoped: a human-only escalation ends the cycle only when it **gates the current fix set**; otherwise `notify.py blocked` carries it while the run is warm and the cycle continues. The catalogue is reused as a route map — existing `do` columns are the walk, `expect` stays functional — and `docs/test-plan.md`'s UAT section (screen inventory + question list) merges before the first UAT round. A `shot` verb (CDP screenshot, Node built-ins, never committed, no image-diff tooling ever) is the one harness addition. `ADR-0090` §2's declared set grows to four files (`agents/uat.md`, mention-only). `DEC-086` — the written bar for *"ready for real users"*, the product owner's — is registered open; no round may be cited as it |
| `DEC-083` | [ADR-0090](../../docs/adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md) | **A skill may write the catalogue or run it, never both in one turn.** Raised and answered 2026-08-29, when the human asked for one skill that writes the missing cases and then runs a cycle over them. `ADR-0089` §2b's *"not another skill invoking it as a step"* is read as a rule about **composition**, not only about automation: *"a human's command"* is a condition only if it means the **immediate caller**, and the composite's sole value is that the cycle begins while the human is elsewhere — a cron whose clock is the length of its first half. **§2b's heading becomes *"No gate, and one caller"*** and its sentence becomes *a cycle is started by the human's own message and nothing else*; the rest of §2b and all of `ADR-0089` §§2a, 2c, 3, 4, 5, 6 stand byte-unchanged. **Three things change for this epic.** (1) The catalogue's missing suites are authored by **`qa-cases`**, a licensed skill that plans and lands cases through ordinary reviewed PRs, runs no browser, dispatches neither `qa` nor `qa-manager`, and whose **terminal act is a report naming the command the human types next**. (2) Every case `qa-cases` writes carries a `source` column citing the merged decision its `expect` is transcribed from — the module for player-facing text (`ADR-0089` §5), otherwise an ADR section or a `docs/duel-rules.md` heading; **a case with no merged source is not written**, and the gap is registered as a `DEC` for the product owner, because an invented expectation is a product claim that step 4 of the loop would change production code to satisfy. `SMOKE` and `CORE` are not retrofitted. (3) **A suite `qa-cases` writes is provisional until its first round** — merged sources prove what was *decided*, not what *shipped*, so they cannot show that a screen exists or that a literal has not moved. `docs/test-plan.md` §*Per-epic suites*' *"filled in when an epic is first tested, not before"* is amended in the same PR to admit the authored-then-tested path, a provisional suite carries a line the first round record deletes, and the honest expectation for that round is a pile of **harness** tickets against this epic — which, since `ADR-0089` §4 excludes them from `B(N)`, can end `PASS` with a dozen of its own cases found broken. (4) The condition is checkable by one command over a declared three-file set, and what no grep can catch is stated rather than claimed |
| `DEC-082` | [ADR-0089](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md) | **A browser drives this client for a QA round, never for a gate.** The harness may live here. It **was** inside `ADR-0088` §1's words — *"here or in CI"* says *here* — so §1's **heading** is amended in the open and nothing else is: `EPIC-03` still ships no fifteenth story, §2's eleven-step hand-check is still the proof of record, `build.yml` keeps its two jobs, no browser dependency enters `web-client/package.json`, and `ADR-0032` §4's *"still jsdom, still no network"* still holds of every test suite. **Four things change for this epic.** (1) The three constraints in *Out of scope* are no longer this epic's self-restraint but **standing conditions on the permission** — no dependency, no gate, no coverage claim — and any one failing returns the question as a new `DEC`. (2) §3: the driver **reads anything and writes nothing** but `pd.roomCode`; no case may seed store, socket or database state to reach a screen, because that is a client asserting a game fact (`ADR-0002`). (3) §4 adds the rule §Termination lacked — a failing case that does **not reproduce by hand** is a **harness** defect, filed against this epic, **excluded from `B(N)`**, and never repaired in production code. (4) §5: a case quoting player-facing text cites the module that owns the literal. A `PASS` is a dated record of one run on one machine at one commit, and `dist/` stays unproven |

## Stories

| ID | Title | Status |
| --- | --- | --- |
| [STORY-1201](../stories/STORY-1201-the-qa-harness-two-agents-one-skill-one-catalogue.md) | The QA harness — two agents, one skill, one catalogue | done — a **retrospective record**; the code merged in `#1159` and `#1161` before the story existed |
| `STORY-1202` | Round 1 — the smoke suite passed, and one case did not run as written | done |
| [STORY-1203](../stories/STORY-1203-the-qa-cases-skill-the-authoring-half.md) | The `qa-cases` skill — the authoring half, whose last act is a printed command | done — **not a round story**; it builds the skill `ADR-0090` §3 licensed and runs no round |
| [STORY-1204](../stories/STORY-1204-the-epic-04-and-epic-05-catalogue-suites.md) | The `EPIC-04` and `EPIC-05` catalogue suites, authored from merged sources | done — **not a round story**; it is the first `/qa-cases` pass, and it takes the number the note below had reserved for the first round |
| `STORY-1205`+ | One story per QA round; the round number lives in the story, not the id | not written |

Rounds are numbered in the story body rather than encoded in the id, because a round is created
when it is run and the ids stay sequential without arithmetic. **`STORY-1203` and `STORY-1204` are
the two stories here that are not rounds**, the way `STORY-1201` is the one that is a retrospective
record: `STORY-1203` builds `qa-cases`, the authoring skill `ADR-0090` §3 licensed, and `STORY-1204`
is the first pass that skill performs — writing the `EPIC-04` and `EPIC-05` suites from merged
sources. Neither brings a stack up, starts a browser or reports a `B(N)`. The round stories
therefore resume at **`STORY-1205`**, which supersedes `STORY-1203`'s sentence reserving `STORY-1204`
for the first round. That is stated rather than done quietly, because **this is the second time the
convention has moved by one**, and a convention silently shifted twice is one nobody can rely on
afterwards — each shift is written into this table's own rows, so a reader never has to infer it.

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
- [x] `STORY-1201` is `done`: the two agents, the skill and the catalogue exist, and the skill's
      stack lifecycle uses no denied verb —
      [`STORY-1201`](../stories/STORY-1201-the-qa-harness-two-agents-one-skill-one-catalogue.md),
      2026-08-29, which carries the command for each half of that sentence. It is a **retrospective
      record**: the harness merged in `#1159` and `#1161` with no story file and no tickets, and the
      story was written afterwards to close the gap in the trail. The box is ticked on the
      structural checks passing at commit `5848e529`, not on the story having been planned first.
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
