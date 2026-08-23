# ADR-0070 — A blast radius is complete only when the gate set is green, and a merged gate may add the row that makes it so

- **Status:** Accepted
- **Date:** 2026-08-23
- **Resolves:** `DEC-065`
- **Amends** [`ADR-0069`](ADR-0069-the-blast-radius-is-probed-not-remembered.md) §3, whose two
  probes at two depths are replaced by one probe run to green, and §2, which stands with one bounded
  exception added by §4 below. `ADR-0069` §§1, 4 and 5 stand exactly as written, and `ADR-0068`
  §§1, 2 and 4 stand as they already did.
- **Constrains:** [`TASK-021301`](../../tasks/tasks/TASK-021301-the-wire-gains-a-rematch-and-the-version-takes-its-step.md),
  `STORY-0213`, `STORY-0214`, `STORY-0405`, every future `atomic:` ticket, the `planner` and `coder`
  agents, and `plan-story`

## Context

This is the third rule change on one question in three days, and the second time the previous
change failed on the same ticket in the same place.

`ADR-0068` §5 published twelve files. Implementation found fifteen. `ADR-0069` deleted the ceiling,
replaced the enumeration with a probe, and said fifteen. Implementation has now found
**seventeen** — the same coder, the same ticket, the same critical path, a second stop.

The two files `ADR-0069` §3's probe could not see, both verified on `develop`:

| # | File | Gate that holds it | When it fails |
| --- | --- | --- | --- |
| 16 | `poker-server/src/test/kotlin/duels/poker/server/protocol/ServerMessageHandshakeTest.kt` | `theErrorSetIsExactlyWhatIsDeclared` asserts `ProtocolError.entries.map { it.name }` equals nine hard-coded names | test **execution** |
| 17 | `poker-server/src/test/kotlin/duels/poker/server/protocol/typescript/TypeScriptDeclarationsTest.kt` | `aSealedHierarchyIsAUnionOfItsVariants` asserts the exact string `export type ClientMessage = Act \| CreateRoom \| Hello \| JoinRoom;` | test **execution** |

### The paragraph contradicts itself, and that is only the first defect

`ADR-0069` §3 defines the probe as two edits and four commands. Probe **(a)**, the version's radius,
runs `./gradlew :poker-server:check` and `npm run check` — both full. Probe **(b)**, the wire
*shape*'s radius, runs `./gradlew :poker-server:compileTestKotlin` and `npm run typecheck` — both
compile-level. Both files above live in **(b)**'s radius and fail at test execution, so **(b)** as
written cannot see either.

The same paragraph's justification sentence reads: *"`:poker-server:check` compiles test sources and
`npm run check` runs `vitest` — which is why the probe finds what §5's reading did not."* That
sentence describes **(a)**'s commands and is offered as the warrant for the whole procedure. The
rule and its own reason disagree, one line apart.

### Why the obvious repair is not enough, which is the actual finding

Make **(b)** run the same full commands as **(a)**. It reads like the whole fix. It is not, for two
independent reasons, and both are general.

**First: a red run of the gate set names a *prefix* of the blast radius, never the whole of it.**
Gradle stops at the first failing task. Adding a `ServerMessage` variant breaks the exhaustive
`when` in `DuelSocket.kt` — **main** sources — so `:poker-server:check` fails at `compileKotlin` and
never reaches `compileTestKotlin`, never reaches `test`, and never runs `ServerMessageHandshakeTest`
or `TypeScriptDeclarationsTest`. An operator running full commands once still reads compile errors,
writes down the files they name, and stops — which is *exactly* what happened in round one, where
side **(a)** already used full commands. Two dispatches, two days and two `DEC`s apart, this
repository has now performed one iteration of a loop by hand and lost the second term of the union
each time. The depth of the command was never the whole problem; **stopping after one run** was.

**Second: `ADR-0069` §3's probe edit is itself an enumeration, and it is missing a member.** It says
*"add one throwaway variant to each sealed hierarchy the story touches"*. `ProtocolError` is an
`enum class`, not a sealed hierarchy, so the probe never adds an error value, so
`theErrorSetIsExactlyWhatIsDeclared` cannot fail under it **at any command depth**. File 16 is
invisible to the corrected-command probe as well. `ADR-0069`'s own alternatives section names this
in passing — *"`ProtocolError` is an enum that an exhaustive `when` could cover tomorrow"* — and
then writes a probe that does not touch it.

### The command list is also narrower than the gate set it stands for

