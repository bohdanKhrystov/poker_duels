# ADR-0084 — A criterion that speaks in shell belongs in `verify:`, and one about an unbudgeted file may only refuse

- **Status:** Accepted
- **Date:** 2026-08-26

## Context

`CLAUDE.md` rule 3 says *done is an exit code*: every command in a ticket's `verify:` block exits 0.
`tasks/README.md` says acceptance criteria must be *checkable*. Neither says what makes a criterion
checkable, and nothing couples the two registers — `lint_tickets.py` reads `verify:` and has never
read a word of `## Acceptance criteria`. So a ticket has **two** lists of obligations, one gated by
CI and one gated by nobody, and which list an obligation lands in is currently an accident.

This run produced two families of defect from that gap, and a planner measured a candidate check for
each against the live backlog **before** proposing either. Both fail as written. That is what makes
this a decision rather than a lint rule somebody forgot: the obvious rule is measurably unshippable,
and the fallback of "a reviewer will catch it" has already been tested four times this run and lost
four times.

### Face one — a criterion demanding content in a file the *Files* table excludes

`TASK-041628`'s criterion 11 read *"`ServerConfigTest` covers both new pairs' env-then-file-then-default
precedence"*, and its `## Files` table names three files, none of them `ServerConfigTest.kt`.
`ADR-0070` §4 permits reading an unnamed file and forbids editing one, and its narrow propagation
exception explicitly excludes *"adds a test"* — so the coder was right to refuse, as were the coders
of the six other tickets of that shape this run.

The criterion was therefore **unsatisfiable by construction**, and the cost is not hypothetical.
`ADR-0079`'s four numbers shipped asserted nowhere; a reviewer swapped the two defaults — five to
attach, ten to forget — ran both `verify:` classes, and got a green build. Verified on `ef47e299`:

```
$ grep -rn "forgotPasswordLimits\|recoveryEmailLimits\|60000" \
      poker-server/src/test/kotlin/duels/poker/server/config/
(no output)
```

The numbers exist in `ServerConfig.kt` at lines 49, 51, 145 and 153 and in no test. Note the
sharpest detail: `TASK-041628`'s `verify:` **did** run `--tests 'duels.poker.server.config.ServerConfigTest'`.
The gate was named and it was green, because a gate over a class the ticket may not edit cannot
change colour on account of the ticket. Naming a command is not gating a claim.

The obvious rule is *a criterion may not name a file the table excludes*. Measured on `ef47e299`
over 870 schema-2 tasks, with my own extractor rather than the planner's: **206 tickets flagged
raw** (the planner measured 194; the difference is which tokens each of us treats as a filename, and
neither number is the argument). Narrowing to criteria carrying a demand verb does not save it —
I split all 174 verb-carrying flags into refusal-worded and demand-worded and read the demand-worded
ones. The dominant population is not a defect:

- *"`SeasonTest` is untouched"*, *"`api.test.ts` is unmodified"* — a refusal, and the whole point.
- *"`CardSecrecyTest` passes with no change to the file"*, *"`PlayerViewOfTest` passes with no edit
  to that file"*, *"`duel-state.test.ts` is byte-identical to `develop`"* — **a refusal written in a
  demand's grammar.** `passes` is a demand verb; the criterion is a refusal.
- *"`.prettierignore` contains the line `src/protocol/protocol.gen.ts`"* — a legitimate demand on a
  budgeted file whose *object* is a path, flagged because the extractor cannot tell a filename from
  a string literal that looks like one.

What separates `TASK-041628` from every one of these is not the verb and not the file: it is whether
what the criterion demands **already exists in the file**. That is not an exit code and no narrowing
of this rule makes it one.

### Face two — a criterion quoting a command that no `verify:` line runs

Three instances this run, each with a bill. `TASK-041210`'s criterion 5 was never in `verify:`, went
unmet through **two** dispatches and a review each time, and was arithmetically unsatisfiable
besides — `grep -c` counts lines, so an imported symbol returns 2 and the criterion demanded 1.
`TASK-041215`'s `grep -cE` gate sat in acceptance criteria alone. `TASK-041634`'s `## Scope` demanded
a `CoroutineName("recovery-mail")` verbatim and nothing checked it. The phrase this run produced is
**"a criterion outside `verify:` is a wish."**

The planner measured *a shell command named in a criterion must appear in `verify:`* at **25
criteria across 12 unsettled tickets**, mostly genuine but with spelling false positives, and
concluded it could not land green without a twelve-file repair that `TASK-000106` refuses.

