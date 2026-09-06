# ADR-0121 — The table reflows, never scales, and the judged shape is measured on the device

- **Status:** Accepted
- **Date:** 2026-09-06
- **Resolves:** `DEC-136` — does the duel table **scale** to the viewport, or keep **reflowing** —
  and **against which viewport is the fit measured**? Raised 2026-09-06 by
  [`EPIC-14`](../../tasks/epics/EPIC-14-the-name-the-showdown-and-the-fit.md) item 3d, on the first
  photograph of this product on a real phone: an iPhone in Safari, over the local network, where the
  table **scrolled** and **both seat plates were clipped at the screen edge**. The human's four
  annotations on that photograph are *"opponent shoud fit screen"*, *"info bar should fit screen"*,
  *"conroller shoud fit screen"*, and *"table area shoud be scaled to fit the screen; scroll is
  nececerry only if table area reach some reasonable min heigt/width"*.
- **Where the answer came from.** Derived, not stated. The licence for *which viewport* is
  `docs/vision.md`'s first success condition — **"Send a link. She opens it in a browser. We play a
  full heads-up match. Someone wins. We hit Rematch."** — the same sentence
  [`ADR-0103`](ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md) and
  [`ADR-0106`](ADR-0106-a-sub-pixel-residual-is-a-fit-and-one-pixel-is-the-fence.md) both cite. It
  says **she opens it in a browser** — hers, on her device — so the viewport the fit is owed to is
  the one her browser actually presents, and a number we assumed about it is a proxy that can lie.
  The licence for *reflow rather than scale* is *Positioning* — **"The reference points are Lichess
  and Chess.com, not PokerStars. Dark, quiet, fast, minimal."** On the reference point, the
  **graphic** scales with the window and the **type** — clocks, moves, controls — does not; that is
  `ADR-0103` §3's give order already, and a uniform scale is the opposite of it. The human's
  ***"we have to support phone size"*** is recorded in
  [`ADR-0096`](ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md) §4 and is
  **applied** here, never re-argued; their three *"shoud fit screen"* notes are granted in full, and
  §1 says plainly which one sentence of theirs is refused and what would reverse the refusal.
- **Applies / qualifies:** **Qualifies `ADR-0103` §1 by its instrument only** — one axis, at one
  assumed number, in a browser with no chrome (§§2–4 below). `ADR-0103` §§2, 3, 4 and 5 — the
  mechanism, the exhaustive give order, one card and the sequencing — stand **byte-unchanged**, and
  §6's *"No viewport smaller than 390 × 664 is promised"* is the clause §4 below builds on.
  **Qualifies `ADR-0096` §4's phone row** in the direction §4's own text asks for, downward and
  never upward. **Adds no criterion and rewords none**: `R2` and `R3` are byte-unchanged, so
  [`ADR-0099`](ADR-0099-the-rubric-is-the-adr-section-and-a-criterion-is-born-merged.md) §2's
  amending path is not entered — §3 below reads `R2`'s and `R3`'s existing words on the axis nobody
  measured. **Applies** `ADR-0106` §1 (at the boundary, the property governs the instrument),
  [`ADR-0088`](ADR-0088-the-two-browser-proof-is-a-written-hand-check.md) (a proof no CI job can
  hold is a written hand-check), [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md)
  §1 (the card is the carrier) and [`ADR-0097`](ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md)
  §5 (portrait only). **Amends nothing.**

## Context

**The mechanism is not missing. It shipped, it was measured, and the phone failed anyway.** The
continuous reflow `ADR-0103` §2 fixed is in the client — `[--w:clamp(...)]` on the board
(`BoardCards.tsx:22`), on the hero's hole cards and the rival's mini hand (`DuelTable.tsx:103` and
`:75`), and the `--wgap` clamp on the column itself (`Lobby.tsx:333`, `WaitingTable.tsx:17`) — and
in the card, which now draws the phone frame `ADR-0103` §4 required, in a box of exactly
`.viewport.phone { width: 390px; height: 664px; }` (`design/screens/duel-table.html:73`).
`ADR-0106` recorded the repair landing at a true 664.90625 px against 664. So the reading that
matters is not *was it built* but *what was it built against*, and the answer is: a number in a
headless browser.

