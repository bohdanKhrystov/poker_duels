# ADR-0115 — Motion never carries a fact, and reduced motion stills every surface

- **Status:** Accepted
- **Date:** 2026-09-02
- **Resolves:** `DEC-124` — **the product owner's** — does a surface this product animates owe a
  **still form** for a player whose system asks for reduced motion, and what governs it? Raised
  2026-09-02 by the planner while splitting
  [`EPIC-13`](../../tasks/epics/EPIC-13-the-living-table.md) into stories: items 1 (the acting
  seat's mark — *"pulsing or running circle"*) and 6 (chips that move) are the first continuous
  motion this product has ever had, and nothing merged reaches the question. Re-verified this run
  rather than taken on trust: zero `prefers-reduced-motion` anywhere in the repository; zero
  `animation`, `@keyframes`, CSS `transition` or Tailwind motion utility in `web-client/src` and
  `design/`; no duration, easing or motion value of any kind in `design/tokens/tokens.css`.
  Registered and answered in the same PR — the `DEC-039` path — so it never appears in an open
  table on this branch; the planner's concurrent registration, if it lands, is struck by this row.
- **Derived from the vision**, not stated by the human: *Positioning* — *"Dark, **quiet**, fast,
  minimal"* — the sentence
  [`ADR-0075`](ADR-0075-the-mark-lives-as-long-as-the-absence-that-produced-it.md) already used to
  rule an animation *furniture*; and the one-sentence version — *"free of everything that makes
  online poker feel like a casino."* The ownership precedent is
  [`ADR-0103`](ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md), which
  answered for the product owner what the table owes a fact of the player's context that the
  browser states (a 390 × 664 viewport); `prefers-reduced-motion` is the same kind of fact,
  delivered by the same messenger.
- **Applies** [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) §2 (a design value is
  born in the sheet) and §3 (taste is the human's, by looking),
  [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §§2–3, and `EPIC-13`'s
  card rule — *a card draws every state of what it draws, named*. **Amends nothing**:
  [`ADR-0102`](ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md)'s step schedule and
  its §4 seam are untouched (§3 below), the wire does not move, `poker-engine` is not opened, and
  no client asserts a game fact.
- **Registers no new `DEC`.** The CSS by which the sheet stills a token'd motion is minting-sized
  work done with the human (`ADR-0091` §3), not a mechanism anyone is blocked on — *a `DEC` nobody
  is working is noise in the open table* (`STORY-1211`).

## Context

### The first motion, and the empty ground under it

`EPIC-13` orders motion in the human's own words: *"some animation like pulsing or running
circle"*, *"shoud be animated"*. Motion is wanted; this is not a decision about whether to
animate. But the product has never animated anything — measured this run, `web-client/src` and
`design/` contain not one CSS animation, keyframe, transition or motion utility — so the first
pulse and the first chip flight will be built on ground where no rule exists.

Meanwhile every browser delivers, with every page load, a request the player has already made:
`prefers-reduced-motion: reduce`. Some players set it because motion makes them ill; some because
they want a quieter machine. Either way it is the one place a player can ask everything they use
for stillness, and they have asked. Nothing merged says what this product does with that request.

### What is actually in tension

**The vision's word is *quiet*, and the epic's word is *animated* — for the same table.** Both are
the human's. A pulsing mark is what the feedback asked for; a product that keeps pulsing at a
player whose system said *reduce* has chosen its own taste over the player's stated one, which is
the casino's move — the lobby full of slot machines is the vision's own image of what is not
wanted here, and a slot machine is precisely unrequested perpetual motion.

**The design system already bends to signals of exactly this kind, twice.** The merged token sheet
sizes type in rem *"so the player's browser preference scales every screen"*, and draws focus as
an outline because *"outlines survive forced-colors mode where box-shadow rings vanish"*. Two
system-stated facts of the player's context are honoured as ordinary design practice. Ignoring
the third would need a reason, and none is on record.

**Against all that: a rule costs design effort forever**, and the vision never says
*accessibility*. Naming a conformance standard, promising an audit, adding an audience — that
would be a commitment the vision does not make, the human's to make, and this ADR must not make
it by the back door. The question is whether honouring one browser signal can be answered
motion-sized, without the programme. It can, and §5 is the fence.

### The deadline

This is the cheapest the decision will ever be. Today the still form of every surface is the
surface itself; a rule stated now costs one drawn state per moving surface, starting from zero.
The same rule stated after the mark, the chips, the clock and whatever follows have shipped is an
audit and a retrofit across all of them. `STORY-1303`'s and `STORY-1306`'s cards are being drawn
next, and a card drawn without knowing whether an at-rest form is owed will be drawn twice.

## Decision

### 1. No fact lives only in motion

Every fact a surface states — whose turn it is, what was bet, where the pot went, how long
remains — is stated by its **still form**: the frame the surface shows when nothing is moving.
Motion may emphasise a fact; it never carries one alone. A surface whose meaning evaporates when
its animation stops is defective under this rule, whatever the player's motion preference.

This is the invariant that makes the rest nearly free: a product that already states everything
still can honour a stillness request by simply not running the garnish.

### 2. `prefers-reduced-motion: reduce` is honoured, wholesale and automatically

When the player's system asks, continuous and decorative motion does not run. The pulse holds as
a steady mark. The chip flight is skipped and the chips appear where they land. The mark's travel
from seat to seat is an arrival, not a journey. The still form is **the same surface at rest** —
nothing hidden, nothing added, no second design — so a screenshot of either form shows the same
facts in the same places.

The system's signal is the whole interface: **no in-product motion setting is added.** A player
who wants this product still says so once, to their system, like they do for every other product.
A toggle is additive later if anyone ever asks; today it would be a control and a stored
preference ahead of any player.

### 3. Motion is *how* the screen changes; a step is *that* it changes — and only motion is stilled

The line, stated once so no card has to invent it: **a step changes what the screen states;
motion is how the change is drawn. Reduced motion keeps every step and skips every how.**

What stands unchanged under the query, because it is facts arriving in order, not decoration:

- `ADR-0102`'s stepped runout — streets landing 600 ms apart are steps; the schedule, its §4 seam
  and its §5 jump-to-end are untouched. Collapsing them would recreate for exactly these players
  the two-frames defect `R1` exists to catch (`ADR-0096`: a runout must be *perceivable*).
- The turn clock's once-a-second change (`EPIC-13` item 4) — each second's numeral is a step. A
  smooth sub-second depletion drawn between them is a *how*, and is what a reduced form skips.
- A mark appearing, being replaced or being cleared (`ADR-0109`) — the replacement is the step;
  any slide or fade performing it is the how.

What the rule reaches: pulsing, travelling, sliding, shimmering, scaling — anything that runs
*between* facts. A card in doubt draws both forms and the pane judges (§4); the classification
never becomes a per-ticket product question.

### 4. One global rule, governed in two merged places: the sheet and the card

**The sheet.** A motion value — duration, delay, easing, travel distance — is a design value, and
`ADR-0024` §2 already says where those are born: `design/tokens/tokens.css`, prefixed `--pd-`,
and nowhere else. The first motion token is named `--pd-motion-*`, and the sheet carries, beside
the tokens, the product's **one** `@media (prefers-reduced-motion: reduce)` block that stills
them — honouring the signal is a property of the vocabulary, not a re-decision per surface. The
exact CSS of that block (`animation: none` versus zeroed durations leaves different mid-flight
artefacts) is the minting ticket's, worked with the human per `ADR-0091` §3, because the first
motion token is new visual language.

**The card.** A card that gives a surface motion does two things: it names what moves and the
tokens that move it, and it **draws the surface at rest as a named state** — the reduced-motion
form. This is `EPIC-13`'s own rule, *a card draws every state of what it draws*, applied: at-rest
is a state the surface has, for every player whose system asked. A card is self-contained HTML
and can genuinely run the motion it proposes, so the human's pane verdict covers both forms
(`ADR-0024` §3) and taste stays exactly where it was.

**No per-surface judgment.** No surface opts out, and no ticket re-asks. A surface whose motion
seems to be information has not found an exemption; it has found a §1 violation, and the fix is
its still form.

### 5. This ADR is motion-sized, and no wider

It answers what this product does with one signal the player's browser already delivers. It takes
no position on contrast, screen readers, keyboard navigation, captions or any conformance
standard, and it commits the product to none. A named accessibility commitment — a standard, an
audit, a promised audience — would add to what the product *is*, which is the vision's table and
the human's alone, and nobody has asked. The token sheet's existing AA contrast notes remain what
they are: design practice, not a promise. Citing this ADR to demand any of that is citing it for
something it deliberately does not say.

### 6. What the two blocked stories owe, concretely

**`STORY-1303` (the acting seat is marked).** The card draws at least three named states:
*waiting*, *acting — moving*, and *acting — at rest*. The at-rest mark must answer *whose turn is
it* by itself — a seat is marked or it is not, with no pulse needed to tell. Whether the moving
form pulses or runs a circle stays the human's choice between drawings, exactly as the epic
already placed it; the period and geometry of whichever wins are `--pd-motion-*` tokens.

**`STORY-1306` (a stack is chips, and chips move).** The flight is garnish. Under the query, a
bet's chips appear at the bet line and a won pot appears at the stack, with no travel. In both
forms every amount is stated still — the pot figure (`ADR-0107`'s total), the stack numerals, the
bet lines — so no count, destination or timing is knowable only from choreography. The card draws
the settled positions it must draw anyway, and names the flight as skipped under reduce.

## Consequences

**What it buys.** Every animated surface this product ever ships is born with a still form,
because the cards that admit motion are governed from the first one — the retrofit this decision
pre-empts never happens, and it was growing with every shipped surface. §1 doubles as a
structural casino guard: motion that may never carry a fact can never become attract-mode, the
slot machine's grammar, however many surfaces animate later — the same posture the sheet already
takes in miniature (*"it counts duels, it does not glitter"*). And any screenshotting round that
wants a deterministic, motionless frame can have one by flipping a single browser preference — a
convenience noted for whoever drives the harness, not a mandate on it.

**What it costs.**

- **Every moving surface is designed twice.** The at-rest drawing is a real state on a real card,
  reviewed by the same eye, forever. That is the price of the rule and it is paid per surface.
- **Motion may never be the sole signal, even where it would be cheapest.** A flying chip cannot
  be the only statement that a bet happened; some still element must also say it. That constrains
  composition on every future surface, including ones not yet imagined.
- **§3's line still needs judgment at its edges.** A crossfade, a depleting ring — some surface
  will sit on the boundary between *how* and *that*. The card settles it by drawing both forms
  and taking the pane's verdict; the cost is that the boundary lives in drawings and judgment,
  not in a grep.

**What it forecloses.** A per-surface exemption path, and any celebration or reward animation
whose information exists nowhere else. **What it does not foreclose:** an in-product motion
setting later (additive), retuning any duration (one token), or the human rejecting a particular
at-rest drawing at the pane — taste is untouched.

## Alternatives considered

**Owe nothing — ignore the signal.** The strongest case: the motion planned is small and
tasteful, no player has asked, the vision never says *accessibility*, and every rule costs design
effort forever — this one doubles the drawn states of every moving surface. Rejected: the
product's first continuous motion would ship overriding a request the player's own system already
states, on a design system that already bends to two signals of exactly that kind (rem type for
the font preference, outlines for forced-colors) — honouring two and ignoring the third is
arbitrary; and the refusal is not free either, it is a retrofit priced at every surface shipped
between now and the day someone re-asks.

**A still form owed, judged per surface.** The strongest case: motion is not all equal — a 12 px
pulse and a full-table chip flight differ in kind, and a global rule spends the human's pane time
on trivial stills. Rejected: *does this surface honour the player's stillness request* is a
product question, and per-surface means it is re-asked in every ticket or — the recorded failure
mode this role exists to prevent — answered silently by a coder inside one. Uniformity is also
this product's habit: one table at two widths (`ADR-0103`), one mark with one clearing rule
(`ADR-0109`), one schedule at one seam (`ADR-0102`).

**An in-product motion toggle.** The strongest case: discoverable, and a player whose system-wide
setting fits every product but this one gets control. Rejected: it adds a control, a stored
preference and a settings surface ahead of any player, when the system signal already exists and
is the quiet default; and it is the one alternative that stays fully available later, because
adding a toggle over §2's behaviour is purely additive — the cheapest-to-reverse ordering points
this way, not away.

**Reduced motion collapses the pacing too — `ADR-0102`'s steps to zero under the query.** The
strongest case: the purist reading — the player asked for less motion, and a timed reveal is
things changing on screen; fewer scheduled changes is quieter still. Rejected: the steps are
facts arriving in order, and collapsing them re-creates the beat-5 defect — two frames and
nothing between — for exactly the players the query names, making `R1` unmet for them by design;
the query's own subject is motion that moves, not information that arrives; and it would fork the
store's one schedule (`ADR-0102` §4) into two, per player, amending a merged mechanism this
decision has no need to touch.

**The still form as a second design, built for stillness.** The strongest case: a form designed
still could beat an animation frozen — a label where the moving form has a ring, say. Rejected:
two designs per surface is double the pane review and a drift pair forever, and it re-opens
`ADR-0024` §3's one-verdict shape; §2's *the same surface at rest* keeps one design whose
reduced delta is mechanically nothing; and a mark that fails when merely motionless was failing
§1 already — the repair belongs in the one design, not in a fork of it.