I reproduced it and found the false positives are an artefact of the **matching relation**, not of
the rule. Under exact substring: **23 criteria across 11 unsettled files**. Under *the criterion's
shell words are a subset of one `verify:` line's words*: **18 criteria across 11 files**. The five
that drop out are exactly the spelling cases — `TASK-041219`'s criterion quotes
`npm run test -- src/account/SignUpForm.test.tsx` while its `verify:` runs
`cd web-client && NO_COLOR=1 npm run --silent test -- src/account/SignUpForm.test.tsx 2>&1 | grep -qE 'Tests +11 passed \(11\)'`.
The `verify:` form is the **hardened** one and it is correct; the criterion is a readable restatement
of a gate that exists. A relation that calls that a defect is the wrong relation.

The remaining 18 are all genuine wishes. Their statuses are `backlog` × 16 and `ready` × 2.

### What is actually in tension

- **A refusal and a demand are the same sentence to a linter, and opposite obligations to a coder.**
  Any rule sharp enough to catch the demand catches ~28 refusals that are the dominant, correct use.
- **`verify:` is the only register with teeth, and it is the register nobody reads for meaning.**
  A reviewer reads criteria. Pushing everything into `verify:` makes the ticket less legible; leaving
  it in criteria makes it unenforced.
- **The backlog cannot be repaired in one pass.** Eleven ticket files is four tickets at the
  three-file cap, and it cannot be one `atomic:` ticket, because `ADR-0068` requires `atomic:` to
  name a **merged** gate and the gate here is the thing being added — the ticket would have to name
  itself.
- **The trail must stay honest.** 856 of the 870 schema-2 tasks are `done`. Their criteria are
  history, and a check that forces them rewritten would falsify the second product.

### The deadline

Sixteen of the eighteen flagged criteria sit in `STORY-0412`'s remaining `backlog` tickets —
`TASK-041220` through `TASK-041229` — which will be dispatched over the next few days. Every one
dispatched before a check exists ships its wishes ungated and then becomes `done`, at which point no
check this ADR could write will ever read it again. The check costs one file and two lines today. In
a week it costs the same and buys ten fewer tickets.

## Decision

### 1. What *gated* means

A criterion is **gated** when some command in the ticket's `verify:` block exits non-zero if the
criterion is false. Nothing else gates anything. `## Acceptance criteria` is the contract a reviewer
reads; `verify:` is the register of the subset of it a machine enforces. A criterion that is not
gated is a **wish**, and a wish is legitimate only where the ticket says out loud that no gate holds
it — `TASK-000106`'s *"a refusal, and it is not gated by a test"* is the pattern.

Naming a command in a criterion does not gate it. Naming a test class in `verify:` does not gate a
claim about that class's contents when the ticket may not edit it.

### 2. A criterion that quotes a shell command must be gated, and `lint_tickets.py` enforces it

A new check, beside `check_links` in `.github/scripts/lint_tickets.py`:

- **A quoted command** is a backtick-delimited span in `## Acceptance criteria` whose first shell
  word is one of a **closed runner list** — `./gradlew`, `npm`, `npx`, `node`, `python`, `python3`,
  `grep`, `rg`, `git`, `cd`, `bash`, `sh` — optionally preceded by leading `NAME=value` assignments.
  The list is closed so that ordinary prose backticks (`` `SeasonTest` ``, `` `#/sign-in` ``) are
  never read as commands.
- **Gated means word-subset, not substring.** The span's shell words (`shlex.split`, falling back to
  `str.split` on an unbalanced quote) must be a subset of the shell words of **one** `verify:` line.
  This is the answer to *"exact or normalised"*: normalised, because the `verify:` form is routinely
  and correctly hardened with `NO_COLOR=1`, `--silent`, `--reporter=verbose` and a `| grep -qE`, and
  wrapping a criterion's `grep -c 'X' f.ts` in `test "$(…)" = "1"` must count as gating it.
- **Scope: tasks whose `status` is `ready`, `in-progress` or `in-review`.** Not `backlog` — the
  criteria are still being written and the ticket is not yet a contract. Not `done` or `dropped` —
  those are history, and the check never reads them.
- The failure names the ticket, the command and the fact that no `verify:` line runs it.

The status predicate is not a novelty: `check_links` already fails a task that is `ready` while a
dependency is unfinished. This is the same idea in the same file — **the linter enforces the
conditions of readiness, not the conditions of existence.**

### 3. The eighteen retire one ticket at a time, at readiness

No grandfather list, no exemption array, no `filed:` date field.

