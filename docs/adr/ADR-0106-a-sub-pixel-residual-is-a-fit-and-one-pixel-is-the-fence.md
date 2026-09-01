# ADR-0106 — A sub-pixel residual is a fit, and one pixel is the fence

- **Status:** Accepted
- **Date:** 2026-09-02
- **Resolves:** `DEC-112` — after [`TASK-121402`](../../tasks/tasks/TASK-121402-the-duel-table-column-fits-the-phone-it-is-nested-in.md)
  removed the 48 px of duplicated outer padding, the duel-table document measures a true
  **664.90625 px** against a 664 px viewport — `scrollHeight` reads **665** — with both action
  buttons at or above the fold and every other criterion on the ticket met. Does *fits the phone*
  admit a sub-pixel residual, so the ticket merges as scoped and the residual is re-filed as its
  own ticket — or does the criterion mean the document must not scroll at all, so the ticket is
  held until the residual is closed? Raised 2026-09-02 by the ticket's implementer doing exactly
  what its Out of scope instructed — *"If 48 px turns out not to be enough, stop — do not take the
  next thing on the list — and say so"* — which is
  [`ADR-0103`](ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md) §3's
  stop rule operating at its first live boundary. Registered and answered in the same PR (the
  `DEC-039` path — it never appeared in an open table).
- **Where the answer came from.** Derived, not stated. The licence is the same sentence `ADR-0103`
  cited — `docs/vision.md`'s first success condition, **"Send a link. She opens it in a browser.
  We play a full heads-up match."** — because that sentence is what *fits* is **for**: she plays
  unscrolled, with nothing she must read or press below the fold. What holds the fence where §2
  puts it is *Positioning* — **"Dark, quiet, fast, minimal"** — via
  [`ADR-0096`](ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md) §2's own text,
  which states the player-facing bar in words this residual cannot fail: the decision's five
  things *"are visible at once, without scrolling."*
- **Applies / qualifies:** **Qualifies `ADR-0103` §1 by exactly one boundary case** — how its
  one-number contract reads when the integer instrument and the true geometry disagree by less
  than the instrument's own unit; §§2–6, the give order and everything else byte-unchanged.
  **Upholds `ADR-0096` §2 rather than relaxing it**, and §2 below forecloses the relaxed bar it
  forbids. Not a rubric amendment in
  [`ADR-0099`](ADR-0099-the-rubric-is-the-adr-section-and-a-criterion-is-born-merged.md)'s form:
  no criterion is added and `R2`'s text does not change — what merges here is the reading of
  `R2`'s existing words at the one boundary its instrument cannot express.

## Context

**The repair is real and the residual is arithmetic.** `TASK-121402` moved 48 px of duplicated
outer padding off `main`, and every enumerated harm in the merged defect table is repaired: the
390 × 664 preflop beat fell from 712 to the reading below with both action buttons'
`getBoundingClientRect().bottom ≤ 664`, 720 × 900 is an exact 900 / 900 (was 948 / 900), the
front-door and account gutters hold at 24 px, nothing moved or reworded, and the whole `verify:`
block exits 0. What remains was traced by measurement, not guessed: `documentElement`, `body`,
`main` and the `min-h-[100dvh]` column all report a true height of **664.90625 px**, with zero
margin and zero border, while the column's own `min-height` resolves to exactly 664 — the excess
is the sum of the column's five children's fractional heights, and three of those non-integer
heights live in `DuelTable.tsx` and `ActionBar.tsx`, files the ticket's merged budget does not
name.

**The ticket's two halves disagree, and both are merged text.** Its first acceptance criterion
demands `scrollHeight ≤ clientHeight` and predicted *"the expected reading is 664 / 664"*. Its Out
of scope anticipated the shortfall and ordered the stop. The implementer obeyed the stop — and the
stop is not the ticket's invention: `ADR-0103` §3 rules *"A ticket that reaches the end of this
list and is still over budget stops and registers a `DEC`; it does not take the next thing it
sees"*, and adds that the list is exhaustive *"precisely so that running out is a visible event."*
This decision is that visible event.

**The prediction was unreachable on the day it was written.** The merged defect table's 712 was
itself a rounding of a true 712.90625, and 712.90625 − 48 = 664.90625 — no outcome of removing
exactly the duplicated padding could read 664 / 664. The instrument was already rounding when the
defect was filed: it recorded **712** for a true 712.90625 then, and reads **665** for a true
664.90625 now, in directions of its own choosing. An integer bar read off a rounding instrument
has always had a dead band narrower than one pixel; the only question this decision can settle is
whether that dead band is **stated or lucky**.

