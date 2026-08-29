---
id: STORY-1202
title: Round 1 — the smoke suite passed, and one case did not run as written
type: story
status: in-progress
parent: EPIC-12
labels: [process, qa]
depends_on: []
---

## The round

**Round 1** of a `/qa-cycle smoke` invocation. This is the round ledger `EPIC-12` §Termination
requires; the round number lives here rather than in the id, per the epic's Stories table.

| | |
| --- | --- |
| Round | **1** |
| Scope | `smoke` |
| Date | 2026-08-29 |
| Commit | `7f7b905f` |
| Stack | `up` — db, server, web |
| Cases | 6 of 6 run; passed 6, failed 0, blocked 0 |
| `B(1)` | **0** |
| `B(0)` | undefined — there is no round 0, so the convergence rule cannot apply |
| Verdict | **`PASS`** |

## What this record is not

`ADR-0089` §2c, stated here because a round record that omits it invites exactly the reading the
condition forbids:

> No coverage claim. The product of a run is a **dated round record**. Neither it nor
> `docs/test-plan.md` may be cited as coverage in an epic's `Metrics`, a Definition of done, or a
> ticket's `verify:`. A `PASS` is a statement about one run, on one machine, at one commit.

So: **this is a statement about one run, on one machine, at commit `7f7b905f`, on 2026-08-29.** It
is not a coverage claim, it may not be cited as one, and nothing here is permitted to appear in a
`verify:` block. Six cases passed. That is the whole of what it says.

Three things it says nothing about, all of them written down already in
[`docs/test-plan.md`](../../docs/test-plan.md) §*What this catalogue does not cover*:

- **The built bundle.** Every case ran against `npm run dev`. `dist/` is loaded by nothing here —
  `ADR-0088` gap 3, which survives this round exactly as it survived that ADR and `ADR-0089`.
- **The CORE suite.** `smoke` is six cases. Seating, the hand, secrecy, coins, rematch and
  reconnect — twenty cases including `CORE-10`, the highest-value case in the catalogue — were
  **not run this round**. A `PASS` on `smoke` is *the product is alive*, not *the product is right*.
- **A real network, performance, load, security, and everything `EPIC-06` owns.**

`ADR-0088` §2's eleven-step hand-check remains the proof of record. This round substitutes for it
in neither direction.

## The evidence, case by case

Recorded by `qa`, which has no `Write` and filed nothing — the separation that stops a tester
grading its own findings.

| id | What was observed | Verdict |
| --- | --- | --- |
| `SMK-01` | `scripts/qa/stack.sh status` → `db: up`, `server: up`, `web: up` | pass |
| `SMK-02` | `A open` rendered `#root` containing the literal `Create a duel room` | pass — see note 2 |
| `SMK-03` | `A device` → `aa5Ve6YjK0ZxUN1STtMpvA`; `B device` → `j3pnYa_yGcPizG-zXJO68w`. Non-empty, and distinct | pass — **but not by the steps as written**; see note 1 |
| `SMK-04` | `A click "Create a duel room"` → clicked; `A wait "Waiting for your rival"` → saw. Room `MW0STXYG`, link `http://localhost:5173/?room=MW0STXYG` | pass |
| `SMK-05` | `B open <link>` seated B at a dealt table — `Blinds 50/100 · Hand 1 · Preflop`, stacks 9,950 / 9,900, `committed 50`. **No `type` call was ever made on B** | pass |
| `SMK-06` | `A text` showed `YOUR TURN` and live controls: `Fold`, `Call 100`, `Raise to 200`, `All in 10,000` | pass |

`SMK-05` is the strongest of the six and worth naming: B reached a seat **by link alone**, which is
the vision's success condition — *"Send a link. She opens it in a browser."* — observed rather than
asserted. `SMK-04`, `SMK-05` and `SMK-06` each turned on a server-derived value (a room code, a
dealt board, a legal action set) that no half-dead build can fabricate.

## Triage

### Dedupe

Nothing to dedupe, and the search that establishes it: this is the **first** round, so no prior
round story exists under `tasks/stories/` and no `TASK-12NNNN` exists under `tasks/tasks/` — both
checked. The report also contained no findings, so the question is doubly empty. **New 0, repeat 0,
regression 0.**

### Hand-reproduction (`ADR-0089` §4)

No `blocker` and no `high` was filed, so §4's precondition was not exercised against any product
defect. The one thing filed is a **harness** defect and its reproduction is stated in its ticket.

### Severity

**No severity assigned by `qa` was changed, because `qa` assigned none — it reported
`FINDINGS: none`.** There is therefore no downgrade to justify, and `B(1)` is not the product of
any judgement of mine.

### `B(1)` = 0

Zero `blocker`, zero `high`, after dedupe and after harness defects are excluded. Nothing was
deferred to the backlog, so there is no deferral hiding inside that zero — the rule that deferrals
still count toward `B(N)` had nothing to count.

**Verdict: `PASS`.** The cycle ends here, successfully, at round 1 of a budget of 3.

## The two methodology notes, and what was done with each

`qa` volunteered two observations about the **catalogue and the harness** rather than about the
product, and explicitly marked neither as a finding. They are recorded here with their disposition,
because an observation that is neither filed nor written down is an observation that will be made
again next round at full cost.