`.github/workflows/build.yml` runs `./gradlew check -PrequireDocker=true` — the **root** task, all
three modules — then `npm run check` and `npm run build` in `web-client/`. `ADR-0069` §3 copied
`:poker-server:check` and dropped `npm run build`. Nothing bad has come of that yet. It is the same
defect one more time: a command list copied out of a gate set drifts from the gate set.

### Four attempts, one mechanism

| Written | Procedure | How it failed |
| --- | --- | --- |
| `ADR-0045` §4 | a list of four files | grew to five |
| `ADR-0047` §6 | a list of five files | grew to twelve |
| `ADR-0068` §5 | a list of twelve files, produced by reading | grew to fifteen |
| `ADR-0069` §3 | a list of **commands**, and a list of **language constructs** | grew to seventeen |

`ADR-0069` correctly refused to write another list of files, and then wrote two lists of something
else. **Every one of the four is a copy** — of files, of categories, of commands, of constructs —
and a copy drifts from what it copies, silently, because nothing couples the two. That is the whole
of the diagnosis, and it says what the replacement has to be: **a reference and a termination
condition, and no copy anywhere.**

### What is in tension, and it is not obvious

`ADR-0069` §2 — *a file the ticket's Files table does not name stops the ticket and raises a `DEC`,
at any count* — is the entire growth protection. It has fired three times on this ticket and it was
right all three times: nothing was invented inside a ticket, and the two ADRs before this one exist
because of it.

It has also cost three architect dispatches, three PRs and two days on a path where `STORY-0213`,
`STORY-0214` and `STORY-0405` are strictly serial behind one ticket (`ADR-0045` §3 allows one
bumping branch at a time). The stop is both the thing that works and the thing that hurts, and the
reason it hurts is that it cannot tell *"I found a file the planner's procedure should have found"*
from *"I decided this file is related"*. Both look identical to the rule: a file not in the table.

### The evidence that settles the count

Already gathered, and not re-derived here. With the two one-line additions applied to files 16 and
17, `./gradlew :poker-server:check` runs **green in full** — `verifyProtocolTypes`, the whole test
task, `verifyDuelScript`, `ktlintCheck` — and `cd web-client && npm run check` is exit 0 at the
fifteen files already committed on `task/TASK-021301-rematch-wire-and-version-bump`. That work is
sound and is not redone.

### The deadline

Unchanged and still real: three stories are serial behind one ticket. Separately, §4 below is
cheapest today, while exactly one `atomic:` ticket exists in the repository.

## Decision

### 1. One probe, and its command set is the CI gate set, by reference

`ADR-0069` §3's probes **(a)** and **(b)** become **one probe**: every stub edit is applied
together, one command set is run over the result, and the blast radius is that one run's output —
not a union assembled by hand from two runs. Two probes that run the same commands are not two
probes; they are one probe and an opportunity to forget half of it, which is the opportunity this
repository has now taken twice.

**The probe's commands are the ones `.github/workflows/build.yml` runs on a pull request, verbatim
and in full.** They are named by reference, never copied: a probe run is valid only if the commands
it ran are the workflow's. As of today the workflow runs `./gradlew check -PrequireDocker=true`,
then `npm ci`, `npm run check` and `npm run build` in `web-client/` — recorded here as an
observation with a date on it, not as the rule. The rule is the reference, so a check added to CI
tomorrow is in every probe from tomorrow with no ADR edited, and **no document may publish a
narrower command for a probe**, this one included.

A run that skips a suite has not run the gate set. `-PrequireDocker=true` is part of the commands
for that reason: without it a missing daemon turns a container test into a skip, and a skipped test
names no file. If the daemon is genuinely unavailable, the probe is **declared incomplete in the
ticket**, naming the suite that did not run; it is not silently treated as green.

### 2. A red run is a prefix. The probe is complete when the gate set is green

The gate set stops at its first failure — Gradle at the first failing task, `npm run check` at the
first failing script. **So any red run names a prefix of the blast radius and can never name all of
it.** Reading one red run's output is exactly as incomplete as reading a source directory, and for
the same reason: something downstream never got a chance to speak.

The probe is therefore a **loop**:

1. Apply the stub edit (§3).
2. Run the gate set (§1).
3. Every path it names becomes a *Files* row, with the failing gate as its *why it cannot be fewer*.
4. At each named path apply the minimal edit that gate demands — the same one-line propagation the
   real change will make, adding no behaviour.
5. Go to 2.

**The enumeration is complete when, and only when, the gate set exits 0 with the probe applied.** No
count, no reading, no single run and no previous ticket's list may stand in for that exit code.
There is no prefix of green.

