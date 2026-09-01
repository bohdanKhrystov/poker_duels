# ADR-0103 — The table fits the phone, and the cards give before the numbers

- **Status:** Accepted
- **Date:** 2026-09-01
- **Resolves:** `DEC-106` — what does the duel table look like on a phone: does it get a phone
  treatment at all, what gives at 390 wide, and which card carries it? Raised 2026-09-01 out of round 1
  of `/qa-cycle audit smoke` (`STORY-1213`, `R2` `not met`, run 2026-08-31) and by
  [`TASK-121302`](../../tasks/tasks/TASK-121302-the-decision-fits-a-390-by-664-screen.md), which
  could not close it by conformance because the merged card describes no phone. Registered and
  answered in the same PR (the `DEC-039` path — it never appeared in an open table).
- **Where the answer came from.** Derived, not stated. The licence is `docs/vision.md`'s *Why this
  exists* — the first success condition, **"Send a link. She opens it in a browser. We play a full
  heads-up match. Someone wins. We hit Rematch."**, with *"Everything else is downstream of that
  moment."* That is a sentence about **one continuous act**, and it is the same sentence
  [`ADR-0096`](ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md) §4 read to put the
  phone in scope (*"Both name **a browser** and **no device**"*). What *gives* comes from
  *Positioning* — **"Dark, quiet, fast, minimal"** — and from *What it is*' first line, **"Heads-up
  Texas Hold'em"**, which [`ADR-0101`](ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md)
  already used to rule that the product's words and emphasis are the game's. The human's
  ***"we have to support phone size"*** is recorded in `ADR-0096` §4 and is **applied** here, never
  re-argued; §1 below notes which arm of the question it had already closed.
