# ADR-0095 — The table states who took the pot, and never names a hand

- **Status:** Accepted
- **Date:** 2026-08-30
- **Resolves:** `DEC-095` — when a hand ends, does the table state the pot's winner and amount in
  place of the pot line, as [`design/screens/duel-table-states.html`](../../design/screens/duel-table-states.html)
  draws it — and **does the product name the made hand at showdown at all**, given that no wire
  field carries one? **Yes to the first, no to the second.** Registered and answered in the same PR
  (the `DEC-039` path), so the id never appears in an open table.
  [`TASK-121101`](../../tasks/tasks/TASK-121101-the-table-says-who-won-the-hand-it-just-finished.md)'s
  first acceptance criterion — *"a `DEC` is registered in both registers and routed to the
  `product-owner`, before any diff exists"* — is met by the answered rows this PR writes into
  `docs/adr/README.md`, `tasks/BOARD.md` and `tasks/epics/EPIC-12-quality-and-defect-repair.md`.
- **Where the answer came from:** **derived from the vision; the human did not state this call.**
  Two halves, two licensing sentences.
  **The banner** is licensed by [`docs/vision.md`](../vision.md) *On variance* — *"**Luck decides a
  hand.** Skill decides whether you come back tomorrow."* The vision names the **hand** as the unit
  at which luck lands and asks the player to make peace with it; a table that resolves a hand in
  silence asks them to absorb a variance it never told them about. *What it is* seconds it in two
  words — *"Replay and **honest feedback**"* — and the client is holding the fact already.
  **The refusal to name a made hand** is licensed by the rest of that same bullet — *"Every hand is
  stored as an event log, so a match can be replayed and **analysed afterwards**"* — which puts the
  explaining surface in the replay and puts it *afterwards*; by *Positioning* — *"The reference
  points are **Lichess** and **Chess.com**, not PokerStars. Dark, quiet, fast, minimal"*, and
  Lichess states the result and the termination on the board while every *why* sits behind a
  separate, opt-in surface; and by the *Roadmap*, where **v0.1** is *"Two browsers, one room link,
  one complete duel, rematch"* and the replay viewer is **v0.4**. Nothing here adds to or subtracts
  from *What it is* / *What it is not*, so it is answered rather than escalated.
- **Builds on:** [`ADR-0046`](ADR-0046-the-table-says-away-timed-out-and-back.md) §0 (the other
  player is *your rival* in every string, and takes a display name in the same slot) and §2 (a line
  clears on a fact, *"never on a timer, never on a fade"*);
  [`ADR-0075`](ADR-0075-the-mark-lives-as-long-as-the-absence-that-produced-it.md) (a line lives as
  long as the fact that produced it); [`ADR-0008`](ADR-0008-loser-mucks-at-showdown.md) (the loser
  never shows, and a fold winner shows nothing);
  [`ADR-0002`](ADR-0002-server-authoritative.md) (a client may never assert a game fact);
  [`ADR-0094`](ADR-0094-opening-the-invite-is-taking-the-seat.md), merged the same day (when a card
  and the product disagree, the product owner says which one is the product, and the card is
  corrected); [`ADR-0092`](ADR-0092-a-uat-round-files-what-a-source-settles-and-asks-the-rest.md)
  §5 (*"an answered question becomes a merged source"*);
  [`ADR-0005`](ADR-0005-analysis-behind-an-interface.md) (analysis is consumed, behind an
  interface, and is not this screen)
- **Constrains:** `TASK-121101` — every string it renders and the condition it renders them on;
  `web-client/src/table/PotStrip.tsx`; and both banner frames of
  `design/screens/duel-table-states.html`, whose second line this ADR deletes. It writes no ticket:
  the planner rewrites `TASK-121101`'s `Scope`, `Tests` and `verify:` from this document, and the
  card correction is its own `module: design` ticket.
- **Amends nothing.** No engine change, no server change, no wire type, no `PROTOCOL_VERSION` step,
  no stored data, and no `expect` column in `docs/test-plan.md` — `CORE-06` and `CORE-09` describe
  this product and keep passing. `poker-engine` is untouched in both directions: it is not asked
  for a hand name, and it is not asked to stop computing one.
- **Raises nothing for the architect.** `TASK-121101` made its technical half explicitly
  conditional — *"a **yes** on the second half raises a conditional architect question (what wire
  field carries a made-hand description); it does not arise on a **no**."* It is answered **no**, so
  no `DEC` is registered for it. §5 below states the shape that question would take on the day this
  ADR is superseded, as a signpost and not as an open row.