**The two merged bars diverge only in the last 0.90625 px.** `ADR-0096` §2's `R2` is the
player-facing bar: the action and every number the decision depends on *"are visible at once,
without scrolling"*, and *"a product that must scroll to show the amount to call is `R2` `not
met`"*. At 664.90625 nothing must be scrolled to be shown — both buttons bottom out at or above
the fold and every number is on screen. `ADR-0103` §1 restated the repair target as the whole
column — `scrollHeight ≤ clientHeight` — calling itself *"a cheaper contract than a list, and on
this table it is not a stronger one by much."* The product now stands exactly inside that *"by
much"*: the property is met and the proxy's integers read 665 / 664.

**Held or merged, the last pixel is a new ticket.** The fractional sources sit in files outside
the ticket's budget, and the one in-budget lever — the `--wgap` clamp's 8 px floor, shared by the
card (`duel-table.html:61`, `var(--pd-space-3)`) and the client (`Lobby.tsx:181`) — is `ADR-0103`
§3.1's give, which the Out of scope forbids by name. So the live question is not *whether* the
pixel gets its own ticket but whether 48 delivered pixels wait for it — and what a future round
may file about a sub-pixel overflow meanwhile.

**The tension.** Hold the bar as a naked integer inequality and the machinery grinds on a pixel
row that cannot hold a glyph: the compliant stop is punished, a merged-clean repair waits on a
ticket that must exist either way, and every future round re-files a defect no player can
experience. Admit a tolerance and the door `ADR-0096` §2 nailed shut — the relaxed phone bar — has
a visible crack for the next argument to widen. Everything below is about making the crack exactly
as wide as the instrument's own unit and not one pixel more.

## Decision

### 1. *Fits* is the player's property, and it admits a residual only where the instrument's own pixel cannot state one

The duel table fits the phone when **nothing a player must read or press sits below the fold and
no scroll is needed to play** — `R2`'s own words, *"visible at once, without scrolling"*.
`ADR-0103` §1's `scrollHeight ≤ clientHeight` remains the one-number contract for that property,
and remains the target every repair aims at. At the boundary where the proxy and the property
disagree — and only there — **the property governs the instrument**:

- A reading of `scrollHeight ≤ clientHeight` is **met**, with no further read, exactly as today.
- A reading of `scrollHeight − clientHeight = 1` is judged on the true geometry, read on the same
  running stack at the same beat: `document.documentElement.getBoundingClientRect().height`. If
  the true excess over `clientHeight` is **strictly less than one CSS pixel**, the criterion is
  **met** — the reading is recorded, and nothing is filed. If it is one CSS pixel or more, `R2` is
  `not met`, the ordinary path.
- A reading of `scrollHeight − clientHeight ≥ 2` is `R2` `not met` with no second read: two
  integer steps cannot round up from a sub-pixel truth.

**The tolerance is a reading of the instrument, not a budget.** It admits only what the integers
cannot express, and nothing readable can hide inside it: no glyph, no numeral, no control renders
within a pixel, which is why admitting it does not reopen the questions `ADR-0103` §1 chose the
one-number form to close — *"may the bet line fall below the fold? may the blinds?"* No element
can fall sub-pixel-below the fold; content invisible for any other reason — clipped, truncated,
occluded — was never a scroll defect and nothing here shields it (`R1`, `R3`).

### 2. The fence is one CSS pixel, and this ADR forecloses its own widening

One CSS pixel is chosen because it is two things at once: the integer instrument's own quantum —
the smallest difference `scrollHeight` can state — and the largest interval into which no
information fits. Below it, the bar and the player agree; at it and above, painted rows exist and
the relaxed phone bar begins. Therefore:

- **A true residual of one CSS pixel or more is a defect at any judged shape, whatever it
  contains — whitespace included.** The moment the contents of an overflow are litigated, the bar
  is a judgment; the fence stays bright instead.
- **The tolerance does not scale.** Not with device pixel ratio, not with zoom, not with argument.
  There is no *"about a pixel"*.