**Two failures are in one photograph, and `ADR-0103` §1 has an instrument for one of them.** §1
promises `document.documentElement.scrollHeight ≤ clientHeight` **and** that *"every control and
every number is on screen unscrolled"*. The sentence covers both axes; **the number covers one**.
The table scrolling is the axis the number watches. Both seat plates cut off at the screen edge is
the axis it does not — and §1 chose that form deliberately, for a stated reason: *"One number a
person can read in one `eval` is a cheaper contract than a list."* The cheapness is real and so is
what it bought; what it also did was leave the width unmeasured.

**The number that was measured carries its own warning, written before anyone had a phone in hand.**
`ADR-0096` §4 justifies the phone shape as *"the iPhone 14/15 portrait width with the browser's
chrome expanded — the **smallest `100dvh`** the card's own column is ever asked to fill"*, and adds
the sentence this decision turns on: ***"A taller value would let `R2` pass for a player who does
not have it, which is the one way this number can lie."*** 664 was a careful estimate of a device
nobody had measured. iOS Safari's URL bar and tab bar grow and shrink as the player scrolls, so the
height a player actually has is a **range** whose bottom is whatever that phone reports — and in a
headless browser there is no chrome at all, so the override's height *is* the viewport and the two
can never disagree. A pass taken there carries no information about the device by construction.

**The human proposes a different mechanism, and it is a good one.** *Scale the table area; scroll
only below a reasonable minimum.* It fits any viewport at any instant, needs no give order, cannot
run out of give, is immune to the chrome moving, and re-opens the size budget `ADR-0103`'s own
Consequences closed — *"in practice the table is closed to additions unless something leaves."*

**And it lands on a merged bar from the other side.** `ADR-0096` §2's `R3` — *"Every amount the
product shows renders at or above the body type size, is not clipped or truncated, and says what it
is"* — is licensed by *Positioning* plus the human's own *"is not a licence to be less finished."*
`ADR-0103` §3 spends whitespace, then the rival's card backs, then the hero's hole cards, then the
board, and states that the numbers never give at all; its alternative E, *shrink the numbers instead
of the cards*, was **rejected outright by `R3`**. A uniform scale takes all of that in one step, and
takes the numbers first rather than last.

**That is the tension.** A merged promise is false on the device that matters; the mechanism that
would make it unconditionally true shrinks the thing the merged give order protects above everything
else; and the instrument that certified the promise cannot see either the width it failed on or the
device it failed on. Everything below is the narrow path between them.

## Decision

### 1. The duel table keeps reflowing. Nothing scales it

The table's fit is achieved by **reflow** — elements taking less room as the column narrows,
continuously, as `ADR-0103` §2 already fixes. **No transform, `zoom`, or whole-column font
multiplier stands between the viewport and the table.** `ADR-0103` §§2 and 3 — one table at two
widths, and the exhaustive give order that ends *"Nothing else gives"* — stand byte-unchanged, and
are how the human's *"opponent shoud fit screen"*, *"info bar should fit screen"* and *"conroller
shoud fit screen"* are delivered: all three, in full, at the shape §4 fixes.

**A uniform scale is still *one* table, and this ADR rules that explicitly.** It removes nothing,
reorders nothing, renames nothing and shows the same words in the same places, so it passes all
three of `ADR-0096` §4's second-surface tests more easily than reflow does. Choosing between the two
mechanisms was therefore mine to make and not a surface question in disguise. **It is refused on
`R3` and on the give order, not on the surface test** — stated that way so a future reader can weigh
a scale fairly if one is ever proposed again.

**Why `R3` refuses it.** A transform that draws a 16 px numeral at 11 px passes `getComputedStyle`
and fails the player, and that boundary is already settled: `ADR-0106` §1 rules that where the
instrument and the property disagree, **the property governs**. `R3` is a player-facing bar — *at or
above the body type size* — and the player reads what is painted. Scaling is `ADR-0103`'s rejected
alternative E arriving by a different route, and it inverts a merged give order whose whole point is
that the amounts a decision is made of are the last thing on the table to give.

**One sentence of the human's is refused, and one sentence reverses it.** Their *"table area shoud
be scaled"* is read here as a proposed mechanism for an outcome — everything fits — that this ADR
grants by the other mechanism. If it was meant as the call rather than as a proposal, that is a
relaxation of `R3`, which is theirs and not mine, and it costs one sentence: no schema ships here,
no name is taken, no data migrates.

### 2. The fit is measured at the smallest viewport the player's browser presents

