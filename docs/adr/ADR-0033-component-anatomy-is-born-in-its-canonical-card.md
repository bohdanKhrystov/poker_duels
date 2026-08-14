# ADR-0033 — A component's anatomy is born in its canonical card; the sheet holds the vocabulary

- **Status:** Accepted
- **Date:** 2026-08-15
- **Resolves:** `DEC-032` as registered on [`tasks/BOARD.md`](../../tasks/BOARD.md) — do the
  wordmark lockup's internal em ratios fall under `ADR-0024 §2`, or may they live in the
  canonical card behind a drift-gate clause? (Distinct from the `DEC-032` in this directory's
  own register, an unrelated client-storage question — the two registers collided on the
  number; the row flip that records this answer should untangle them.)
- **Clarifies:** [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) §2 — bounds what
  "size" means there; nothing else in it moves
- **Constrains:** [`TASK-060114`](../../tasks/tasks/TASK-060114-lockup-constants-join-the-drift-gate.md)
  (which it unblocks), the rest of `STORY-0601`'s gate work, and every future component card

## Context

The wordmark is deliberately not lettering: it is a CSS lockup — the product name in the
system stack with the duel coin as the mark — and four em constants in
`design/graphics/wordmark.html` are the entire drawing: a 0.92em coin, a 0.42em gap, 0.01em
letter-spacing, a 0.06em inset ring. Being em, they scale with whatever type size a card
sets; the card's own caption says it — "the coin scales with the type."
`design/screens/create-duel.html` already carries a copy of the `.mark` rules, more duel-flow
screens will, and no gate compares any copy to the canonical (#456 review, round 2). A retune
of the mark leaves stale lockups behind with no failure anywhere.

[`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) §2 reads, in full force: "Every
color, size, spacing step and radius is born in `design/tokens/tokens.css`, prefixed
`--pd-`, and nowhere else." Under the plain reading, 0.92em is a size and the four constants
belong on the sheet — where the gates that already exist (TASK-060106's name pinning,
TASK-060111's value comparison) would cover every copy with zero new code. `TASK-060114` was
instead drafted to build a bespoke gate clause and keep the constants in the card — answering
the §2 question implicitly, in ticket prose, where nobody would find it again. The #474
review pulled that answer out into `DEC-032` and blocked the ticket on it.

The precedents point both ways, which is what makes this a real decision. Sheet-ward:
TASK-060108 promoted the card-born resting shadow and back-stripe texture onto the sheet;
`--pd-track-code: 0.14em` is a single-purpose, em-denominated value living there; and the
other half of the very finding behind this DEC — the coin's glint `#b8c6d6`, a color born in
`duel-coin.svg` — was settled by conforming to §2 (TASK-060115–060117 move it to the sheet).
Card-ward: TASK-060111–060113 built the pattern of the gate reading a canonical file and
failing its stale copies, and TASK-060108 itself kept the playing card's inset ring local,
with the reason in its scope prose: "it scales with card size."

The tension, named: §2's value is its unconditionality — a rule with no boundary needs no
judgment, and every reviewer applies it identically. The sheet's value is its legibility —
it is the index of what can be retuned about this system ("everything else composes it,"
`design/README.md`), and every name on it is an invitation to consume. Component-internal
geometry is where the two collide: tokenize it and the rule stays absolute while the index
fills with names that are not system decisions; leave it card-born and the index stays honest
while the rule gains a boundary someone has to write down. This ADR is the writing-down.

## Decision

### 1. The sheet holds the vocabulary; a component's canonical file holds its anatomy

A value is born in `design/tokens/tokens.css` when it answers a question about the *system* —
a decision more than one component could legitimately consume: every color, unconditionally;
the type scale; the spacing ladder; the radii; tracking for a class of text. A value is born
in a component's canonical file when it is that component's *anatomy* — geometry that defines
the component's own shape, scales with it, and travels only where the component itself is
copied. **"Size" in `ADR-0024` §2 means the vocabulary.** Anatomy was never the sheet's, any
more than the glint position `cx=.36 cy=.30` inside `duel-coin.svg` is.

Two clarifications, so the boundary is applied rather than argued:

- **Em-denomination is evidence, not the test.** The lockup's ratios are em because they are
  shape — the mark drawn correctly at any type size. But `--pd-track-caps` and
  `--pd-track-code` are em and are vocabulary, because each answers "how does this system
  track a class of text," and a class has more members than one. The test is consumers, not
  units.
- **A color is never anatomy.** §2's "every color" stands unbounded. That is why the glint
  settlement is correct and stands: `#b8c6d6` is shared lighting across distinct renderings —
  the SVG and every CSS coin — a palette fact, on the sheet.

### 2. The four lockup constants stay born in the canonical card, and the gate pins the copies

The 0.92em coin, 0.42em gap, 0.01em letter-spacing and 0.06em ring are the wordmark's
anatomy. They are born in `design/graphics/wordmark.html`'s `.mark` / `.mark .coin` rules and
nowhere else; every other card renders the lockup only as a copy of those rules.
`TASK-060114` proceeds as filed: `design/check-drift.sh` gains a clause that reads the four
values from the canonical card and fails any other card whose `.mark` / `.mark .coin`
declarations differ, staying silent for cards that draw no lockup. This is the treatment the
gate already gives graphics geometry (TASK-060112, TASK-060113), applied to a graphic that
happens to be drawn in CSS.

### 3. Promotion is one shared consumer away

The moment a second, *independent* component legitimately wants one of these values — a
different thing, not another copy of the lockup — that value has become vocabulary. It moves
to the sheet through an ordinary ticket, exactly as TASK-060108 moved the shadow, and the
gate clause narrows by one value in the same PR. Because the boundary is a statement about
consumers, the appearance of a second consumer settles any future argument mechanically; a
case that stays genuinely unclear is a `DEC`, not a guess.

### 4. The prose stands; this ADR is its gloss

`ADR-0024` §2, the sheet's header ("The one place a design value is born") and
`design/README.md` keep their wording: they describe the vocabulary, which is all they ever
governed. This ADR is the recorded answer for the case that wording did not anticipate.
Folding a one-line pointer into the README may ride any later design ticket; nothing
requires it.

## Consequences

**What it buys.**

- The sheet stays an honest index: every `--pd-` name is a decision some other screen may
  consume, and a designer retuning the system never scrolls past the wordmark's ring width to
  reach the spacing ladder.
- `TASK-060114` unblocks exactly as filed, and the question the #474 review refused to let a
  ticket answer is answered where a reader will look.
- One rule for geometry in every medium: TASK-060108's "the ring stays local (it scales with
  card size)" remains settled rather than retroactively wrong, and SVG coordinates and
  CSS-drawn ratios stop needing separate justifications.

**What it costs.**

- A bespoke clause in `check-drift.sh` — real gate code with real maintenance, where
  tokenizing would have covered every copy with machinery that already exists. The clause is
  selector-shaped: a card that copies the lockup under some class other than `.mark` escapes
  it silently. Accepted: `.mark` is the convention, the clause fails closed for every card
  that follows it, and visual review still stands behind the ones that do not.
- §2 stops being judgment-free. "Vocabulary or anatomy?" is now a question a reviewer can
  raise; §1's consumer test is the tiebreak, and §3 names the escalation for the unclear
  case.
- Nothing is guarded until `TASK-060114` lands — this ADR builds no gate by itself, and the
  copies stay bare in the meantime.
- When `EPIC-03` builds the client's wordmark it copies literals from the canonical card
  instead of consuming `--pd-mark-*` names. Already true of every component's layout CSS; the
  mark adds no new class of problem.

**What it forecloses.** Nothing hard — deliberately, and the asymmetry is part of why this
direction wins on thin evidence. Promoting a card-born value to the sheet later is mechanical
and twice-precedented (TASK-060108 and TASK-060115 are both exactly that move); demoting a
token is a breaking rename across every card plus the vendored client sheet, because a public
name grows consumers. Card-birth keeps the cheap direction open; token-birth would have spent
it.

**The deadline, honestly: soft.** Every new screen that copies the lockup grows the migration
surface of whichever route wins, so deciding before `STORY-0602`/`0604` multiply the copies
was cheaper than deciding after. Nothing was about to become impossible either way.

**What this does not settle.**

- The ring's paint, `rgba(255,255,255,0.25)`, is a **color**, and colors stay under §2's
  unbounded clause — §1 deliberately does not shelter it. Whether it joins the sheet the way
  the glint did is a finding for the story's review loop, not part of `DEC-032`, and nothing
  here pre-answers it.
- The register collision. `tasks/BOARD.md` and this directory's README both minted a
  `DEC-032`, for different questions; this ADR answers the board's. The registers are the
  driver's to reconcile — per house rule, this ADR edits neither.

## Alternatives considered

**Tokenize all four — `--pd-mark-coin`, `--pd-mark-gap`, `--pd-mark-track`,
`--pd-mark-ring`.** The strongest case is strong: §2 stays absolute, and an unconditional
rule is the cheapest kind to hold — no boundary, no judgment, no ADR like this one; the
existing name and value gates cover every copy the day the tokens land, zero new gate code;
and `--pd-track-code` proves a single-purpose em value on the sheet is not unprecedented.
Rejected because it buys rule-simplicity by making the sheet lie: a token with exactly one
legitimate consumer, which must never diverge from that consumer, is not a retunable decision
— it is indirection wearing a public name, and public names get consumed. A future component
reaching for `--pd-mark-gap` as "a nice small gap" couples its layout to the mark's shape,
which is the exact coupling tokens exist to prevent. It also quietly re-decides TASK-060108's
merged call on the playing card's ring, and it generalizes badly: every future component's
insets and offsets follow, and the sheet's length starts tracking component count instead of
decision count.

**Tokenize only the letter-spacing (`--pd-track-mark`); gate the other three.** Strongest
case: tracking is the one of the four with sheet-resident siblings — `--pd-track-caps`,
`--pd-track-code` — so the sheet's tracking block would remain the complete answer to "how is
text tracked here." Rejected: it pays both routes' costs — the gate clause still gets built,
the sheet still gains a one-consumer name — while splitting a single component's anatomy
across two files. And the siblings argue the other way: each covers a class of text; the
mark's 0.01em covers the mark. Same test, same side. If a second brand element ever wants
that tracking, §3 promotes it then.

**No gate: the canonical card's comments are the guard, and visual review catches drift.**
Strongest case: zero machinery for four values that have changed zero times, and the #456 and
#474 reviews demonstrably caught this class by hand. Rejected: caught-by-hand is the failure
mode the gate chain was built to retire, finding by finding (TASK-060106, 060111–060113). A
mark retune that strands a stale lockup on a shipped screen stays invisible until someone
happens to review that screen beside the wordmark card again — the silent-drift shape every
clause of `check-drift.sh` exists to close.

**Make copies impossible: cards include or iframe the canonical wordmark.** Strongest case:
drift becomes unrepresentable rather than detected, and the four numbers live in one file
forever. Rejected on `ADR-0024`'s own recorded grounds: the render surface requires
self-contained cards, and an include or build step is the generator that ADR already declined
while the system is this small — "build machinery in service of four files inverts the cost."
If cards multiply until that trade flips, it flips on `ADR-0024`'s terms, not this
question's.
