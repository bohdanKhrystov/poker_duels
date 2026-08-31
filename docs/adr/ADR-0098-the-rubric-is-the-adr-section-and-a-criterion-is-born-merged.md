# ADR-0098 — The rubric is the ADR section, and a criterion is born merged

- **Status:** Accepted
- **Date:** 2026-08-31
- **Resolves:** `DEC-098` — is the audit rubric
  [`ADR-0096`](ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md) §2 **itself**,
  grown by an amending ADR the way
  [`ADR-0092`](ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md) §2 and
  [`ADR-0097`](ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md) §4 both grew
  [`ADR-0090`](ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md) §2's declared-file
  set, or a **working document elsewhere** that §2 founds — and what does a round cite when it
  records a criterion `not met`: an ADR section, or a document path? Raised 2026-08-31 by the
  planner splitting
  [`STORY-1212`](../../tasks/stories/STORY-1212-the-audit-focus-the-observer-the-resize-and-what-a-criterion-costs.md),
  which routed rather than guessed (`CLAUDE.md` rule 5). Blocked nothing; due *before the rubric
  gains its sixth criterion*, and answered inside that window — before any round cited a criterion
  at all.
- **Amends:** nothing. `ADR-0096` §§2, 3, 6 and 7 are applied byte-unchanged; `ADR-0090` §2's
  declared-file set stays at **five**, because this decision adds no file anywhere. One
  **register** is repaired in this ADR's PR — `ADR-0090`'s row in the [index](README.md), one
  growth stale since yesterday (§3 below) — and an index row is a record *about* an ADR, never the
  ADR's own text.

## Context

**Both readings have merged text behind them, which is why the planner refused to pick one.**
`ADR-0096` §7 lists *"the rubric"* as a deletion distinct from *"the ADR that says why"*, and a
rubric deleted separately from its ADR reads as a file. Against that: §2 calls the rubric
*"merged, closed and general"* and supplies all five criteria in full; §3's *"a criterion
**merged** mid-invocation applies to the **next** invocation"* is satisfied exactly by an amending
ADR; and `ADR-0097`, whose stated job was *"what text changes in which files to make the focus
legal"*, names four files and no file for the rubric. The home fell between two decisions without
being refused by either.

**The force that settles it sits in neither list: where a criterion is born.** `ADR-0096` §3
fixes the only door into the rubric — a proposed criterion is *"routed exactly as `ADR-0092` §5
routes a question: a `DEC` for the product owner where `docs/vision.md` settles it, the human
where it does not, and a merged PR either way"* — and `CLAUDE.md` rule 5 says what a merged answer
is: *"An ADR is an answer once it is merged."* So every future criterion arrives as merged ADR
text **whatever this decision says**, exactly as the five founding criteria already did. The only
thing `DEC-098` can actually move is whether that text is then **copied into a second file**. Read
that way, *a working document that §2 founds* does not mean *the rubric lives elsewhere*; it means
*the rubric lives in ADRs and a copy lives elsewhere*.

**What a copy costs is not hypothetical in this repository.** `ADR-0092` §8 priced it in four
words — *"two copies of a rule drift"* — and built two agent files to avoid one; `TASK-120705`
refused a transcribed register one focus earlier; `STORY-1212` wrote the same refusal into every
ticket it split, pending exactly this decision; and two observer briefs are frozen by `sha256`
gates in five tickets because a prose copy is how a rule changes without being argued. A rubric
document would be that shape aimed at the audit's own law: the text findings are judged against,
able to disagree silently with the merged sections it transcribes, and checked by nothing — no
check compares prose across files, and this decision builds none.

**The force the other way is assembly, and its precedent shows both halves.** Once the rubric
grows, *the current rubric* stops being one table; a reader reconstructs it from a chain. The
declared-file set — the very pattern `DEC-098` names — has grown twice and worked twice, and its
register lagged once: `ADR-0090`'s index row still read *"grown to four by 0092"* today, one day
after `ADR-0097` §4 made the set five. A decision that adopts the pattern owes a mechanism for
that lag, not a denial of it.

**The deadline is real: the answer is free today and contested later.** The rubric is exactly one
merged section; no round has recorded a criterion; no copy exists. The day a round record cites a
document path, retracting the document orphans evidence; the day `R6` arrives with no decided
home, whoever answers that `DEC` improvises one inside it. Both directions get expensive from
here — a reason to decide now, not a reason to decide a particular way.

## Decision

### 1. The rubric is `ADR-0096` §2 itself, and no working document exists