The viewport the table is judged against is the **smallest one the browser offers while the duel is
being played** — on iOS Safari, with the URL bar and tab bar **fully expanded**, the state the bars
are in when the player arrives and whenever they are not scrolling.

The judged height is the **smaller of** `document.documentElement.clientHeight` and
`window.visualViewport.height`, read on the running client in that state. The judged width is
`document.documentElement.clientWidth`, read the same way.

**A fit at the smallest height is the whole promise, and that is why there is no second reading.**
Retracting the chrome only hands room back; a document that fits with the bars expanded fits with
them gone. So nothing is promised at any other height, no beat is measured at any other height, and
**the client owes no response to the chrome moving** — no listener, no refit, no size change while a
player is reading the table.

In a headless browser the two readings are equal and both are the device-metrics override's own
height. That identity is precisely why the headless pass said nothing about the phone, and naming it
here is what stops the next repair being certified the same way.

### 3. The contract is two numbers, one per axis — and this adds no criterion

`ADR-0103` §1's contract, at every beat of the walk, is:

- `document.documentElement.scrollHeight ≤ clientHeight`, **and**
- `document.documentElement.scrollWidth ≤ clientWidth`,

with every control and every number on screen, unscrolled and uncut, both read at the shape §4
fixes. `ADR-0106` §§1, 2 and 5 apply to **each** number unchanged: the sub-pixel reading, the
one-CSS-pixel fence and the table of what a round may file are per-axis, and nothing here widens
them.

**The clipped seat plates needed no decision and never did.** `R2` says the decision is visible
*"at once, without scrolling"* and names no axis; `R3` says every amount is *"not clipped or
truncated"*. A seat plate cut off at the screen edge is `R2` `not met` — the rival's stack is one of
the five things `R2` enumerates — and `R3` `not met` for whatever amount the cut takes. Both were
true before this ADR. What §3 adds is the **instrument**, not the bar — which is why no criterion is added, reworded,
re-ranked or retired, and `ADR-0099` §2's amending path is not entered. `R2` and `R3` continue to
apply wherever they already did, on every screen a round walks; the two-number pair above is the
duel table's own contract, as §1 of `ADR-0103` always was.

### 4. The judged shape is 390 × 664, it is an assumption about a device, and it may only move down

**The number stays what `ADR-0096` §4 set** — `phone` **390 × 664**, *"the iPhone 14/15 portrait
width with the browser's chrome expanded"* — and that is the number a later ticket verifies against
today. It is not re-derived here, because nothing measured has yet contradicted it.

**It is a stand-in, and a reading beats a stand-in.** When a reading taken under §2 on a real phone
reports a smaller number on either axis, **that reading replaces the stand-in on that axis**: it
becomes the shape the round walks, the shape the headless gate overrides to, and the box the card is
drawn in — `design/screens/duel-table.html`'s `.viewport.phone`, which moves with it so the card
does not come to describe a screen no player has (`ADR-0091` §1).

**It never moves up.** `ADR-0096` §4's own sentence is the reason: *"A taller value would let `R2`
pass for a player who does not have it."* Correcting the number downward **applies** §4; raising it
relaxes a bar the human's *"we have to support phone size"* was written into, and that is theirs.

**The first reading is owed by the work that repairs the photograph**, and it is a **written
hand-check** in `ADR-0088`'s form, because no CI job owns an iPhone: the phone in the photograph,
the browser in the photograph, the beat in the photograph, with §2's readings and §3's two pairs
written down beside them. The hand-check is the source of the number; it is not a gate and it gates
nothing.

**Below the judged shape, nothing is promised.** `ADR-0103` §6 already says so, and this is the
human's *"scroll is nececerry only if table area reach some reasonable min heigt/width"* granted in
their own structure — with **the reasonable minimum defined as the smallest viewport a phone this
product is actually played on reports**, rather than as a number somebody picks.

### 5. This is a defect against `ADR-0103` §1's instrument, not a reason to change its mechanism

Of the two readings the decision was registered with, this ADR takes the first. `ADR-0103` §1's
**property** — a player never scrolls to act, and the whole column fits — is upheld and is what the
repair aims at. Its **instrument** is qualified in three ways and only three: it watches one axis
(§3), at one assumed number (§4), in a browser with no chrome (§2).