## Context

Round 3 of the UAT cycle found that when a hand ends the table says nothing about it. The frame
array steps from the acting state straight to `Hand complete`; `PotStrip.tsx` has exactly one
`return` and no branch that could ever carry a banner. The card draws two frames that do — *Showdown
— you win, the loser mucks* and *Fold — you win on the river, nobody shows* — and each carries two
lines: an amount, and a second line beneath it.

**The two lines are not equally buildable, and that asymmetry is the decision.** `PotAwarded` carries
`seat` and `amount`, so *You win 4,850* is a transcription. **No `GameEvent` names a made hand**, so
*Two pair, aces and sevens* could only be computed by the client from the cards — which `CLAUDE.md`'s
non-negotiables and `ADR-0002` forbid, and which `web-client/src/table/no-derivation.test.tsx`'s
`HAND_TALK` matcher already gates. Half a card is renderable today; the other half needs a protocol.

Five things are in tension.

**The card is not the product, but it was accepted by a human.** `ADR-0094` settled the general
question the same day: where a merged card and the shipped client disagree, the product owner says
which is the product, and `ADR-0092` §5 then closes the repeat conformance finding mechanically. That
makes deleting a line from a card an available move — it does not make it a free one. Two frames were
drawn, reviewed and accepted with a made-hand line in them, and somebody wanted it there.

**This is not a secrecy question, and it must not be argued as one.** `ADR-0008` already decided the
hard version: the loser never shows, a fold winner shows nothing, and a mucked hand appears in no
event. What a showdown *does* publish — the winner's `HandRevealed` cards and the board — is public
to both seats. A name computed over those cards leaks nothing. The reason the client may not compute
it is `ADR-0002`, which is a rule about **who asserts**, not about **what is secret**; and the reason
the product might still decline to say it is nothing to do with either. So the question has to be
answered on the product's own terms.

**The engine knows exactly, and it is the only thing that does.** It evaluates the made hand to
settle the pot. Declining to say it is the product choosing to withhold a fact it holds — which is a
choice, not an omission, and it needs a reason better than *it is not wired up*.

**The vision points both ways, once each.** *"Showing a player that they lost the match but made the
better decisions is more interesting than hiding the maths"* argues for saying more. *"Dark, quiet,
fast, minimal"*, and an event log *"replayed and analysed **afterwards**"*, argue for saying less
here and more there. Both sentences are in the same short document and neither is decorative.

**The deadline is the protocol, and it argues for deciding now rather than for deciding either
way.** A made-hand field is cheapest today: nobody has played a duel, `PROTOCOL_VERSION` has no
deployed client to migrate. It gets more expensive every week. Against that, `TASK-121101` is the one
open ticket whose defect a player meets in **every single hand** (`STORY-1211`), and it cannot be
gated while half of what it builds is undecided.

## Decision

### 1. The banner exists, and it is one line, in the amount slot

When the hand a player is watching has ended — the view's street is `COMPLETE` — the table states who
took the pot and how much, **where `Pot N` stands**. The facts line beside it is untouched on this
street as on every other: `Blinds N/N · Hand N · Hand complete` goes on saying what it says today.

**Nothing is added beneath it.** The banner is one line replacing one line. The card's second row
goes, at both endings.

### 2. The three lines

| the ended hand's awards | the line |
| --- | --- |
| one award, to the viewer's seat | `You win 4,850` |
| one award, to the other seat | `Your rival wins 4,850` |
| two awards — a split pot | `Split pot — you win 2,425` |

Five rules, in the order they are most easily broken:

- **The verb is `win`, present tense, in every line.** Never *takes*, *collects*, *scoops*, *wins the
  pot*, *wins it*. No exclamation mark, nothing congratulatory, nothing consoling. This is the
  register `ADR-0046` §2 already fixed for the table's sentences.
- **The number is a `PotAwarded.amount` this client received**, formatted the way every other chip
  figure on this screen is formatted — never a figure the client works out. Not a sum of what the
  seats committed, not a stack delta, not the view's `pot`, which is zero by then.
- **On a split, the number is the viewer's own award.** `docs/duel-rules.md` §Showdown sends the odd
  chip to the player out of position, so the two shares can differ by one chip. Stating the reader's
  own share is what keeps the line true; the rival's share is not stated.
- **The other seat is `Your rival`** — `ADR-0046` §0 — and takes a display name in the same slot in
  the same sentence on the day one exists.
- **The line says who and how much, and never why.**

### 3. No hand is ever named, anywhere on this table