- **`TASK-041219`'s two are repaired in the check's own PR.** It is `ready` today, and its two
  ungated criteria — `` grep -cEi 'setTimeout|setInterval|retry' web-client/src/account/SignUpForm.tsx ``
  returns `0`, and `` grep -c 'throttled' web-client/src/account/sign-in.ts `` returns `0` — become
  `verify:` lines. One ticket file, two commands, named here in advance. This is exactly
  `TASK-000106`'s own pattern, which repairs the two board cells its measurement named and refuses
  only repairs *beyond* them.
- **The other sixteen are repaired by the PR that moves each ticket `backlog` → `ready`.** That PR
  already edits the ticket file to change `status:`, so the repair rides on an edit somebody makes
  anyway, one ticket at a time, reviewed against that ticket's own criteria.

### 4. Face one is a written rule, not an exit code

`tasks/README.md`, under *What makes a task ready*, gains the rule the measurement supports:

> A criterion may demand **new content** only in a file the ticket's *Files* table carries as a
> `create`/`modify`/`regenerate`/`delete`/`rename` row. About any other file a criterion may assert
> only that it is **unchanged**, or that it **still passes**. A criterion that cannot be satisfied
> without an edit the table forbids is not a criterion — it is the next ticket, and it is filed in
> the same split.

That is `ADR-0068`'s *"a change no merged gate forbids splitting is two tickets"* applied to
criteria, and it is what `TASK-041645` eventually did four days late and after the damage.

**No linter check is written for this**, and the reason is stated in `tasks/README.md` beside the
rule so the next reader finds a decision rather than an oversight: the rule was measured at 206
flagged tickets raw, and every narrowing tried leaves the legitimate refusals dominant, because a
refusal and a demand differ only in whether the thing demanded already exists in the file.

### 5. Where the check ships

**Its own ticket, `depends_on: [TASK-000106]`** — not folded into `TASK-000106`. Three files:
`.github/scripts/lint_tickets.py` (modify), `.github/scripts/test_lint_tickets.py` (modify), and
`tasks/tasks/TASK-041219-….md` (modify, per §3). `files_touched: 3`, `estimate: S`, no `atomic:`.

`TASK-000106` creates `test_lint_tickets.py` and gives `.github/workflows/tickets.yml` a `discover`
run, so this check needs no workflow edit and no second harness. The dependency is the ordering
risk and it is named: **if `TASK-000106` has not merged by the time `STORY-0412` resumes**, this
check is cut with its own test file and its own workflow line instead, and the sixteen it would have
caught are lost as they are dispatched.

### 6. What this does not decide

- **Whether `files_touched` is ever checked against the actual diff.** `ADR-0068` §7 and `ADR-0070`
  §6 stand unchanged.
- **`## Scope` and `## Tests` are not read.** The contract is `## Acceptance criteria`; a scope item
  that must be true is a criterion, and `tasks/README.md` already says so.
- **`TASK-041634`'s defect is not covered by anything here**, and it is the one of the four cited
  that no version of this rule catches: what its `## Scope` demanded was a Kotlin code block, not a
  shell command, and no closed runner list sees it.
- **Whether the 856 settled tickets are ever audited.** Deliberately never — see the cost below.

## Consequences

**What it buys.**

- The eighteen known wishes cannot be dispatched without being gated, and they retire without a
  repair pass anyone has to schedule.
- *Gated* has a written definition for the first time, so *"a criterion outside `verify:` is a
  wish"* is a rule rather than a phrase.
- The word-subset relation settles *exact or normalised* and makes a hardened `verify:` line
  first-class, which is what killed the twelve-file blocker: the five spelling false positives were
  never defects.
- It is cheap to reverse — one function and one status predicate, deleted in a diff smaller than
  this ADR. That is deliberate, and it is why the rule is scoped narrowly on thin evidence: three
  measured instances of face two is enough to justify a check that flags nothing legitimate today,
  and not enough to justify one that flags 206 tickets.

**What it costs — and which side of the trade this is.**

The driving constraint here is that **a rule sharp enough to catch `TASK-041628` necessarily flags
the legitimate refusals, and a rule that flags no legitimate refusal cannot catch `TASK-041628`.**
This ADR buys the second side, and the bill is:

- **`TASK-041628` would happen again today.** Its criterion is English, not shell; §2 never sees it.
  `ADR-0079`'s four numbers would be unasserted a second time, and only §4's written rule — applied
  by a planner or a reviewer, with no exit code behind it — stands between the project and a repeat.
  That is the cost, stated plainly, and it is the larger of the two defects by consequence.
- **It taxes the honest form.** A planner who writes `` `grep -c 'throttled' sign-in.ts` `` returns
  `0` is flagged; one who writes *"no throttle string appears in `sign-in.ts`"* is not. The rule
  makes the **most executable** criterion the **most expensive** to write, and creates a real
  gradient toward vaguer prose. §4 is the only counterweight and it is not an exit code.