**The cause is not diagnosed here and this ADR does not pre-judge it.** A shorter viewport than 664,
a content box that is wider on the device than headless, a font metric — the decision is robust to
which, because §§2–4 fix what the fit is measured against, and a cause is findable once the
measurement is the player's. The repair itself is ordinary work under the merged give order.

**If the give list runs out, `ADR-0103` §3's stop rule fires.** *"A ticket that reaches the end of
this list and is still over budget stops and registers a `DEC`; it does not take the next thing it
sees."* That is the sanctioned exit if the measured shape turns out to be far below 664, and it is
the one this ADR expects to be used rather than a quiet scale taken inside a ticket.

### 6. What this deliberately leaves open

- **The cause of the photographed failure**, and therefore the file set of its repair. Measured, not
  guessed — the `TASK-120908` precedent `ADR-0103` §5 already invokes.
- **Whether the merged give order suffices at a shape smaller than 390 × 664.** §5's stop rule is
  the answer when a number exists; nothing here promises it will.
- **Which phones.** This ADR pins the judged shape to *a device reading*; it opens no supported-device
  list. A second phone reporting smaller than the first is a question for whoever brings it, and a
  list of devices the product commits to is `docs/vision.md`'s, not an ADR's.
- **Orientation.** Portrait, `ADR-0097` §5, the human's *"we are ok to support only one orientation
  for mobile form factor."* Not reopened.
- **`DEC-103`** — whether a compound label may break mid-phrase — stays **open**. `ADR-0103` §3
  makes wrapping legal in general and this changes nothing about a particular wrap.
- **The other screens.** The front door, the result screen and the rematch offer are not re-measured
  here. `R2` and `R3` reach them exactly as they always did, on both axes (§3), and this ADR
  licenses no give order for a screen nobody has measured as over budget.

## Consequences

**What it buys.** The fit becomes a promise about the player's phone instead of about a number in a
headless browser, and a later ticket can be verified without argument: read the device under §2,
read the document under §3, at the shape §4 fixes. The width failure acquires an instrument, so the
clipped plates can be closed rather than re-photographed. Three of the human's four annotations —
the opponent, the info bar and the controller — were already `R2`/`R3` `not met` under merged text
and are unblocked with no decision attached to them at all.

**The number can move, and everything downstream moves with it.** A device reading below 390 × 664
changes the round's shape, the headless override, and the card's `.viewport.phone` box — and may
change more than that: `ADR-0103`'s give list is exhaustive and was spent buying the current
contents 664 pixels. A smaller number can exhaust it, and the honest outcome of that is a new
decision, not a quiet give. **This ADR makes it more likely, not less, that the table's design is
re-opened.**

**A hand-check enters the chain of evidence for the product's most-checked criterion.** No CI job
holds a phone, so the number every future `R2` reading is taken against comes from a human with a
device, once. It goes stale silently when they get a different phone, and nothing detects that.
Named rather than gated, which is `ADR-0088`'s own position and its own accepted cost.

**`ADR-0103` §1's one-`eval` contract is spent for good.** It was already two reads at the
boundary after `ADR-0106`; it is now two numbers, sometimes three reads, plus a device state to be
in. The readability that form was chosen for is gone, and what replaces it is a contract that can
fail on the axis the photograph failed on.

**The table's ceiling comes closer.** `ADR-0103` already closed the table to additions unless
something leaves; measuring against a viewport that can only be smaller tightens that further. The
showdown work in this same epic — a revealed rival hand, a marked winning five — arrives into a
smaller room than it was scoped for, and this is the ADR that made it smaller.

**The human's proposed mechanism is refused.** If *"scaled"* was their call rather than their
suggestion, this decision is the wrong one and one sentence from them reverses it — §1 says exactly
which sentence and what it relaxes. That is the cheapest reversal available and it was chosen partly
for that: nothing here ships a schema, takes a name, or migrates a row.

**Foreclosed.** A uniform scale of the table, by anyone but the human; a phone-only layout or a
breakpoint (`ADR-0103` §2, upheld); any **upward** move of the judged shape, by anyone but the
human; and a client that changes the table's size in response to the browser's chrome moving.

## Alternatives considered