Not at a showdown, not at a fold, not in the amount slot, not in the facts line, not in an
`aria-label`, not in a `title`, not in a tooltip. *Two pair, aces and sevens* is not built, and the
card's showdown frame loses that line.

The fold frame's second line — *Nobody shows — your river bet of 800 goes uncalled and returns* —
goes with it, and for its own reason rather than by association: **the numbers already agree without
it.** `settleHand` emits `UncalledBetReturned` **before** `PotAwarded`, and the awarded amount is the
pot *after* the return, so the figure this banner prints and the movement the player's stack makes
are the same movement. There is nothing to explain.

### 4. The banner lives as long as the hand it describes

It stands while the street is `COMPLETE` and it goes when the next hand begins. **Never on a timer,
never on a fade** — `ADR-0046` §2 and `ADR-0075` already hold the table to that, and this line is not
an exception to it.

A client that arrives at a completed hand **without** having received that hand's award — a reload
during the pause between hands — shows the ordinary `Pot N` line. The banner is not restated on the
wire and no field is added to `PlayerView` to carry it.

### 5. `no-derivation.test.tsx` is not touched, and here is why it stays green

The `HAND_TALK` gate stands **byte-unchanged**. It is not scoped to a street, not weakened, not given
an exception, and this ADR grants no licence to edit it. Its fixture renders a view at
`street: "TURN"` — a hand in progress — where none of §2's lines may appear, so a banner built on the
condition §1 names leaves both of its assertions green.

**A coder who meets that test red has built the banner on the wrong condition.** The redness is the
gate doing its job, and the fix is the trigger, never the matcher.

§3 also *widens* what that gate is guarding, in meaning if not in text: the made-hand half of its
vocabulary — `pair`, `trips`, `set`, `straight`, `flush`, `full house`, `quads`, `high card` — is now
a rule about **every** street rather than about mid-hand only. A future ADR that reverses §3 must say
what becomes of the matcher, in that ADR, before any diff exists.

### 6. What would have to happen for §3 to be reversed

Named so the reversal is a decision rather than a drift: **the first duels played by people who are
not the author showing that a player cannot say why they lost.** The fix then is a **server-sent**
descriptor — never a client computation, under any circumstances — and it supersedes this ADR rather
than amending it. It would raise a question that is the **architect's**, stated here and deliberately
not registered, because it does not arise until §3 is reversed: which wire type carries the
description and on what frame, whether `poker-engine` publishes a name or the server renders one from
a `HandRank` it already receives, what that costs the engine's purity and its
`docs/architecture.md` contract, and what `PROTOCOL_VERSION` step it takes.

## Consequences

**What it buys.** A hand that ends says so. The whole of §1–§4 is a client change over facts already
on the wire — `PotAwarded` passes `EventRedaction` unfiltered to both seats and lands in
`DuelState.narration` today — so nothing in `poker-engine`, the server, the wire types or
`PROTOCOL_VERSION` moves, and `TASK-121101` becomes a small client ticket that can carry a real
`verify:` block instead of a `manual-verify` label. The table keeps exactly one rule about game
facts: **it transcribes what the server sent and it never reads the cards** — no exception, no
street where it behaves differently, and `no-derivation.test.tsx` goes on meaning the whole of what
it says.

**What it costs.**

1. **A player who cannot read seven cards is not helped, and that is the price.** At a showdown the
   winning hand is face-up on the table, the engine knows precisely what it is, and the product
   declines to say. That falls hardest on exactly the person the vision was written for — the
   author's sister, at hand one. It is paid because the *outcome* is stated and only the *reason* is
   withheld, and because the reason is on the table as cards with the ranking written down in
   `docs/duel-rules.md`. If that trade is wrong, §6 says how it is undone.
2. **A deferred protocol change is a dearer protocol change.** Today a made-hand field costs a
   version bump with no deployed client to migrate. This ADR spends that cheapness on purpose, and
   it is the strongest argument against the answer it gives.
3. **Two human-accepted card frames lose a line each**, one of them the frame's most distinctive
   line, and a design ticket has to make the deletion rather than the client quietly diverging.
4. **A reload during the between-hands pause loses the statement.** The banner is a moment, not a
   state, and no wire field is added to make it survivable. A player who refreshes at the wrong
   second learns the result from the stacks, as they do today.
5. **The split line states one of two numbers.** A player is never told their rival's share, which
   the odd chip can make one chip larger than their own.