The audit rubric **is** merged ADR text: today, `ADR-0096` §2's five-row table, whole and
unchanged; after any growth, that table plus every criterion a later amending ADR states. No file
holds a copy — not under `docs/`, not an agent brief, not a skill, not `docs/test-plan.md`, not a
ticket. Transcribing a criterion's text outside the ADR that states it is a defect on sight; the
refusals `STORY-1212` and its tickets carry as *pending `DEC-098`* are, from this ADR on, the
standing rule. Consumers **cite**: the `audit` observer and `qa-manager` point at §2 by criterion
id, the same way the `uat` observer points at `docs/test-plan.md` §*UAT* — the relationship is
identical, cite and never copy, and what differs between the two targets is only where each list
is born (§Alternatives 1). No file is added, so `ADR-0090` §2's declared-file set stays at five
and no declared set of any kind moves.

### 2. Growth is one amending ADR, and the id is the criterion's permanent name

A criterion is added, reworded, re-ranked or retired **only by a merged ADR amending `ADR-0096`
§2** — the pattern `ADR-0092` §2 and `ADR-0097` §4 used on the declared-file set, applied to its
second list. This adds no new door: §3 already makes every criterion's birth a merged `DEC`
answer, so the amending ADR **is** that answer — the document that argues the criterion and the
document that is its home are one document. Four rules keep the chain assemblable without a copy:

- **Ids are sequential and never reused.** `R6` is next. A retired id stays retired — the rule
  `DEC` and `ADR` numbers already follow, and for the same reason: an id in an old round record
  must never come to mean something else.
- **The amending ADR states its criterion in §2's own three-column form** — id, criterion,
  licensed by — so the assembled rubric is uniform however many ADRs state it.
- **It restates the resulting priority order as a one-line list of ids, and nothing else.**
  `ADR-0096` §5 repairs top-down, so a new criterion must take a rank; the order line in the
  newest amendment is the rubric's spine. Ids only — restating another criterion's *text* is the
  copy this decision exists to refuse, and an id has no wording to drift.
- **A criterion has one statement in force: the newest Accepted one.** Rewording `R2` is an
  amending ADR stating the new `R2`; the older section stays in its file as history, the way
  every amended ADR section here already does, and §3's index annotation says which statement is
  current.

### 3. The index row answers *what is the rubric now*, in the same PR

Every amending ADR annotates `ADR-0096`'s row in the [index](README.md) — the *"Amended by 0023,
0028"* convention already in that table — in the form *rubric grown to six by 0102 (`R6`)*, say,
**in the same PR that merges the amendment**. That row is where *what is the rubric today* is
read without opening a chain, and it is what keeps `ADR-0096` §6's metric — *criteria added per
invocation*, the row `EPIC-12` §Metrics already carries — countable from two dated registers:
growth ADRs in the index, invocations in the round stories. No archaeology; two tables and a
subtraction.

The same-PR clause is the load-bearing half, and its precedent lagged once already: `ADR-0090`'s
row read *"grown to four by 0092 (`agents/uat.md`, mention-only)"* while the set had been five
since `ADR-0097` merged — observed 2026-08-31, one day old, repaired in this ADR's PR as the
convention's first exercise. The strike-every-row rule this repository applies to `DEC` tables
applies to this annotation for the same reason: a register updated in somebody else's next PR is
a register nobody updates.

### 4. A round cites an id and an ADR section, never a path — and the freeze is a commit fact

This is `DEC-098`'s second half, the one that matters for tickets. A round record answering a
criterion cites the id and the section that states it: `R2` (`ADR-0096` §2) today; `R6` and its
own ADR the day `R6` exists. Never a document path, because there is no document. `TASK-121203`
and `TASK-121205` already cite exactly this form — *"cite it"* is the one's instruction and
transcription is refused in both — so **neither ticket changes a word and neither is rewritten**;
what changes is their footing: the refusal stops being *pending `DEC-098`* and becomes merged
law. (`TASK-121207`'s *Out of scope* observes that `EPIC-12` §*Open decisions* carries `DEC-098`
— true at its pinned commit `f8383c4e`, stale once this merges, the way every answered `DEC`
stales the sentences that watched it; its four gates read §*Termination* and `qa-manager.md`
only, and none is touched.)

`ADR-0096` §3's freeze — frozen for the invocation, a criterion merged mid-invocation applies to
the next — stops being a discipline and becomes checkable: rubric text changes only by merged
ADR, so **the rubric in force for an invocation is the amendment chain as merged at the commit
its first round names**, and every round record already names one commit. Which criteria that was
is a `git` fact. A working document could only match this by accepting edits solely from
answering PRs — at which point it adds nothing but the copy.

### 5. `ADR-0096` §7 is read, and both reversals are priced

§7 listed *"the rubric"* apart from *"the ADR that says why"* because this decision had not been
made. With the rubric in §2, those deletions are **one act**: superseding `ADR-0096` and its
amendment chain deletes the rubric, and the audit's reversal gets cheaper — no file to hunt, no
orphaned copy to remember. Reversing **this** decision is likewise one ADR: the day the chain's
length is the demonstrated obstruction, a superseding ADR founds the working document, transcribes
once at that day's size, and points citation practice forward. That asymmetry decides the thin
evidence: a document is cheap to add later and expensive to retract after rounds have cited its
path — so the reversible choice is not creating it, and that is why it is chosen.