### Note 1 — `SMK-03` cannot run as written → **filed**, `TASK-120201`

The case reads `A device` and `B device`. At that point in the suite B has never been opened:
`stack.sh chrome-up` starts Chrome at `about:blank`, `drive.mjs`'s `attach()` falls back to that
page when no `localhost:5173` target exists, and a profile that has never loaded the app has no
`pd.deviceId` for the app's origin. The read returns empty. `SMK-03`'s own `fails if` column is
*"they are equal, or **empty**"* — so the case, executed in the order it is written, is **red for a
reason that has nothing to do with the product.**

The tester got a pass by navigating B to the app root first. That was the right call and a licensed
one — navigation is a player's hands, `ADR-0089` §3 — but **the catalogue does not say to do it**,
and the gap between what the document says and what was run is precisely the silent rot `ADR-0089`
§5 and its §Consequences predict for this harness.

It is filed because leaving it costs more than fixing it, in one of two ways, every future round:

- a `qa` agent runs it literally, `SMK-03` reads red, and a manager must spend the round proving it
  is not a product defect — or, worse, a less careful one counts it in `B(N)` and the convergence
  rule trips on a healthy product. That is the exact failure `ADR-0089` §4 exists to prevent.
- or the agent deviates silently again, and the catalogue permanently describes something other
  than what is executed.

By construction this is a **harness** defect: it lives in `docs/test-plan.md`, it is repaired
there, it is **excluded from `B(1)`**, and **no production code may change for it** (`ADR-0089` §4,
`EPIC-12` §Termination rule 6).

**This does not tick `EPIC-12`'s Definition-of-done box** for telling a harness defect from a
product defect on the record. That box asks for *"a failing case that did not reproduce by hand"*,
and `SMK-03` did not fail — a tester's judgement absorbed it before it could. The box stays
unticked, and saying so is worth more than claiming it.

### Note 2 — `SMK-02`'s assertion is the weakest of the six → **not filed**

The tester is right on the facts: `SMK-02`'s verdict rests entirely on the static string
`Create a duel room` in `#root.innerText`, and a build that painted the first tree and then died —
handlers unwired, socket never opened, hydration abandoned mid-way — would satisfy it
byte-identically.

Filed as nothing, for two reasons.

1. **The case does what it says.** Its `fails if` is *"the page is blank"*, and it detects a blank
   page. It exists for one named gap — `ADR-0088`'s *"the root render is executed by no test"* —
   and it closes that gap. A case is not defective for failing to be a different, larger case.
2. **The suite already covers the failure mode.** `SMK-04` clicks `Create a duel room` and waits
   for `Waiting for your rival`; a tree that painted and died fails there, and fails again at
   `SMK-05` and `SMK-06`. Widening `SMK-02` would buy a second detector for something already
   detected two cases later, at the cost of a coder, a reviewer and a merge.

A ticket manufactured from a relayed observation is worse than no ticket. This one is written down
instead, which is what it was worth.

### One thing observed in passing, also not filed

`drive.mjs`'s `device` verb prints the empty string and **exits 0** when the id cannot be read,
because it discards the `error` half of `evaluate`'s result. The file's own header says *"A QA case
reads the exit code, never the prose."* Not filed: `SMK-03`'s written `fails if` catches the empty
value by comparison anyway, and `TASK-120201`'s fix removes the situation in which the verb is
called on a page that has no app origin. Recorded so that a future case reading storage on a
never-opened profile knows the verb will not tell it so.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-120201](../tasks/TASK-120201-smk-03-reads-a-device-id-from-a-profile-that-has-been-to-the-app.md) | `SMK-03` reads a device id from a profile that has been to the app | ready |

One ticket, and it is a harness ticket. The fix set is **empty** — `EPIC-12` §Termination rule 2
admits only `blocker` and `high`, and there were none. `TASK-120201` is not repaired by this cycle:
the verdict is `PASS`, which ends the run at step 3 of the loop and never reaches step 4.

## Acceptance criteria

- [x] Every finding in round 1's report was deduped against the existing round stories and tickets
      before triage — trivially, there were none of either.
- [x] `B(1)` is computed and stated: **0**.
- [x] The verdict is exactly one of the five named exit states: **`PASS`**.
- [x] Every severity change is written down with its reason — none were made, and why is stated.
- [x] The harness defect is filed against `EPIC-12`, repaired in `docs/test-plan.md`, and excluded
      from `B(1)`; no production file appears in its `## Files` table.
- [x] The record states, in its own words, that it is one run on one machine at one commit and not
      a coverage claim (`ADR-0089` §2c).
- [ ] `TASK-120201` is merged.

## Out of scope

- **Repairing `TASK-120201`.** A `PASS` ends the cycle; the ticket is `ready` and will be taken by
  the next `build-epic` run on `EPIC-12`, or by the next round that needs a repair step.
- **Running the CORE suite.** The invocation was `/qa-cycle smoke`. Widening a round's scope
  mid-round would break `EPIC-12` §Termination rule 1 — the round's set is frozen at triage.
- **Changing `EPIC-12`'s Definition of done, or ticking any box in it.** Nothing this round did
  earns one; see note 1.
- **Any change to `scripts/qa/drive.mjs`.** The one observation about it is recorded above and
  deliberately unfiled.
