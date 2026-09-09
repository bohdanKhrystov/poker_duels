# ADR-0145 — A card's scaffolding is composing; what a coder will transcribe names the mint

- **Status:** Accepted
- **Date:** 2026-09-09
- **Resolves:** `DEC-160` — does a design card's own **internal scaffolding** — a stand-in drawn
  so the subject reads *over something*, in existing tokens, never transcribed into the client —
  count as **minting** under [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md)
  §3, or is it **composing**? Registered 2026-09-09 while landing
  [`TASK-141502`](../../tasks/tasks/TASK-141502-the-card-sets-the-panel-on-two-screens-at-two-widths.md),
  whose `light` review returned `fail` on exactly this
- **Applies:** `ADR-0091` §3 (who authors a card follows what the card does) and §2 (a mechanical
  floor paired with the planner's judgment); [`ADR-0033`](ADR-0033-component-anatomy-is-born-in-its-canonical-card.md)
  §1 and §3 (the consumer test, and *promotion is one shared consumer away*);
  [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) §2 and §3 (the sheet is the only
  birthplace; the visual verdict is the human's). **Amends and supersedes nothing.** `ADR-0024`
  §3's assignment of the verdict to the human is untouched, and this ADR takes nothing away from it
- **Registers:** `DEC-161`, the architect's — §5

## Context

**The question is not whether the drawing is good. It is which side of `ADR-0091` §3 a ticket sits
on before anybody has looked at it.** §3 splits card work in two: *minting* — *"a new token, a new
component, new visual language"* — *"is worked interactively with the human, because taste does not
survive a verify block"*; *composing* — *"a screen card assembled from the settled vocabulary"* — is
*"an ordinary dispatched ticket: `module: design`, estimate `S` read as one card one file,
`review: light`"*. The planner classifies at split time, from prose, with no artifact in front of
it.

**A real artifact split two competent readers.** `TASK-141502` was dispatched as composing and its
`light` review returned `fail`, on the ground that the new `.screen` class — a dashed rectangle
holding an `<h3>` and rows, standing in for the ladder and the account screen so the panel has
something to cover — is new visual vocabulary and therefore minting. Every gate passed, the words
matched `RematchControl` exactly, and the card holds no motion. The driver landed it as composing.
Neither reader was careless; the ADR does not say which one is right.

**The axis both readers reached for does not hold weight.** `TASK-141501`'s stand-in calls itself
*"abstract on purpose"*; `TASK-141502`'s names `Leaderboard` and `Account` and draws rows. That is a
step along a specificity axis, and specificity is exactly the property a planner cannot grade
without the artifact — the thing §3 has to be applied without. A rule keyed on *how detailed is the
stand-in* is a rule that can only be applied after the drawing exists, which is one dispatch too
late.

**Scaffolding is not a novelty this ticket introduced; it is what every card in the tree is mostly
made of.** Measured on `develop` at `702408ed`, across all 21 cards under `design/`: `.wrap` in 20
of 21, `1020px` — the wrap's own column width, invented, on no sheet — in 19 of 21; `.frame` and
`.note` in all 12 cards under `design/screens/`; **406 raw `px` lengths outside `:root`**, in every
single card. None of it was ever minted, none of it is on the token sheet, and no client file has
ever transcribed a `.eyebrow`. If drawing card furniture were minting, all 86 `module: design`
tickets were misclassified, including the 43 the human has already accepted at the pane.

**The measurement the landing commit rested on is not quite right, and the record should say so.**
The claim was *"every value in `.screen` is a token; the only raw numbers are `664px` and `420px`,
the device heights `ADR-0103` §1 fixes."* The same diff also landed
`.frame.over.phone { width: 390px }` and `.frame.over.laptop { width: 960px }`, so four raw lengths
arrived, not two — and `ADR-0103` §1 fixes **phone 390 × 664 and laptop 720 × 900**. `960px` and
`420px` appear nowhere in it. They are the card's own wide column and a stand-in height chosen to
suit it: furniture, with no external source. The conclusion survives, but not for the stated reason,
and a conclusion resting on a misstated mechanism is worth correcting even when it is right.

**And the two readings of §3 that were never distinguished diverge exactly here.** Is the split
about what the card **creates for the product**, or about what the human **has not yet seen**? Every
card before this one satisfies both readings, so nothing forced a choice. `.screen` is the first
drawing that creates nothing for the product *and* has not been seen.

**The tension, named.** A stand-in that is too abstract makes the card prove nothing — `ADR-0123`
§8's *"what the panel covers is the thing most likely to be wrong"* is unanswerable over a blank
rectangle, which is why `TASK-141502` exists at all. A stand-in that is too concrete becomes a
second, ungraded drawing of a screen that already has a card, or — worse — the **only** drawing of a
screen that has none, which is precisely the failure `ADR-0091` §2 was written to stop. Both
failures are real. Neither is measured by detail.

## Decision

### 1. `ADR-0091` §3 splits on what the card creates for the product. It has never been about what the human has not yet seen.

This is settled by §3's own sentence, not by preference. §3 reads: *"**Either way** the verdict that
matters stays the human's visual one (`ADR-0024` §3), and it may **trail the merge**."* *Either way*
places the human's sight downstream of **both** branches. At merge time the human has seen no card
of either kind, so under the not-yet-seen reading the composing branch is empty and §3 contradicts
itself in its own paragraph. `ADR-0091`'s Consequences says the same thing from the other side, as
its first named cost: *"a merged card ticket reads as* the look was approved *when it only means*
the look was recorded and is structurally sound."* That sentence is only coherent if composing cards
merge unseen.

**So the split governs authorship, never approval.** It asks whether the ticket requires somebody to
choose a value that will outlive the card. The verdict on how the card looks is `ADR-0024` §3's, it
is the human's on **every** card including its scaffolding, and nothing here moves it.

### 2. A card holds two kinds of drawing, and the test is the transcriber.

- **Subject** — what the product will render. A coder transcribes it into `web-client/`. The
  card is the source, and the value is born there.
- **Scaffolding** — what the card draws so the subject reads: `.wrap`, `.eyebrow`, `.lede`,
  `.frames`, `.frame`, `.note`, and stand-ins such as `.stub` and `.screen`. Nothing transcribes it.

**The test, applied per drawing at split time: name the file that will transcribe it.** If the
answer is a client file — one that exists, or one a ticket in the same story will write — the ticket
**mints** that drawing. If the answer is *"nothing; it exists so the subject reads"*, the drawing is
scaffolding, and **scaffolding is composing, at any level of detail.**

This is [`ADR-0033`](ADR-0033-component-anatomy-is-born-in-its-canonical-card.md) §1's consumer test
lifted from values to drawings — *"The test is consumers, not units"* — and §3's escalation carries
over word for word: **scaffolding stops being scaffolding one transcriber away.** The moment a
client file is pointed at a stand-in, that stand-in has become subject and the next ticket touching
it is minting.

**The test has a direction.** Minting runs card → client. A ticket that copies *from* shipped code
into a card mints nothing, because the authority already exists downstream.

### 3. The mechanical floor: three clauses, read from the ticket and from `ls`.

`ADR-0091` §2 pairs a mechanical floor — *"a new member of the `Screen` union"* — with *"the same
rule applied by the planner's judgment"* above it. This is that shape, not a stronger claim. A card
ticket is **minting, without argument**, if any of these is true, and all three are answerable from
the ticket's `## Files` table plus a directory listing, with no artifact:

1. **It writes `design/tokens/tokens.css`.** `ADR-0024` §2 makes the sheet the only place a design
   value is born, so a token cannot be minted without this line; `check-drift.sh` already fails any
   card naming a `--pd-` the sheet does not declare.
2. **It creates a file under `design/components/` or `design/graphics/`.** `ADR-0033` §1 makes a
   component's canonical file the birthplace of its anatomy; creating one creates a component.
3. **Its scaffolding depicts a named product screen that has no card under `design/screens/`.**
   Then the stand-in is the only drawing of that screen in the tree, and `ADR-0091` §2 guarantees a
   future ticket will be pointed at it. Checked with `ls design/screens/`.

Above the floor, the planner judges by §2's test. The floor is deliberately a floor: it is
sufficient for minting and not necessary, and §1 of this ADR says which question the judgment is
answering.

**Clause 3 is what decides `TASK-141502`, and it is not the driver's stated ground.** The card names
`Leaderboard` and `Account`; `design/screens/leaderboard.html` and `design/screens/account.html`
both exist and are authoritative. The stand-in cannot become the design of either screen, because
each screen's design is already occupied. Had either card been missing, the same drawing would have
been **minting** — which is the honest answer to *where on the axis does a stand-in stop being
furniture*: not at a level of detail, but at the point where nothing else in the tree holds the
authority it depicts.

### 4. Disposition of the three tickets.

- **`TASK-141502` stands as composing. Nothing is owed.** It clears all three clauses. Its stated
  ground is corrected here rather than in a repair: the four raw lengths, and the fact that `960px`
  and `420px` are not `ADR-0103` §1's numbers, change no outcome, because raw furniture lengths are
  the tree's norm (406 of them) and none has a transcriber.
- **`TASK-141501` stands. Nothing is owed.** Its `.panel` **is** subject — `STORY-1415`'s client
  tickets transcribe it — so `minting` was the right label, and it landed at the stricter tier it
  called for.
- **The `fail` was aimed at the split, not at the diff.** A classification finding is a finding
  against the ticket's shape, which a review of a merged artifact cannot change and which no gate
  can express; the artifact met every one of its gates. Its correct destination is a `DEC`, which is
  where it went. **A `light` review may return a classification finding, and the finding travels;
  the verdict on the artifact is given on the artifact.**
- **`TASK-141511` is composing, and its ticket, tier and dispatch are unchanged.** It clears every
  clause — it writes no `tokens.css`, creates no file under `components/` or `graphics/`, and
  depicts no screen at all. It is also the cleanest possible case under §2's direction rule: it
  copies *from* shipped code, repairing two sentences to `RematchControl.tsx:65,74` and drawing a
  fourth state `RematchControl.tsx:39-45` already renders, in an existing `.dealing` treatment, with
  its own gates holding the stylesheet unchanged and `class="dealing"` at 2. Nothing on that card is
  the card's to decide.

### 5. What `minting` costs on the other side is **not** settled here, and is registered.

Measured across all 86 `module: design` tickets: four carry the `minting` label — `TASK-130301`,
`TASK-130601`, `TASK-130701`, `TASK-141501`. All four are `review: standard`. **All four were
dispatched as ordinary tickets and merged as ordinary PRs.** §3's *"worked interactively with the
human"* has not been practised once since `ADR-0091` merged on 2026-08-30, and nothing failed;
`.claude/agents/qa-manager.md` independently reads §3 as prescribing a dispatched
`module: design`, `review: light` ticket for a whole missing card.

`DEC-160` asks which side of the line scaffolding falls on, and §§1–4 answer that. What the minting
side *requires* — an interactive session, or a dispatched ticket at `review: standard` — is a
different question, and this ADR **neither ratifies nor repairs the deviation**. It is registered as
**`DEC-161`**, the architect's. It blocks nothing; its trigger is the next ticket a planner would
label `minting`.

### 6. What the planner must do, and where it is written.

`ADR-0091` §2 states its rule *"lives in **one** place: the planner's split procedure,
`.claude/agents/planner.md`"*. **Measured on `develop` at `702408ed`, that file contains no such
rule** — it does not mention `ADR-0091`, a design card, or `screen.ts`. The rule §2 placed has never
been written into the file §2 names, ten days on.

That is a defect with a known repair and no decision in it. `.claude/agents/planner.md` owes, in one
place: **`ADR-0091` §2's trigger** — a story putting a new screen in front of a player names its
card in `## Design notes`, and if no card exists the split's first ticket is the card, with the
`Screen` union as the mechanical floor — and **§3's classification as sharpened by §§2–3 of this
ADR** — name the transcriber; the three-clause floor; scaffolding is composing at any detail. This
ADR is the durable record and that file is the working copy, exactly as `ADR-0091` §2 arranged it.
The ticket is the planner's to cut from this ADR.

## Consequences

**What it buys.** The classification becomes answerable from a ticket instead of from a render, which
is the only place a planner can answer it. It predicts the thing that actually matters: a trailing
rejection of subject costs the card *plus every client file that transcribed it*, and a trailing
rejection of scaffolding costs the card alone — so the test and the blast radius are the same
question asked twice. `ADR-0091`'s trailing-verdict cost is unchanged in both directions; this ADR
grants no new permission to merge unseen and takes no verdict away from the human. And the reviewer
who dissented is answered rather than overruled on authority: detail *is* dangerous — but only when
nothing else in the tree holds the authority the drawing depicts, which is clause 3, and which
`TASK-141502` clears.

**What it costs.**

- **The floor misses the case that produced this story's strongest minting label.** `.panel` is a
  genuinely new treatment for a product surface, drawn inside a screen card. It writes no
  `tokens.css`, creates no file under `components/`, depicts no cardless screen — it clears all
  three clauses and is composing *by the floor*. Only §2's judgment catches it, and only because
  someone can name the client file that will transcribe it. `TASK-141501` was labelled correctly by
  a planner who happened to look; the floor would not have forced it. Artifact-free determinism is
  bought at exactly that price, and this is the cost most likely to be underestimated.
- **Licensed scaffolding accretes, and nothing compares it to anything.** A card may now carry a
  second, divergent picture of a screen that has its own card, and no gate notices:
  `check-drift.sh` reads token *names*, not shapes. Clause 3 checks that a canonical **exists**, not
  that the stand-in agrees with it — deliberately, because a stand-in that agreed would be a copy,
  and `TASK-141502` forbade copies for the drift reason `ADR-0033` §2 already paid for. The gap is
  real and this ADR does not close it.
- **Scaffolding is licensed to reproduce shipped strings, and one gate reads card text.**
  `TASK-141502`'s stand-in headings are `LADDER_HEADING` and `ACCOUNT_HEADING` verbatim.
  [`ADR-0142`](ADR-0142-a-text-module-is-checked-against-the-rendered-card.md) §4's one-module-one-card
  pairing contains this today — `account-text.ts` is checked against `account.html` and against no
  other file — but the containment is a property of that pairing, not of this rule. The day a text
  module is paired with a card whose scaffolding quotes another screen, a scaffolding string can
  satisfy a gate that meant to read a frame.
- **"Nothing will transcribe this" is a prediction, and predictions are wrong quietly.** The
  escalation — `ADR-0033` §3's one-consumer-away — fires only when a human or an agent notices a
  client file reaching for a stand-in. Nothing detects it.
- **A third classification vocabulary now exists**: subject/scaffolding, beside `ADR-0033`'s
  vocabulary/anatomy and `ADR-0091`'s minting/composing. Three distinctions for a planner to hold,
  where there were two.
- **`DEC-161` is created and blocks nothing**, which is by this repository's own record how a row
  sits open for a long time. Its trigger is named to make that less likely, not impossible.
- **One agent file gains a rule it should have had since 2026-08-30**, and the ticket that writes it
  is a ticket that ships no behaviour.

**What it forecloses.** Little, deliberately, and that is part of why it wins on thin evidence — one
dissenting review and one card. If scaffolding ever does acquire a transcriber, the rule inverts for
that drawing in one ticket, by `ADR-0033` §3's mechanism, with no register to rebuild. What it does
close off is **specificity as the trigger**: a rule keyed on how detailed a stand-in is could only
be applied to a finished artifact, and would have to be re-argued per card forever. It also closes
off the not-yet-seen reading of §3 — but that reading was never open, because §3's own *"either
way"* excludes it.

**The deadline: real, and short.** `STORY-1415` has eight client tickets behind these two cards, and
`TASK-141511` is the next card to be dispatched. Every further card drawn before the rule exists is
another artifact a reviewer and a driver can disagree about at merge — which has already cost one
round trip and one held story — and each disagreement is settled by an argument rather than by a
rule, which is how a precedent forms that nobody voted for. Nothing becomes impossible; the price is
paid per card.

## Alternatives considered

**Scaffolding is minting — the dissenting reviewer's position.** Its strongest case is the strongest
of the four, and it is not the one the review stated. It is this: `ADR-0091` §3's minting list ends
with *"new visual language"*, which is broader than tokens and components on purpose, and a dashed
box with a heading and rows **is** a small visual language — somebody chose a border style, a gap,
a row treatment and a height, and no verify block can hold an opinion about any of them. Routing
those choices to the human is exactly what *"taste does not survive a verify block"* asks for, and
the cost of being wrong in this direction is one interactive session, against a look nobody graded
in the other. Rejected on measurement, not on principle: taken seriously, it classifies every card
in the tree as minting — `.wrap` in 20 of 21, `.frame` and `.note` in all 12 screen cards, 406 raw
`px` outside `:root` — including the 43 `EPIC-06` cards the human has already accepted, and
including `TASK-141511`, whose scaffolding is the `.frame`/`.note` furniture it inherits. A rule
that reclassifies 86 merged tickets to catch one is not a boundary; it is the abolition of the
composing branch. What survives of it is clause 3, which is its real insight: the danger is
authority, not vocabulary.

**Keep the driver's ground — *card furniture is never transcribed into the client*, full stop.**
Strongest case: it is short, it is true today, and it needs no directory listing — a planner can
apply it from the ticket alone. Rejected because it is an assertion about the future stated as a
fact about the present. It is true of `.screen` **because** `leaderboard.html` and `account.html`
exist; drop that condition and the same sentence licenses the only drawing of an uncarded screen,
which `ADR-0091` §2 exists to prevent and which is exactly how `EPIC-04` and `EPIC-05` shipped six
undesigned screens. Clause 3 is this alternative with its unstated premise made into a check.

**Draw the line at specificity — a stand-in must be abstract; naming a real screen is minting.**
Strongest case: it is the axis both parties actually argued, it matches the two cards' own words
(*"abstract on purpose"* against `Leaderboard`), and it has a real hazard behind it — a detailed
stand-in is a drawing someone may believe. Rejected on applicability, which is `ADR-0084`'s
discipline: *abstract enough* cannot be judged from a ticket, so the planner cannot classify at
split time and the argument moves to the merge, where it just happened and cost a round trip. It is
also self-defeating on `ADR-0123` §8 — *"what the panel covers is the thing most likely to be
wrong"* is unanswerable over a blank rectangle, so the rule would forbid the card from proving the
thing it was commissioned to prove.

**Refuse the question and route it to the product owner as taste.** Strongest case, and it deserves
stating: the thing genuinely undecided is *how much drawing is too much drawing*, which sounds like
judgment about how the product should look, and `ADR-0024` §3 puts every look-judgment with the
human. Rejected because the question asked is not *is this drawing good* but *which agent authors
this ticket, at which review tier* — a routing question about the process, answerable from three
merged ADRs and from counts in the repository, and one that two competent engineers with these
constraints land on identically. The taste half is not answered here and is not deferred: the
human's visual verdict on both rematch-panel cards is owed, unchanged in scope, and covers the
scaffolding along with everything else.

**A mechanical gate — a scaffolding-class allowlist in `check-drift.sh`, or a `lint_tickets.py`
check on the `minting` label.** Strongest case: `ADR-0084` mechanised what could be mechanised, and
a rule in an agent file binds only an obedient agent — an allowlist of furniture classes
(`.wrap`, `.frame`, `.note`, `.stub`, `.screen`) would make the boundary a diff, not an opinion.
Rejected after measuring, on the same discipline: a class name carries no information about what
will transcribe it, so the gate would be checking spelling and would be defeated by naming a
subject `.stub2`. And `check-drift.sh` is `design/`'s self-consistency check (`ADR-0091` §4); the
fact it would need — which client file transcribes which rule — lives on the other side of a fence
that ADR deliberately does not reach across. What is mechanisable is §3's three clauses, and those
are read at split time from a ticket, where no new gate is needed.