- **No round, ticket, review or triage may widen it — and neither may a future ADR deriving from
  the vision.** A tolerance wide enough to hold painted content is the *"relaxed phone bar"*
  `ADR-0096` §2 forbids in as many words, and §2's licence is *Positioning* plus the human's
  recorded *"we have to support phone size"* — so a content-admitting tolerance is an amendment to
  a human-licensed criterion and goes to the human with the case laid out. This ADR's own
  tolerance is defensible precisely because nothing can be inside it.

### 3. `TASK-121402` merges as scoped

The measured 664.90625 against 664, with both action buttons' bottoms at or above the fold,
satisfies §1's reading; the ticket's first acceptance criterion is discharged by the pasted
readings **plus this ADR**, and the prediction it carried — *"the expected reading is
664 / 664"* — is recorded as arithmetically unreachable from the day it was written, not as a
shortfall of the work. The implementer's stop is the conduct `ADR-0103` §3 commands; the
alternative — quietly taking the clamp floor the Out of scope forbids by name — is the
coder-invents-design failure the give list exists to prevent, and merging the compliant work is
what keeps that instruction credible the next time it is needed. No ticket field is edited here;
the driver applies this disposition at landing.

### 4. The last 0.90625 px is re-filed once, as ordinary backlog, and its closer stays inside §3.1

The planner cuts **one** ticket, from this ADR, to bring the true document height to
`≤ clientHeight` at 390 × 664. Under §1 the residual is not a defect, so the ticket must say what
it buys, and it is two things: **headroom** — the column stands 0.09375 px from the fence, so any
future fraction added anywhere in it tips a met reading into a filed defect without anyone
touching *layout* — and **the tolerance's operating cost retired** — at a true fit, no round ever
runs §5's second read. Constraints on the closer, merged here: it spends **`ADR-0103` §3.1
whitespace and nothing further down the list** — a sub-pixel never justifies the rival's hand, the
hole cards or the board, and advancing a merged give order over 0.9 px is disproportion the order
exists to prevent. The named lever is the `--wgap` clamp's floor, shared by card and client; the
mechanism and file set are the ticket's to measure, not this ADR's to guess, and if it moves a
number the card owns, `ADR-0103` §4's composing path applies unchanged. If §3.1 cannot yield the
pixel — nothing measured suggests that — the stop rule fires again and the next `DEC` says so. No
due date; §5 keeps rounds honest meanwhile.

### 5. What a round may and may not file about a sub-pixel overflow

At any shape a round walks, at any beat:

| integer reading | the round does | what is filed |
| --- | --- | --- |
| `scrollHeight ≤ clientHeight` | nothing further | nothing — met, exactly as today |
| `scrollHeight − clientHeight = 1` | reads `document.documentElement.getBoundingClientRect().height`, same stack, same beat | **nothing, if** the true excess is strictly under one CSS pixel — both numbers go in the round's record and the criterion is **met**. Otherwise `R2` `not met`, the ordinary path |
| `scrollHeight − clientHeight ≥ 2` | nothing further | `R2` `not met` — no second read can save it |

A sub-pixel overflow **may not be filed** — not as a finding, not as a proposed criterion, not as
a `DEC`, and not as precedent for anything wider; a round that files one contradicts this merged
source. This clause adds work only where the integers already accuse: green readings gain no
second read. And it shields exactly one thing — the sub-pixel document residual. It shields no
clipped or truncated content at any residual (`R3`), no imperceivable event (`R1`), no element a
player must press or read that is actually cut, and no beat, shape or screen measuring a full
pixel or more over — those all walk the ordinary path, `TASK-121402`'s unmeasured beats included:
this ADR speaks for the readings taken and rules a boundary, not a table.

## Consequences

**What it buys.** The 48 px repair lands now, at four decision beats that were unplayable a week
ago; `ADR-0103` §3's stop rule stays credible — the first implementer to obey it was not punished
for obeying it; and rounds stop being able to grind on a pixel row that cannot hold a glyph, with
a fence that is categorical rather than negotiated.

**`ADR-0103` §1's one-eval contract is spent at the boundary, and that is the largest cost.** The
whole point of the one-number form was *"one number a person can read in one `eval`"*. At a
reading of exactly one over, the check is now two reads and a rule, and every future round that
lands there pays it — until §4's ticket lands, and nothing forces that ticket to land.

**A tolerance now exists where none was written, and it will be cited.** The defence is structural
— nothing fits inside it, and §2 routes any widening to the human by name — but the argument will
still have to be had, and this ADR is the document it will be had against.