Then revert **every** path the loop touched — the stubs and the propagations both. By then that is
more files than the operator started with, so `git status` is the list, and the probe must be the
only thing in the tree when it runs.

### 3. The probe's edit is the story's declared surface, in stub form

For **every declaration the story adds, removes, renames or re-values** — a message type, an enum
entry, a field, a constant's value — the probe makes that same declaration change under a throwaway
name, carrying no behaviour. It is derived from the story, which is a document the planner already
has, and not from a list of language constructs, which is a copy that drifts.

Two consequences worth stating, because each is a defect that has already occurred:

- An **enum entry is a declaration**. `ADR-0069` §3's *"each sealed hierarchy"* excluded
  `ProtocolError` by wording, and that is why file 16 was invisible.
- **A stub carries the fields the story states.** A gate can key on a field's presence or type, and
  a field-free stub does not trip it. Where `ADR-0044` says `RematchOffered(seat)`, the probe's stub
  carries an `Int`.

### 4. A merged gate may complete the *Files* table; nothing else may

`ADR-0069` §2 stands: a file the table does not name stops the ticket and raises a `DEC`, at any
count. **One exception, and it is bounded by four conditions that are each an exit code or a diff.**

A coder may add a row to the ticket's own *Files* table, update `files_touched` to match, and
continue, when **all four** hold:

1. **A merged gate fails and its output names the path.** The failure message goes in the PR body,
   quoted.
2. **The ticket's own declared edits are what make it fail.** Reverting that one file and nothing
   else leaves the gate failing. If the failure needs an edit the ticket did not authorise, this
   does not apply.
3. **The edit is propagation, not decision**: it brings a declaration or an expectation back into
   agreement with the authorised change, adds no behaviour, adds no test, and **weakens or deletes
   no assertion**. If the gate admits more than one correct edit, it is a decision.
4. **The full gate set (§1) then exits 0** with no other file changed.

The added row carries the gate that forced it, exactly as a planned row does, and the PR body lists
every row added this way. Anything failing any of the four — a file needed for design, a new type, a
rename, a refactor, a test the coder wants — is a `DEC`, unchanged.

**Condition 3 is aimed at a specific temptation.** Both files this ADR adds hold *golden*
assertions: a hard-coded list of nine error names, and a hard-coded union string. The tidy-looking
fix at each is to derive the expectation from the thing it checks — which turns both into `x == x`
and destroys them. `theErrorSetIsExactlyWhatIsDeclared` is the only thing in the repository that
makes adding a `ProtocolError` value a deliberate act, and `TypeScriptDeclarationsTest`'s literal is
the generator's independent witness. This is `ADR-0069` §4 rule 1 in a second place: **the
expectation is updated, never derived and never deleted.**

**What this buys is precisely the distinction the stop could not make.** After §§1–3, a file a coder
finds this way is a file the correct probe would have named — the coder is finishing an enumeration,
not widening a ticket — and conditions 1, 2 and 4 say so mechanically rather than on the coder's
word. `ADR-0068` §4's *"a scope that grew after the ticket was written"* earns nothing, still.

### 5. `TASK-021301` becomes seventeen files

`files_touched: 17`, the two rows added to its *Files* table with the gate that forces each,
`atomic:` extended by the one gate class it did not name — a merged test asserting a hard-coded
expectation over the declared set — `status: ready`, and the fifteen files already implemented on
`task/TASK-021301-rematch-wire-and-version-bump` unchanged.

**Why seventeen is the whole set**, stated as the argument rather than as a count: a file is in the
blast radius exactly when some merged gate exits non-zero until it changes (`ADR-0068` §4's
definition of a gate). Every gate in this repository is reachable from the commands in §1 — that is
what makes CI a merge condition. With those seventeen files changed and nothing else, those commands
exit 0. So an eighteenth gate-held file would have to be one that exits non-zero while the gate set
exits 0, which is a contradiction. Four, five, twelve and fifteen were each the output of a *reading*
or of a *red* run, and a red run is a prefix; seventeen is the output of a **green** one. That is a
difference in kind, not in thoroughness.

Seventeen appears in that ticket and nowhere else. It is not a rule and not a fact about protocol
bumps.

### 6. What this does not decide

- **Whether `files_touched` is ever checked against the actual diff.** `ADR-0068` §7 stands. §4
  makes the field mutable during implementation, which makes a merge-time check *more* attractive
  and no more feasible than it was.
- **The five silently-stale client fixtures.** `ADR-0069` §5 stands unchanged: no gate holds them,
  they are not in `TASK-021301`, and they are a named ticket.
