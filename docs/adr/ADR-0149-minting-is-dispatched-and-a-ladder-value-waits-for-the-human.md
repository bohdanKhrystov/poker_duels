# ADR-0149 — Minting is dispatched, and only a value that joins a ladder waits for the human

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** `DEC-161`, the architect's — what does **minting** actually require now? Registered
  2026-09-09 by
  [`ADR-0145`](ADR-0145-a-cards-scaffolding-is-composing-and-the-transcriber-names-the-mint.md) §5,
  which measured the deviation and deliberately neither ratified nor repaired it
- **Supersedes** [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §3's
  **minting branch, and that branch alone** — *"**Minting** — a new token, a new component, new
  visual language — is worked interactively with the human, because taste does not survive a verify
  block."* Everything else in §3 stands byte-unchanged: the split on what a ticket **creates**, the
  composing branch and its `module: design` / `S` / `review: light` shape, and the two closing
  sentences that keep the verdict the human's and let it **trail the merge**. `ADR-0091` §§1, 2, 4
  and 5 are untouched, and so are `ADR-0145` §§1–4, which sharpened §3's split and are **applied**
  here rather than amended. `ADR-0091`'s own file is not edited: a one-sentence supersession is
  recorded in the superseding ADR, on
  [`ADR-0117`](ADR-0117-the-proofs-of-record-load-the-built-bundle.md)'s precedent, which superseded
  `ADR-0088` §2 step 3 without touching that file's status line
- **Applies, and amends nothing:** [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) §3 —
  the visual verdict is the human's, given by looking at the rendered card — and §5, which leaves
  the visual **values** to the tickets that draw them. **No verdict moves here.** `ADR-0091`
  §Consequences names delegating taste as the one thing only the human can give away; this ADR
  changes who **drafts** a value, which no merged source ever assigned to the human, and leaves who
  **judges** it exactly where `ADR-0024` §3 put it
- **Constrains:** `.claude/agents/planner.md` (the working copy of the rule),
  `.claude/agents/qa-manager.md` (one clause, §7), `.github/scripts/lint_tickets.py` (one check,
  §5), and the `## Acceptance criteria` of every story that merges a `minting` ticket
- **Registers:** nothing

## Context

`ADR-0091` §3 generalised `EPIC-06`'s recorded deviation — *"Design tasks are worked interactively
in a Claude session… not dispatched to a coder agent. Taste does not survive a verify block"* — by
splitting on what a ticket creates: minting interactive, composing dispatched. Ten days on, the
repository does one of those two things and has never once done the other.

**Measured on `develop` at `cee03146`.** Of 86 `module: design` tickets, four carry the `minting`
label — `TASK-130301`, `TASK-130601`, `TASK-130701`, `TASK-141501`. All four are `tier: sonnet`,
`review: standard`, `status: done`; all four were dispatched to a coder and merged as ordinary PRs.
`TASK-130601` is the sharpest case, because it *quotes the rule it is breaking*: its body says
*"So this is worked **interactively with the human** — taste does not survive a verify block"*, and
it was dispatched anyway, reviewed, and merged. Nobody noticed, which means the sentence was not
being read as an instruction by any actor that executes a ticket.

**The interactive channel is `EPIC-06`'s, and it did not survive the epic.** `ADR-0145` §3's floor —
a ticket that writes `design/tokens/tokens.css`, or creates a file under `design/components/` or
`design/graphics/` — has fired **twelve** times in this repository's history. Nine of the twelve are
`EPIC-06` tickets worked under its deviation, `TASK-060101` among them — the sheet's birth, the
paradigm mint, `tier: opus`, `review: light`, `labels: [design]`. The other three are `EPIC-13`'s,
and all three were dispatched. The deviation was scoped *"in this epic"*; §3 extended it, and
nothing carried it across.

**Nothing can execute the interactive branch.** There is no ticket status for *await human*: a
schema-2 ticket is dispatched, or it is `blocked`. And `ADR-0091` §2 makes the card **the split's
first ticket**, so an unrunnable mint does not stall one ticket — it stalls every ticket behind the
card, which is the whole story. §3 itself refused this trade one sentence later, for the *verdict*:
*"it may **trail the merge**… so an unattended run never stalls at a pane."* The authorship half was
inherited rather than re-derived against the same constraint.

**What dispatch has actually decided is narrower than the sentence suggests.** The sheet declares 67
`--pd-` names and 46 hex literals. Since `ADR-0091` merged, two commits touched it and added eight
names — the `Chips` and `Motion` groups — and **not one of them is a colour**. `--pd-chip-face` is a
gradient over `--pd-text-muted`, `--pd-text-faint` and `--pd-hairline`, and says so in its own
comment (*"its face composes tones the sheet already declares, as `--pd-coin-face` composes the
coin's; no new hue is minted"*); `--pd-chip-edge` is a `var()`; and the values a coder genuinely
chose are six: `14px`, `2400ms`, `420ms`, `28px` and two easing keywords. **Zero hex literals have
entered the sheet since 2026-08-15.**

**Where the rule *was* obeyed, it was obeyed about ladders.** Twice, agents declined work citing §3,
and both refusals have the same shape. `ADR-0143` §1 declined a danger treatment because *"a
treatment needs a **new** hue and minting is the human's (`ADR-0091` §3)"*. `STORY-1309`'s close
found `.clock { font-size: 1rem }` on a card — a size the type ladder does not have — and referred
it away: *"whether the answer is minting a `1rem` step or redrawing at `1.125rem` is `ADR-0091` §3
minting, which is the human's."* That was 2026-09-04. The `1rem` still stands on **three** cards —
`design/screens/duel-table.html:121`, `design/screens/duel-table-states.html:97`,
`design/components/seat-and-pot.html:81` — and no repair ticket exists anywhere in `tasks/`.
Referred to a channel that has never run, the work simply did not happen.

**The last force is the one nobody can check.** `EPIC-06` closed recording a real verdict — *"the
human signed off the full set in the claude.ai/design pane on 2026-08-15: 16 cards"* — and each of
its four stories carries an acceptance criterion of the form *"The human has seen them there and
signed off"* — two verbatim, one *"on the direction"*, one *"seen the table there"* — each with a
dated note under it. Since that day **48 of the 90 commits under `design/`** have landed, six
cards have been created (`duels`, `leaderboard`, `sign-in`, `account`, `name-ask`, `rematch-panel`),
and **15 of the 21 cards on `develop` today have been created or modified**. No story since
`EPIC-06` carries the line — `STORY-1303`, `STORY-1306`, `STORY-1307` and `STORY-1415` each merged a
mint and none of them mentions a verdict — and no verdict of any kind is recorded anywhere in the
repository after 2026-08-15. So `ADR-0145` §5's *"nothing failed"* cannot carry the weight it looks
like it carries: the only instrument that can fail a dispatched mint is an eye, and the repository
holds no record of one being applied. That is not an argument for the interactive branch — an
unrunnable rule produced the same silence — but it is why the answer cannot be *carry on*.

## Decision

### 1. Minting is a dispatched ticket. The authorship sentence is superseded, and nothing is owed as repair.

`ADR-0091` §3's minting branch now reads:

> **Minting** — a new token, a new component, new visual language — is an ordinary dispatched
> ticket: `module: design`, estimate `S` read as one card one file, **`review: standard`**, labelled
> `minting`, and bound by §§2–4 below.

The four merged tickets are **the practice, not a standing deviation**: they owe no repair, no
re-authorship and no re-review, and neither do the nine `EPIC-06` tickets that predate the
vocabulary. `DEC-161` asked which of the two it was, and it is the second — §3's minting branch
means a stricter tier and a labelled ticket, not a different authorship channel.

**The interactive session is not abolished; it is un-mandated.** The human may work any card in a
session at any time, exactly as `EPIC-06` did, and it lands as a ticketed diff under `ADR-0024` §3's
lifecycle like everything else. What is removed is the *obligation*, which had turned a permission
the human already held into a step no agent could take.

### 2. A value that joins a ladder is the human's. That is the only exception, and it never blocks.

A dispatched minting ticket **may not add**:

- a **colour** — any hex, `rgb()`, `hsl()` or named colour that is not composed from `var(--pd-*)`
  names the sheet already declares;
- a **step in a named ladder** — `--pd-fs-*`, `--pd-space-*`, `--pd-radius-*`, `--pd-weight-*`,
  `--pd-lh-*`, `--pd-track-*`.

It **may** mint anything that opens a dimension the sheet does not yet have — a new group under its
own name, as `Chips` and `Motion` were.

The reason is not that a hue is more precious than a duration. **Inserting into a ladder changes
what every existing step means.** The sheet's own comment says the spacing scale is *"one ladder,
named by step so the scale can be retuned in one move"*; a step wedged between two others makes
every prior choice of its neighbours ambiguous and the retune impossible in one move. A colour is
the same relation without the ordering: the palette comments — *"three steps, all AA on
`--pd-bg`"*, *"one steel blue… and nothing else"*, *"Amber exists only for the turn clock running
down"* — are statements about a **set**, which a new member falsifies. A new group falsifies
nothing, because nothing composed against it yesterday. This is also the line the repository has
been drawing on its own: both recorded refusals are ladder insertions, and every one of the sheet's
46 hex literals came out of an interactive session.

**The fallback always renders, so no run stops here.** A ticket that needs a value it may not mint
draws with the nearest declared token, ships, and says which value it wanted and where it settled —
one line in the PR and one comment on the card. `TASK-130906` already did exactly this against the
`1rem` finding, composing `--pd-fs-large` and naming the deviation. The wanted value then reaches
the human as a rendered card with a note on it, which is a better brief than a ticket description,
and the mint that follows is an ordinary ticket like any other.

### 3. A minting ticket mints only what it draws, in the same diff.

The token and the drawing that uses it land in one ticket. No ticket mints a name it does not draw,
and no ticket splits the sheet from the card. This is `TASK-130601`'s own argument, generalised:
*"splitting the token from the drawing would mean choosing a chip size and a flight duration before
anything had been drawn at them, and the card ticket that then found them wrong could not fix them:
the sheet would not be in its budget."* It is also what makes a trailing verdict worth anything —
every minted value is visible on a rendered card from the moment it exists, never a line in a
stylesheet nobody looks at. All four merged mints already satisfy it.

### 4. Every minted value states what it derives from, in the sheet.

Each `--pd-` name a minting ticket adds carries a comment naming what it composes from, or the rule
it serves, in the sheet's existing idiom — `--pd-overlay: rgba(19, 18, 17, 0.8); /* = --pd-bg at
80% */`, `--pd-coin-face`'s *"composed from the three above… because `duel-coin.svg` lights from
`cx="0.36" cy="0.30"` — the two must match"*, the `Chips` group's citation of `ADR-0115` §§1, 4.
This is already the sheet's practice and costs a dispatched ticket nothing. What it buys is that a
drafted value arrives at the pane **with its reasoning attached**, so the human overrules an
argument rather than guessing at an intention.

### 5. One lint clause, and the one that is deliberately not written.

`.github/scripts/lint_tickets.py` gains, in `check_task_schema`: **a schema-2 ticket with
`module: design` whose `labels` contain `minting` must declare `review: standard` or
`review: deep`.** Measured, it fires on exactly the four minting tickets and all four already pass —
the clause costs zero rewrites and binds the next one written.

Two keys were rejected by measurement rather than by taste:

- **Not the bare `minting` label.** Four `module: web-client` tickets in `STORY-1407`
  (`TASK-140702`–`TASK-140705`) carry `minting` for the name-suggestion vocabulary, and
  `TASK-140703` is `review: light`. A gate over the label alone would fail merged work over a word
  that means something else in that module.
- **Not the `## Files` table**, which is where the *interesting* check lives: `ADR-0145` §3's floor
  clauses 1 and 2 are decidable from the table the linter already parses for `atomic:`, and they
  would catch the mint a planner forgot to label — the only failure that matters. It fails **nine
  merged tickets**: `TASK-060101` and the eight `EPIC-06` tickets that create a component or a
  graphic, every one `review: light` with no label, and every one correct under the rules in force
  when it was written. Adopting the clause today means editing a closed epic's front matter to
  satisfy a vocabulary invented after it. **Deferred with a trigger**, in the shape `ADR-0024` used
  for the generator: the first `module: design` ticket that writes `design/tokens/tokens.css`, or
  creates a file under `design/components/` or `design/graphics/`, **without** the `minting` label
  makes the Files-keyed clause a ticket, and an exemption list of the nine goes in it.

### 6. The verdict gets one place where its absence shows.

A story that merges a `minting` ticket carries one acceptance criterion naming the cards its tickets
minted onto and whether a pane verdict has been given on them, with the date if it has.
***No verdict recorded* is a complete answer; silence is not, and a story does not close on
silence.** This is not a new instrument: it is `EPIC-06`'s own line — *"The human has seen them
there and signed off."* — with its dated note, used four times in four stories and by no story
since.

An acceptance criterion is checked at story close, which is after the tickets shipped, and that is
the mechanism `ADR-0091` §Alternatives rejected for the *trigger* rule. The reason it was rejected
does not apply here: a criterion checked at close is fatal for an obligation that must fire
**before** work starts, and harmless for one `ADR-0024` §3 defines as **trailing**. A close is the
first honest moment to ask whether the trailing thing has landed. The story, not the epic, because a
story closes in days and an epic in months.

**The outstanding set, recorded here so it lives in one findable place and no closed epic has to
reopen to hold it.** As of 2026-09-10 no pane verdict is recorded for anything merged after
2026-08-15: six cards created (`duels`, `leaderboard`, `sign-in`, `account`, `name-ask`,
`rematch-panel`), nine modified (`duel-table`, `duel-table-states`, `seat-and-pot`, `action-bar`,
`flow-actions`, `create-duel`, `duel-end`, `enter-code`, `rematch-states`), and eight token names in
`tokens.css`. One sync and one pass at the pane discharges all of it.

### 7. What the working copies owe.

- **`.claude/agents/planner.md`**, lines 159–160, carries the superseded sentence **verbatim** and
  so instructs planners to do the one thing no run can do. It takes §1's replacement text and
  §§2–4's obligations. `ADR-0091`, `ADR-0145` and this ADR are the durable record; that file stays
  the working copy, unchanged in its role.
- **`.claude/agents/qa-manager.md`**'s missing-card paragraph is **right as a default** — a missing
  card is composing, and `review: light` is correct for it — and owes one clause: a missing-card
  ticket whose card would mint (`ADR-0145` §3's floor, or the transcriber test) is a minting ticket
  and takes `review: standard`.
- **`.github/scripts/lint_tickets.py`** takes §5's clause.

One ticket, three files, no shipped behaviour. It is the planner's to cut from this ADR, and its
deadline is **the next split that mints**: `ADR-0145` §6 found `ADR-0091` §2's rule missing from its
own named working copy ten days after it was written, and the same gap here leaves a file telling
planners to stall the run.

## Consequences

**What it buys.** The rule and the practice agree, so a design story no longer contains a step
nobody can take and a planner reading the working copy is not being told to stall a chain. The least
reversible taste in the system — a colour, a step in a ladder — stays with the human **without
blocking anything**, because §2's fallback always renders. The `minting` label acquires a mechanical
consequence for the first time. And the verdict acquires a place where its absence is visible: it
took a decision to discover that fifteen of twenty-one cards have changed since anyone recorded
looking at them.

**What it costs.**

- **A drafted value now ships and is transcribed with no eye on it, by rule rather than by
  accident.** `ADR-0091` priced the trailing verdict for layout; this ADR extends it to values, and
  `ADR-0145` §Consequences names the blast radius — a trailing rejection of subject costs the card
  **plus every client file that transcribed it**. The eight tokens and fifteen cards now standing
  without a recorded verdict are what that cost looks like unpaid, and this ADR makes the
  arrangement deliberate rather than accidental. This is the cost most likely to be underestimated,
  because a merged `minting` PR reads as *the value was approved*.
- **The ladder/group line will misfile something, and the direction is knowable in advance.** A new
  group can be more load-bearing than a step: `--pd-motion-turn-period: 2400ms` was the first motion
  value this product ever had, it set the whole animation grammar, and **nothing in §2 would have
  stopped a coder choosing it**. The line is drawn where the evidence is, not where the stakes are.
  It will be wrong the first time a dispatched group reads wrong across every screen, and the repair
  is a repair ticket plus one ADR moving the line.
- **§6 records debt; it does not collect it.** A story may close saying *no verdict recorded* and
  close anyway, and a close is after everything shipped. If the trailing-obligation reasoning that
  makes an acceptance criterion acceptable here is wrong, this is the first clause to delete.
- **§5's gate holds the tier only after a planner has already made the judgment.** An unlabelled
  mint passes every gate in this repository, exactly as before. That is the same honest limit the
  linter documents for `atomic:` — *"checks that it is there and that the count matches… never that
  the claim is true"* — and it is why §5 names a trigger for the clause that would actually bite.
- **A fourth distinction for a planner to hold**, on top of `ADR-0033`'s vocabulary/anatomy,
  `ADR-0091`'s minting/composing and `ADR-0145`'s subject/scaffolding: ladder versus group.
- **The history is left disagreeing with the rule.** The nine `EPIC-06` tickets that would be
  minting today carry no label and `review: light`, and this ADR does not backfill them, so a grep
  for `minting` under-counts this repository's real mints by nine, permanently. Accepted rather than
  hidden: rewriting a closed epic's tickets to match a later vocabulary is the more expensive lie.

**What it forecloses.** Very little, which is why it is safe on this much evidence — four dispatched
mints, two refusals and one unpaid verdict. The interactive channel is not dismantled: re-mandating
it costs one ADR and one agent-file sentence, and no code at all. Moving §2's line costs the same,
in either direction. What it does close off is the reading that a merged `minting` PR means a look
was approved — it means a value was drafted, structurally gated, and is now waiting to be seen,
which is `ADR-0091`'s own first cost restated one level down.

**The deadline.** This is free today and dearer every month. `EPIC-14` is minting now; every story
that mints without §6 adds cards to a set nobody can enumerate, and every week §3's sentence stands
in `planner.md` is a week in which a split can be told to wait for a session that will not happen —
which has already cost three cards a size the type ladder does not declare.

## Alternatives considered

**Repair: enforce §3 as written, and make minting wait for the human.** Its strongest case is the
strongest case in this document. Taste really does not survive a verify block; the interactive
channel is not a theory — it produced every colour, every type step and every spacing step this
product has, and the human accepted sixteen cards at the pane in one sitting. A rule broken four
times in ten days may need enforcing rather than rewriting, and *"nothing failed"* is worth nothing
when nothing checked. Rejected because **nothing can execute it**: there is no `await human` state,
the driver dispatches or blocks, and `ADR-0091` §2 puts the card **first** in the split, so the
stall is the whole story's. §3 refused exactly this trade one sentence later, for the verdict, on
reasoning it never applied to itself. And it has already been measured failing in the small: two
pieces of work were referred to this channel — `ADR-0143`'s treatment and `STORY-1309`'s `1rem` —
and one was declined outright while the other is six days old and untouched on three cards. A
channel that runs zero times a month is not a slower path; it is a bin.

**Ratify without §2's exception — dispatch everything, and let the pane catch a bad hue.** Strongest
case: it is the simplest possible rule, it is exactly what the four merged tickets did, it adds no
vocabulary for a planner to hold, and the human's verdict still governs, so a wrong colour is a
repair ticket like any other. Rejected because it overturns the one part of §3 the repository has
actually been obeying — both recorded refusals are ladder insertions — and because a trailing
verdict is the wrong instrument for this class in particular: a colour reaches every screen through
`app.css`'s `@theme static` block, so it is transcribed a hundred times before it is seen, and its
rejection is the largest repair this system can generate. The exception costs nothing to hold,
because §2's fallback renders.

**Keep the rule and give the human a queue — a `status: awaiting-human`, a parked mint, a batch the
driver holds.** Strongest case: it makes the interactive branch honest instead of unrunnable and
keeps every value under the eye that owns taste, and the driver already knows how to park work.
Rejected: parking the **first ticket of a split** parks the story, so the queue is not a queue but a
stalled epic; and a state only a human can clear is precisely what *run until blocked* exists to
avoid. It would also need the thing this workflow adds states and agents for — a decision to own —
and there is none: the human is not deciding whether to look, only when.

**A sign-off register — a per-card `@dsVerdict` marker, or `design/SIGNOFF.md`.** Strongest case: it
makes *seen* a fact a gate can read, and the measured gap is precisely that no verdict is recorded
anywhere. Rejected: it is a register invented in service of its own check — the shape `ADR-0091`
rejected for the slug↔card mapping, and the drift class `EPIC-06` spent fifteen tickets paying down.
Nothing can verify a marker is true, and it goes stale the instant the human looks at the pane
without editing a file, which is what looking at a pane is. §6 records the same fact in a document
that already exists, is already written by hand at the right moment, and is read by both actors who
care.

**Write the rule into `planner.md` in this PR instead of owing a ticket.** Strongest case: this is
the second time an ADR has placed a rule in that file, and the first time it sat unwritten for ten
days (`ADR-0145` §6). Rejected on role rather than on merit: the architect writes the record and the
planner cuts the tickets, and one PR touching `BOARD.md`, the ADR index, an agent file and a linter
is a landing hazard this driver has been bitten by before. §7 states the replacement text and the
exact lines so the ticket is transcription, and names its deadline.