**A. Scale the table area to fit, with a floor below which it scrolls — the human's own proposal.**
The strongest case, and it is strong: it fits **any** viewport at **any** instant, so iOS Safari's
variable chrome stops being a problem rather than being bounded; it needs no give order and cannot
run out of give, which is the failure mode `ADR-0103` §3 had to write a stop rule for; it preserves
on the phone exactly the composition the human approved on the laptop, so the phone is the same
drawing rather than a differently-proportioned relative; it retires the two-frames-in-one-card drift
`ADR-0103` named as an unguarded discipline; and it re-opens the size budget that ADR closed, which
is the single largest standing constraint on this table's future. **Rejected** because a uniform
scale shrinks the numbers, and `R3` — merged, licensed by *Positioning* and by the human's own
*"not a licence to be less finished"* — puts every amount at or above the body type size. A
transform passes the computed style and fails the eye, and `ADR-0106` §1 already settled that where
the two disagree the property governs. It is also `ADR-0103` §3's give order inverted: that order
spends whitespace, then card backs, then the hero's hand, then the board, and never the type; a
scale takes all of them at once and takes the type first. Relaxing `R3` is the human's call, not
mine, and §1 says so and says what it would take.

**B. Reflow first, scale only below the width where reflow runs out.** The strongest case: it grants
the human's mechanism exactly where it is needed and nowhere else, keeps full-size type on every
phone large enough to have it, and replaces the scroll below the floor with something a player can
still read — strictly better than scrolling, on the axis the human cared about. **Rejected** because
it is two mechanisms with a threshold between them, and the threshold is the thing `ADR-0103` §2
refused by name: *"390 is not a threshold"* is what makes *one table at two widths* checkable rather
than merely asserted. It also decides today, with no device measurement in hand, that the give list
will run out — a prediction §5's stop rule will answer with a number attached if it ever comes true.
Nothing here forecloses B; it is cheaper to take later, when it is known to be needed.

**C. Treat the photograph as a plain defect and change nothing about the measurement.** The
strongest case: the mechanism is merged and shipped, the repair is ordinary CSS, and a decision that
changes no mechanism arguably should not be an ADR at all — this repository's own rule is *do not
write one for choices the code already makes obvious*. **Rejected** because it leaves in place the
instrument that certified the
false promise: one assumed number, on one axis, in a browser with no chrome. The next repair would
be measured the same way, pass the same way, and be photographed failing the same way — and the
registered question asks *against which viewport*, to which *"the one we already used"* is an answer
only if that viewport is the player's, which is the one thing nobody has checked.

**D. Pick a smaller number now — 390 × 640, or the smallest iPhone SE viewport — and gate on it.**
The strongest case: it needs no device in anybody's hand, it is immediately gate-able in CI, it
closes the question today, and picking a conservative shape is how most products handle a viewport
they cannot enumerate. **Rejected** because the number would be invented, and an invented number is
what put the product here: 664 was a careful estimate about a device nobody had measured, and a
second careful estimate is not an improvement on the first — it is a choice to be wrong more
precisely, when one hand-check would produce the real figure. It also over-promises silently: a
shape below anything a real phone presents buys design obligations, out of a give list that is
already spent, for a player who does not exist.

**E. A `visualViewport` listener that refits as the bars grow and shrink.** The strongest case: it
is the only answer that is exactly right at every instant rather than at the worst instant, and it
would let the table use the room a retracted URL bar hands back instead of leaving it empty.
**Rejected** because it buys nothing the player can use: a table that fits with the bars expanded
fits with them retracted, so the extra room is room the table does not need, and §2's *fit the
smallest* gets the same guarantee with no moving parts. Its only visible effect would be a table
that changes size while a player is reading it — motion carrying no fact, which `ADR-0115` would
then need an opinion about. It is also a **mechanism**, and mechanisms are the architect's; had the
answer required one, this ADR would have registered it instead of designing it.

**F. Escalate the whole question — the human proposed a mechanism, so the mechanism is theirs.**
Considered seriously, because the boundary matters more than any single answer here, and because
*"table area shoud be scaled"* is the human's own sentence about their own product. **Rejected**
because `EPIC-14` registered it as a decision rather than as an instruction, and because the merged
sources contain the answer: `R3` is merged, `ADR-0103` §3's give order is merged, and `ADR-0096`
§4's number carries an explicit instruction to be corrected in the one direction §4 takes it. What
*is* the human's is named rather than quietly taken — relaxing `R3` (§1), raising the judged shape
(§4), and any list of devices the product commits to (§6). Escalating everything would also stall
`STORY-1405` on all four annotations, three of which are `R2`/`R3` `not met` today and need no
decision from anyone.