- **Whether `Welcome.protocolVersion` is generated as the `ProtocolVersion` literal alias.**
  `ADR-0069` §7's trigger stands.
- **Anything about the rematch feature itself.** `ADR-0044` is the specification and is untouched.

## Consequences

**What it buys.**

- `TASK-021301` finishes on its next dispatch at its true size, and three serial stories move.
- **The enumeration acquires a termination condition.** Every previous formulation ended when
  somebody stopped reading — a list, a directory, a compiler's first complaint. This one ends on
  `exit 0`, which is not a judgement and cannot be arrived at early.
- **No copy remains in the procedure.** No file list, no category list, no command list, no
  construct list. A check added to CI joins every future probe with nothing edited.
- The three failures this run produced become one loop that runs inside one dispatch, at build cost
  rather than at ADR cost.
- §4 converts the cheapest class of stop — a one-line propagation forced by a merged gate — from
  three PRs into a row and a quoted failure message, without touching the class of stop that
  matters.

**What it costs.**

- **The probe is now the most expensive step in planning.** A root `./gradlew check` with Docker,
  plus `npm ci`, `npm run check` and `npm run build`, iterated until green — for a protocol bump
  that is several full builds and tens of minutes, spent before any design exists, by a planner who
  may find the story is not viable. The pressure to run something narrower will be constant, and §1
  answers it only with a rule. This is the sharpest cost here and it is deliberate: the alternative
  is a cheap procedure that has now been wrong four times.
- **§4 hands the coder a unilateral edit of its own contract.** The *Files* table stops being purely
  the planner's promise and becomes the planner's promise plus what the gates forced. The four
  conditions are checkable, but *condition 3* is judgement, and on a `review: standard` ticket the
  reader is a cheap model. A coder that quietly makes an unauthorised edit inside an already-named
  file can manufacture failures that look like propagation, and nothing mechanical catches that.
- **`files_touched` on an atomic ticket becomes as-implemented rather than as-planned.** Any future
  metric comparing planned size to actual size loses its input unless the PR body preserves the
  original, which §4 requires by convention and no script enforces.
- **A probe run without Docker is quietly narrower than CI**, and §1's remedy is that the operator
  declares it. Declarations are the part humans skip; this is a known soft spot rather than a solved
  one.
- **Four documents now answer one question**, spanning three days: `ADR-0047` §6, `ADR-0068` §5,
  `ADR-0069` §3 and this. A reader who wants the current rule must read the newest and trust the
  amendment headers, and `docs/protocol.md` is the only place the whole current procedure appears in
  one piece. Five files learned `atomic:` on Friday, were edited on Saturday, and are edited again
  here.
- **The loop can be run wrong in a new way.** An operator who applies a *behavioural* fix at step 4
  instead of the minimal propagation makes the probe green early and under-reports. That failure did
  not exist before this ADR.

**What it forecloses.** A probe defined by a copied command line, permanently — any future proposal
has to argue against four consecutive procedures that failed by copying something. Reading a single
red run as an answer. And `ADR-0069` §2 as an *unconditional* rule: a coder holding a green gate set
and a quoted failure message is now finishing an enumeration, and the register that records a stop
will no longer contain the cheapest kind.

**Why this shape when the evidence is thin.** It is the cheapest of the candidates to reverse. §§1–3
are a documented procedure with nothing built — they revert by deleting a section, and
`lint_tickets.py` is untouched by this ADR on purpose, so no code depends on any of it. §4 reverts
by deleting one section and restoring one sentence in the `coder` agent, and it leaves behind only
*Files* rows that name a gate, which is what a planned row looks like anyway. The one thing that
would be expensive to undo is a *narrowing* — which is why §1 is written as a reference rather than
a list, and why the words that would let someone write a shorter command are not in this document.

## Alternatives considered

**Make probe (b) run the same full commands as probe (a), and change nothing else.** The obvious
repair, and by far the cheapest: two words in `ADR-0069` §3, no new mechanism, and it names exactly
what was missed. It has a real argument behind it — §3's own justification sentence already claims
full checks, so this is arguably a typo being corrected rather than a rule being changed. Rejected
on two independent grounds, either of which is fatal. A single run of the full gate set stops at
`compileKotlin`, because the probe's own `ServerMessage` variant breaks `DuelSocket.kt` in main
sources, so the two test-execution gates never run and the operator reads a prefix — which is what
already happened in round one, where side **(a)** was full. And probe (b)'s *edit* says *"each
sealed hierarchy"*, so `ProtocolError` is never touched and file 16 stays invisible however deep
the command goes. Repairing one of a paragraph's two defects is how this run reached its third rule
change.