- **Applies:** `ADR-0096` §2 (`R2`, `R3`, and *one bar, checked more than once*) and §4 (two shapes,
  and the test for when a phone becomes a second surface) — both reached unchanged;
  [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §§1 and 3 (the card is the
  carrier, and who authors one follows what it does);
  [`ADR-0024`](ADR-0024-design-follows-the-code-workflow.md) §3 (the visual verdict is the human's);
  [`ADR-0097`](ADR-0097-a-resize-is-two-numbers-and-the-observer-is-the-fifth-file.md) §5 (portrait
  only). **Amends nothing.**

## Context

**The human's call is merged, and it made 390 × 664 a shape the product is judged at.** `ADR-0096`
§4 records *"we have to support phone size"* verbatim and fixes the two shapes an audit round walks:
`phone` 390 × 664 and `laptop` 720 × 900. §2's `R2` — *the decision fits the screen* — is **one bar
checked twice**, and it says so in as many words: *"Nothing here defines a relaxed phone bar, and no
round may invent one; a product that must scroll to show the amount to call is `R2` `not met`,
whether that happens at 390 px or at 720."*

**Round 1 answered `R2` `not met`, at every beat that asks the player to act.** Measured on the
running client at 390 × 664: `scrollHeight` 885 against `clientHeight` 664 preflop; Fold and All-in
at `bottom: 820.578` facing a raise; 868/664 facing an all-in; 866/664 on the flop. At 720 × 900 the
same four screens measured 900/900 with everything visible — the control that rules out a broken
instrument.

**`TASK-121302` assumed this was conformance to a merged card, and it is not.** The ticket says so
in as many words — *"No `DEC` is needed here — this is conformance to a merged card"* — and lists
`design/screens/duel-table.html` as `read`, never edited. Its coder reproduced the card's
`.table { min-height: 100dvh }` and `.center { flex: 1 }` faithfully and got no closer; a review
established why, and the reason is not a coder's mistake: **`min-height` is a floor and `flex-grow`
only distributes slack**, and at 885 px of content in a 664 px viewport there is no slack to
distribute. The coder then searched the card for anything that narrows as the viewport does and
found **one rule in the whole file** — `--bw: clamp(48px, calc((100cqi - 64px) / 5), 72px)` — read
only by the five board cards. The hero's hole cards and the rival's mini hand are hardcoded in the
card itself at `--w:96px` and `--w:40px`.

**Measured this run, the card does not fit the phone either.** `design/screens/duel-table.html`,
rendered headless at 390 × 664 with `Emulation.setDeviceMetricsOverride`:

| | at 390 × 664 | at 720 × 900 |
| --- | --- | --- |
| `document.documentElement` | `scrollHeight` **732** / `clientHeight` **664** | **900 / 900** |
| `.bar` bottom | **715.7** — 51.7 px below the fold | on screen |
| `.hole` (the hero's two cards) | 134.4 tall at the hardcoded 96 px | 134.4 |
| `.sizing` / `.actions` | 59 / 61.5 — both wrapped | 32 / 44.3 |

So a client that transcribed the merged card **perfectly** would still fail `R2` by 68 px. The card
was drawn for a wider screen and contains no phone rules, and no amount of conformance produces a
fit the source does not describe. That is why this is a decision and not a ticket: the missing rule
is design, `ADR-0091` §1 makes the card the carrier of design into implementation, and a coder
inventing a clamp is inventing design — the guess `CLAUDE.md` rule 5 exists to stop.

**And the answer is fenced on the other side too.** `ADR-0096` §4 wrote the test for when a phone
stops being a width and becomes a surface: *"the day the product does something **different** on a
phone — a second layout, a reduced feature set, a separate application — it has acquired a second
surface and that belongs in `docs/vision.md`, not in an ADR."* `ADR-0097` §5 restates the same test
for orientation.

**That is the tension, and it is a real one: the shape has to change at 390 and the surface must
not.** An answer that changes nothing fails a merged criterion and the human's own sentence; an
answer that gives the phone its own table takes a decision that is not mine. Everything below is the
narrow path between them.

## Decision

### 1. The duel table fits the phone. A player never scrolls to act — and the whole column fits, not
just the decision

At **390 × 664**, at every beat of the walk, the duel table's column fits the viewport:
`document.documentElement.scrollHeight ≤ clientHeight`, and every control and every number is on
screen unscrolled.

**Stated as the whole column rather than as `R2`'s list, deliberately.** `R2` enumerates five things
(the action, the viewer's stack, the rival's stack, the pot, the amount to call), and a repair aimed
at exactly those five invites the next question — *may the bet line fall below the fold? may the
blinds?* — which nobody has asked and nobody should have to. One number a person can read in one
`eval` is a cheaper contract than a list, and on this table it is not a stronger one by much: the
five things `R2` names are spread from the top of the column to the bottom of it, so any layout that
shows all five at once on a 664 px screen very nearly shows everything.

**This arm of the question was already closed, and the ADR records that rather than choosing it.**
*"A player scrolls"* was available to me as a question but not as an answer: `ADR-0096` §2 is merged
and rules that scrolling to see the amount to call is `R2` `not met` at either shape, and forbids
the relaxed phone bar by name. Answering that way would mean amending a criterion whose licence is
the vision's *Positioning*, and contradicting the human's *"we have to support phone size"* — an
amendment that is theirs to make and not mine. §§2–5 are where this ADR does its work.

**One frame answers every beat, and that is the merged card's own doing.**
`design/screens/duel-table-states.html` states the rule: *"Every slot — the opponent's hidden hand,
the bet/muck line, the pot row, the bar — exists in every state, so nothing appears, disappears, or
moves; only text and card faces change."* The board reserves five card widths whether or not the
cards are dealt, the bet line reserves its height with no bet live, the status line reserves
`1.5em`, and the bar reserves both rows in its `off` state. **The table's height therefore does not
vary by beat**, so a single phone frame that fits is not a sample — it is the whole answer, and the
design work is one frame rather than four.

The client does not have that property today: 885, 868 and 866 at three beats are three different
heights where the card promises one. **That discrepancy is named here and decided nowhere** — it is
an input to the rewrite's measurement (§5), not a finding this ADR rules on.

### 2. It is one table at two widths. There is no width at which the player gets a different table

Every element the table shows at 720 it shows at 390, **in the same order, with the same words**.
Nothing is removed, nothing is collapsed behind a disclosure, nothing appears only on a phone,
nothing moves to a different place in the column. What changes is how much room an element takes,
and it changes **continuously with the column's own width** — the idiom the card already uses for
the board, where `100cqi` reads the column and not the viewport.

**Continuity is the property, and it is what keeps this inside my authority.** A measurement that is
a continuous function of the column's width means 390 is not a threshold, there is no "phone
version" of the table to keep in step, and a player dragging a window narrower watches one table
get tighter rather than watching a second table replace the first. Against `ADR-0096` §4's three
tests: it is not a second layout (one markup), not a reduced feature set (nothing is dropped), and
not a separate application. It is `ADR-0096` §4's own sentence — *"The product has **one layout**,
and the two shapes are that layout at two widths"* — made true instead of merely asserted.

The *mechanism* is the card author's; `100cqi` is named because it is already in the file and works,
not because it is mandated. The property to preserve, whatever the mechanism, is that **no width
shows a different table**.

### 3. What gives at 390, in order — and the list is exhaustive

1. **Whitespace.** The column's outer padding and the gaps between its three blocks tighten first,
   because they are the only give that costs no information at all.
2. **The rival's face-down hand.** Two card backs whose entire content is *she still holds cards*.
   Her name, her stack, her dealer button and whose turn it is are on the plate directly above them.
   It narrows furthest of anything on the table.
3. **The player's own hole cards.** At `--w:96px` they are the tallest single block on the table —
   134 px, a fifth of a 664 px phone — and they are the one thing a player reads once a hand and
   then knows. They narrow, with a floor: **never smaller than a board card.** A player's two cards
   are the only cards that are theirs, and a table that draws the shared five larger than the
   private two inverts the game's own emphasis — *What it is*' first line, applied the way
   `ADR-0101` applied it to a control's label.
4. **The board.** It already narrows with the column, and it gives **last** of the three card
   groups, because it is what the decision is read off, at every beat, repeatedly.
5. **Nothing else gives.** Both seat plates' names and stacks, the pot and its line, the bet lines,
   the amount to call, the sizing row and the action buttons keep their type size, their labels and
   their place. `R3` is merged and is the reason: every amount at or above body size, not clipped,
   not truncated, and saying what it is.

**The action bar does not give; it may grow.** At 390 the sizing row wraps to two rows and the
action buttons wrap their labels to two lines — 59 and 61.5 measured at 390, against 32 and 44.3
at 720. Wrapping is *fitting*, not *giving*, and it is allowed. Truncating a label, hiding a chip, or
dropping a row is not. (Whether `Raise to 3,650` may break *between* the words and the number is
`DEC-103`, open, and this ADR does not answer it — §6.)

**If the list runs out before the column fits, that is a decision to re-open, not a scroll to
accept.** A ticket that reaches the end of this list and is still over budget stops and registers a
`DEC`; it does not take the next thing it sees. The list is exhaustive precisely so that running out
is a visible event.

**A fit exists.** Probed this run against the card's own markup — the hole cards and the mini hand
narrowing on the board's idiom, the vertical rhythm stepping down with the column, no element
removed — `scrollHeight` came to **664** against `clientHeight` 664 at 390 × 664, while 720 × 900
stayed byte-identical to today. That is an **existence proof, not a prescription**: it says the
decision is deliverable, and it chooses no number. The numbers are the card's, under the human's eye
(`ADR-0024` §3), and a probe is not a design.

### 4. `design/screens/duel-table.html` carries it — amended, and gaining a second frame

- **One file, not a second card.** A `duel-table-phone.html` free to diverge is the second layout §2
  refuses, and two files drift.
- **The existing frame is amended** so that the two hardcoded widths — `--w:96px` on the hero's hole
  cards, `--w:40px` on the rival's mini hand — narrow with the column, as the board's `--bw` already
  does, together with whatever §3's list requires above them.
- **The file gains a second frame at 390 × 664**, beside the existing one, in
  `duel-table-states.html`'s `.frames` / `.frame` form. The two frames are the **same markup**; the
  only difference between them is the width and height of the box they are drawn in. That sameness
  is what makes *one layout, two widths* checkable by eye and by diff rather than merely claimed,
  and it is the line between this and alternative C — **markup identity, not file count.**
- **The phone frame is boxed at 664 tall as well as 390 wide.** A `min-height: 100dvh` column inside
  a tall page proves nothing about a short viewport, and taking the card's width while leaving its
  height behind is the exact confusion that put the client here.
- **The card is in arrears today, and was before this ADR.** It draws one shape while the product
  claims two, and at the shape the product claims it is 68 px too tall. Until the design ticket
  lands, an audit or UAT round reading the card would judge a conforming client against a drawing
  that cannot pass.
- **Who authors it is `ADR-0091` §3's split, applied and not overridden.** Composing from the
  settled vocabulary — the same components, the same tokens, the clamp idiom already in the file —
  is an ordinary dispatched `module: design` ticket, `review: light`. If the treatment needs a **new
  token** — a size step `tokens/tokens.css` does not have — that is **minting**, and minting is
  worked interactively with the human. Which of the two it is belongs to the ticket, not to this
  ADR. Either way the visual verdict is the human's (`ADR-0024` §3) and may trail the merge.

### 5. `TASK-121302` cannot be worked as written; the design ticket goes first

Two of its sentences are now false: its *Files* table lists the card as `read`, and its cause
section says *"No `DEC` is needed here — this is conformance to a merged card."* It is **rewritten
by the planner, not amended**, and it is **blocked on the design ticket** — a coder cannot conform
to a shape the card does not draw, which is the wall the first attempt hit and the wall `CLAUDE.md`
rule 5 puts there on purpose. **The design work precedes the client work**, and that ordering is the
whole of this ADR's sequencing claim.

**Its height-budget half survives, and is necessary but not sufficient.** The column still owes the
card's `min-height: 100dvh`, the centre block still owes `flex: 1`, and `Lobby.tsx`'s and
`DuelTable.tsx`'s two nested `max-w-[560px]` columns still need to become one — without those, the
column's height is the sum of its content, nothing absorbs slack, and the 900/900 laptop pass is an
arithmetic accident rather than a property. The review that established those rules cannot **close**
`R2` was right, and it is recorded here so nobody re-litigates it: the sum has to come down first;
the height budget is what keeps it down afterwards.

**The file set is to be measured, not copied** — the `TASK-120908` precedent. The known starting
points are `web-client/src/table/DuelTable.tsx` (the two hardcoded widths at lines 36 and 51, and
the inner column) and `web-client/src/lobby/Lobby.tsx` (the outer column and the height budget). The
client's 885 exceeds the card's 732 by 153 px, and that difference lives in components neither the
card nor `TASK-121302`'s two-file budget accounts for; whichever they are, they enter the file set
by measurement.

### 6. What this deliberately leaves open

- **`DEC-103`** — whether a compound label may break mid-phrase — and **`DEC-104`** — what the
  number labelled *Pot* counts — stay **open**. Both were observed at phone width and neither is
  touched here. §3 makes wrapping legal in general; whether a *particular* wrap is acceptable is
  `DEC-103`'s question and this ADR does not pre-empt it.
- **Landscape is settled elsewhere and not reopened**: `ADR-0097` §5, the human's *"we are ok to
  support only one orientation for mobile form factor."* Portrait.
- **The other screens.** Round 1 measured the front door, the result screen and the rematch offer as
  fitting at 390 (`bottom` 634.5, 653.75, 653.75 against 664). This ADR is about the table, and it
  licenses no give order for a screen nobody has measured as over budget.
- **No viewport smaller than 390 × 664 is promised.** 390 × 664 is the shape `ADR-0096` §4 fixed and
  the shape this fits. Nothing here says anything about 320 px, and a future device that needs it
  gets its own question.
- **How a beat is paced** stays where `ADR-0096` §2 left it and where `DEC-105` now sits. A runout
  that reveals streets over time changes nothing about the table's height (§1: every slot is
  reserved), so the two repairs do not collide.

## Consequences

**What it buys.** `R2` becomes reachable, and reachable *by conformance again*: once the card draws
the phone, the coder's question goes back to *does the client match the card*, which is the only
question `ADR-0091` gives a coder the tools to answer. The second-surface line stays exactly where
`ADR-0096` §4 drew it, and stays **checkable** — one file, one markup, no breakpoint, so a reviewer
can settle "is this still one product?" by diffing two frames.

**The player's own hand gets smaller on a phone, and that is a real loss.** The hero's two cards at
96 px are the largest, most deliberate thing on the table; on a phone they will not be. This ADR
trades the size of the thing a player looks at **once a hand** for the presence of the thing they
have to **press every beat**, and it says so plainly rather than pretending the phone gets the same
table for free. It does not get the same table. It gets the same *information*.

**The table acquires a size budget today, and that is the foreclosure a future reader will feel.**
Everything §3 lists as giving is spent buying the current contents their 664 pixels. The next thing
anyone wants on the table — a clock, a hand-strength line, the opponent's last action in prose,
chat — arrives with nowhere to come from: it must buy its space from something already there, or
not ship on a phone, and *not shipping on a phone* is the reduced feature set §2 forbids. In
practice the table is closed to additions unless something leaves.

**A merged card is wrong, and round 1's fix set grows a dependency it did not have.**
`TASK-121302` was one `S` client ticket in a three-ticket set; it becomes a design ticket plus a
rewritten client ticket, in that order, and it cannot land in the round it was filed in. Under
`ADR-0096` §5, *"filing does not reduce `A(N)`, only repair does"* — so if round 2 runs before both
land, `R2` is counted `not met` again and `A(2)` does not fall for it. **The metric will read worse
than the work is**, and that is the honest reading rather than a defect in the count; `A(N)` was
built to be a count of unrepaired criteria and it is doing exactly that.

**Two frames in one card is two things to keep in step**, and `design/check-drift.sh` checks token
names, not that two frames share their markup. A stale phone frame is a card that *lies*, and
`ADR-0091` §1's whole mechanism rests on a card being the thing a coder can trust. Nothing gates
this; it is a discipline, and naming it is the only defence this ADR can offer.

**The give order is now a merged constraint on design taste.** A future card that makes the hero's
cards give last, or the board give first, or that shrinks a stack to buy room, does not merely draw
differently — it contradicts §3 and needs a superseding ADR. That is a real cost to a design
practice that has so far answered to the human's eye alone (`ADR-0024` §3), and it is accepted
because the alternative is that the eye has to re-derive the same priority every time the table
changes.

**Cheap to reverse in the direction that matters.** If the human decides the phone should get a
genuinely different table, nothing here is in the way: this ADR is superseded, `docs/vision.md`
gains a clause, and the card gains a second file. No schema ships, no name is taken, no data
migrates. The one thing that becomes expensive later rather than now is the **size budget** — every
element added to the table between now and then is another thing a second layout would have to
place.

## Alternatives considered

**A. The player scrolls to act.** Its strongest case is genuinely strong: it is free, it ships
today, no card moves, no design ticket exists, and every mobile web page in the world expects a
thumb to scroll. A poker table is content-dense; something has to give, and a scroll is the only
give that costs no information at all. **Rejected, and not on preference.** `ADR-0096` §2 is merged
and says a product that must scroll to show the amount to call is `R2` `not met` *"whether that
happens at 390 px or at 720"*, and *"Nothing here defines a relaxed phone bar, and no round may
invent one."* Answering this way would require amending a criterion licensed by the vision's
*Positioning* and contradicting the human's own *"we have to support phone size"* — theirs to do,
not mine. And on the merits: the vision's first success condition is one continuous act — *"She
opens it in a browser… We play a full heads-up match"* — and a player hunting for Fold below the
fold, on a clock, is not inside it.

**B. A sticky action bar, the table scrolling behind it.** The strongest case: it is the smallest
possible change, it guarantees the controls are under the thumb no matter how the table grows, it
needs no give order and no new card rules, and it is the pattern a phone user already knows.
**Rejected** because `R2` asks for the action *and every number the decision depends on*, at once. A
pinned bar guarantees the buttons and guarantees nothing about the rival's stack or the pot, so the
player still scrolls **mid-decision** — and now the bar occludes the table while they do it, which
is worse than the defect at the beats where the pot sits behind it. It also introduces an overlay
the card does not draw: a layout mechanism that exists only on the phone, which is the second layout
§2 exists to avoid.

**C. A second card, `design/screens/duel-table-phone.html`.** The strongest case is real: it is the
honest way to *draw* a phone, the frame can be exactly 390 × 664 with nothing else in the file to
distract the eye, and a designer can tune the narrow shape without risking the wide one that already
passes. **Rejected** because a second file is free to diverge, and a phone drawing free to diverge
is exactly the *"second layout"* `ADR-0096` §4 says has acquired a second surface. The distinction
being drawn is **markup identity, not file count** — stated that way so a future reader can judge a
second file fairly if one is ever proposed: a second file whose markup is provably identical would
not be a second layout, it would just be harder to prove.

**D. Drop something at 390 — the rival's card backs, the board's undealt slots, the blinds/hand/
street line.** The strongest case: it is by far the largest saving per unit of effort, and several
of these carry very little information; the pot line's meta is plausibly the least-read text on the
table. **Rejected** because that is *a reduced feature set on a phone* — the second of `ADR-0096`
§4's three named tests — and it is the human's call, not mine. It is also the give that cannot be
taken back quietly: once a phone shows less, a player on a phone and a player on a laptop are
playing two different products, and every feature after it has to answer which one it is for.

**E. Shrink the numbers instead of the cards.** The strongest case: type is the cheapest pixel on a
screen, and dropping the two stacks and the pot one step each would save more than the rival's mini
hand ever will, with no card touched. **Rejected outright by `R3`**, merged: every amount renders at
or above the body type size, unclipped and labelled. The numbers are what the decision is *made of*;
shrinking them trades away the decision in order to make room for it.

**F. Leave the card alone and let the client hold the phone rules.** The strongest case: the client
is where the measurement happens, it is one PR instead of two, and the card stays the "reference
drawing" it always was rather than becoming a specification of responsive behaviour. **Rejected**
because `ADR-0091` §1 makes the card the carrier of design into implementation and `ADR-0024` §3
puts the visual verdict on the rendered card — a phone treatment that exists only in TSX is a design
the human never saw and cannot review, and the next round reading the card would file the client's
correct behaviour as a defect. It also puts the choice of *what gives* back inside a ticket, which
is the failure this decision exists to prevent.
