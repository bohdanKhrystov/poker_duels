# ADR-0092 — A UAT round files what a merged source settles, and asks the product owner the rest

- **Status:** Accepted
- **Date:** 2026-08-30
- **Resolves:** `DEC-085` — may a UAT round exist, what may it judge, and what may it file?
  Raised 2026-08-30 by the human; registered and answered in the same PR (the `DEC-039` path —
  it never appeared in an open table). Registers `DEC-086` open for the product owner.
- **Constrains:** the agent roster, `.claude/skills/qa-cycle/SKILL.md`,
  `.claude/agents/qa-manager.md`, `docs/test-plan.md`, `scripts/qa/drive.mjs`, and `EPIC-12`'s
  round arithmetic
- **Amends:** [`ADR-0090`](ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md)
  §2's declared file set — three files become four, mention-only; everything else in it stands
  byte-unchanged
- **Applies:** [`ADR-0089`](ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
  (all six sections stand as written),
  [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) §3 (byte-unchanged), and
  [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §§1–4 (byte-unchanged;
  the human's run-now call narrows how §5's debt is collected, and that narrowing is recorded
  here rather than made silently)

## Context

The human asked, on 2026-08-30, for a **second testing pass that runs after `/qa-cycle`, over the
same catalogue cases, with a different focus** — in their words: *"focus is on UX (also question
the ux like: is the main info properly highlighted? is it clear to user what is going on? are all
options accessible? etc) and client match the designs (most of functional bugs should already be
fixed by previous qa cycle)… in case of missing design it should be reported as bug → addressed
(according to workflow in build-epic) → then testing should resume. Same for any blocker
bug/issue… same cycle as previous qa cycle but with different focus. The goal is after this
testing finished, product should look like ready for real users."*

Three forces make this a decision rather than a feature request — and the human resolved two
sub-questions while it was being decided, which this record keeps distinct from what the
architect decided.

**1. `ADR-0089`'s permission was written for a QA round, and a UAT round is a round-kind it did
not contemplate.** Its title says *for a QA round, never for a gate*; its §2 says the permission
"is exactly their conjunction" and that any condition failing *"returns the question as a new
`DEC-NNN`"*. A new round-kind is not, strictly, a condition failing — but treating it as covered
*by analogy* would make §2 satisfiable by relabelling, which is the exact property `ADR-0090`
spent an ADR refusing for §2b. So each condition is checked deliberately below, one at a time,
and the single place the extension needs a text change — `ADR-0090` §2's declared file set — is
amended in the open rather than argued around.

**2. The judgment question, which is the sharp one.** `ADR-0024` §3: *"Taste is reviewed
visually. The human accepts or rejects a design by looking at the rendered card… A verify block
cannot hold an opinion, so it does not try."* `ADR-0091` puts taste with the human at the pane,
makes the card the carrier, and names delegating taste the one thing genuinely closed off. The
human is asking an agent to ask *"is the main info properly highlighted?"* — on its face the
opinion those ADRs reserve. **The human settled the routing mid-decision**: *a UX question raised
during UAT is delegated to the `product-owner` agent; based on that agent's decision, a bug
ticket may be created and addressed through the ordinary `build-epic` workflow.* That dissolves
the contradiction without touching either ADR. The `uat` agent *raises* a question and answers
none; the product owner answers by deriving from `docs/vision.md` and the merged ADRs — its
standing licence in `docs/workflow.md` §Who answers a DEC — escalating to the human only what
would *change* the vision; and once an answer merges, the opinion is held where this repository
already keeps opinions: in a merged ADR, not in a verify block and not in an agent's taste. What
remained open, and is decided here, is the mechanics that make the routing survivable: the bar
and budget for promotion, the frozen-set timing, and the `B(N)` arithmetic.

**3. Sequencing: conformance is undefined for six of the seven screens.** `ADR-0091` §5
registers `duels`, `leaderboard`, `account`, `sign-in`, `verify` and `reset` as shipped with no
card; `design/screens/` holds exactly the v0.1 duel flow. Four options were put to the human —
land the `EPIC-06` retrofit first, run UAT now and let it file the missing cards, scope the first
run to the carded screens, split the UX half from the conformance half — and **they chose: run
UAT now, and let it file them**, on their original framing (*"reported as bug → addressed → then
testing should resume"*), content for round 1 to be largely design authoring. That choice is
theirs and stands. What it leaves the architect is the arithmetic it creates: six findings of a
class `B(N)` was never meant to count, meeting a convergence rule and an eight-ticket cap that
were built to measure a product getting better, plus a first round whose `PASS` could satisfy the
machinery while saying almost nothing about how the product looks.

**What kind of decision each piece is.** That UAT exists, that it runs now, and where a UX
question routes were the human's, and enter this record as inputs. Whether the standing
conditions license the round-kind, what separates a filable finding from a question, who owns
which judgment, and how the termination arithmetic absorbs two new classes of work are the
technical residue — two competent engineers with these ADRs in front of them should land where
this document lands. The one product-shaped question in reach — what *"ready for real users"*
means as a bar — is refused and registered (`DEC-086`, the product owner's) rather than answered.

## Decision

### 1. UAT is a second focus of the same cycle, started only by the human's own message

`/qa-cycle uat smoke`, `/qa-cycle uat epic <ID>`, `/qa-cycle uat regression`. A UAT cycle is a
cycle: `ADR-0089` §2b as amended by `ADR-0090` §1 applies to it word for word — the human's own
message, `/qa-cycle` the first act of the turn it starts, and no merge, cron, hook, agent or
other skill between the keystrokes and step 1. Three corollaries, stated because the human's
phrasing — *"next testing after regular qa cycle"* — invites the wrong reading:

- **The QA focus never chains into the UAT focus.** One turn that runs both is a skill running a
  cycle as one of its steps — condition **b** failing, `ADR-0090`'s exact holding, not a
  refinement of it. The human types two commands, on two occasions of their choosing.
- **Neither focus's report prints the other's command.** `qa-cases` prints the command the human
  types next because authoring is incomplete without a first run; a QA `PASS` is complete in
  itself, and a standing *"next: `/qa-cycle uat …`"* line would teach the reader that UAT follows
  every QA pass — false: it is the pass the human runs when they judge function settled.
- **A preceding QA cycle is the human's practice, never a checked precondition.** A skill that
  verified *"has a QA round passed at this commit?"* before running UAT would cite a round as a
  gate — §2c failing — so nothing checks it, and this sentence exists so no helpful someone adds
  the check.

### 2. The three conditions reach the UAT focus — checked one at a time, not by analogy

- **§2a, no dependency — holds, and the screenshot question is settled inside it.** UAT looks at
  screens, so `scripts/qa/drive.mjs` gains one verb, `shot`: CDP `Page.captureScreenshot` over
  the WebSocket it already holds, the PNG written with `node:fs` into the round's own
  `mktemp -d` directory. Node built-ins only; no module's dependency set changes; Chrome remains
  a machine-local binary this repository does not vendor, pin or ship. **No image-comparison
  tooling enters this repository under this ADR** — a screenshot is read by a reader, never
  diffed by a program — and the day pixel tooling is wanted, §2a is failing and the question
  returns as a new `DEC`.
- **§2b — §1 above.** Nothing further.
- **§2c, no coverage claim — holds, with its UAT corollary said out loud.** A UAT round's
  product is a dated round record: one walk, one machine, one commit. The human's stated goal —
  *"product should look like ready for real users"* — is what the record is *for* and precisely
  what the record may never *claim*: **no round, and no `PASS`, may be cited as the thing that
  made the product ready.** Readiness is a judgment made while reading the record. What its
  written bar is, if it ever has one, is `DEC-086` — the product owner's, registered open by
  this PR, because *which risks inside the software are acceptable to ship with* is that agent's
  column, and *there is no written bar; the human judges by reading* is a complete answer that
  needs saying out loud rather than falling out.
- **§3, act with a player's hands, read with anything — holds; two reads are named as reads.**
  Capturing a screenshot reads the compositor's output and touches no DOM, storage, socket or
  application state. Rendering a design card — opening `file:///…/design/screens/<card>.html` in
  a harness tab — is reading a repository file with a renderer; the card is not the application,
  so no application state is written. `forget-room` remains the single licensed storage write.
  Screenshots are working artefacts under the round's temp directory and are **never
  committed**: the durable evidence in any finding is text — rendered copy, computed styles and
  geometry read through `eval`, quoted verbatim — because the round ledger's discipline is
  verbatim quotation, and a repository of PNGs would be a second render surface nobody reviews.
- **§4 transposes: a UAT finding must be observable by a human looking.** At the screen; and,
  where a card is cited, at the screen and the rendered card side by side — by eye, not by pixel
  count. A finding a looking human cannot see — a clipped headless capture (widths under
  ~500 px clip rather than overflow), a stale card path, a geometry read taken mid-transition —
  is a **harness defect**: filed against `EPIC-12`, repaired in `scripts/qa/`,
  `docs/test-plan.md` or the UAT section, **excluded from `B(N)`**, and never repaired in
  production code. Same rule, same reason: it keeps `B(N)` a measurement of the product.
- **§5 stands and does more work here**: a conformance finding cites the card file it judged
  against, and a copy finding cites the owning module — the same reference that turns silent rot
  into findable rot.

**The one text change the extension needs.** `.claude/agents/uat.md` must say what
`.claude/agents/qa.md` says — that the cycle owns the stack lifecycle the agent does not — and
saying it names `qa-cycle`, which `ADR-0090` §2 permits in exactly three declared files, adding:
*"a new mention is a new caller **until an ADR says otherwise**."* This is that ADR, for this one
file and this one purpose. The declared set becomes **four**, `agents/uat.md` licensed to
*mention*, never to invoke, and the check becomes:

```bash
grep -rl "qa-cycle" .claude/skills .claude/agents \
  | grep -Ev '^\.claude/(agents/(qa|uat)\.md|skills/qa-(cycle|cases)/SKILL\.md)$' \
  | awk 'END{exit (NR==0)?0:1}'
```

It exits 0 while those four are the only files naming the cycle, and 1 the moment a fifth does.
Everything else in `ADR-0090` — §1's amendment, §3's two commands, §4's source rule, §5's
provisional line, §6 and §7 — stands byte-unchanged.

### 3. A UAT round makes three checks per screen, and the merged-source line decides what may be filed

The unit of UAT observation is the **screen-state**, not the catalogue case. For every screen the
scope reaches, in every state the routes produce, three checks:

- **a. Conformance** — the shipped screen against its merged card under `design/screens/`. The
  card is *"a versioned, rendered, human-accepted reference that a coder transcribes"*
  (`ADR-0091` §1), so checking the transcription is conformance, not taste. The check is not
  pixel equality — the client is responsive and a card is a fixed-width preview artefact, so
  pixel identity is false-red by construction — it is the card's structure present, its
  vocabulary (tokens, components) used, its copy verbatim, its states rendered.
- **b. Reachability** — *"are all options accessible"*: every control the product offers on this
  screen is visible and operable by a player's hands, by some route a player has. `drive.mjs`
  already reports a control that exists but cannot be seen (*"found N match(es) …, all
  invisible"*), so the observation is mechanical.
- **c. Copy against merged sources** — player-facing text contradicting the module that owns it,
  a merged ADR, or a `docs/vision.md` sentence.

**The line between a finding and a question — the classifier an agent must apply at the boundary
— is the merged source.** An observation may be filed as a finding **only when it contradicts
something merged**: a card, `design/tokens/tokens.css`, an owned literal, an ADR section, a
`docs/duel-rules.md` heading, a vision sentence. An observation with no merged source to
contradict — *this could be clearer*, *the emphasis feels wrong* — is a **question**, and §5 is
its only route. This is `ADR-0090` §4 transposed from authoring to observing: there, a case whose
expectation has no merged source is not written; here, a judgment with no merged source is not
filed. Same rule, same reason — an invented expectation is a product claim, and step 4 of the
loop changes production code for whatever gets filed.

### 4. A missing card is a `high` finding outside `B(N)`, and its screen is walked, not parked

The human chose this branch explicitly — *run UAT now, and let it file them* — over waiting for
`ADR-0091` §5's retrofit story. So:

- **A screen in scope with no merged card is a finding, severity `high`** — the round cannot do
  its conformance job there, and `ADR-0091` §2 and §5 are the merged sources the absence
  contradicts. Its repair ticket **is the card**: composition from the settled vocabulary, an
  ordinary dispatched ticket per `ADR-0091` §3, the human's visual verdict trailing the merge as
  that section allows.
- **Excluded from `B(N)`.** The six cardless screens are debt `ADR-0091` §5 already registered;
  collecting registered debt is not the product decaying, and counting six of them in `B(1)`
  would set round 2 the bar of "beat six" over a queue of design authoring — rule 4 governing
  the wrong quantity. Card tickets **do** enter the fix set and consume its eight slots — the
  cap bounds work per round, and composing cards is work — with product defects (`blocker`
  first, then `high` that counts in `B(N)`) taking slots before card tickets, and the remainder
  deferred by rule 3 as usual.
- **The dedupe key is the card's own path.** No missing-card ticket is filed while
  `design/screens/` holds the slug's card or an open ticket names that path. That one key keeps
  `ADR-0091` §5's debt in a single register even with two possible filers: UAT rounds file the
  cards their scopes reach first, and the `EPIC-06` retrofit story, when split, covers only the
  slugs still cardless. The human's call narrows §5's vehicle to that remainder; §5's register
  and every other sentence of it are byte-unchanged.
- **The screen is walked, not parked.** Checks **b** and **c** have sources independent of any
  card, so their findings file normally; only check **a** reads `BLOCKED — no card` in the round
  record, and it resumes — *"then testing should resume"* — at the first round after the card
  merges.

### 5. A UX question is the product owner's, through a bounded gate

The human's routing, fixed here as mechanics that survive a second round:

- **The `uat` agent asks and answers nothing.** Its report carries a `QUESTIONS` section — at
  most **three per screen**, the sharpest it has, each phrased so it can be answered in one
  sentence. It files nothing, holds no opinion of record, and never grades its own question as a
  finding.
- **`qa-manager` is the only promoter.** At triage it registers at most **three** `DEC`s per
  round, at most **one per screen**, for the product owner. The bar has two halves and both must
  hold: the question names a **concrete choice** answerable in one sentence — *"should the pot
  be the most prominent number on the table screen?"*, never *"does this feel right?"* — and it
  bears on **a player's ability to tell what is going on or what they may do**, the human's own
  examples (main info highlighted, clarity of state, options accessible). Below the bar, or over
  the budget: recorded in the round story **unanswered**, not re-recorded while the screen is
  unchanged, and never a ticket.
- **An answered question becomes a merged source.** The product owner's ADR either changes what
  the product should show — the shipped state now contradicts a merged source, and `qa-manager`
  files the ticket at the next triage it holds — or blesses what shipped, closing the question
  permanently: a later round re-raising it would contradict a merged source, so suppressing the
  repeat is mechanical rather than remembered.
- **The frozen set survives (`EPIC-12` §Termination rule 1).** Round *N*'s fix set is frozen at
  *N*'s triage with what *N*'s report grounds. A `DEC` registered at that triage is answered on
  its own clock, off the cycle's path; a ticket its answer yields enters the earliest
  **subsequent** round's triage, or the ordinary backlog when the cycle has ended. Nothing a
  mid-cycle answer produces can extend the round that asked.
- **Excluded from `B(N)` — the third exclusion.** A ticket born from a product-owner decision is
  the product being asked to improve, not the product being found broken: at the moment it was
  observed, the shipped state contradicted no merged source. Counting it would make a round that
  asked good questions read as a product getting worse and trip `STOP_DIVERGING` on a product
  that is improving — `ADR-0089` §4's failure class arriving through a third door. So `B(N)`
  counts product defects alone: not harness defects, not missing cards, not decision-born
  improvements.
- **Escalation does not end a cycle that was not waiting.** If the product owner returns a
  question for the human, `notify.py blocked --decision DEC-NNN` goes out while the run is warm,
  the question is carried open in the round story — and the cycle **continues**, because no step
  of the loop waits on a UX answer. `STOP_BLOCKED` fires only when the unanswered decision
  **gates a member of the current fix set**, the one case in which the cycle genuinely cannot
  proceed. Read literally, `EPIC-12`'s exit table (*"a `DEC` was raised that only the human can
  answer"*) would end three budgeted rounds over one aesthetic escalation; this ADR scopes that
  row to *gates the fix set*, and the epic's register records the scoping.

### 6. The machinery carries over; a baseline round is compared with nothing; the verdict line is qualified

Rounds as stories under `EPIC-12` (the story states its focus), one ledger, one manager, `B(N)`,
the eight-ticket cap, the three-round budget, the frozen set and the five exit states all carry
over unchanged. One ledger is load-bearing: it is what lets `qa-manager` dedupe **across**
focuses — a UAT walk stumbles on functional defects too (it does not hunt them), and a defect
found by both focuses must be one ticket. Two additions:

- **The baseline rule.** When a screen becomes conformance-judgeable for the first time in round
  *N* — its card merged in round *N−1*'s repairs — round *N* is a **baseline round**: rule 4
  does not compare its `B(N)` against `B(N−1)`, exactly as it does not compare round 1 against a
  round 0 that does not exist, because the two rounds measured differently-sized judgeable sets
  and the comparison would score the unlock as decay. Rule 5's three-round budget binds
  regardless, so the loop's worst case is unchanged.
- **The qualified verdict.** A UAT round record carries a per-screen table — checks
  **a**/**b**/**c**, each `judged`, `BLOCKED — no card`, or `out of scope` — and a verdict over
  any `BLOCKED` cell carries the qualification **inline, in the verdict line itself**:
  `PASS (conformance unjudged on 6 of 7 screens)`. The terminal report repeats that line
  verbatim. This is §2c's corollary made unlaunderable: under the human's run-now call, round 1
  will be largely design authoring, and the one line anyone reads must say so.

### 7. The catalogue is reused as a route map; the UAT half is one new section

The existing cases' `do` columns are the routes — they already reach every screen-state the
product has — and their `expect`/`fails if` columns stay functional and are never graded on UX: a
case graded on two rubrics is ambiguous on both. `SMOKE` and `CORE` rows are not touched (the
no-retrofit precedent of `ADR-0090` §4). `docs/test-plan.md` gains one **UAT section**: the
screen inventory — screen ↔ card path ↔ the case ids whose routes reach it, plus the in-duel
states the state cards draw — and the standing question list, transcribed from the human's
2026-08-30 request with this ADR as its `source`. It is catalogue content: authored under the
`qa-cases` licence or as ordinary ticketed work, landed through reviewed PRs **before the first
UAT round** (`ADR-0090` separated authoring from running, and that separation is not re-litigated
here), its sources all merged — `web-client/src/routing/screen.ts`, `design/screens/`, the case
tables themselves.

### 8. One new agent; no new manager, no new skill

- **`uat` joins the roster**: the observer under the second focus — no `Write`, acts with a
  player's hands, reads with anything, `qa`'s report shape plus the `QUESTIONS` section. A
  sibling rather than a mode of `qa`, because the two briefs contradict at the sentence level:
  `qa.md`'s noise rule — *"Do not report: wording you dislike, spacing, colour, anything
  `EPIC-06` owns"* — is load-bearing for function rounds and is the entire subject here. One
  file holding both lists, switched by a scope word, is how spacing complaints leak into
  function rounds and function-blindness leaks into UAT; two files make the leak structurally
  impossible. `qa.md` stands byte-unchanged — its refusal list now refuses things that finally
  have an address.
- **`ADR-0091`'s roster test is answered, not dodged.** *What decision would it own?* The same
  kind `qa` owns — the first opinion under its focus — while every decision this ADR creates
  lands on a role that already owns its kind: promotion, severity, classification, the
  exclusions and the verdict on `qa-manager`; answers on the product owner; card taste on the
  human at the pane. The designer agent `ADR-0091` refused would have *owned taste*; the `uat`
  agent is forbidden taste by construction — the merged-source line is the fence around it.
- **No `uat-cycle` skill and no `uat-manager`.** The termination contract exists in exactly one
  prose copy per document today; a second skill or manager would be a second copy of the
  stopping rules, and two copies of a rule drift — the *three documents hold the browser rule*
  cost `ADR-0090` §Consequences already paid is not paid twice. Two filers would also break
  dedupe across focuses and re-file each other's defects; one manager, one ledger.
- The working copies — `qa-cycle`'s `SKILL.md` (the `uat` focus, the per-screen table, the
  baseline rule, the qualified verdict), `qa-manager.md` (the promotion gate, the exclusions),
  the new `uat.md`, the `shot` verb, and the test-plan section — land through the planner's
  tickets from this ADR, through the ordinary gate.

### 9. Reversing this is one superseding ADR and five deletions

Delete `.claude/agents/uat.md`, the `uat` focus from `qa-cycle`'s `SKILL.md`, the UAT section of
`docs/test-plan.md`, the `shot` verb, and the fourth entry in §2's check, plus the ADR that says
why. Nothing imports any of it and nothing gates on any of it — §2b and §2c held, so nothing
became load-bearing — which is `ADR-0089` §6's own grade of reversibility, and the same reason
this direction is the one to try while the yield is unevidenced.

## Consequences

**The cost most likely to be underestimated: round 1's conformance is close to a tautology.** The
six missing cards will be composed by looking at the shipped screens — the natural draft —
merged on `light` structural review with the human's visual verdict trailing (`ADR-0091` §3). A
screen then conforms to a card derived from it, by construction. Until the pane verdicts land, a
UAT `PASS` proves *transcription*, not *acceptability* — and no repository artefact records which
cards the human has actually judged, a gap `ADR-0091` already named (a merged card *"reads as
'the look was approved' when it only means 'the look was recorded'"*). The qualified verdict line
says which screens were judged; nothing can say against how much taste. What redeems it is the
batched pane sign-off the retrofit cards owe, and it trails the rounds by design.

**Up to nine product-owner dispatches per invocation** — three per round, three rounds — each an
Opus run, an ADR and a merge. The promotion gate is what keeps a walk over every screen from
becoming a decision mill, and the gate is prose `qa-manager` follows: the fourth
judgment-not-exit-code rule in this structure, after `ADR-0089` §4, the merged-source line and
the exclusions below. Each addition grows the surface `ADR-0089` §Consequences already called
*"weaker than everything around it"*; the mitigations remain the manager's written-reason
discipline and the ordinary review gate on every ticket, and they remain weaker than a gate.

**`B(N)` now has three exclusions, every one prose.** Harness defects, missing cards,
decision-born improvements. The count is more honest about the product and harder to compute
mechanically; a manager that forgets one exclusion flips a verdict. Accepted because every
alternative miscounts something real: rot as decay, debt as decay, or improvement as decay.

**The baseline rule weakens rule 4.** A cycle whose repairs merge a card every round is never
compared round-over-round and runs to rule 5's budget whether or not it is winning. Bounded —
three rounds, whatever else is true — but for exactly those rounds the convergence guarantee the
human asked for by name is the budget, not the arithmetic.

**One debt, two filers, one key.** Design work will land under a QA epic's round stories — card
tickets under `STORY-12NN`, parent `EPIC-12` — while `ADR-0091` §5's retrofit story under
`EPIC-06` covers the remainder. The card path is the dedupe key that keeps the debt single, and
this paragraph is where a future reader is told the register spans two epics.

**`ADR-0090` §2's "exactly three files" lasted one day** — the same one-day precedent-weakening
`ADR-0089` §Consequences confessed of `ADR-0088`. The defence is the same shape: the growth used
the clause's own licensed route (*"a new mention is a new caller until an ADR says otherwise"*,
and this is that ADR), it is mention-only, and the check still returns one fixed answer.

**Unanswered questions accumulate in round stories, and nothing tracks them.** Below-bar and
over-budget questions are recorded and expire with their round. No register is built for them — a
register invented in service of its own check is the drift class `ADR-0091` just refused — and
the named trigger for one is the first time somebody does real work re-mining old round stories
for lost questions.

**What it buys.** The pass the human asked for exists inside every standing condition rather
than beside them. Taste moves nowhere: `ADR-0024` §3 and `ADR-0091`'s assignments stand
byte-unchanged, and the one new place an opinion can end up is a merged ADR, which is where this
repository keeps opinions. The six undesigned screens get their cards through the loop the human
chose, with the arithmetic unable to score the collection of registered debt as decay. And the
first question a future reader will ask of a green record — *did UAT say we are ready?* — has a
pre-written answer: no round may say that (§2c), and the bar, if one is ever written, is
`DEC-086`.

**What it forecloses.** Chaining the QA focus into the UAT focus in one turn — restated, not
new. Pixel tooling without a fresh `DEC` (§2a). And an agent answering a UX question by any
route: the `uat` agent reports, `qa-manager` promotes, the product owner answers, and the human
answers only what would change the vision.

## Alternatives considered

**1. Refuse the round-kind.** Strongest case: `ADR-0024` §3 and `ADR-0091` reserve visual
judgment for the human; an agent asking *"is this clear?"* is an agent holding an opinion with
extra steps; and the human can look at seven screens faster than a cycle can walk them. Rejected:
half the request is mechanical — conformance to a merged card, reachability, copy against owned
literals — which no ADR reserves for anyone; and the judgment half was settled by the human's own
routing before this ADR closed: the agent asks, the product owner answers from the vision, the
human answers only what would change it. Refusing after that settlement would be refusing an
application of `docs/workflow.md`'s routing table, not defending a principle.

**2. Block UAT on the `EPIC-06` retrofit** — this document's initial lean. Strongest case: a
single home for the card debt, no near-tautological first round, conformance defined for every
screen before the first walk — and the wait is work already owed, not new latency. **Overruled by
the human on 2026-08-30**, choosing the loop from their original request; recorded as their call
rather than re-argued. What survives of the alternative is its two sharpest points, absorbed:
the card-path dedupe key (§4) and the trailing-pane caveat (§Consequences, first paragraph).

**3. `qa` gains a `uat` scope — one agent, mode-switched.** Strongest case: no roster growth,
and `ADR-0091` refused an agent one day ago on the ground that it *"would own no decision"* —
this observer owns none either. Rejected: the two briefs contradict at the sentence level —
`qa.md`'s refusal list is load-bearing noise control and is UAT's entire subject — and a
scope-conditional inversion of a load-bearing list inside one prompt is a leak by construction,
in both directions. `ADR-0091`'s test priced a *decision-owning* agent (the designer would have
owned taste); an observer is priced by whether its brief can share a file, and this one cannot.

**4. A parallel `uat-cycle` skill and a `uat-manager`.** Strongest case: clean separation, and
UAT's stopping rules could diverge from QA's if the round-kinds turn out to need different
budgets. Rejected: nothing in the termination genuinely differs today; the stopping rules would
exist in two prose copies and drift; and two filers over one product break dedupe across focuses
— the same defect found by both kinds becomes two tickets, and `B(N)` double-counts it.

**5. Count missing cards and decision-born tickets in `B(N)`.** Strongest case: one count, no
exclusion prose, nothing for a manager to forget. Rejected on both classes: six registered-debt
cards in `B(1)` make rule 4 govern a queue of design authoring, and a decision-born ticket in
`B(N)` trips `STOP_DIVERGING` on a product that is improving. The simple count is simply wrong
twice, and `ADR-0089` §4 already established that `B(N)` measures the product or it measures
nothing.

**6. Pixel or screenshot-diff conformance.** Strongest case: wholly mechanical, no taste
anywhere, drift caught to the pixel. Rejected: the tooling fails §2a (or gets vendored, which is
worse); a responsive client against fixed-width preview cards is red by construction; and the
diff threshold is an opinion in a verify block — the thing `ADR-0024` §3 says a verify block
cannot hold and so does not try.

**7. `STOP_BLOCKED` on any human escalation** — `EPIC-12`'s exit table read literally. Strongest
case: it is what the table says, and a cycle that continues past a human-only question risks
burying it. Rejected as over-broad: the cycle never waits on a UX answer, so ending it spends the
remaining budget on nothing — and the question is not buried: `notify.py blocked` carries it to
the human while the run is warm, the round story carries it open, and the terminal report
restates it. Scoped instead to the one case with teeth: the decision gates the current fix set.