**Keep two probes, both full.** Defensible: the two edits genuinely have different radii, and
`ADR-0069` was right that a version bump alone does not break an exhaustive `when` while a new
variant alone does not break a version fixture. Rejected because that is an argument for two
*edits*, not two *runs*. With identical commands the two runs' outputs must be unioned by hand, and
the failure this ADR exists to end is precisely a hand-computed union losing a term — twice, on one
ticket. Applying both edits at once yields the same set or a superset, at half the build time.

**Add the two files to `TASK-021301` and change no rule.** The smallest possible response, with a
real case: the branch is pushed, rebased and green at fifteen; the two additions are one line each;
`ADR-0069` is one day old and rewriting a rule on its second exercise is how a workflow becomes
unreadable. Rejected because it would correct the ticket to seventeen **by the same method that
produced twelve and fifteen** — someone reading a failure and writing down what they saw. Seventeen
would then rest on a coder's green run with no rule anywhere saying that green is what makes a
count complete, so the next bump would size itself by reading `ADR-0069` §3 and land here a fourth
time. The rule, not the number, is what has been wrong every time.

**Keep `ADR-0069` §2 unconditional and grant the coder nothing.** The strongest case against §4, and
it deserves the full statement: the stop is the only mechanism in this whole story that has never
failed. It fired three times, was right three times, and produced two ADRs and a corrected procedure
instead of a silent diff — while every *procedure* built to make it unnecessary has been wrong. A
rule with that record should not be weakened one day after its second success, and no set of
conditions distinguishes *"the gate forced it"* from *"I decided it was related"* as reliably as a
human-reviewed stop does. Rejected because §§1–3 change what a stop *means*: with a termination
condition on the enumeration, a file discovered during implementation is a file the correct probe
would have named, and conditions 1, 2 and 4 detect that mechanically rather than on the coder's
word. Against that stands a measured cost — three stalls in two days on a path with three stories
serial behind it — and the protection that actually matters, the *why it cannot be fewer* column
naming a merged gate, is preserved verbatim in the added row. It reverses by deleting §4.

**Grant the coder unconditional amendment of the *Files* table.** Simplest to write, simplest to
follow, and it has a genuine argument: the coder always holds better evidence than the planner,
because it has the real change rather than a stub. Rejected because it deletes the growth protection
outright and leaves `ADR-0068` §4's *"a scope that grew after the ticket was written"* with no
mechanism at all; `atomic:` would mean *"everything related"* on its second-ever use, which is the
exact failure both prior ADRs were narrowed to exclude. The four conditions are the whole difference
between completing an enumeration and widening a ticket.

**Have CI, or a script, compute the *Files* table.** Appealing because it removes the operator
entirely, and it is the honest end state of *"the gates know"*. Rejected on mechanism, as
`ADR-0069` already rejected its sibling: the blast radius is only observable by running the gate set
against a change, so the computation *is* the loop in §2, and putting it in a script means a
markdown lint that runs a Gradle build, an `npm ci` and a Docker daemon, several times over. That
turns a two-second lint into a half-hour one and is disabled within a month. Worse, it would have to
choose step 4's minimal propagation automatically, which is exactly the judgement §4 condition 3
reserves for a person.

**Derive the two hard-coded expectations instead of updating them** — compute the expected
`ProtocolError` names from the enum, and the expected union string from the descriptor. Genuinely
tempting, and it is the same move `ADR-0069` §4 made for version literals: it would shrink the blast
radius by two files permanently, at every future bump. Rejected, and §4 forbids it explicitly. Both
assertions are *golden*: their subject **is** the declared set, so deriving them makes each assert
`x == x` and removes the only thing that makes adding a `ProtocolError` value or changing the
generated union a deliberate act. The version-literal rule applies to fixtures where the number is
*scenery*; here the string is the assertion. Two files in a bump is the correct price for that, and
this ADR pays it.

**Write the rule as *"run every gate that exists"* and leave the operator to find them.** Shortest
possible statement of the intent, with no reference to a workflow file that might be renamed.
Rejected because *"every gate that exists"* is unrunnable — it asks the operator to enumerate, which
is the original defect at one more level of abstraction. `.github/workflows/build.yml` is not a
description of the gates; it is the gate set, because a PR merges when it passes. Naming the
artifact that already decides makes the rule checkable: you either ran those commands or you did
not.
