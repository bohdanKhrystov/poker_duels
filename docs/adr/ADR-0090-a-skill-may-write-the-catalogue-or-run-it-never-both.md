# ADR-0090 — A skill may write the catalogue or run it, never both in one turn

- **Status:** Accepted
- **Date:** 2026-08-29

## Context

The human asked for **one skill** that writes the missing test cases for existing functionality and
then runs a full QA cycle over them. The need behind the request is real and it is the largest piece
of work left in `EPIC-12`: `docs/test-plan.md` §*Not yet written* lists `EPIC-04`, `EPIC-05` and
`EPIC-06` with **no cases at all**, and the catalogue's own per-epic template stands unfilled.

The obstacle is [`ADR-0089`](ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
§2b, one of the three standing conditions the browser-driving permission is the **conjunction** of:

> **b. No gate.** `build.yml` keeps its two jobs. No pull request, `verify:` block or ticket waits
> on a QA case, and a cycle is started by a **human's command** — not a merge, not a cron, not
> another skill invoking it as a step.

A composite is, literally, another skill invoking `/qa-cycle` as a step. It is also, plainly, a
human's command — the human types it, and no merge waits on it. §2 says a condition that stops
holding *"returns the question as a new `DEC-NNN`"*. This is that `DEC`, raised rather than argued
around, which is the only reason this document exists instead of a wrapper.

**Two readings are available and only one can be taken.**

*The clause is about automation.* Its heading is **"No gate"**, its enumeration is a list of
**unattended** triggers, and §2's own preamble says the three conditions *"are the terms that made
§1's cost zero"* — where §1's costs were a third CI job, flake on every pull request, and a browser
dependency in `package.json`. A human-typed wrapper incurs none of the three and gates nothing. On
this reading the clause forbids `build-epic` deciding on its own to run QA while landing a ticket,
and says nothing about a human-typed composite.

*The clause is about composition.* It names skill-invocation **separately** from merge and cron,
which is redundant if automation were the whole subject. §2 says the conditions are *"written as
conditions rather than preferences so that the next reader can check them mechanically"*, and its
closing sentence pre-refuses the argument form outright: *"Putting `scripts/qa` behind a CI job is
condition **b** failing, not a refinement of it."*

**This ADR takes the composition reading, and rejects the automation reading.** Four things decide
it.

**1. "A human's command" is a condition only if it means the immediate caller.** Every automated
trigger in this repository has a human behind it at some distance. A human arms the cron. A human
starts `build-epic`, and `build-epic` merges, and the merge would be the trigger. If provenance may
be traced through one intermediary it may be traced through five, and the clause then forbids
nothing that anyone would actually build. Read as *immediate caller*, it is a one-line check that
returns the same answer for every reader. Read as *ultimate origin*, it is an argument — and §2 says
in its own words that it was not written to be argued.

**2. §2b was given a job in the future, and that job requires checkability.** `ADR-0089`
§Consequences: *"it forecloses the clean version of this repository's position… it will have to be
defended again the first time someone proposes a nightly. **§2b is the sentence to point at when
that happens.**"* A clause that can be satisfied by relabelling the caller cannot be pointed at.
The nightly's author writes a wrapper, types it once, and arrives with a precedent.

**3. The composite's only value is that the cycle begins while the human is elsewhere.** If the
human were going to sit at the boundary, the two-command shape costs them one typed line, once.
The composite buys exactly one thing: the run continues, unattended, from the authoring half into
the cycle. So a human-typed composite is a **cron whose clock is the length of its first half** —
and it fails the attended/unattended test that the automation reading itself proposes. This is the
point at which the permissive reading stops being a narrower rule and becomes a different one.

**4. The failure mode a composite creates, judged rather than asserted.** The concern is that the
half which writes the cases is followed, in one unattended stretch, by the half that grades them and
by step 4 of the loop, which runs `build-epic` and **changes production code** for every `blocker`
and `high`. Two things about it are true and they pull opposite ways.

- The separation `EPIC-12` built two agents for **survives** a composite. `qa` still has no `Write`,
  `qa-manager` is still the only thing that files, and catalogue rows written through `build-epic`
  still pass the ordinary review gate. The blunt form of the objection — *an agent grades its own
  case* — is answered by the reviewed PR.
- What the reviewed PR does **not** answer is whether a case's `expect` is a claim the product ever
  made. A reviewer checks a diff against its ticket; nobody runs the case. `ADR-0089` §4's
  discriminator is *reproduces by hand*, and §Consequences already names it *"a rule an agent
  follows, not an exit code… weaker than everything around it."* A composite hands that weakest
  member a whole catalogue of **never-executed** cases in the same stretch in which step 4 is
  merging diffs. Round 1 is the evidence that this is not theoretical: `SMK-03` was written by hand,
  merged, and **could not run as written** — the defect survived into a live round and was caught
  only because a tester deviated from the catalogue and said so (`STORY-1202` note 1).

So the real defect in the composite is not authorship. It is that **nothing has read a new case with
the product in front of it** before that case is able to move production code, and the composite is
precisely the shape that removes the one moment where somebody might.

**The evidence is thin in both directions, and reversibility decides the tie.** The composite has
never been built; no round has yet cost anyone an overnight wait. The two costs are not symmetric in
the one respect that matters: forbidding produces a cost that is **visible every round** — a person
waits — and therefore generates its own evidence for reversal, while permitting produces a cost that
is **invisible** — a standing condition becomes satisfiable by argument about the caller — and
precedent about how conditions are read does not come out with a `git rm`. Choosing the direction
whose mistake announces itself is the whole of the tiebreak, and it is why this ADR says no with an
explicit invitation to supersede it on round evidence.

**The deadline is real and it is short.** No composite exists today, so this costs one paragraph to
settle. The first authoring pass — the one that writes `EPIC-04`, `EPIC-05` and `EPIC-06` — is the
pass with the most never-executed cases meeting step 4 at once, and it is next. Deciding after it
means unpicking a merged round.

## Decision

### 1. `ADR-0089` §2b is amended: the heading, and one sentence

The heading **"b. No gate."** is amended to **"b. No gate, and one caller."** The heading was the
whole of the permissive reading's case and it undersold the clause; it is corrected in the open
rather than left to be re-argued, exactly as `ADR-0089` §1 corrected `ADR-0088` §1's heading.

The sentence *"and a cycle is started by a **human's command** — not a merge, not a cron, not
another skill invoking it as a step"* is amended to:

> **and a cycle is started by the human's own message and nothing else.** That message names the
> scope, `/qa-cycle` is the **first act of the turn it starts**, and no merge, cron, hook, agent or
> other skill stands between the human's keystrokes and step 1. A skill that runs a cycle as one of
> its steps is condition **b** failing, whatever started that skill.

Everything else in §2b stands byte-unchanged — `build.yml` keeps its two jobs, and no pull request,
`verify:` block or ticket waits on a QA case. **`ADR-0089` §§2a, 2c, 3, 4, 5 and 6 are untouched**,
as are `ADR-0088` §1's body and §§2–5. In particular §2's structure is unchanged: the permission is
still the conjunction of three conditions, and any one of them failing still returns the question as
a new `DEC-NNN`.

### 2. The check is one command over a declared set, and its default is refusal

The next reader checks §2b against the repository like this. It exits **0** on the tree as this ADR
merges — where two files name the cycle — and **1** the moment a fourth one does:

```bash
grep -rl "qa-cycle" .claude/skills .claude/agents \
  | grep -Ev '^\.claude/(agents/qa\.md|skills/qa-(cycle|cases)/SKILL\.md)$' \
  | awk 'END{exit (NR==0)?0:1}'
```

**Exactly three files may name the cycle**, and this ADR is the document that declares them: its own
`.claude/skills/qa-cycle/SKILL.md`, `.claude/agents/qa.md` — which names it once, to say the skill
owns the stack it does not — and `.claude/skills/qa-cases/SKILL.md` when §3's skill lands, for the
one purpose §3 allows. `.claude/agents/qa-manager.md` names it **nowhere today** and gains no licence
to. Any other skill or agent file that names it fails the check, **including one that only mentions
it**: a new mention is a new caller until an ADR says otherwise, and telling an instruction from a
remark is precisely the judgement this condition exists to avoid. Documentation outside `.claude/` —
this file, `docs/test-plan.md`, `EPIC-12` — is out of the check's scope and describes the cycle
freely.

**What it catches is the realistic evasion**: a composite arrives as a *new file*
(`.claude/skills/epic-and-qa/SKILL.md`) and the grep goes red on it without anyone reading a word.
**What no grep can catch** is whether one of the three declared files *runs* the cycle rather than
naming it, because *print this command* and *run this command* are the same string. For two of the
three that is answered structurally — `qa` is dispatched **by** the cycle and has no way to start
one; the cycle's own file is the cycle. For `qa-cases` it is answered by §3's prohibition, which is
written as a list of verbs rather than a principle so that one reviewer can check it against one
file. Saying which half is mechanical and which is read is worth more than a check that claims both.

Running this command cites no round and claims no coverage, so `ADR-0089` §2c does not reach it: it
is an exit code about which files name a skill, not a statement about a product's behaviour. Nothing
here requires it to be wired into a `verify:` block, and until some ticket carries it, it is a
command in an ADR — which §Consequences prices honestly rather than pretending otherwise.

The runtime half belongs to `qa-cycle`'s own `SKILL.md`, whose condition-**b** bullet is updated by
this ADR's PR to match §1 and to stop on any invocation that is not the human's own message.

### 3. The human types two commands, and the first one is the expensive one

```
/qa-cases EPIC-04          authoring: plans, writes and lands the cases; runs no browser
…tickets merge through the ordinary review gate…
/qa-cycle epic EPIC-04     the human's own message, first act of its turn
```

**`qa-cases` is licensed and its shape is fixed here.** It may read the epics, the ADRs,
`docs/duel-rules.md`, `docs/vision.md` and the client's own literals; plan a story and its tickets;
run them through `build-epic`, so every case lands as an ordinary reviewed PR; and update
`docs/test-plan.md`, including its §*Not yet written* table.

It may **not**: bring the stack up, start a browser, dispatch `qa` or `qa-manager`, or invoke
`/qa-cycle` by any route. **Its terminal act is a report naming — verbatim, with the scope filled
in — the command the human types next.** It prints that line; it does not run it.

That last sentence is what forecloses the obvious evasion. There is no composite for a cron to type,
because the authoring skill's last act is a report; and a cron that types the cycle command itself,
or a prompt that strings the two halves together, is a cron standing between the human's keystrokes
and step 1, which §1 forbids by name.

### 4. A case cites the merged source of its expectation, or it is not written

`docs/test-plan.md` has two rules about this and **no column for either**. §*Per-epic suites* →
*Template* rule 3 says *"cite the ADR a case derives from"*; §*How a case is written* documents an
`owner` field — *"the module holding any player-facing string the case quotes (`ADR-0089` §5)"* —
that appears in no table. The measurement: **6 of the 26 existing cases cite a merged source, and
all six do it in prose inside the `fails if` column**, which is the column for something else. A
rule honoured by under a quarter of its subjects, in a field it has to borrow, is a rule with
nowhere to live.

For every case `qa-cases` writes, both rules become **one** fifth column, `source` — which *is*
`owner`, generalised, so no table carries both:

- player-facing text → the module holding the literal (`ADR-0089` §5, byte-unchanged);
- otherwise → an ADR section, or a `docs/duel-rules.md` heading.

**A case whose expectation has no merged source is not written.** The gap is registered as a
`DEC-NNN` for the **product owner**, and the case waits for the answer. This extends `ADR-0089` §5
where it was silent — §5 governs cases that quote player-facing text — and amends nothing.

This is the clause that answers §Context's fourth force. A transcribed expectation is a technical
act; an invented one is a product claim, and step 4 of the loop will change production code to
satisfy it. Existing suites are **not** retrofitted: this ADR buys no churn in `SMOKE` and `CORE`,
and the resulting four-column-and-five-column catalogue is a cost named below.

### 5. This licenses no schedule, and says so

Nothing here weakens §2b toward a nightly, a label trigger or a cron. `ADR-0089` §Alternatives 3
already named what would: rounds as evidence, in an ADR of their own. That remains the only route.

### 6. Reversing this is one superseding ADR and one skill file

If two or three rounds show the boundary costs hours and buys nothing, the composite becomes a
superseding ADR with those rounds in it — and that evidence is exactly what does not exist today.
Reversal touches `ADR-0089` §2b, one `SKILL.md`, and nothing that runs.

## Consequences

**The workflow gains its one unchainable step, and pays for it every round.** `build-epic` runs
unattended for hours precisely so a person need not be present; QA now cannot be reached that way.
The authoring pass ends at an hour nobody can predict, and the cycle then waits on one typed line —
possibly overnight, possibly a weekend. That is a cost paid on **every** round, forever, against a
failure that has not yet occurred once. Anyone who finds that a bad trade is holding the same facts
this ADR held; the answer is §6, and the evidence they will have is exactly the evidence this
decision lacked.

**The condition binds artifacts, not prompts.** The grep in §2 sees a committed file. It does not
see a human — or a cron prompt — typing *"write the cases and then run a full QA cycle"* in one
message. §1's words forbid it and nothing mechanical catches it; the only guard is `qa-cycle`'s own
stop rule, which is prose an agent follows. This is the same weakness `ADR-0089` §Consequences
admitted of its §4, and it is admitted here rather than buried: what the condition actually buys is
that **defeating it leaves nothing in the repository**, so the process this project documents stays
honest only while the human respects a rule they can break in one sentence.

**Three documents now hold the browser rule, and two of them have amended headings.** `ADR-0088` §1
(heading amended by 0089), `ADR-0089` §2b (heading and one sentence amended here), and this file. A
reader asking *what is permitted?* reads three ADRs and two amendments. The trail is longest at
exactly the point it is most argued over, which is the opposite of what a trail is for. The
mitigation is that `.claude/skills/qa-cycle/SKILL.md` restates the current text of all three
conditions in one place and is updated by this PR — so the **operative** document is one file, and
the three ADRs are the reasoning behind it.

**§4 will slow the first authoring pass and produce `DEC`s.** A case for `EPIC-04`'s account screens
must find the ADR or the module that settles what the screen says; where none does, the case is not
written and a decision is registered for the product owner. The first pass will raise several, each
stalling a slice of the catalogue behind another agent's run. That is the intended trade — a case
with no merged source is a product claim wearing a test's clothes — but it is paid in wall-clock
time, and **the catalogue will be smaller and later than the human asked for.**

**The catalogue's tables stop being uniform.** New suites carry a `source` column; `SMOKE` and
`CORE` do not, because retrofitting 26 rows is churn nobody asked for. A reader will notice and
wonder which shape is correct; the answer is *the five-column one*, and it is written only here.

**What it buys.** §2b stays checkable by one command with a fixed answer, which is the property it
was written to have and the only thing that lets it be pointed at when the nightly is proposed. The
human is present exactly once per round — at the moment a set of never-executed cases becomes able
to move production code, which is the moment worth being present for. And the expensive half of the
request is delivered as asked: `/qa-cases EPIC-04` is one command that reads an epic, writes its
suite and lands it through the ordinary gate.

**What it forecloses.** `build-epic` closing an epic and QA'ing what it just built, in one run — the
most natural chaining anyone here will want, and now foreclosed by name rather than by silence. Any
unattended regression, including the overnight one. And, for the avoidance of a third round of this
argument: a skill that runs the cycle *conditionally*, or *only when the human is watching*, or
*only for smoke*, is condition **b** failing, not a refinement of it.

## Alternatives considered

**1. Permit the composite — read §2b as a rule about automation.** Its strongest case, and it is
strong: §2's preamble states the conditions are *"the terms that made §1's cost zero"*, and §1's
costs were a third CI job, flake on pull requests that are mostly markdown, and a `package.json`
dependency. A human-typed wrapper incurs **none** of the three, and its heading says *No gate*,
which a wrapper is not. Enforcing the clause against it therefore reads the letter against the
stated ratio — precisely the error `ADR-0089` §Alternatives 1 rejected: *"it answers by the heading
rather than by the argument the heading summarised."* The composite also has a benefit no rule gets
for free: the cases would be written by the same run that just learned which gaps the last round
exposed. Rejected on three grounds, each sufficient. Provenance traced through one intermediary can
be traced through five, so *"a human's command"* forbids nothing unless it means the immediate
caller. §2's own closing sentence pre-refuses the argument form — *"condition **b** failing, not a
refinement of it"* — and a condition whose author anticipated the evasion is not ambiguous; it is
being read against its author. And the composite's sole benefit is that the cycle starts while the
human is elsewhere, so it fails the attended/unattended test this reading itself proposes.

**2. Permit the composite, but bar the cases it authored from that run's `B(N)`.** The best of the
permitting family: the human keeps one command, and the sharp failure is neutralised directly — a
never-executed case cannot move production code in the run that wrote it, which is `ADR-0089` §4's
discriminator applied in advance instead of after the fact. Rejected because it needs a per-case
provenance ledger nobody maintains: every row would have to carry the round it was authored in, and
`qa-manager` would have to read it before computing `B(N)`, adding a second judgement to the rule
`ADR-0089` §Consequences already calls the weakest thing in the structure. It also produces a first
round that can find nothing actionable by construction — a full stack boot, a suite and a triage,
run for a report — which is a worse deal than typing the second command. And it leaves §2b
satisfiable by relabelling, which is the property being defended.

**3. Forbid the composite and forbid `qa-cases` too — cases are planned like any other work.** Its
case: `docs/test-plan.md` is a repository document, `CLAUDE.md` rule 1 says work is a file under
`tasks/`, and the planner plus `build-epic` already do exactly this. A skill that plans a story and
runs `build-epic` is a wrapper over two things that exist, and this repository has one ADR too many
already. Rejected because it answers a request for one command with *"you already have it"*, and
because the authoring pass has a repeated shape worth capturing — read the epic's Definition of
done, find the promises no case covers, source each expectation, write the rows, refuse the ones
with no source. §3 and §4 are that content; without them `qa-cases` would indeed be a wrapper, and
with them it is the thing that keeps an authored case from being an invented one.

**4. Route the question to the product owner.** Its case: the human asked for this personally, and
*how much friction the human accepts in their own workflow* has the shape of a product judgement
about the second deliverable — the documented process is a product here, and its ergonomics are part
of it. Rejected because nothing in the question turns on what a player sees, what a duel is, or what
this product refuses to be. It asks which caller may start a process in this repository and how the
next reader checks that it did: two competent engineers with `ADR-0089` §2 in front of them land in
the same place. The one genuinely product-shaped thing nearby is §4's residue — *what should the
catalogue assert where no merged decision says?* — and §4 routes each instance of it to the product
owner as its own `DEC` rather than guessing a general answer here.