**The product ships 0.09375 px from the fence.** One added fraction anywhere in the column's five
children — a line-height, a padding step, a font metric — flips a met reading into a filed defect,
invisibly to any compile-time gate. Until §4's ticket lands, that hair-trigger is the standing
state, and it is this ADR's own doing: a stricter answer would have forced the headroom now.

**A criterion merges undischarged as literally written.** `TASK-121402`'s first criterion says `≤`
and the integers say 665 / 664; the trail's honesty now hangs on the PR body carrying the true
readings and this ADR's id — the exact failure shape `STORY-1213` recorded for `TASK-121302`, a
criterion closed unmet, avoided here only by the cross-reference. The process lesson is recorded
with it: an acceptance criterion that predicts an exact integer by doing integer arithmetic on a
rounding instrument writes a cheque the geometry may decline to cash; predict the property, and
let the number be what the stack says.

**On classic-scrollbar platforms the residual can be furniture.** A desktop window dragged to
phone width — no rubric shape, but reachable, and `ADR-0103` §2's continuity is why — can grow a
scrollbar track for a document scrollable by a rounded pixel, where an exact fit would show none.
Accepted, named, and retired by the same §4 ticket.

**Foreclosed.** Holding a merged-clean repair hostage to sub-pixel arithmetic, ever again; and any
tolerance wider than the instrument's unit, by anyone but the human.

## Alternatives considered

**A. Hold `TASK-121402` until it reads 664 / 664.** The strongest case: the bar stays a naked
integer inequality — no second instrument, no clause for rounds, no tolerance for anyone to cite —
the pressure to finish lands while the trace is warm, and it is the only option under which this
ADR need not exist. **Rejected.** The closer lives outside the ticket's merged file budget and
behind its own Out of scope, so the last pixel is a new ticket under either disposition — holding
couples 48 delivered pixels to 0.9 undelivered ones and buys nothing a player can see for the
wait. It punishes the exact conduct `ADR-0103` §3 commands, teaching the next implementer to take
the give quietly instead — the failure the give list exists to prevent. And the exactness it
defends was never on offer: the instrument that would judge it recorded 712 for a true 712.90625
on the day the defect was filed.

**B. Merge as scoped, but keep the residual a standing `R2` defect until closed.** The strongest
case: no tolerance is written anywhere, the bar stays one number, the metric stays maximally
suspicious — and `ADR-0103`'s own Consequences already accepted a metric that *"will read worse
than the work is"*. **Rejected** because it makes the rubric assert something false about the
product: `R2`'s text is *"visible at once, without scrolling"*, and everything is. A standing
`not met` no player can experience re-enters every round's fix set, holds the unrepaired count
above zero, and can stall a cycle's `PASS` on a pixel row that cannot hold a glyph — the rubric
judges the product for the player, not the geometry for the instrument. The honest form of B's
suspicion is §4's ticket plus §2's fence.

**C. Restate the bar on the true geometry, exactly — `getBoundingClientRect().height ≤
clientHeight`, no tolerance.** The strongest case: one read of a finer instrument ends the
rounding question forever, and zero tolerance is the easiest rule to defend. **Rejected** because
it decides nothing here — 664.90625 fails it exactly as 665 fails the integers, so the registered
question comes back unanswered — and it quietly **tightens** a merged bar: a true 664.4 that
passes today's integers would fail it, and a tightening is as much an amendment as a loosening. It
also pins a pass to platform font metrics summing to a friendly fraction, a brittleness nothing in
the vision buys.

**D. Admit the integer step itself — `scrollHeight − clientHeight ≤ 1`, no second read.** The
strongest case: still one `eval`, today's reading passes, and no second instrument enters the
contract. **Rejected** because an integer difference of one can round up from a true excess
approaching a pixel and a half — space that paints — so it admits residuals the instrument *can*
state, which is a relaxed bar in exactly the sense `ADR-0096` §2 forbids. The chosen rule admits
only what the integers cannot say; D erases the one line that makes the tolerance defensible.

**E. Escalate to the human — it touches their recorded *"we have to support phone size"*.**
Considered seriously, because the boundary test matters more than any single answer. **Rejected**
because nothing in this residual reaches a player that sentence protects: the phone is supported
in every sense it has — she opens the link, everything is on screen, she plays unscrolled — and
the merged sources already contain the answer at this boundary. What *would* be the human's is the
thing §2 forecloses instead of taking: a tolerance wide enough to hold content. Spending the
human's attention on an instrument's rounding is the opposite of what the escalation lane is for.