- **It fires on the wrong person.** `tasks/README.md` puts the `backlog` → `ready` flip in *"the PR
  that finishes the thing it was waiting for"* — usually a coder finishing the previous ticket, not
  the planner who wrote the criterion. And `lint_tickets.py` lints the whole tree, not the diff, so
  one red ticket reddens **every** PR until it is fixed. The correct response is to leave the ticket
  in `backlog` and dispatch the planner, which costs an agent run and a stall.
- **The 856 settled tickets are never read**, so this check can never measure how often the defect
  happened, and the historical answer to *"is a criterion outside `verify:` common?"* stays the
  planner's one-off script. Deliberate — rewriting a merged ticket's criteria falsifies the trail —
  but it forecloses the audit.
- **A closed runner list rots silently.** The first `verify:` block to use `make`, `deno` or `uv`
  will carry criteria this check cannot see, and its blindness produces no error. That is the same
  class of silent-absence failure this ADR exists to close, reintroduced one level up.
- **It cannot see a wrong command, only a missing one.** `TASK-041210`'s criterion 5 was *also*
  arithmetically unsatisfiable — `grep -c` counts lines, so an imported symbol returns 2, never 1 —
  and moving it verbatim into `verify:` would have produced a red gate rather than a green wish.
  That is an improvement and it is not a fix; §2 makes a wrong criterion fail loudly instead of
  passing silently, which is the most it can do.

## Alternatives considered

**Cross-reference criteria against the *Files* table and fail (face one as a gate).** The strongest
case for it is decisive on paper: it is the **only** rule proposed that catches `TASK-041628`, and
`TASK-041628` is the defect that actually cost coverage — four numbers a reviewer could swap
unnoticed. Rejected on measurement, not on taste. 206 tickets flagged raw on `ef47e299`; narrowing
to demand verbs leaves 174; narrowing further to criteria naming a *test* class leaves 59 across 38
tickets, of which the visible majority are `X passes`, `X passes with no edit to that file` and `X
is byte-identical to develop` — refusals wearing a demand's grammar. One true positive against
roughly twenty-eight correct-as-written criteria is not a gate; it is a nuisance that teaches
planners to phrase criteria for the linter, which is the failure this ADR is trying to prevent.

**The same rule as a warning rather than a failure.** Its case: it names all 206 without blocking
anyone, and costs nothing to switch on. Rejected because nothing in this repository reads a warning.
`tickets.yml` is a pass/fail job, and a 206-line advisory printed on every pull request is read once
and then scrolled past — an unread warning is indistinguishable from no check, except that it feels
like one exists.

**Repair the eleven files now and land face two unscoped.** Its case is real: it clears the debt in
one pass, needs no status predicate, and leaves a simpler check behind. Rejected on two grounds.
Eleven ticket files is four tickets' worth at the three-file cap, and it cannot be one `atomic:`
ticket because `ADR-0068` requires `atomic:` to name a **merged** gate — the gate here is the check
being added, so the ticket would have to name itself. And `TASK-000106` refuses exactly this shape:
*"a repair made to turn a gate green is a change nobody reviewed against its own criteria."*

**A `filed:` date field, with the rule applying from a date.** The cleanest grandfathering available,
and it needs no repair at all. Rejected: adding a required field to 870 tickets is a diff two orders
of magnitude larger than the check; the dates would be back-filled from git history and would
therefore be invented rather than recorded; and it leaves a permanently bimodal backlog that nobody
ever clears, because nothing ever forces an old ticket over the line. The status predicate buys the
same grandfathering from a field that already exists, is already load-bearing, and already forces
the transition.

**Leave both faces to review, and codify nothing.** The strongest case, and it is half-accepted: the
discriminator in face one is genuinely a judgement, reviewers exist to make judgements, and every
mechanical rule proposed here has a false-positive tail. §4 **is** this alternative, adopted for
face one because the measurement says nothing else works. It is rejected for face two on the
evidence: review had four chances at that family in a single run and missed all four, and
`TASK-041210`'s criterion 5 survived **two** dispatches with a review each time. A defect a reviewer
has demonstrably missed four times in one run is not a defect review catches.

**Fold the check into `TASK-000106` as a second check.** Its case: same script, same test harness,
same workflow, zero new *Files* rows, one PR instead of two. Rejected — `TASK-000106`'s table is
already full at three files, its `## Out of scope` refuses additions its own measurement turns up,
and two unrelated register checks inside one `S` ticket is precisely the ticket growth
`tasks/README.md` says is this project's failure mode. It depends on `TASK-000106` instead, which
buys the harness without buying the scope.
