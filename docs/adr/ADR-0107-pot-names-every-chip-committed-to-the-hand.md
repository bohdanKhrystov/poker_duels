# ADR-0107 — `Pot` names every chip committed to the hand

- **Status:** Accepted
- **Date:** 2026-09-02
- **Resolves:** `DEC-114` — **the product owner's** — what does the table's `Pot` name: the
  **collected** pot, or the **total including this street's commitments**? Raised 2026-09-02 by the
  human after playing a duel end to end, registered by
  [`EPIC-13`](../../tasks/epics/EPIC-13-the-living-table.md), and gating its item 2.
- **Where the answer came from.** Derived, not stated. The human's words — *"pot size is not
  correct; it shoud include all bets including the latest one that was just made"* — are treated as
  the report, not the specification: evidence of what a player expected, from the only player who
  has played this product end to end. The licence is the first line of `docs/vision.md`'s *What it
  is* — **"Heads-up Texas Hold'em. Two players. Never three."** — read the way
  [`ADR-0101`](ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md) already
  read it: the product *is* Texas Hold'em, so a word the game owns means, on this product's
  surfaces, what the game's audience means by it. And for this word the repository has already
  said which meaning that is, in a merged sentence:
  [`ADR-0100`](ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) §6
  ruled that *"a label naming a game quantity is a statement, and it is the server's"*, and then
  named the quantity a truthful pot label needs — *"the pot reaches the control ... as `view.pot +
  seats[0].committedThisStreet + seats[1].committedThisStreet`"*. The vision's *On variance*
  closes the loop on why the honest number is also the useful one: *"showing a player that they
  lost the match but made the better decisions is more interesting than hiding the maths."*
- **Applies, and does not touch:**
  [`ADR-0100`](ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) §6 — the
  sum it hands the bar is unchanged and now also printed where the word `Pot` is printed; no
  sentence of it moves. [`ADR-0101`](ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md)
  §1 — the presets' base `P` is exactly the number the strip now prints; nothing about the chips
  moves. [`ADR-0095`](ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md) — the
  award line still replaces the figure at `COMPLETE` and still states only received amounts.
  [`ADR-0102`](ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md) §§2–3 — a
  runout step still lags only the board and the street; by the tick a step stands, the street's
  commitments are swept and the total equals `view.pot`, so the paced frames print what they print
  today. [`ADR-0002`](ADR-0002-server-authoritative.md) — the printed number is composed of three
  facts the server stated and nothing else; §5 below says why that stays on the right side of
  `ADR-0100` §6's line.
- **Touches no module.** No code ships with this ADR. No wire change, no `PROTOCOL_VERSION` move,
  no engine change: every term is already on the view, and `web-client/src/lobby/Lobby.tsx:154`
  already computes the sum on the same screen. The implementing ticket is `EPIC-13` item 2's, the
  planner's to write, card first (`ADR-0091` §2, and the epic's own rule that the card merges
  before the implementing ticket is startable).

## Context

The duel table's strip prints `Pot {view.pot}` — the pot as the server has **collected** it, chips
swept at the end of each street. That is deliberate, said in the component's own words (*"the pot
is `view.pot` and not a sum of what the seats put in"*, `PotStrip.tsx`) and defended by a merged
guard: `no-derivation.test.tsx`'s *"shows no number the view does not carry"* renders the table
against a fixture built so that no two of its numbers sum to a third, and fails if any figure on
screen is not a view field verbatim. The model is the live table: the pot in the middle is what
the dealer swept, and this street's bets stand in front of the seats until the street ends.

Three forces stand against it, and one stands for it.

**The same screen already computes the other number, under the same word.** `Lobby.tsx:154` sums
`view.pot` and both seats' `committedThisStreet` into `potIncludingStreet` and hands it to the
action bar, because `ADR-0100` §6 ruled that a chip labelled `pot` may not state a quantity the
client manufactured — and the quantity that makes the label true is the total. `ADR-0101` §1 then
sized every fraction against that base. So the strip and the sizing row use one word for two
numbers, and the number the `pot` chip prices is the one the strip does not print.

**A player cannot reconstruct the total from what is drawn.** Only the rival gets a `committed`
bet-line (`DuelTable.tsx:58`); the hero's own street commitment appears on no surface — it has
already left their stack numeral and has not yet reached the pot numeral. At 50/100 preflop the
small blind sees `Pot 0`, the rival's `committed 100`, and their own 50 nowhere. The screen's
ledger has a hole exactly one street-commitment wide, while `docs/duel-rules.md` asserts as an
engine invariant that *"the sum of both stacks plus all pots is constant for the entire hand"* —
mid-street, a committed chip is on the pot side of that ledger, or the invariant could not survive
the event that moved it. A player pressing the `pot` chip therefore gets an amount they cannot
derive from the screen, which is the sharp fact `DEC-114`'s registration named.

**The audience's arithmetic runs on the total.** Pot odds and the pot-sized raise — the only two
calculations Hold'em asks of a player mid-hand — are both computed against the pot including live
bets. `Pot 0` on the first decision of every hand is a statement no poker player recognises: with
blinds posted there is never a moment without a pot. The human read it as a defect; it is not one,
which is exactly why it needed a decision and not a bug ticket.

**For the collected reading:** it never draws a chip twice, it mirrors the physical game, and it is
the shipped, guarded behaviour. If it stands, this ADR's job is to say so out loud and account for
the sizing-row gap; if it falls, the account is owed of what the guard, the tests and the cards
that pinned it now say.

## Decision

### 1. `Pot` names the total: every chip committed to the hand so far

```
Pot = view.pot + seats[0].committedThisStreet + seats[1].committedThisStreet
```

This is identically `Lobby.tsx:154`'s `potIncludingStreet` and `ADR-0100` §6's `P` — one quantity,
one word, everywhere the word appears. The blinds are in it the moment they are posted: at 50/100
the first frame of every hand reads `Pot 150`, never `Pot 0`. A bet joins the number at the act,
not at the sweep — and because a sweep only moves chips between the sum's own terms, the printed
number is continuous across street boundaries and never moves backwards while a hand runs; chips
leave it only when the hand resolves, by which tick the award line has replaced the figure for any
client that received the award (`ADR-0095`, unchanged).

### 2. One line, one figure, one word

The label stays `Pot`. No *Total pot* relabel, no second figure beside the first, no parenthetical.
The vision's positioning is *"Dark, quiet, fast, minimal"*; one true number is quieter than two
reconciled ones. The rest of the strip — blinds, hand number, street name, the award line at
`COMPLETE` — is untouched.

### 3. What the word promises — committed, not netted

While a raise stands uncalled, the number includes chips that a fold would hand back to the raiser
(`duel-rules.md`: an excess over the opponent's stack *"is returned as an uncalled bet when the
hand resolves"*). That is deliberate and it is the honest reading: `Pot` states what has been
committed to the hand as of now — a fact — not what the winner will net, which is a forecast. The
view moves at the resolution and the figure follows it, exactly as it follows every other event.

### 4. The bet-lines are not resettled here

The rival's `committed` line is a per-seat fact and stands. This means the rival's street chips
now appear twice on screen — on their line and inside the pot figure — and §Consequences owns
that. Whether the bet-lines keep standing once `EPIC-13` item 6 makes stacks and bets *drawn*
chips is that item's card question and the human's eye, not this ADR's; nothing here forbids or
requires it.

### 5. The never-derives guard narrows by exactly one named quantity, and no further

*"The table renders and never derives"* remains the law for everything else: no hand named, no
winner declared, no street read off the card count, no bound or total the server did not
determine. The pot total is not an exception carved into `ADR-0100` §6's line — it sits on the
line's statement side lawfully, because it is composed of three server-stated facts by the one sum
a merged ADR already defined, computed on this same screen since `ADR-0100` landed. Whether the
strip receives `Lobby.tsx`'s existing sum as a prop or sums the view itself is the implementing
ticket's shape, per `ADR-0101` §7's precedent; either way `no-derivation.test.tsx` admits this one
sum and stays red for any second derived figure. `GameState.potTotal` exists in the engine and is
not on the view; this ADR neither asks for it on the wire nor forbids it ever arriving — if an
architect someday moves it, that is an `ADR-0070` probe matter, and the label's meaning does not
change.

### 6. The cards, measured

`design/screens/duel-table.html` draws `Pot 2,450` in both frames beside a rival bet-line of 400:
under §1 both pot nodes read **2,850**. `design/screens/duel-table-states.html` draws `Pot 3,250`
beside an **empty** bet-line, so it already agrees and does not move. Item 2's design-first card —
which the epic requires merged before the implementing ticket is startable — carries the
correction; until it lands, the card and this ADR disagree by 400 in two nodes, the same shape
`ADR-0101` §5 already put on record for the same screen.

## Consequences

**The cost, named plainly: one chip is now drawn inside two numbers.** The rival bets 400; their
bet-line says `committed 400` and the pot figure has grown by the same 400. A reader who sums
everything visible double-counts the street. The live table's sweep model is gone from the
numerals — it survives, if anywhere, in item 6's future chip animation, and it is worth recording
that the same feedback that raised this decision described that animation as *"when bet is maid
chips going to the pot"*: chips joining the pot at the act is this semantics, drawn.

**A crisp guard gets a caveat.** `no-derivation.test.tsx` currently proves the table derives
*nothing*; it will prove the table derives *exactly one named quantity*. Its fixture-independence
sweep — no two fixture numbers may sum to a third — must now carve out the legitimate sum, which
is a real weakening of a test whose whole value was having no exceptions. The trade accepted: the
alternative was a label that stays true only by being about a different quantity than its own
sizing row's.

**Merged pins move, and the ticket re-measures rather than computes.** `PotStrip.tsx`'s docstring
and its test *"takes the pot from the view and not from what the seats put in"* invert in intent —
though the sum that test actually guards against, `view.pot + committedThisHand`, stays wrong
under this ADR too (it double-counts swept streets), so its successor guards the same mistake
under a truthful name. `DuelTable.test.tsx`'s `Pot 5,675` beside commitments of 125 and 825
becomes `Pot 6,625`; every `Pot 0` and `Pot 30` pin across `DuelTable.test.tsx`, `Lobby.test.tsx`
and `reconnect.test.tsx` moves wherever its fixture holds street commitments, and each new
expected value is read off its own fixture, not computed from a rule in a comment.

**The figure now moves at every act.** Today it moves at sweeps and hand starts; under §1 it ticks
with every blind, bet, call and raise. That is more motion on a deliberately quiet screen — and it
is the motion the epic exists to add: a table that tells the player the hand is happening.

**What it buys.** The reported case is gone — no hand ever opens at `Pot 0`. The `pot` chip's
amount becomes derivable from the screen: the pot after the call is the printed `Pot` plus the
printed call cost, and the raise puts that on top of the call — textbook arithmetic over visible
numbers, where before one term existed on no surface. The table's three money numerals — two
stacks and the pot — now sum to `duel-rules.md`'s invariant constant at every instant, so the
screen exhibits the rules' own conservation law instead of hiding a street's chips in an undrawn
hole. And one screen stops using one word for two numbers.

**What it forecloses.** `Pot` is this quantity on every surface, forever: the replay viewer
(v0.4), match history, and anything else that prints the word prints this sum or contradicts a
merged ADR. And the strip now depends on the seats' `committedThisStreet`, not on `view.pot`
alone — a projection change that dropped that field would break the pot label itself, not merely
the sizing row, extending the dependency `ADR-0101` already accepted for the same reason.

## Alternatives considered

**Keep the collected pot, say so out loud, and close the ledger by drawing the hero's missing
bet-line.** *Its strongest case:* it is the physical game — the pot is what the dealer swept, and
bets stand in front of the seats until the street ends; it never draws a chip twice; it is the
shipped, deliberate, guard-tested behaviour, so it costs nothing to keep; and adding the hero's
bet-line — the one honest gap in the drawing — would at least make the total derivable by eye.
**Why it lost:** it keeps `Pot 0` on the first decision of every hand, a statement about a Hold'em
hand that is never true once blinds are posted and the exact line the product's only real player
read as a bug; its answer to the sizing gap is three numerals and a mental sum on a clock, where
the vision's own line is that showing the maths beats hiding it; and it leaves the screen's merged
law (`ADR-0100` §6) calling a different number *the pot* than the label does — the confusion this
decision exists to end, made permanent.

**Print the pot after the call — `P + toCall`.** *Its strongest case:* it is precisely `ADR-0101`
§1's base, so the strip would print the number the fractions are fractions *of*; it is symmetric —
the same figure for both viewers — and it never includes a chip that a fold would return. **Why it
lost:** it is a forecast, not a fact — it states the pot that exists only if an act not yet taken
is taken, which crosses `ADR-0100` §6's line from statement into manufacture; and the question
asked *collected or total* — a third quantity is an answer bigger than the question. Where no bet
is outstanding it equals the total anyway, so all it adds is hypothesis exactly where honesty is
cheapest.

**Print both — `Pot 2,450 (2,850 in play)`, or a second *Total pot* line.** *Its strongest case:*
several commercial rooms do it, so part of the audience has seen it; nobody chooses between the
models because both are printed; and the never-derives guard could even keep its purity by scoping
the derived figure to the parenthetical. **Why it lost:** *"Dark, quiet, fast, minimal"* — two pot
figures on a strip that also carries blinds, hand and street is reconciliation homework in the
middle of a decision, and it re-opens rather than closes the question of which number the `pot`
chip means. The casino clients that print two numbers do so because rake makes their two numbers
genuinely differ; this product has no rake and needs no second number.

**Put `GameState.potTotal` on the view and print a server-sent field.** *Its strongest case:* the
label becomes a pure read; the never-derives guard survives at full strength with no caveat; the
engine already owns the number and the projection already filters per recipient. **Why it lost as
the decision:** it is a mechanism, not a meaning — the wire would gain a field whose value the
client already holds term by term, at the price of a `PROTOCOL_VERSION` bump (`ADR-0070`'s probe,
serialised by `ADR-0047` against `EPIC-13`'s clock work) for zero player-visible difference. The
meaning decided here does not depend on it, so §5 leaves the shape to the ticket and the wire to
the architect if ever wanted.