**What it forecloses.** Client-side hand naming, permanently and at every street — that door is now
shut by an ADR and not only by a test. It does **not** foreclose a made-hand name existing in this
product: the event log stores every card that was shown, so a replay viewer (v0.4) or `ADR-0005`'s
analysis interface can name a hand from stored data with no wire commitment at all. What is
foreclosed is naming it **at the table, live**, and only until something in §6 happens.

**What it does not settle**, said plainly rather than implied away: the banner's *treatment* — the
card draws the winning viewer's amount in mono and the win colour and draws no losing frame at all,
so what a loss looks like is the card's business and `EPIC-06`'s, not this ADR's. Nor does it say
anything about the hand that ends the **duel**: a `DuelFinished` is the frame on which this client
enters its result screen, which states the verdict in its own words, and this banner has no opinion
about that handover.

## Alternatives considered

**1. The banner names the made hand, from a new server-sent field.** The strongest case in the set.
The showdown is the moment the hand resolves and the only moment a player learns whether their read
was right; heads-up duels reach showdown far more often than full-ring poker does, so this is the
common case rather than a corner; the engine already evaluates the hand exactly and is the only
component that can, so the product would be withholding something it holds; a name over revealed
cards leaks nothing, so `ADR-0008` does not object; and the vision says in as many words that
*"showing a player that they lost … is more interesting than hiding the maths"*. Rejected on three
counts, in order of weight. The vision puts the explaining surface *afterwards* — the same bullet
that promises *honest feedback* promises it through an event log *"replayed and analysed
afterwards"*, and the roadmap dates that surface to v0.4 while v0.1 is *"one complete duel,
rematch"*. What is withheld is a **reading of public cards**, not information: the cards are on the
table and the ranking is in `docs/duel-rules.md`. And it is the irreversible half — a wire field
outlives every string in this product, and there are no players yet whose difficulty could tell us
the field is needed. §6 keeps the door open with a named trigger.

**2. The banner names the hand, computed in the client.** Its case is not negligible: zero protocol
cost, zero server work, and at a showdown the client genuinely holds every card the computation needs
— the board plus whatever `HandRevealed` published — so it would derive nothing secret and leak
nothing. Rejected on a non-negotiable rather than on taste. `CLAUDE.md` and `ADR-0002` make the
server the only thing that asserts a game fact, and *"the client happened to have enough information
this time"* is precisely the reasoning that rule exists to refuse — the next such case is the one
where it does not. It is also gated by a merged test, and the right response to a merged guard is
never to weaken it to fit the diff.

**3. No banner at all — the stacks already say it.** Round 3 graded this gap `medium` for exactly
this reason and the reasoning is sound as far as it goes: the stacks move, the winner's hole cards
appear, a folder's seat reads `Folded`, and a table with fewer strings is a quieter table, which is
what *"dark, quiet, fast, minimal"* asks for. Rejected because a moved stack is a number the player
must diff against a number they no longer have, in the seconds before the next hand is dealt; and
because the pot's winner and amount are a fact the client is **already holding and declining to
print**, which is the opposite of *honest feedback*. Quiet is a property of how much is said, not of
whether the thing that happened is said at all.

**4. Keep the card's two rows, and let the second row carry whatever the wire can say — the returned
uncalled bet at a fold, nothing at a showdown.** It preserves the accepted card's shape, it is
buildable today (`UncalledBetReturned` carries `seat` and `amount`), and it explains a chip movement
a player might otherwise puzzle over. Rejected because the movement needs no explaining: the engine
returns the uncalled portion **before** it awards the pot, so the printed amount and the stack change
already agree to the chip. And a second row present at one ending and absent at the other is a shape
that moves between states — the exact thing the card's own margin note sets out to prevent when it
says the pot row *"becomes the banner without changing shape"*.

**5. `Split pot 4,850` — name the whole pot rather than the viewer's share.** It is the pot, it is
one number rather than two, both players read the same line, and no seat comparison is needed.
Rejected because it is the one figure in the set that is not what happened to the reader: the odd
chip goes out of position, so a share can be one chip short of half, and a line stating a total the
reader did not receive would be the first false number this screen prints.

**6. Escalate the second half to the human.** Tempting, because a human accepted the card with the
hand name on it, and because deleting an accepted line looks like overruling them. Rejected because
the boundary does not run there: `docs/vision.md`'s *What it is* and *What it is not* are untouched
by either answer, no money, no roadmap milestone and no new kind of thing is involved, and *what a
player sees, and what they are told* is the product owner's by `docs/workflow.md`'s routing table.
Escalating a question the vision answers is its own failure — it stalls a ticket that a player meets
in every hand, for a call the human can overrule with one sentence whenever they read this.