## Consequences

**The rubric is never again one table once it grows, and every consumer pays assembly.** An
auditor's brief, a triage and a human reader all reconstruct *the rubric now* from §2 plus the
chain the index row names. At one amendment this is nothing; at five it is real friction, paid on
every invocation, and §5 prices the reversal for exactly that day. Foreclosed with it: the rubric
can never be handed to anything as a single file — no future harness check over a rubric file, no
export, no paste.

**The mechanism leans on a hand-maintained register whose precedent lagged within a day.** §3's
annotation is a discipline a reviewer checks, not a gate — nothing mechanical compares an index
row to the ADRs it summarises, and this ADR builds no such check. The repair of `ADR-0090`'s row
is on the record as both the evidence and the first exercise of the same-PR rule; the risk does
not become zero because it was named.

**A sixth criterion can never be cheaper than a full merged ADR.** No append, no quick row — even
an obviously-worded criterion pays the whole overhead: a registered `DEC`, an argued ADR, the
index annotation, the register strikes. `ADR-0096` §3 set most of that floor already; this
decision closes the residual *and then just edit the file* reading. If the human's play yields
criteria faster than ADRs merge, the queue shows up as open `DEC`s — visible, and slow on
purpose, chosen over a fast door into the list findings are judged against.

**What it buys.** No second register to rot — the answer every merged refusal already leaned
toward, now load-bearing instead of pending; a freeze that inherits merge semantics instead of
memory; a metric countable from two dated tables; two tickets that ship byte-unchanged; and a §7
reversal that got simpler. The first audit round runs exactly as `STORY-1212` planned it, citing
`R1`–`R5` (`ADR-0096` §2).

## Alternatives considered

**1. A working document that §2 founds** — `docs/audit-rubric.md`, or a sibling of the test plan.
The strongest case is real and in-house: the `uat` observer already points at a working document
(`docs/test-plan.md`), a single path would hold the whole current rubric with no assembly ever,
§7's *"delete the rubric"* would read literally, and a changelog table inside the file would carry
§6's metric by itself. **It loses on where a criterion is born.** A test-plan case is *born in
the test plan* — authored there through PRs, provisional until its first round (`ADR-0090` §5) —
so that file is a first register. A criterion is born as merged ADR text: §2 supplied all five in
full, and §3 makes every later one a merged `DEC` answer. A rubric document is therefore a second
register from its first line — downstream of the ADRs forever, able to disagree with them
silently, guarded by nothing. One assembled table is not worth a file that can quietly outvote the
law it copies; *"two copies of a rule drift"* was paid for once (`ADR-0092` §8) and is applied
here, not re-argued.

**2. §2 itself, but every amending ADR restates the whole rubric table** — the way `ADR-0097` §4
restated the whole five-file check, so the newest ADR is always the complete rubric and nobody
assembles anything. Its case is the executable-check precedent working exactly as designed. **It
loses because the two artifacts verify differently.** A restated grep is a command: `ADR-0097`
ran it and recorded three exit codes, so a drifted restatement fails loudly. A restated criterion
is prose no execution checks — alternative 1's drift, scattered across every growth ADR instead
of concentrated in one path, and a restated `R2` with one changed word is an unargued amendment
wearing a familiar table. §2's order line stays restatable because ids have no wording to drift.

**3. The rubric lives in the observer's brief** (`.claude/agents/audit.md`) — zero hops at run
time, in the one file the auditor must read anyway, already structurally gated by `STORY-1212`
(all five ids named, forbidden words, a length cap). **It loses because the brief is working
prose the process edits.** The merged source findings contradict would live in a file the round's
own machinery may touch — the judge keeping its own law — and the freeze would rest on nobody
editing an agent file, which is what this repository's `sha256` gates exist to disbelieve.
`TASK-121203` already gates the brief *"too short to have transcribed `ADR-0096` §§1–2"*: the
observer names the ids so every criterion has somewhere to be answered, and may never state them.

**4. A section of `docs/test-plan.md`** — one instrument register for all three focuses, and
§*Settled, and not a finding* proves the file already holds normative rows. **It loses on
opposite lifecycles.** A test-plan row is provisional until a round runs it; a criterion is
merged, closed and frozen before any round may see it (`ADR-0096` §§2–3). One file carrying both
regimes recreates the switched-by-a-scope-word shape `ADR-0092` §8 refused for briefs — and
`STORY-1212` kept `docs/test-plan.md` untouched with an argued reason: the catalogue is
per-screen by construction, and the audit's route map is `ADR-0096` §1's beat table, not a case
list. The transcription objection from alternative 1 then applies in full.
